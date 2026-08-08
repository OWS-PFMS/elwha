#!/usr/bin/env python3
"""Renders the #531 candidate cursor sets and the review strips the doc embeds.

    python3 docs/research/assets/531/render.py

Writes, relative to this directory:

    candidates/<key>/<state>-<theme>-<size>.png    the cursor assets
    candidates/<key>/<state>-<theme>-<size>.svg    the vector source of each
    review/*.png                                   the strips the review doc shows

Deterministic: same inputs, byte-identical outputs. Nothing here ships -- the
assets land under docs/ so the swap PR is a separate, deliberate change.

Requires numpy, scipy and Pillow.
"""

from __future__ import annotations

import pathlib
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

import candidates as cand  # noqa: E402
import cursorforge as cf  # noqa: E402

SIZES = (32, 16)
THEMES = {"light": cf.LIGHT_THEME, "dark": cf.DARK_THEME}
STATES = ("grab", "grabbing")

INCUMBENT = (
    HERE.parents[3] / "src/main/resources/com/owspfm/elwha/list/cursors"
)

# Grounds. Light and dark are the panel backgrounds the two variants are chosen
# for; mid is the worst case a cursor has to survive -- content, imagery, a
# half-tone selection fill -- and is what the halo exists for.
LIGHT_GROUND = ((246, 246, 248), (232, 233, 238))
DARK_GROUND = ((26, 26, 30), (40, 40, 46))
MID_GROUND = ((126, 128, 134), (146, 148, 154))

LABEL = (232, 232, 238, 255)
SHEET_BG = (58, 58, 64, 255)

# The incumbent's two hotspots, from ReorderCursors before the swap. They differ by
# (2, 5) design units, which is what makes its pair jump on mouse-press.
INCUMBENT_HOTSPOTS = {"grab": (15.0, 8.0), "grabbing": (13.0, 13.0)}

_FONT_CANDIDATES = (
    "/System/Library/Fonts/Supplemental/Arial.ttf",
    "/System/Library/Fonts/Helvetica.ttc",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
)


def _font(size: int = 13):
    for path in _FONT_CANDIDATES:
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            continue
    return ImageFont.load_default()


FONT = _font(13)
FONT_SMALL = _font(11)


# ------------------------------------------------------------------- ground fill


def ground(width: int, height: int, colors, cell: int = 8) -> Image.Image:
    first, second = colors
    image = Image.new("RGBA", (width, height), (*first, 255))
    draw = ImageDraw.Draw(image)
    for y in range(0, height, cell):
        for x in range(0, width, cell):
            if (x // cell + y // cell) % 2:
                draw.rectangle([x, y, x + cell - 1, y + cell - 1], fill=(*second, 255))
    return image


def busy_ground(width: int, height: int) -> Image.Image:
    """A left-to-right sweep from white to black, banded, plus a mid-tone check.

    A cursor that stays legible across the whole width has a halo that works.
    """
    columns = np.linspace(255, 12, width)
    banded = (np.round(columns / 28.0) * 28.0).clip(6, 252)
    tile = np.repeat(banded[None, :], height, axis=0)
    rgb = np.dstack([tile, tile, tile]).astype(np.uint8)
    image = Image.fromarray(rgb, "RGB").convert("RGBA")
    draw = ImageDraw.Draw(image)
    for y in range(0, height, 6):
        for x in range(0, width, 6):
            if (x // 6 + y // 6) % 2:
                draw.rectangle([x, y, x + 5, y + 5], fill=(255, 255, 255, 26))
    return image


# ------------------------------------------------------------------ asset output


def write_assets() -> dict:
    """Emits every candidate's PNGs and SVGs; returns the measured extents."""
    extents = {}
    for candidate in cand.CANDIDATES:
        folder = HERE / "candidates" / candidate.key
        folder.mkdir(parents=True, exist_ok=True)
        for state_name in STATES:
            state = candidate.states[state_name]
            for size in SIZES:
                geometry = state.geometry(size)
                spec = cf.SIZE_SPECS[size]
                x0, y0, x1, y1 = cf.bounds(geometry)
                margin = spec.halo_px * 32.0 / size
                extents[(candidate.key, state_name, size)] = (
                    x0 - margin, y0 - margin, x1 + margin, y1 + margin,
                )
                for theme_name, palette in THEMES.items():
                    stem = f"{state_name}-{theme_name}-{size}"
                    cf.render(geometry, size, palette, spec).save(folder / f"{stem}.png")
                    (folder / f"{stem}.svg").write_text(
                        cf.svg(geometry, size, palette, spec)
                    )
    return extents


def check_extents(extents: dict) -> list[str]:
    """Flags any candidate whose halo would clip against the cursor image edge."""
    problems = []
    for (key, state, size), (x0, y0, x1, y1) in sorted(extents.items()):
        if min(x0, y0) < 0.0 or max(x1, y1) > 32.0:
            problems.append(
                f"{key}/{state}@{size}: extents "
                f"({x0:.2f},{y0:.2f})-({x1:.2f},{y1:.2f}) leave the 32-unit grid"
            )
    return problems


# ------------------------------------------------------------------- review strips


def load_incumbent(state: str, theme: str, size: int) -> Image.Image | None:
    path = INCUMBENT / f"{state}-{theme}-{size}.png"
    return Image.open(path).convert("RGBA") if path.exists() else None


def candidate_image(key: str, state: str, theme: str, size: int) -> Image.Image:
    return Image.open(
        HERE / "candidates" / key / f"{state}-{theme}-{size}.png"
    ).convert("RGBA")


def _text(draw, xy, message, anchor="la", small=False):
    draw.text(xy, message, fill=LABEL, anchor=anchor, font=FONT_SMALL if small else FONT)


def strip_actual_size(rows) -> Image.Image:
    """Every set at true 16 and 32 px. Zoomed views flatter; this one does not."""
    pad, gap, label_w, row_h = 16, 20, 168, 52
    per_row = 4  # grab/grabbing x 32/16
    tile = 44
    width = pad * 2 + label_w + per_row * (tile + gap) * 2 + gap
    height = pad * 2 + len(rows) * row_h + 34
    sheet = Image.new("RGBA", (width, height), SHEET_BG)
    draw = ImageDraw.Draw(sheet)
    _text(draw, (pad, pad), "actual size - left: light-theme asset on a light panel; "
                            "right: dark-theme asset on a dark panel")
    for index, (label, getter) in enumerate(rows):
        y = pad + 26 + index * row_h
        _text(draw, (pad, y + tile // 2), label, anchor="lm")
        x = pad + label_w
        for theme, colors in (("light", LIGHT_GROUND), ("dark", DARK_GROUND)):
            for size in SIZES:
                for state in STATES:
                    cell = ground(tile, tile, colors, 6)
                    image = getter(state, theme, size)
                    if image is not None:
                        cell.alpha_composite(
                            image, ((tile - size) // 2, (tile - size) // 2)
                        )
                    sheet.alpha_composite(cell, (x, y))
                    x += tile + gap
            x += gap
    return sheet


def strip_detail(label: str, getter, hotspots=None, zoom32: int = 8) -> Image.Image:
    """One set, magnified, with the hotspot marked. Nearest-neighbour, no smoothing."""
    pad, gap = 16, 14
    cell = 32 * zoom32
    width = pad * 2 + 4 * (cell + gap) - gap
    height = pad * 2 + 2 * (cell + gap) - gap + 26
    sheet = Image.new("RGBA", (width, height), SHEET_BG)
    draw = ImageDraw.Draw(sheet)
    _text(draw, (pad, pad), f"{label} - top: light-theme asset, bottom: dark-theme asset")
    hotspots = hotspots or {state: cand.HOTSPOT for state in STATES}
    for row, (theme, colors) in enumerate((("light", LIGHT_GROUND), ("dark", DARK_GROUND))):
        for column, (size, state) in enumerate(
            [(s, st) for s in SIZES for st in STATES]
        ):
            zoom = zoom32 * 32 // size
            image = getter(state, theme, size)
            tile = ground(cell, cell, colors, zoom32 * 2)
            if image is not None:
                scaled = image.resize((size * zoom, size * zoom), Image.NEAREST)
                tile.alpha_composite(scaled, (0, 0))
                marker = ImageDraw.Draw(tile)
                hx, hy = hotspots[state]
                px = int(hx * size / 32) * zoom + zoom // 2
                py = int(hy * size / 32) * zoom + zoom // 2
                marker.line([px - zoom, py, px + zoom, py], fill=(255, 32, 96, 235), width=2)
                marker.line([px, py - zoom, px, py + zoom], fill=(255, 32, 96, 235), width=2)
            sheet.alpha_composite(
                tile, (pad + column * (cell + gap), pad + 24 + row * (cell + gap))
            )
            if row == 0:
                hx, hy = hotspots[state]
                _text(
                    draw,
                    (pad + column * (cell + gap), pad + 24 + 2 * (cell + gap) - gap + 4),
                    f"{state} {size}px   hotspot ({int(hx * size / 32)},{int(hy * size / 32)})",
                    small=True,
                )
    return sheet


def strip_ground_test(rows) -> Image.Image:
    """Each grab state at 1:1 and 4x across a white-to-black sweep."""
    pad, label_w, row_h = 16, 168, 46
    sweep_w = 560
    width = pad * 2 + label_w + sweep_w + 20 + 32 * 4
    height = pad * 2 + 26 + len(rows) * 2 * row_h
    sheet = Image.new("RGBA", (width, height), SHEET_BG)
    draw = ImageDraw.Draw(sheet)
    _text(draw, (pad, pad), "halo test - the same asset repeated across a white-to-black "
                            "sweep, then magnified at mid-tone")
    y = pad + 26
    for label, getter in rows:
        for theme in ("light", "dark"):
            _text(draw, (pad, y + row_h // 2 - 4), f"{label} · {theme}", anchor="lm")
            sweep = busy_ground(sweep_w, 40)
            image = getter("grab", theme, 32)
            if image is not None:
                for step in range(7):
                    sweep.alpha_composite(image, (12 + step * 76, 4))
            sheet.alpha_composite(sweep, (pad + label_w, y))
            mid = ground(32 * 4, 32 * 4, MID_GROUND, 8)
            if image is not None:
                mid.alpha_composite(image.resize((128, 128), Image.NEAREST), (0, 0))
            sheet.alpha_composite(mid.resize((40, 40), Image.LANCZOS),
                                  (pad + label_w + sweep_w + 20, y))
            y += row_h
    return sheet


def _fade(image: Image.Image, factor: float) -> Image.Image:
    faded = image.copy()
    alpha = faded.getchannel("A").point(lambda value: int(value * factor))
    faded.putalpha(alpha)
    return faded


def strip_transition(rows) -> Image.Image:
    """Grab and grabbing in pointer space, to judge whether the pair holds still.

    The third panel is what the user experiences. A cursor image with hotspot ``h``
    is drawn with its top-left at ``pointer - h``, so two states whose hotspots
    differ put their artwork in different places under a stationary pointer. Both
    states are therefore offset by their own hotspot here and the pointer is drawn
    once; any displacement in the overlay is displacement the user sees on
    mouse-press.
    """
    pad, gap, label_w = 16, 18, 168
    zoom, canvas = 7, 40  # a canvas wider than 32 so an offset state cannot clip
    origin = (canvas - 32) // 2
    cell = canvas * zoom
    width = pad * 2 + label_w + 3 * (cell + gap) - gap
    height = pad * 2 + 26 + len(rows) * (cell + gap)
    sheet = Image.new("RGBA", (width, height), SHEET_BG)
    draw = ImageDraw.Draw(sheet)
    _text(draw, (pad, pad), "pair coherence at 32 px - grab, grabbing, then both in "
                            "pointer space with the pointer drawn once")
    for index, (label, getter, hotspots) in enumerate(rows):
        y = pad + 26 + index * (cell + gap)
        _text(draw, (pad, y + cell // 2), label, anchor="lm")
        states = {name: getter(name, "light", 32) for name in STATES}
        # In pointer space the pointer is fixed; put it where grab's hotspot lands.
        anchor = hotspots["grab"]
        for column in range(3):
            tile = ground(cell, cell, LIGHT_GROUND, zoom * 2)
            layers = (
                [(STATES[column], 1.0)] if column < 2
                else [("grab", 0.40), ("grabbing", 0.90)]
            )
            for name, opacity in layers:
                image = states[name]
                if image is None:
                    continue
                hx, hy = hotspots[name]
                offset_x = origin + int(anchor[0] - hx)
                offset_y = origin + int(anchor[1] - hy)
                scaled = _fade(image, opacity).resize((32 * zoom, 32 * zoom), Image.NEAREST)
                tile.alpha_composite(scaled, (offset_x * zoom, offset_y * zoom))
            marker = ImageDraw.Draw(tile)
            px = (origin + int(anchor[0])) * zoom + zoom // 2
            py = (origin + int(anchor[1])) * zoom + zoom // 2
            marker.line([px - zoom, py, px + zoom, py], fill=(255, 32, 96, 235), width=2)
            marker.line([px, py - zoom, px, py + zoom], fill=(255, 32, 96, 235), width=2)
            sheet.alpha_composite(tile, (pad + label_w + column * (cell + gap), y))
    return sheet


# -------------------------------------------------------------------------- main


def main() -> int:
    extents = write_assets()
    problems = check_extents(extents)

    rows = [("incumbent (Capitaine)", load_incumbent)] + [
        (
            candidate.title.split(" — ")[0].split(" - ")[0],
            lambda state, theme, size, key=candidate.key: candidate_image(
                key, state, theme, size
            ),
        )
        for candidate in cand.CANDIDATES
    ]

    review = HERE / "review"
    review.mkdir(parents=True, exist_ok=True)
    unified = {state: cand.HOTSPOT for state in STATES}
    transition_rows = [
        (label, getter, INCUMBENT_HOTSPOTS if index == 0 else unified)
        for index, (label, getter) in enumerate(rows)
    ]
    strip_actual_size(rows).save(review / "actual-size.png")
    strip_ground_test(rows).save(review / "ground-test.png")
    strip_transition(transition_rows).save(review / "transition.png")
    strip_detail(
        "incumbent (Capitaine)", load_incumbent, INCUMBENT_HOTSPOTS
    ).save(review / "detail-incumbent.png")
    for candidate in cand.CANDIDATES:
        strip_detail(
            candidate.title,
            lambda state, theme, size, key=candidate.key: candidate_image(
                key, state, theme, size
            ),
        ).save(review / f"detail-{candidate.key}.png")

    for candidate in cand.CANDIDATES:
        for state in STATES:
            for size in SIZES:
                x0, y0, x1, y1 = extents[(candidate.key, state, size)]
                print(
                    f"{candidate.key:16s} {state:9s} {size:2d}px  "
                    f"extent {x1 - x0:5.2f} x {y1 - y0:5.2f} units  "
                    f"at ({x0:5.2f},{y0:5.2f})"
                )
    if problems:
        print("\nFAIL — geometry leaves the grid:")
        for problem in problems:
            print("  " + problem)
        return 1
    print("\nall candidates fit the 32-unit grid with their halo")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
