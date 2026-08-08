"""Geometry for the three #531 reorder-cursor candidates.

Each candidate supplies a ``grab`` and a ``grabbing`` state at 16 and 32 device
pixels, authored on the shared 32-unit design grid that :mod:`cursorforge` uses.
Geometry is per size on purpose: 16 px is not a scaled 32 px, because a halo wide
enough to carry contrast erases any feature narrower than about 2 device px.

Grid budget, calibrated by measuring what the incumbent Capitaine set gets away
with and by rendering candidates until features stopped surviving:

  32 px   halo 1.0 px   minimum body feature 2 px   minimum gap 1.0 px
  16 px   halo 0.75 px  minimum body feature 2 px   minimum gap 1.0 px

A gap narrower than twice the halo fills with halo colour rather than showing
background, which is the mechanism that turns the space between fingers or
between grip dots into a crisp contrast separator. It is deliberate, not a
tolerance being exceeded.

Hotspots are unified across both states of every candidate -- see the review doc
for why -- and both coordinates are even so ``ReorderCursors``' integer
``hotspot * size / 32`` scaling stays exact at 16 px.
"""

from __future__ import annotations

import cursorforge as cf
from cursorforge import Candidate, State, capsule, circle, rrect

# --------------------------------------------------------------- source glyphs

# Material Symbols Rounded, weight 400, fill 1, 20 dp optical size, from
# fonts.gstatic.com. Apache 2.0, Google. Verbatim so the derivation is auditable.
BACK_HAND_FILL1 = (
    "M511-48q-72 0-134.5-31.5T269-166L68-409q-9-11-8.5-24.5T70-457l3-3q17-18 42-21.5"
    "t46 9.5l127 74v-382q0-14 10.5-25t25.5-11q14 0 25 11t11 25v300h80v-396q0-14 10.5-25"
    "t25.5-11q14 0 25 11t11 25v396h80v-348q0-14 10.5-25t25.5-11q14 0 25 11t11 25v348h80"
    "v-252q0-14 10.5-25t25.5-11q14 0 25 11t11 25v379q0 128-89 216.5T511-48Z"
)

# Measurements read off the path above, in its own 960-unit em.
KNUCKLE_Y = -480.0  # the "h80" seam where all four fingers meet the palm
PALM_MID_X = 552.0  # midpoint of the 288..816 finger span
GLYPH_HEIGHT = 864.0  # -912 to -48
# Finger lengths above the knuckle line, as a fraction of the longest. These
# describe hand anatomy rather than the glyph's expression, and candidate C reuses
# them; disclosed for completeness.
TIP_RATIOS_4 = (0.778, 1.0, 0.889, 0.667)

# drag_indicator, same axes. Its diameter-to-pitch ratios drive candidate B. This
# is the glyph ElwhaItemList's own drag handle draws, so a cursor built from it and
# the handle it hovers over are the same motif.
DRAG_RATIO_X = 240.0 / 144.0  # 1.667
DRAG_RATIO_Y = 216.0 / 144.0  # 1.500

HOTSPOT = (16.0, 14.0)


# ------------------------------------------------------------ shared primitives


def hand(
    size: int,
    *,
    fw: float,
    gap: float,
    ratios,
    flen: float,
    palm_h: float,
    thumb_base,
    thumb_tip,
    thumb_w: float,
    wrist_base=None,
    wrist_tip=None,
    wrist_w: float = 0.0,
    palm_extra: float = 0.3,
    center_x: float | None = None,
) -> str:
    """A palm-forward open hand, measured in device pixels of ``size``.

    Fingers are round-capped bars rising from a rounded palm, longest in the
    middle. ``thumb_*`` and ``wrist_*`` are ``(dx, dy)`` offsets from the palm's
    left edge at the knuckle line. The thumb angles up-left so the notch it opens
    against the index finger is what makes it read as a thumb rather than as a
    bulge in the palm.

    Two constraints on the thumb, both learned by rendering and looking. Its tip must
    stay at or below the knuckle line, and its base must stay inside the palm's
    straight left flank rather than down in the rounded bottom. Violate either and the
    thumb web closes into an enclosed concavity; because the halo is a dilation of the
    whole silhouette, a concavity narrower than twice the halo fills with halo colour,
    and an enclosed one reads as a bright hole punched through the hand. Left open to
    the outside it reads as a thumb web, which is what a hand actually has.

    Finger aspect ratio is the parameter that matters most. Below roughly 1:3.5 the
    fingers read as toes no matter what else is right.

    ``center_x`` defaults to the hotspot but can be pushed right, which is what the
    16 px drawings do: the thumb and wrist both extend left, so centring the palm on
    the hotspot there would push them off the image before the halo even lands. The
    hotspot then sits between the index and middle finger instead of on the palm's
    midpoint -- optical placement per size, which is normal cursor practice and
    invisible because the toolkit only ever shows one size.
    """
    unit = 32.0 / size

    def scale(value: float) -> float:
        return value * unit

    fw, gap, flen = scale(fw), scale(gap), scale(flen)
    palm_h, thumb_w, palm_extra = scale(palm_h), scale(thumb_w), scale(palm_extra)
    knuckle = HOTSPOT[1]
    center_x = HOTSPOT[0] if center_x is None else center_x
    pitch = fw + gap
    span = len(ratios) * pitch - gap
    left = center_x - span / 2
    radius = fw / 2
    parts = []
    for index, ratio in enumerate(ratios):
        x = left + index * pitch + radius
        parts.append(capsule(x, knuckle - flen * ratio + radius, x, knuckle, radius))
    palm_w = span + 2 * palm_extra
    palm_left = center_x - palm_w / 2
    parts.append(
        rrect(
            palm_left,
            knuckle - radius,
            palm_w,
            palm_h + radius,
            (radius, radius, palm_w / 2, palm_w / 2),
        )
    )
    parts.append(
        capsule(
            palm_left + scale(thumb_base[0]),
            knuckle + scale(thumb_base[1]),
            palm_left + scale(thumb_tip[0]),
            knuckle + scale(thumb_tip[1]),
            thumb_w / 2,
        )
    )
    if wrist_w:
        parts.append(
            capsule(
                palm_left + scale(wrist_base[0]),
                knuckle + scale(wrist_base[1]),
                palm_left + scale(wrist_tip[0]),
                knuckle + scale(wrist_tip[1]),
                scale(wrist_w) / 2,
            )
        )
    return "".join(parts)


def dot_block(size: int, *, diameter: float, rows: int, cols: int,
              ratio_x: float = DRAG_RATIO_X, ratio_y: float = DRAG_RATIO_Y) -> str:
    """A drag_indicator-style dot grid, measured in device pixels of ``size``."""
    unit = 32.0 / size
    diameter *= unit
    pitch_x, pitch_y = diameter * ratio_x, diameter * ratio_y
    center_x, center_y = HOTSPOT
    parts = []
    for row in range(rows):
        for col in range(cols):
            parts.append(
                circle(
                    center_x + (col - (cols - 1) / 2) * pitch_x,
                    center_y + (row - (rows - 1) / 2) * pitch_y,
                    diameter / 2,
                )
            )
    return "".join(parts)


def jaws(size: int, *, reach: float, half_height: float, thickness: float) -> str:
    """Two vertical bars flanking the hotspot -- the hand, abstracted to a grip."""
    unit = 32.0 / size
    reach, half_height, thickness = reach * unit, half_height * unit, thickness * unit
    center_x, center_y = HOTSPOT
    return capsule(
        center_x - reach, center_y - half_height,
        center_x - reach, center_y + half_height, thickness / 2,
    ) + capsule(
        center_x + reach, center_y - half_height,
        center_x + reach, center_y + half_height, thickness / 2,
    )


# ------------------------------------------------ A. palm -- Material Symbols hand

# 32 px takes the glyph outline verbatim. Its own proportions are marginal at this
# size -- 2.2 px fingers separated by 2.4 px gaps -- but they resolve, and nothing
# hand-authored matched it. Placement puts the knuckle line on the hotspot.
_A_GRAB_32 = cf.transform(
    BACK_HAND_FILL1,
    scale=22.0 / GLYPH_HEIGHT,
    dx=HOTSPOT[0] - PALM_MID_X * (22.0 / GLYPH_HEIGHT),
    dy=HOTSPOT[1] - KNUCKLE_Y * (22.0 / GLYPH_HEIGHT),
)
# 0.30 is the shallowest curl where the squashed caps still read as four knuckles
# rather than one bar.
_A_GRABBING_32 = cf.curl(_A_GRAB_32, seam=HOTSPOT[1], factor=0.30)

# 16 px keeps Material's finger-to-gap proportion -- 1.9 px fingers separated by
# 2.1 px, the glyph's own 1:1.1 -- and drops to three fingers to afford it. Four
# fingers fit, but only at 0.85 px gaps, which antialias to grey; see the review doc.
_A_GRAB_16 = hand(
    16,
    fw=1.9, gap=2.1, ratios=(0.80, 1.0, 0.72), flen=6.0, palm_h=4.4,
    thumb_base=(1.0, 1.8), thumb_tip=(-1.8, 0.4), thumb_w=2.6,
    center_x=18.0,
)
_A_GRABBING_16 = cf.curl(_A_GRAB_16, seam=HOTSPOT[1], factor=0.34)

PALM = Candidate(
    key="a-palm-m3",
    title="Palm — Material Symbols hand",
    provenance=(
        "32 px is the Material Symbols Rounded back_hand fill-1 outline verbatim, "
        "scaled and positioned (Apache 2.0, Google). 16 px is an original "
        "re-proportioning of the same silhouette. Both closed states are a "
        "programmatic curl of their own open state, not a second drawing."
    ),
    states={
        "grab": State(
            "grab", {32: _A_GRAB_32, 16: _A_GRAB_16}, HOTSPOT,
            "Knuckle line at the palm's midpoint.",
        ),
        "grabbing": State(
            "grabbing", {32: _A_GRABBING_32, 16: _A_GRABBING_16}, HOTSPOT,
            "The same knuckle line: the curl leaves everything below the seam "
            "untouched, so the palm does not move between states.",
        ),
    },
)


# ------------------------------------------- B. grip -- drag_indicator dots + jaws

_B_GRAB_32 = dot_block(32, diameter=3.0, rows=3, cols=2) + jaws(
    32, reach=8.0, half_height=6.0, thickness=2.0
)
# Closed: jaws draw in, shorten and thicken, and the dot columns squeeze. Dot
# diameter never changes, so the two states are the same object.
_B_GRABBING_32 = dot_block(32, diameter=3.0, rows=3, cols=2, ratio_x=1.40) + jaws(
    32, reach=5.8, half_height=4.0, thickness=2.6
)

# 16 px cannot hold six dots and moving jaws in 14 usable px, so the block degrades
# from 2x3 to a single column of two. That is enough to still read as the drag
# handle, and it leaves the jaws room to visibly travel -- which is what carries the
# state change. Dropping to one dot, or keeping more dots and freezing the jaws, were
# both rendered and were worse.
_B_GRAB_16 = dot_block(16, diameter=2.2, rows=2, cols=1) + jaws(
    16, reach=4.8, half_height=3.6, thickness=2.0
)
_B_GRABBING_16 = dot_block(16, diameter=2.2, rows=2, cols=1) + jaws(
    16, reach=3.2, half_height=2.8, thickness=2.4
)

GRIP = Candidate(
    key="b-grip-dots",
    title="Grip — drag_indicator dots and jaws",
    provenance=(
        "Original work. Dot diameter and the diameter-to-pitch ratios are measured "
        "from Material Symbols drag_indicator (Apache 2.0, Google), which is the "
        "glyph the list's own drag handle draws."
    ),
    states={
        "grab": State(
            "grab", {32: _B_GRAB_32, 16: _B_GRAB_16}, HOTSPOT,
            "Centre of the dot block, about which the motif is symmetric.",
        ),
        "grabbing": State(
            "grabbing", {32: _B_GRABBING_32, 16: _B_GRABBING_16}, HOTSPOT,
            "The same centre. The jaws close toward the hotspot, never past it.",
        ),
    },
)


# --------------------------------------------------- C. palm native -- own drawing

# Chunkier than Material by design: finger-to-gap is 1:0.61 where back_hand is
# 1:1.09, which buys a body feature wide enough to survive the halo at both sizes.
_C_GRAB_32 = hand(
    32,
    fw=2.8, gap=1.7, ratios=TIP_RATIOS_4, flen=11.0, palm_h=9.5,
    thumb_base=(1.6, 3.0), thumb_tip=(-3.8, 0.8), thumb_w=5.2,
)
_C_GRABBING_32 = cf.curl(_C_GRAB_32, seam=HOTSPOT[1], factor=0.32)

# 16 px also runs three fingers, but at 1:0.5 rather than Material's 1:1.1 -- fatter
# fingers, tighter separators. Same count as candidate A, visibly different weight.
_C_GRAB_16 = hand(
    16,
    fw=2.0, gap=1.0, ratios=(0.82, 1.0, 0.74), flen=5.8, palm_h=4.4,
    thumb_base=(0.8, 1.5), thumb_tip=(-1.9, 0.4), thumb_w=2.6, palm_extra=0.4,
    center_x=18.0,
)
_C_GRABBING_16 = cf.curl(_C_GRAB_16, seam=HOTSPOT[1], factor=0.34)

NATIVE = Candidate(
    key="c-palm-native",
    title="Palm native — hand drawn for the pixel grid",
    provenance=(
        "Original work. No glyph outline is reused; the finger-length ratios are "
        "shared with candidate A and describe hand anatomy."
    ),
    states={
        "grab": State(
            "grab", {32: _C_GRAB_32, 16: _C_GRAB_16}, HOTSPOT,
            "Knuckle line at the palm's midpoint, as in candidate A.",
        ),
        "grabbing": State(
            "grabbing", {32: _C_GRABBING_32, 16: _C_GRABBING_16}, HOTSPOT,
            "The same knuckle line.",
        ),
    },
)


CANDIDATES = [PALM, GRIP, NATIVE]
