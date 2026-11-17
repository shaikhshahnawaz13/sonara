# SONARA 0.7.1

- Mini-player and full player are now one component (PlayerDeck). A single progress value positions the sculpted surfaces, artwork ring, title, artist, seek bar and every button, so the mini-player is reshaped into the full player instead of being swapped for it. Collapse runs the same transformation in reverse. Opening takes 5.5 s, closing 4.2 s (PLAYER_OPEN_MS / PLAYER_CLOSE_MS in Deck.kt). With system animations off it jumps straight to the end state.
- Player surfaces follow the reference silhouettes: a round artwork bulb with a cyan progress arc, a body with title/artist/seek, and a control pod that becomes the dock below the artwork in the full player.
- Equalizer: now needs a playing track (clear message otherwise), announces the audio session to system equalizers, falls back to the system sound settings when no equalizer app exists, and no longer fails silently.
