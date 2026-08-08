package com.owspfm.elwha;

import static org.assertj.core.api.Assertions.assertThat;

import com.owspfm.elwha.button.ButtonInteractionMode;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.button.ElwhaButtonSelectionGroup;
import com.owspfm.elwha.chip.ChipInteractionMode;
import com.owspfm.elwha.chip.ElwhaChip;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.IconButtonInteractionMode;
import com.owspfm.elwha.radio.ElwhaRadioGroup;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.swing.JComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tier A coverage of the conventions §10 subscription-shape ruling (#725): a {@code JComponent}
 * that fires a named state publishes its {@code PROPERTY_X} key and nothing else, while a
 * non-visual controller — which has no inherited subscription to point at — exposes a symmetric
 * scoped pair.
 *
 * <p>What went wrong is worth stating, because the missing half was invisible from the API alone.
 * {@code ElwhaButton}, {@code ElwhaIconButton} and {@code ElwhaChip} each wrapped {@code
 * addPropertyChangeListener(PROPERTY_SELECTED, l)} in a one-arg {@code addSelectionChangeListener},
 * and none of them could offer the matching {@code remove} without inventing a second name. The
 * library's own {@code ElwhaButtonSelectionGroup} then subscribed through the wrapper and
 * unsubscribed through the inherited keyed call — two idioms for the two halves of one
 * subscription.
 *
 * @see <a href="https://github.com/OWS-PFMS/elwha/issues/725">#725</a>
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class SelectionObservationTest {

  private static Stream<Arguments> toggles() {
    return Stream.of(
        Arguments.of(
            "ElwhaButton",
            ElwhaButton.PROPERTY_SELECTED,
            (Function<Boolean, JComponent>)
                on ->
                    new ElwhaButton("Pin")
                        .setInteractionMode(ButtonInteractionMode.SELECTABLE)
                        .setSelected(on),
            (BiConsumer<JComponent, Boolean>) (c, on) -> ((ElwhaButton) c).setSelected(on)),
        Arguments.of(
            "ElwhaIconButton",
            ElwhaIconButton.PROPERTY_SELECTED,
            (Function<Boolean, JComponent>)
                on ->
                    new ElwhaIconButton()
                        .setInteractionMode(IconButtonInteractionMode.SELECTABLE)
                        .setSelected(on),
            (BiConsumer<JComponent, Boolean>) (c, on) -> ((ElwhaIconButton) c).setSelected(on)),
        Arguments.of(
            "ElwhaChip",
            ElwhaChip.PROPERTY_SELECTED,
            (Function<Boolean, JComponent>)
                on ->
                    new ElwhaChip("Tag")
                        .setInteractionMode(ChipInteractionMode.SELECTABLE)
                        .setSelected(on),
            (BiConsumer<JComponent, Boolean>) (c, on) -> ((ElwhaChip) c).setSelected(on)));
  }

  @ParameterizedTest(name = "{0} announces selection on the inherited key-scoped subscription")
  @MethodSource("toggles")
  void anInheritedKeyedSubscriptionCarriesSelection(
      final String name,
      final String key,
      final Function<Boolean, JComponent> factory,
      final BiConsumer<JComponent, Boolean> setSelected) {
    final JComponent component = factory.apply(false);
    final List<Object> arrivals = new ArrayList<>();
    component.addPropertyChangeListener(key, e -> arrivals.add(e.getNewValue()));

    setSelected.accept(component, true);

    assertThat(arrivals).as("%s fires once on a real selection change", name).hasSize(1);
    assertThat(arrivals.get(0)).as("%s carries the typed new value", name).isEqualTo(Boolean.TRUE);
  }

  /**
   * The half the retired wrapper could not offer. Without this the ruling would rest on the add
   * path alone, which is exactly how the asymmetry survived unnoticed.
   */
  @ParameterizedTest(name = "{0} lets a listener unsubscribe through the same key")
  @MethodSource("toggles")
  void anInheritedKeyedSubscriptionCanBeUndone(
      final String name,
      final String key,
      final Function<Boolean, JComponent> factory,
      final BiConsumer<JComponent, Boolean> setSelected) {
    final JComponent component = factory.apply(false);
    final int[] changes = {0};
    final PropertyChangeListener listener = e -> changes[0]++;
    component.addPropertyChangeListener(key, listener);

    setSelected.accept(component, true);
    component.removePropertyChangeListener(key, listener);
    setSelected.accept(component, false);

    assertThat(changes[0]).as("%s stops notifying a removed listener", name).isEqualTo(1);
  }

  /**
   * The doctrine half: the wrapper must not come back. A convenience {@code addXxxListener} over an
   * inherited keyed subscription is a second way to say the same thing, and — as #725 found — it
   * arrives without its {@code remove} because there is no one-arg shape to mirror.
   */
  @ParameterizedTest(name = "{0} publishes no convenience selection wrapper")
  @ValueSource(classes = {ElwhaButton.class, ElwhaIconButton.class, ElwhaChip.class})
  void aJComponentPublishesNoSelectionWrapper(final Class<?> type) {
    assertThat(type.getMethods())
        .as(
            "%s extends JComponent, so the inherited keyed pair is the whole selection surface"
                + " (conventions §10)",
            type.getSimpleName())
        .noneMatch(m -> m.getName().equals("addSelectionChangeListener"));
  }

  /**
   * The other side of the same rule. A non-visual controller has no inherited subscription to point
   * consumers at, so its scoped pair is the only surface it can offer — and being the only surface,
   * it has to be symmetric.
   */
  @Test
  void aNonVisualControllerKeepsItsSymmetricScopedPair() {
    for (final Class<?> type : List.of(ElwhaButtonSelectionGroup.class, ElwhaRadioGroup.class)) {
      assertThat(JComponent.class.isAssignableFrom(type))
          .as("%s is a controller, not a component — that is why it owns a pair", type)
          .isFalse();
      assertThat(type.getMethods())
          .as("%s exposes the add half", type.getSimpleName())
          .anyMatch(m -> m.getName().equals("addSelectionChangeListener"));
      assertThat(type.getMethods())
          .as("%s exposes the matching remove half", type.getSimpleName())
          .anyMatch(m -> m.getName().equals("removeSelectionChangeListener"));
    }
  }
}
