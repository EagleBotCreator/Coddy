---
version: alpha
name: Coddy Android
description: A private pocket notebook with visible memory provenance.
colors:
  primary: "#18364A"
  secondary: "#527C8E"
  accent: "#A74D33"
  background: "#EEF3F8"
  surface: "#FAFCFD"
  success: "#24685E"
  danger: "#A12D39"
  muted: "#4F6575"
  border: "#CAD7DF"
typography:
  display:
    fontFamily: "sans-serif-medium, system-ui, sans-serif"
    fontSize: "2rem"
  body:
    fontFamily: "sans-serif, system-ui, sans-serif"
    fontSize: "1rem"
  utility:
    fontFamily: "monospace"
    fontSize: "0.75rem"
rounded:
  DEFAULT: "1rem"
  sm: "0.75rem"
  lg: "1.5rem"
spacing:
  page: "1.5rem"
  section: "1.5rem"
  control: "0.75rem"
components:
  button:
    height: "3rem"
  card:
    rounded: "1.5rem"
  input:
    height: "3.5rem"
---
# Coddy Android design system
## Overview
Product register for Matt's offline Android assistant. The reference is a coastal instrument notebook: quiet mist surfaces, navy type, a copper marker and a vertical source rail. Familiar native Android interactions win over decoration. No robot mascot, gradients, oversized promotional hero or fabricated account statistics.
The repository has no prior graphical identity. These are implementation defaults for the requested Android app. English interface, multilingual assistant replies. No Japan-market scope.
Runtime ownership: android/design/tokens.json is canonical (Model B). scripts/generate_design_tokens.py generates Compose colors and preview CSS; DESIGN.md mirrors its values. Generated outputs are not hand-edited.
## Colors
Ink/primary owns text and main actions. Steel owns subordinate controls. Copper identifies memory provenance and review. Evergreen plus a written label signals local readiness. Danger accompanies error text. Pale blue is the screen canvas; white is a working surface. Ship a deliberate light theme with system night mode handled by Android outside this app; no incomplete dark palette.
## Typography
Native sans avoids downloaded fonts and late font swaps. Display uses medium weight with restrained scale, body is 16sp equivalent, utility metadata is monospaced. Support Android font scaling and accented Portuguese/Spanish. Keep titles short but wrap rather than truncate meaningful content.
## Layout
A 24dp outer gutter and 12dp control rhythm suit one-handed portrait use; 48dp minimum tap targets. Center content within 720dp on larger screens. Bottom Chat/Memory/Connect navigation and a persistent source/mode label. Compose Scaffold owns insets; input uses IME padding and list scroll ownership.
## Elevation & Depth
Tonal layers and quiet borders carry hierarchy. Reserve overlay elevation for native dialogs. No shadows on every card.
## Shapes
24dp working cards, 16dp controls, simple thin source rail. The monogram is drawn as geometry, never a raster dependency.
## Components
Use shared CoddyTheme, SectionCard, StatusChip and primary/secondary action components. Native Material3 fields, dialogs, progress and navigation provide accessibility and focus behavior.
### Foundational states
Every async operation has pending, success and persistent error feedback. Disable duplicate submit without changing button geometry. Read-only memory is labeled. Empty memory offers Import; no results offers Clear search. User text survives errors.
### Forms and navigation
Masked token with an accessible show/hide action. Native Android file picker owns file selection; native dialogs own review. Use text buttons with explicit verbs. Preserve draft fields across tabs and restart.
### Iconography and motion
Material outlined icons with visible labels for navigation. Decorative source rail is excluded from accessibility. Keep transitions immediate except system-native feedback; honor the device animation scale.
### Content
Plain English labels: Choose model, Import snapshot, Save proposal, Review, Export reviewed proposals, Test connection. “Reviewed for export” never means “Saved to Obsidian.”
## Do's and Don'ts
- Do show exactly where computation and memory come from.
- Do preserve access to Stop, Retry and imported content.
- Don't show fake model readiness, connection status or private memories.
- Don't present a static preview as the installed Android app.
