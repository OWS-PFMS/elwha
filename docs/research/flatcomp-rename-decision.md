# FlatComp Rename — Decision Doc

**Status:** DECIDED 2026-05-14 — rename approved, name locked to **Elwha**. Execution is pending and sequenced after #30 merges (§5). The rationale below is retained as the record; it is not to be re-litigated.

**Drafted:** 2026-05-14 · **Decided:** 2026-05-14

**Author:** Charles Bryan (`cfb3@uw.edu`), via design conversation with Claude.

**Parents:**
- [`flatcomp-design-direction.md`](flatcomp-design-direction.md) — the design-system stance that makes the current name wrong.
- [`flatcomp-token-taxonomy.md`](flatcomp-token-taxonomy.md) — LOCKED; the `FlatComp.*` UIManager namespace it pins is in the churn inventory (§4).
- [`flatcomp-theme-install-api.md`](flatcomp-theme-install-api.md) — LOCKED; `FlatCompTheme` is in the churn inventory.

---

## TL;DR — the decision

1. **Rename: yes.** The "Flat" prefix anchors the library's identity to its *substrate* (FlatLaf), which the design direction explicitly says is backwards — and "flat" is also a visual-style claim that the v2 elevation/tonal-lift system will falsify.
2. **Not "Material 3 / M3."** That re-introduces the trademark + spec-compliance implication the design direction and taxonomy deliberately sidestep ("Material-*flavored*, not spec-compliant"). MUI's own rename *away from* "Material UI" is the cautionary precedent.
3. **The name is `Elwha`** (§3) — a PNW river, named for the largest dam-removal river-restoration in US history. It names the library after OWS's clean-water mission, is collision-free, and is `groupId`-consistent with `com.owspfm`.
4. **Decision and execution are separated.** The decision is made now (cheapest it will ever be — pre-1.0, pre-token-code, zero theme-API consumers). Execution is **one atomic sweep after #30 merges**, to avoid merge-conflicting with the in-flight token-foundation work.

---

## 1. Why "Flat" is wrong now

When the components were `FlatCard` / `FlatChip` inside OWS-Local-Search-GUI, "Flat" was a fine, honest signal: "these are built on FlatLaf." The design-system repositioning makes it wrong on two counts:

- **It anchors identity to the substrate.** Design-direction §12 is explicit: "FlatLaf is the substrate, not the design system." The library *is* the token layer + the design language; FlatLaf is plumbing it configures. Naming the library after the plumbing inverts that. The analogy in §12 — "FlatComp : FlatLaf :: a design-system config : Tailwind" — is precisely the point: you don't name your design system "Tailwind-thing."
- **"Flat" is a visual-style claim that ages out.** Flat design = no shadows, no depth. The taxonomy already captures the elevation tokens (`surfaceContainer*`, `surfaceTint`, `shadow`), and design-direction §6 commits to a v2 tonal-lift elevation system. The day that ships, the library's name actively contradicts its appearance.
- **Minor, but real: it collides with FlatLaf's own namespace.** FlatLaf ships `Flat*`-prefixed classes (`FlatLightLaf`, `FlatSVGIcon`, …). A consumer importing both sees our `Flat*` next to FlatLaf's `Flat*` with no way to tell substrate from design system at the import line.

The do-nothing option — keep the names, just stop *describing* the positioning as "flat" — is incoherent. The prefix is on every public class; you can't have a design-system identity while every type you expose is named after the LAF.

## 2. Why not "Material 3 / M3"

The instinct ("we lifted M3's vocabulary, name it that") is understandable, but it walks into the trap the existing locked docs were careful to avoid:

- **The design direction is deliberate about this.** §3: "FlatComp does not claim spec compliance; it does not need to. It claims to use the M3 *token taxonomy* as its semantic API." It is **Material-flavored, not Material**. A name like `material3-swing` or classes like `M3Card` make a brand and spec-compliance claim the library explicitly does not stand behind.
- **Trademark.** "Material Design" / "Material" is Google's. A library named after it invites a problem that a neutral name simply doesn't have.
- **The precedent points the other way.** MUI was "Material-UI" and renamed *to* "MUI" — moving *away* from the Material brand as it drifted from spec compliance. FlatComp is in exactly that posture (Material-flavored, drifting by design). Copying pre-rename MUI's mistake is the wrong lesson to take from the most relevant prior art.

## 3. The chosen name — Elwha

**`Elwha`.** A river on Washington's Olympic Peninsula, and the site of the largest dam removal and river restoration in US history — the Elwha ran free again in 2014 and its salmon runs returned. The name puts the library's identity on **OWS's clean-water mission** rather than on its substrate.

**Why it satisfies the criteria:**

- **Names the mission, not the substrate or the brand.** Not FlatLaf-derived, not Material/M3-derived. It says what Open Water Systems exists to do.
- **`groupId`-consistent.** `com.owspfm:elwha` reads as one coherent thing — the org owns it, the name reflects the org's work.
- **Short, prefix-clean.** Five letters; `ElwhaCard` / `ElwhaChip` / `ElwhaList` / `ElwhaSurface` / `ElwhaTheme` all read well. A prefix has to stay — an unprefixed `List` would clash with `java.util.List` — and `Elwha` is a clean one.
- **Collision-free.** Maven Central: zero artifacts named `elwha`. GitHub: ~40 repos, all small, all environmental / water-science — thematic neighbors, no software-library collision.

The naming search also weighed North Cascades names (Cascade itself — rejected: too crowded in dev tooling, and "Open CASCADE Technology" echoes awkwardly for an "Open" org; then Shuksan, Sahale, Skagit). **`Skagit` was the close runner-up** — the North Cascades' glacier-fed river, with a personal tie to the maintainers' climbing. `Elwha` won on **mission fit**: river *restoration* is clean-water work in one word.

The name is used consistently across every surface — no split-brain:

```
artifact:   com.owspfm:elwha
packages:   com.owspfm.elwha.theme, com.owspfm.elwha.card, com.owspfm.elwha.chip,
            com.owspfm.elwha.list, com.owspfm.elwha.icons
classes:    ElwhaCard, ElwhaChip, ElwhaList, ElwhaSurface, ElwhaTheme, ElwhaCardList, ElwhaChipList
UIManager:  Elwha.color.primary, Elwha.shape.md, Elwha.type.bodyMedium, ...
repo:       OWS-PFMS/elwha
```

`groupId` stays `com.owspfm` — it's the org, not the product.

### 3.1 The package path collapses too

The current package root is `com.owspfm.ui.components.*`, which made sense when the library was a component grab-bag. With the design-system repositioning, that umbrella is wrong for the same reason "Comp" in the artifact name was: **the theme isn't a component** — it's the design language components layer on top of (design-direction §13's "components depend on theme; never the other way"). Calling its parent package `components` understates what now lives there.

The rename sweep therefore also flattens the path: drop the `ui.components` intermediate so theme sits as a *peer* of the components, with the system name as the root. This is MUI's `@mui/material/Card` pattern, and Carbon's `@carbon/react/Button` — the system name is the package root; theme and components are siblings.

Concrete mapping:

```
com.owspfm.ui.components.theme.*    →  com.owspfm.elwha.theme.*
com.owspfm.ui.components.card.*     →  com.owspfm.elwha.card.*
com.owspfm.ui.components.chip.*     →  com.owspfm.elwha.chip.*
com.owspfm.ui.components.flatlist.* →  com.owspfm.elwha.list.*
com.owspfm.ui.components.icons.*    →  com.owspfm.elwha.icons.*
```

Note `flatlist` → `list` — the package name carried the same "Flat" anchor the rename removes; it drops in the same pass. Sub-packages (`card.list`, `card.playground`, `chip.list`, `theme.playground`) collapse correspondingly. Resource paths under `src/main/resources/com/owspfm/ui/components/...` move in lockstep so resource loads still resolve.

**Why fold this into #42 rather than a follow-up:** the rename already rewrites every file's `package` declaration. Adding the path collapse to that same pass is nearly free; deferring would mean rewriting every package declaration twice and leave a transitional state where the lib is called `Elwha` but its packages still say `ui.components.theme` — contradicting the design direction structurally.

## 4. Churn inventory — what a rename touches

The packages are *already* neutral (`com.owspfm.ui.components.card`, `.chip`, …) — no "flat" in them — which meaningfully shrinks the blast radius. What actually moves:

| Surface | Current → Becomes | Notes |
|---|---|---|
| Maven `artifactId` | `flatcomp` → `elwha` | `groupId` `com.owspfm` unchanged |
| GitHub repo | `OWS-PFMS/flatcomp` → `OWS-PFMS/elwha` | GitHub redirects old URLs; low-risk |
| Publish target | `…/OWS-PFMS/flatcomp` → `…/OWS-PFMS/elwha` | path changes; `publish.yml` updated |
| **Package path** | `com.owspfm.ui.components.<x>` → `com.owspfm.elwha.<x>` for `theme` / `card` / `chip` / `list` / `icons` (+ sub-packages); `flatlist` → `list` | §3.1 — flattens `ui.components` intermediate; resource paths move in lockstep |
| Public classes | `FlatCard`→`ElwhaCard`, `FlatChip`→`ElwhaChip`, `FlatList`→`ElwhaList`, `FlatCardList`/`FlatChipList`, `FlatCardPlayground`/`FlatChipPlayground`, … | the bulk of the mechanical work |
| Theme class | `FlatCompTheme` → `ElwhaTheme` | from the LOCKED install-API doc |
| UIManager namespace | `FlatComp.*` → `Elwha.*` (`Elwha.color.primary`, …) | from the LOCKED taxonomy doc |
| `FlatChip.*` UIManager keys | being **removed** by #31 anyway | no action — the rebuild deletes them |
| Docs | CLAUDE.md, all 8 `docs/research/*.md` (this doc included), `docs/development/*` | text sweep |
| Memory | the design-direction / semver / rename memory files | text sweep |

**Two locked docs take a mechanical amendment.** The taxonomy's `FlatComp.*` namespace and the install API's `FlatCompTheme` are *named* in LOCKED docs. A rename forces a find-replace amendment to both — annotate it as "namespace rename only, no semantic change" so it's clearly not a reopening of the locked decisions.

Pre-1.0, **zero consumers of the theme API**, and the one consumer of `FlatCard` (OWS-Local-Search-GUI) is mid-migration and dev-paused — so this is a pure mechanical sweep with no compat surface to preserve. Per the `api-breakage-ok-during-buildout` feedback, breakage cost is not a factor here anyway.

## 5. Timing & coordination with #30

A separate agent is implementing **#30 (token foundation)** right now. #30 creates:

- `com.owspfm.ui.components.theme.*` — **neutral package, no conflict.**
- `FlatCompTheme` — **carries the old name.**
- the `FlatComp.*` UIManager keys — **carry the old name.**

So #30 bakes the name in. But it does **not hard-block** the rename: pre-1.0 with no theme-API consumers means a later sweep over `FlatCompTheme` + `FlatComp.*` is cheap and safe. The real cost of *not deciding* is (a) every class/doc/key added between now and the decision enlarges the sweep, and (b) naming inertia — "it's called that now" gets harder to undo.

**Recommended sequencing:**

1. **Now:** operator decides §6 — rename yes/no, and if yes, the name.
2. **Tell the #30 agent the outcome.** If rename is happening, they can either adopt the new name in #30 directly (if §6 is decided before they finish) or knowingly write the old name expecting the sweep.
3. **Execute after #30 merges**, as one atomic `refactor:` PR — a single sweep over a settled tree, no merge-conflict war with in-flight work.

Do **not** run the rename concurrently with #30 against the same files.

## 6. The decisions — resolved 2026-05-14

| # | Question | Decision |
|---|---|---|
| 1 | Rename at all? | **Yes** (§1). |
| 2 | The name | **`Elwha`** (§3). Not Material/M3 (§2); chosen over the North Cascades candidates on mission fit, with `Skagit` the runner-up. |
| 3 | Execution timing | **Decide now, execute later** — one atomic `refactor:` sweep after #30 merges, never concurrently with it (§5). |

**Next action:** file the rename as a `refactor:` issue on Project #5, blocked-by #30. The #30 agent should be told the outcome now — they can either adopt `Elwha*` / `Elwha.*` directly if #30 is still open, or knowingly write `FlatComp*` expecting the post-merge sweep.
