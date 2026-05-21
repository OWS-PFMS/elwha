#!/usr/bin/env python3
"""Convert Material Theme Builder exports to Elwha-normalized palette JSON.

Implements the conversion documented in Appendix B of
``docs/research/elwha-theme-install-api.md``: a raw Material Theme Builder export
(``{seed, coreColors, extendedColors, schemes{light, dark, *-contrast}, palettes}``)
becomes an Elwha palette (``{name, description, light{49 roles}, dark{49 roles}}``).

The conversion takes the standard-contrast ``schemes.light`` / ``schemes.dark``
verbatim -- a modern MTB export already carries all 49 ``ColorRole`` keys -- and
drops the medium / high-contrast scheme variants and the source-color metadata.

Usage::

    python3 scripts/convert_mtb_palette.py [INPUT_DIR] [OUTPUT_DIR]

INPUT_DIR  defaults to ``docs/research/themes/``.
OUTPUT_DIR defaults to ``src/main/resources/com/owspfm/elwha/theme/palettes/secondary/``.

Each ``material-theme-<color>.json`` in INPUT_DIR becomes ``<color>.json`` in
OUTPUT_DIR (e.g. ``material-theme-deep-orange.json`` -> ``deep-orange.json``).

A color already curated in the primary tier -- a ``<color>.json`` present in the
sibling ``primary/`` directory -- is skipped, so the primary and secondary tiers
stay disjoint.
"""
import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
DEFAULT_IN = REPO / "docs/research/themes"
DEFAULT_OUT = REPO / "src/main/resources/com/owspfm/elwha/theme/palettes/secondary"
PREFIX = "material-theme-"


def title(slug: str) -> str:
    """deep-orange -> Deep Orange."""
    return " ".join(part.capitalize() for part in slug.split("-"))


def convert(raw_path: Path, out_dir: Path) -> None:
    slug = raw_path.stem[len(PREFIX):]
    raw = json.loads(raw_path.read_text(encoding="utf-8"))
    schemes = raw["schemes"]
    palette = {
        "name": f"Material {title(slug)}",
        "description": (
            "Secondary demo palette for The Elwha Showcase. "
            f"M3 Theme Builder export, seed {raw.get('seed', 'n/a')}."
        ),
        "light": schemes["light"],
        "dark": schemes["dark"],
    }
    out_path = out_dir / f"{slug}.json"
    out_path.write_text(json.dumps(palette, indent=2) + "\n", encoding="utf-8")
    print(f"  {raw_path.name} -> {out_path.relative_to(REPO)}")


def main() -> None:
    in_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_IN
    out_dir = Path(sys.argv[2]) if len(sys.argv) > 2 else DEFAULT_OUT
    out_dir.mkdir(parents=True, exist_ok=True)
    # The two tiers stay disjoint: a color already curated in the primary tier is
    # not duplicated into the secondary tier.
    primary_slugs = {p.stem for p in (out_dir.parent / "primary").glob("*.json")}
    exports = sorted(in_dir.glob(f"{PREFIX}*.json"))
    if not exports:
        sys.exit(f"No {PREFIX}*.json files found in {in_dir}")
    print(f"Converting MTB exports -> {out_dir}")
    converted = skipped = 0
    for raw_path in exports:
        slug = raw_path.stem[len(PREFIX):]
        if slug in primary_slugs:
            print(f"  skip {raw_path.name} ({slug} is in the primary tier)")
            skipped += 1
            continue
        convert(raw_path, out_dir)
        converted += 1
    print(f"Done — {converted} converted, {skipped} skipped (primary-tier colors).")


if __name__ == "__main__":
    main()
