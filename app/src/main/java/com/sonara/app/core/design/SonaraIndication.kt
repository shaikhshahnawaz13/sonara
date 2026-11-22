package com.sonara.app.core.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch

/**
 * The press response of every tappable row and surface that does not draw its own: a flat veil over the pressed
 * element, in place of Material's rippling circle. It comes on at once and eases away on release, so a tap is
 * acknowledged in the first frame. With [instant] (motion Off) it appears and disappears without easing.
 *
 * The veil is drawn from a single animated value read at draw time, so a press never recomposes anything.
 */
class SonaraPressIndication(private val veil: Color, private val instant: Boolean) : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode = PressNode(interactionSource, veil, instant)

    override fun equals(other: Any?): Boolean =
        other is SonaraPressIndication && other.veil == veil && other.instant == instant

    override fun hashCode(): Int = 31 * veil.hashCode() + instant.hashCode()

    private class PressNode(
        private val interactionSource: InteractionSource,
        private val veil: Color,
        private val instant: Boolean,
    ) : Modifier.Node(), DrawModifierNode {

        private val amount = Animatable(0f)

        override fun onAttach() {
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> {
                            if (instant) amount.snapTo(1f) else amount.animateTo(1f, tween(PRESS_IN_MS))
                        }
                        is PressInteraction.Release, is PressInteraction.Cancel -> {
                            if (instant) amount.snapTo(0f) else amount.animateTo(0f, tween(RELEASE_MS))
                        }
                    }
                    invalidateDraw()
                }
            }
        }

        override fun ContentDrawScope.draw() {
            drawContent()
            val a = amount.value
            if (a > 0f) drawRect(veil.copy(alpha = veil.alpha * a))
        }
    }

    private companion object {
        const val PRESS_IN_MS = 40
        const val RELEASE_MS = 160
    }
}
