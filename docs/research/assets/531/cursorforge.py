"""Deterministic SVG-to-cursor-PNG rasteriser for the #531 reorder-cursor redesign.

Every candidate is authored once as vector geometry on a 32-unit design grid and
emitted two ways from that single source: an ``.svg`` that a browser renders
identically, and ``.png`` rasters at the sizes AWT asks for.

The rendering model has three layers, painted bottom-up:

  shadow  a Gaussian-blurred, downward-offset copy of the body, low alpha
  halo    the body dilated by ``halo_px`` -- equivalent to a round-join stroke of
          width ``2 * halo_px`` under ``paint-order: stroke``
  body    the geometry itself

Dilating the *union* of the subpaths is what produces the interior separators
between fingers or between grip dots for free: where two subpaths sit closer than
``2 * halo_px``, their halos merge and the gap reads as a contrast line. That is
the same mechanism the incumbent Capitaine assets use, so a candidate authored
this way drops into ``ReorderCursors`` with no loader change.

Antialiasing is box-filtered supersampling -- fill a binary mask at ``SUPERSAMPLE``
times the target size, then average. No ringing, and coverage is exact area.

Nothing here ships. It lives beside the candidate assets so the chosen set can be
re-rendered or nudged deterministically.
"""

from __future__ import annotations

import math
import re
from dataclasses import dataclass, field

import numpy as np
from PIL import Image
from scipy.ndimage import distance_transform_edt, gaussian_filter

DESIGN = 32.0
SUPERSAMPLE = 16

# ---------------------------------------------------------------- path parsing

_TOKEN = re.compile(r"[-+]?(?:\d*\.\d+|\d+\.?)(?:[eE][-+]?\d+)?|[A-Za-z]")


def _tokens(d: str):
    for match in _TOKEN.finditer(d):
        text = match.group()
        yield text if text.isalpha() else float(text)


def flatten_path(d: str, tolerance: float = 0.02):
    """Flattens SVG path data into polygons in user units.

    Returns a list of closed subpaths, each a list of ``(x, y)`` vertices. Curves
    are subdivided until the control polygon is within ``tolerance`` user units of
    the flattened chord, so the caller can raise fidelity by lowering the number.
    """
    tokens = list(_tokens(d))
    index = 0
    subpaths: list[list[tuple[float, float]]] = []
    current: list[tuple[float, float]] = []
    cursor = (0.0, 0.0)
    start = (0.0, 0.0)
    prev_cubic_ctrl = None
    prev_quad_ctrl = None
    command = None

    def take(count: int):
        nonlocal index
        values = tokens[index : index + count]
        index += count
        return values

    def emit(point):
        current.append(point)

    def close_subpath():
        nonlocal current
        if len(current) > 2:
            subpaths.append(current)
        current = []

    while index < len(tokens):
        if isinstance(tokens[index], str):
            command = tokens[index]
            index += 1
        elif command in ("M", "m"):
            command = "L" if command == "M" else "l"
        relative = command.islower()
        upper = command.upper()

        if upper == "M":
            x, y = take(2)
            if relative:
                x, y = cursor[0] + x, cursor[1] + y
            close_subpath()
            cursor = start = (x, y)
            emit(cursor)
            prev_cubic_ctrl = prev_quad_ctrl = None
        elif upper == "Z":
            if current:
                cursor = start
                close_subpath()
            prev_cubic_ctrl = prev_quad_ctrl = None
        elif upper in ("L", "H", "V"):
            if upper == "L":
                x, y = take(2)
                if relative:
                    x, y = cursor[0] + x, cursor[1] + y
            elif upper == "H":
                (x,) = take(1)
                x = cursor[0] + x if relative else x
                y = cursor[1]
            else:
                (y,) = take(1)
                y = cursor[1] + y if relative else y
                x = cursor[0]
            cursor = (x, y)
            emit(cursor)
            prev_cubic_ctrl = prev_quad_ctrl = None
        elif upper in ("C", "S"):
            if upper == "C":
                x1, y1, x2, y2, x, y = take(6)
                if relative:
                    x1, y1 = cursor[0] + x1, cursor[1] + y1
                    x2, y2 = cursor[0] + x2, cursor[1] + y2
                    x, y = cursor[0] + x, cursor[1] + y
            else:
                x2, y2, x, y = take(4)
                if relative:
                    x2, y2 = cursor[0] + x2, cursor[1] + y2
                    x, y = cursor[0] + x, cursor[1] + y
                if prev_cubic_ctrl is None:
                    x1, y1 = cursor
                else:
                    x1 = 2 * cursor[0] - prev_cubic_ctrl[0]
                    y1 = 2 * cursor[1] - prev_cubic_ctrl[1]
            for point in _cubic(cursor, (x1, y1), (x2, y2), (x, y), tolerance):
                emit(point)
            prev_cubic_ctrl = (x2, y2)
            prev_quad_ctrl = None
            cursor = (x, y)
        elif upper in ("Q", "T"):
            if upper == "Q":
                x1, y1, x, y = take(4)
                if relative:
                    x1, y1 = cursor[0] + x1, cursor[1] + y1
                    x, y = cursor[0] + x, cursor[1] + y
            else:
                x, y = take(2)
                if relative:
                    x, y = cursor[0] + x, cursor[1] + y
                if prev_quad_ctrl is None:
                    x1, y1 = cursor
                else:
                    x1 = 2 * cursor[0] - prev_quad_ctrl[0]
                    y1 = 2 * cursor[1] - prev_quad_ctrl[1]
            control1 = (cursor[0] + 2 / 3 * (x1 - cursor[0]), cursor[1] + 2 / 3 * (y1 - cursor[1]))
            control2 = (x + 2 / 3 * (x1 - x), y + 2 / 3 * (y1 - y))
            for point in _cubic(cursor, control1, control2, (x, y), tolerance):
                emit(point)
            prev_quad_ctrl = (x1, y1)
            prev_cubic_ctrl = control2
            cursor = (x, y)
        elif upper == "A":
            rx, ry, rotation, large_arc, sweep, x, y = take(7)
            if relative:
                x, y = cursor[0] + x, cursor[1] + y
            for point in _arc(cursor, rx, ry, rotation, large_arc, sweep, (x, y), tolerance):
                emit(point)
            cursor = (x, y)
            prev_cubic_ctrl = prev_quad_ctrl = None
        else:
            raise ValueError(f"unsupported path command {command!r}")

    close_subpath()
    return subpaths


def _cubic(p0, p1, p2, p3, tolerance):
    span = (
        math.dist(p0, p1) + math.dist(p1, p2) + math.dist(p2, p3)
    )
    steps = max(3, min(160, int(math.ceil(span / max(tolerance, 1e-6) ** 0.5))))
    for step in range(1, steps + 1):
        t = step / steps
        u = 1 - t
        yield (
            u**3 * p0[0] + 3 * u**2 * t * p1[0] + 3 * u * t**2 * p2[0] + t**3 * p3[0],
            u**3 * p0[1] + 3 * u**2 * t * p1[1] + 3 * u * t**2 * p2[1] + t**3 * p3[1],
        )


def _arc(p0, rx, ry, rotation, large_arc, sweep, p1, tolerance):
    if rx == 0 or ry == 0 or p0 == p1:
        yield p1
        return
    phi = math.radians(rotation)
    cos_phi, sin_phi = math.cos(phi), math.sin(phi)
    dx2, dy2 = (p0[0] - p1[0]) / 2, (p0[1] - p1[1]) / 2
    x1p = cos_phi * dx2 + sin_phi * dy2
    y1p = -sin_phi * dx2 + cos_phi * dy2
    rx, ry = abs(rx), abs(ry)
    lam = (x1p / rx) ** 2 + (y1p / ry) ** 2
    if lam > 1:
        scale = math.sqrt(lam)
        rx, ry = rx * scale, ry * scale
    numerator = rx**2 * ry**2 - rx**2 * y1p**2 - ry**2 * x1p**2
    denominator = rx**2 * y1p**2 + ry**2 * x1p**2
    factor = math.sqrt(max(0.0, numerator / denominator)) if denominator else 0.0
    if large_arc == sweep:
        factor = -factor
    cxp, cyp = factor * rx * y1p / ry, -factor * ry * x1p / rx
    cx = cos_phi * cxp - sin_phi * cyp + (p0[0] + p1[0]) / 2
    cy = sin_phi * cxp + cos_phi * cyp + (p0[1] + p1[1]) / 2
    theta1 = math.atan2((y1p - cyp) / ry, (x1p - cxp) / rx)
    theta2 = math.atan2((-y1p - cyp) / ry, (-x1p - cxp) / rx)
    delta = theta2 - theta1
    if not sweep and delta > 0:
        delta -= 2 * math.pi
    elif sweep and delta < 0:
        delta += 2 * math.pi
    steps = max(4, int(abs(delta) / 0.15))
    for step in range(1, steps + 1):
        theta = theta1 + delta * step / steps
        yield (
            cx + rx * math.cos(theta) * cos_phi - ry * math.sin(theta) * sin_phi,
            cy + rx * math.cos(theta) * sin_phi + ry * math.sin(theta) * cos_phi,
        )


# ------------------------------------------------------------- geometry helpers

_KAPPA = 0.5522847498307936


def circle(cx: float, cy: float, r: float) -> str:
    """Circle as a four-arc cubic subpath, wound clockwise."""
    k = _KAPPA * r
    return (
        f"M{cx:.4f},{cy - r:.4f}"
        f"C{cx + k:.4f},{cy - r:.4f} {cx + r:.4f},{cy - k:.4f} {cx + r:.4f},{cy:.4f}"
        f"C{cx + r:.4f},{cy + k:.4f} {cx + k:.4f},{cy + r:.4f} {cx:.4f},{cy + r:.4f}"
        f"C{cx - k:.4f},{cy + r:.4f} {cx - r:.4f},{cy + k:.4f} {cx - r:.4f},{cy:.4f}"
        f"C{cx - r:.4f},{cy - k:.4f} {cx - k:.4f},{cy - r:.4f} {cx:.4f},{cy - r:.4f}Z"
    )


def capsule(x0: float, y0: float, x1: float, y1: float, r: float) -> str:
    """A round-capped bar of radius ``r`` from one point to another."""
    dx, dy = x1 - x0, y1 - y0
    length = math.hypot(dx, dy)
    if length < 1e-9:
        return circle(x0, y0, r)
    ux, uy = dx / length, dy / length
    nx, ny = -uy, ux
    k = _KAPPA * r
    ax, ay = x0 + nx * r, y0 + ny * r
    bx, by = x1 + nx * r, y1 + ny * r
    cx, cy = x1 - nx * r, y1 - ny * r
    dx2, dy2 = x0 - nx * r, y0 - ny * r
    return (
        f"M{ax:.4f},{ay:.4f}L{bx:.4f},{by:.4f}"
        f"C{bx + ux * k:.4f},{by + uy * k:.4f} {cx + ux * k:.4f},{cy + uy * k:.4f} {cx:.4f},{cy:.4f}"
        f"L{dx2:.4f},{dy2:.4f}"
        f"C{dx2 - ux * k:.4f},{dy2 - uy * k:.4f} {ax - ux * k:.4f},{ay - uy * k:.4f} "
        f"{ax:.4f},{ay:.4f}Z"
    )


def rrect(x: float, y: float, w: float, h: float, r) -> str:
    """Rounded rectangle. ``r`` is a true corner radius, or a 4-tuple TL/TR/BR/BL."""
    if not isinstance(r, (tuple, list)):
        r = (r, r, r, r)
    tl, tr, br, bl = (min(v, w / 2, h / 2) for v in r)
    k = _KAPPA
    return (
        f"M{x + tl:.4f},{y:.4f}"
        f"L{x + w - tr:.4f},{y:.4f}"
        f"C{x + w - tr + tr * k:.4f},{y:.4f} {x + w:.4f},{y + tr - tr * k:.4f} "
        f"{x + w:.4f},{y + tr:.4f}"
        f"L{x + w:.4f},{y + h - br:.4f}"
        f"C{x + w:.4f},{y + h - br + br * k:.4f} {x + w - br + br * k:.4f},{y + h:.4f} "
        f"{x + w - br:.4f},{y + h:.4f}"
        f"L{x + bl:.4f},{y + h:.4f}"
        f"C{x + bl - bl * k:.4f},{y + h:.4f} {x:.4f},{y + h - bl + bl * k:.4f} "
        f"{x:.4f},{y + h - bl:.4f}"
        f"L{x:.4f},{y + tl:.4f}"
        f"C{x:.4f},{y + tl - tl * k:.4f} {x + tl - tl * k:.4f},{y:.4f} {x + tl:.4f},{y:.4f}Z"
    )


def transform(d: str, scale: float = 1.0, dx: float = 0.0, dy: float = 0.0,
              rotate: float = 0.0, pivot=(0.0, 0.0), tolerance: float = 0.004) -> str:
    """Bakes an affine transform into path data by flattening then re-emitting.

    Flattening first keeps the emitted geometry honest: what the SVG contains is
    exactly what the rasteriser fills, with no transform attributes for a viewer
    to interpret differently.
    """
    theta = math.radians(rotate)
    cos_t, sin_t = math.cos(theta), math.sin(theta)
    parts = []
    for polygon in flatten_path(d, tolerance):
        points = []
        for (x, y) in polygon:
            x, y = x * scale, y * scale
            x, y = x - pivot[0], y - pivot[1]
            x, y = x * cos_t - y * sin_t, x * sin_t + y * cos_t
            points.append((x + pivot[0] + dx, y + pivot[1] + dy))
        parts.append(
            "M" + "L".join(f"{px:.4f},{py:.4f}" for px, py in points) + "Z"
        )
    return "".join(parts)


def curl(d: str, seam: float, factor: float, tolerance: float = 0.004) -> str:
    """Foreshortens everything above ``seam`` toward it, leaving the rest untouched.

    This is how a closed hand is derived from an open one without drawing a second
    hand: every vertex above the knuckle line collapses to ``factor`` of its height
    while the palm and wrist keep their exact geometry. Finger widths, gaps and cap
    radii survive unchanged, the seam stays continuous so no gap opens, and the
    round fingertips squash into ellipses that read as curled knuckles.

    The pair is therefore provably the same hand in two poses, which is what makes
    the grab-to-grabbing transition legible.
    """
    parts = []
    for polygon in flatten_path(d, tolerance):
        points = [
            (x, seam - (seam - y) * factor if y < seam else y) for (x, y) in polygon
        ]
        parts.append("M" + "L".join(f"{px:.4f},{py:.4f}" for px, py in points) + "Z")
    return "".join(parts)


def bounds(d: str):
    """Tight bounding box ``(x0, y0, x1, y1)`` of flattened path data."""
    xs, ys = [], []
    for polygon in flatten_path(d):
        for (x, y) in polygon:
            xs.append(x)
            ys.append(y)
    return min(xs), min(ys), max(xs), max(ys)


def fit(d: str, box, flip_y: bool = False) -> str:
    """Uniformly scales path data to fit ``box`` = ``(x, y, w, h)``, centred."""
    x0, y0, x1, y1 = bounds(d)
    src_w, src_h = x1 - x0, y1 - y0
    bx, by, bw, bh = box
    scale = min(bw / src_w, bh / src_h)
    off_x = bx + (bw - src_w * scale) / 2 - x0 * scale
    off_y = by + (bh - src_h * scale) / 2 - y0 * scale
    parts = []
    for polygon in flatten_path(d, 0.004):
        points = [(x * scale + off_x, y * scale + off_y) for (x, y) in polygon]
        if flip_y:
            points = [(px, by + bh - (py - by)) for px, py in points]
        parts.append("M" + "L".join(f"{px:.4f},{py:.4f}" for px, py in points) + "Z")
    return "".join(parts)


# ------------------------------------------------------------------ rasterising


def fill_mask(d: str, size: int, supersample: int = SUPERSAMPLE) -> np.ndarray:
    """Rasterises path data with the nonzero winding rule into a binary mask.

    The mask is ``size * supersample`` square; the design grid is assumed to be
    :data:`DESIGN` units wide, so geometry scales with the requested size.
    """
    resolution = size * supersample
    scale = resolution / DESIGN
    edges = []
    for polygon in flatten_path(d, 0.004):
        points = [(x * scale, y * scale) for (x, y) in polygon]
        for i in range(len(points)):
            x0, y0 = points[i]
            x1, y1 = points[(i + 1) % len(points)]
            if y0 != y1:
                edges.append((x0, y0, x1, y1))
    mask = np.zeros((resolution, resolution), dtype=bool)
    if not edges:
        return mask
    edge_array = np.array(edges)
    ex0, ey0, ex1, ey1 = edge_array.T
    y_min = np.minimum(ey0, ey1)
    y_max = np.maximum(ey0, ey1)
    winding = np.where(ey1 > ey0, 1, -1)
    slope = (ex1 - ex0) / (ey1 - ey0)

    for row in range(resolution):
        y = row + 0.5
        hit = (y_min <= y) & (y < y_max)
        if not hit.any():
            continue
        xs = ex0[hit] + (y - ey0[hit]) * slope[hit]
        ws = winding[hit]
        order = np.argsort(xs, kind="stable")
        xs, ws = xs[order], ws[order]
        total = np.cumsum(ws)
        inside = total[:-1] != 0
        if not inside.any():
            continue
        spans_start = xs[:-1][inside]
        spans_end = xs[1:][inside]
        for sx, ex in zip(spans_start, spans_end):
            left = int(math.ceil(sx - 0.5))
            right = int(math.ceil(ex - 0.5))
            if right > left:
                mask[row, max(0, left) : min(resolution, right)] = True
    return mask


def _dilate(mask: np.ndarray, radius_px: float) -> np.ndarray:
    if radius_px <= 0:
        return mask
    return distance_transform_edt(~mask) <= radius_px


def _downsample(rgba: np.ndarray, factor: int) -> np.ndarray:
    height, width = rgba.shape[0] // factor, rgba.shape[1] // factor
    view = rgba.reshape(height, factor, width, factor, 4)
    return view.mean(axis=(1, 3))


@dataclass
class Palette:
    """Body and halo colours for one theme variant, plus the drop-shadow strength."""

    body: str
    halo: str
    shadow_alpha: float = 0.30
    shadow_blur_px: float = 1.1
    shadow_offset_px: float = 0.6


LIGHT_THEME = Palette(body="#1B1B1F", halo="#FFFFFF", shadow_alpha=0.22)
DARK_THEME = Palette(body="#FFFFFF", halo="#17171A", shadow_alpha=0.34)


def _rgb(value: str):
    value = value.lstrip("#")
    return tuple(int(value[i : i + 2], 16) for i in (0, 2, 4))


@dataclass
class RenderSpec:
    """Per-size render parameters, all measured in device pixels of the output."""

    halo_px: float
    grow_px: float = 0.0
    shadow: bool = True


SIZE_SPECS = {
    32: RenderSpec(halo_px=1.0, grow_px=0.0),
    16: RenderSpec(halo_px=0.75, grow_px=0.0),
}


def render(d: str, size: int, palette: Palette, spec: RenderSpec | None = None) -> Image.Image:
    """Renders path data to a cursor PNG at ``size`` square."""
    spec = spec or SIZE_SPECS[size]
    supersample = SUPERSAMPLE
    body = fill_mask(d, size, supersample)
    if spec.grow_px > 0:
        body = _dilate(body, spec.grow_px * supersample)
    halo = _dilate(body, spec.halo_px * supersample)

    resolution = size * supersample
    canvas = np.zeros((resolution, resolution, 4), dtype=np.float32)

    if spec.shadow and palette.shadow_alpha > 0:
        shift = int(round(palette.shadow_offset_px * supersample))
        source = halo.astype(np.float32)
        shadow = np.zeros_like(source)
        if shift > 0:
            shadow[shift:, :] = source[:-shift, :]
        else:
            shadow = source
        shadow = gaussian_filter(shadow, palette.shadow_blur_px * supersample)
        shadow = np.clip(shadow, 0, 1) * palette.shadow_alpha
        canvas[..., 3] = shadow  # premultiplied black: rgb stays 0

    halo_rgb = np.array(_rgb(palette.halo), dtype=np.float32) / 255.0
    body_rgb = np.array(_rgb(palette.body), dtype=np.float32) / 255.0
    canvas[halo] = np.array([*halo_rgb, 1.0], dtype=np.float32)
    canvas[body] = np.array([*body_rgb, 1.0], dtype=np.float32)

    canvas[..., :3] *= canvas[..., 3:4]  # premultiply before averaging
    small = _downsample(canvas, supersample)
    alpha = small[..., 3:4]
    rgb = np.divide(small[..., :3], alpha, out=np.zeros_like(small[..., :3]), where=alpha > 1e-6)
    out = np.concatenate([rgb, alpha], axis=2)
    return Image.fromarray(np.clip(out * 255.0 + 0.5, 0, 255).astype(np.uint8), "RGBA")


# ------------------------------------------------------------------ svg output


def svg(d: str, size: int, palette: Palette, spec: RenderSpec | None = None) -> str:
    """Emits the same geometry as an SVG whose browser rendering matches :func:`render`.

    ``paint-order: stroke`` draws the round-join stroke under the fill, and a stroke
    of width ``2 * halo_px`` puts exactly ``halo_px`` outside the outline -- the
    vector equivalent of the dilation the rasteriser performs.
    """
    spec = spec or SIZE_SPECS[size]
    stroke_units = 2 * spec.halo_px * DESIGN / size
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" '
        f'viewBox="0 0 32 32">\n'
        f'  <path d="{d}" fill="{palette.body}" stroke="{palette.halo}" '
        f'stroke-width="{stroke_units:.3f}" stroke-linejoin="round" stroke-linecap="round" '
        f'paint-order="stroke" fill-rule="nonzero"/>\n'
        f"</svg>\n"
    )


# ------------------------------------------------------------------- inspection


def ascii_preview(image: Image.Image) -> str:
    """Coarse text dump of a rendered cursor, for eyeballing pixel-grid crispness."""
    pixels = np.array(image)
    rows = []
    for y in range(image.height):
        row = []
        for x in range(image.width):
            r, g, b, a = pixels[y, x]
            if a < 24:
                row.append(".")
            elif a < 140:
                row.append(":")
            elif r > 190:
                row.append("W")
            elif r < 70:
                row.append("#")
            else:
                row.append("+")
        rows.append(f"{y:2d} " + "".join(row))
    return "\n".join(rows)


@dataclass
class State:
    """One cursor state: its geometry, its hotspot, and why the hotspot is there."""

    name: str
    d: str | dict[int, str]
    hotspot: tuple[float, float]
    note: str = ""

    def geometry(self, size: int) -> str:
        """Path data for one output size, which may be authored per size."""
        return self.d if isinstance(self.d, str) else self.d[size]


@dataclass
class Candidate:
    """A complete candidate set -- both states, plus the design rationale."""

    key: str
    title: str
    provenance: str
    states: dict[str, State] = field(default_factory=dict)
