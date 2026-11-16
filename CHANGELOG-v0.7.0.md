# SONARA 0.7.0

- Light theme is the default on a fresh install; the choice made in Settings is saved and restored.
- Home, Search and Library share one header: the transparent SONARA mark on the left, a Settings gear on the right.
- Swipe left/right between Home, Search and Library (no wrap-around). Carousels, seek bars and the search field keep their own gestures.
- Settings is a separate screen: mini-player and bottom navigation are hidden, and Back returns to the tab it was opened from.
- Home, Search and Library rebuilt around the reference layouts using only real library data.
  Recently played and recent searches are stored on the device (ids and search terms only).
- New mini-player (ring artwork, title, slim seek bar, previous/play/next) and full player with a dock of controls.
- Tapping the mini-player morphs it into the full player: artwork, title, controls and the container itself move and resize together. Collapse reverses it.
- Shadows no longer clip into rectangles; tab changes slide in the direction of the destination.
