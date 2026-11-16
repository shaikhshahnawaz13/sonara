# SONARA v0.4.1

- Replaced the previous basic three-tab layout with the six-screen structure from the supplied reference.
- Added a launch splash, home discovery layout, full-screen player, collection-style library, mood-based search layout, and settings overview.
- Added shared dark glass surfaces, cyan selection accents, metallic-blue borders, and horizontal discovery cards.
- Retained local MediaStore audio scanning and basic MediaPlayer playback.

Not yet included: real photographic cover artwork, online music providers, persistent playlists/liked songs, a functional equalizer, and a continuously updating seek/progress display.

## Liquid glass refinement
- Replaced flat gradient glass panels with a custom layered Android Drawable.
- Added translucent blue-black glass tint, soft upper reflections, restrained diagonal sheen, cool outer edge and subtle inner highlight.
- Applied the refined glass surface through the shared glass() helper, covering mini-player, navigation, search field, collection rows, settings rows and other existing glass surfaces.
- Kept the six-screen layout, navigation, cyan accent palette and v0.4.1 version identity unchanged.
