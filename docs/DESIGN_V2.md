# SONARA V2 design foundation (M1.1)

Swiss minimalism with an editorial music voice: ivory paper, ink type, one calm interactive colour and a rare coral
signal. Hierarchy comes from type, space, alignment and hairlines, not from boxes, shadows or gradients.

All tokens live in `app/src/main/java/com/sonara/app/core/design/` and are read through `SonaraTheme`. Nothing in a
screen should hardcode a colour, size or font.

## Colour (`SonaraPalette`, `SonaraColors`)

| Role | Token | Value | Notes |
|---|---|---|---|
| Background | `background` | `#F4EFE4` warm ivory | |
| Panels, placeholders | `surface` | `#EEE8DB` | |
| Pressed and selected row | `surfaceRaised` / `selected` | `#E8E1D2` | |
| Primary text | `foreground` | `#171B18` deep ink | 15.2:1 on ivory |
| Secondary text | `foregroundMuted` | `#65635B` | 5.25:1 on ivory. The reference `#716F68` is 4.39:1 and under the 4.5:1 target, so it was deepened |
| Primary action, text actions, active destination | `accent` / `onAccent` | `#123D32` forest / ivory | 10.5:1. The only colour the person can change |
| Secondary action | outline button | ink 1dp outline, no fill | |
| Selected state | `selected` background, accent rule | | |
| Divider | `divider` (`borderMuted`) | `#D9D1C4` | decorative hairline |
| Identifying outline | `outline` | `#85806F` | 3:1 on every surface |
| Playback progress | `progress` / `progressTrack` | `#C8452B` / divider | thin graphics only |
| Signal fill | `highlight` / `onHighlight` | `#FF8065` coral / ink | a fill with ink on it, never text (2.15:1 on ivory) |
| Error / warning | `danger` / `warning` | `#B3261E` / `#8A5700` | 5.7:1 / 5.3:1 |
| Disabled | `disabledContent` / `disabledContainer` | `#A8A396` / `#E6E0D3` | deliberately quiet |

Colour hierarchy: ink carries almost everything; forest is for things you can press; coral appears a few times per
screen at most (what is playing, how far along, a chosen chip).

Accent choices (Settings > Appearance): Forest (default), Cobalt, Brick, Plum, Umber. Each is checked for 4.5:1 as
text on all three surfaces and with ivory text on it (`AccentChoiceTest`). Ids saved by V1 fall back to Forest.

## Typography (`SonaraTypography`, `SonaraFonts`)

Headings, kickers, buttons and labels: **Barlow Condensed** (SIL OFL 1.1), bundled in `res/font` as Medium, SemiBold
and Bold. Body: the Android system sans (Roboto), nothing bundled.

| Style | Font | Size / line | Weight | Tracking |
|---|---|---|---|---|
| `display` | condensed | 64 / 60 | Bold | -0.5 |
| `hero` | condensed | 48 / 48 | Bold | -0.25 |
| `screenTitle` | condensed | 34 / 36 | Bold | 0 |
| `section` | condensed | 24 / 28 | SemiBold | 0.2 |
| `eyebrow` | condensed | 13 / 16 | SemiBold | 1.6 (upper-case) |
| `trackTitle` | system sans | 16 / 22 | Medium | 0 |
| `body` | system sans | 14 / 20 | Normal | 0.1 |
| `metadata` | system sans | 12 / 16 | Medium | 0.2 |
| `button` | condensed | 16 / 20 | SemiBold | 1.0 |
| `microLabel` | condensed | 12 / 14 | SemiBold | 1.0 |

## Layout (`SonaraLayout`, `SonaraSpacing`, `SonaraArtworkSizes`, `SonaraBorders`, `SonaraSizes`)

- Screen side margin 20dp (`Modifier.screenMargin()`); gutter 16dp.
- Vertical rhythm: item 12dp, block 24dp, section 40dp.
- Row minimum height 56dp. Touch target 48dp (`Modifier.minTouchTarget()`).
- Artwork: row 48dp, tile 144dp, feature 240dp. Always square, no border or shadow.
- Icons: 16 / 20 / 24 / 28dp.
- Lines: hairline 1dp (dividers, quiet outlines), rule 2dp (one deliberate heavy line).
- Radii: 0, 2, 4, 6dp. Nothing is a rounded card.

## Components

Reused and restyled (existing names kept so V1 screens keep working): `SonaraButton` (primary solid forest,
secondary outline), `SonaraIconButton`, `SonaraSectionHeader` (now with optional `eyebrow` and `ruled`),
`SonaraTrackRow`, `SonaraArtwork`, empty/loading/error/permission states (no box; a short rule, a condensed title),
`SonaraWordmark`.

New: `SonaraDivider`, `SonaraProgressBar` (progress read at draw time, no recomposition), `SonaraCategorySelector`
(text with a 2dp forest rule under the chosen one), `Modifier.screenMargin()`, `Modifier.minTouchTarget()`.

## Not in M1.1

Navigation shell, Home, Library, Player, Playlists and Settings are not redesigned yet. Until M1.2 to M1.7 reach them,
those screens show the V2 palette and type on their V1 layout and will look transitional.

## App shell (M2)

- **Bottom navigation:** 56dp, text-only cells, 1dp `divider` hairline on top. The selected label is ink, the others muted. One 2dp x 24dp accent tick travels between cells, driven by the page strip's fractional position (read at layout time, no recomposition). It pads itself for the navigation-bar inset.
- **Top-level header:** `SonaraScreenHeader`, fixed 80dp, 20dp start margin, optional eyebrow above the title, one hairline below. Search is the first action and the screen's own action (Settings, Rescan, Add) is last. The last icon's visible edge lands on the 20dp margin (`actionEndInset` = margin minus icon inset).
- **Pushed screens:** `SonaraTopBar`, same margins and the same hairline (`SonaraDivider`).
- **Mini-player:** geometry unchanged. It stacks directly above the nav; `BottomChromeGeometry` now counts a 1dp nav rule.
