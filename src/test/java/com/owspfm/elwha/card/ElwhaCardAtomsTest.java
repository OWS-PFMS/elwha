package com.owspfm.elwha.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.testkit.EdtInterceptor;
import com.owspfm.elwha.testkit.ThemeExtension;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.accessibility.AccessibleRole;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tier A coverage of the Layer 2 atoms — {@link ElwhaCardTitle}, {@link ElwhaCardSubtitle}, {@link
 * ElwhaCardSupportingText}, {@link ElwhaCardLeadingIcon}, {@link ElwhaCardThumbnail} — plus {@link
 * ElwhaCardDivider} and {@link ElwhaCardMedia}, which are inert enough to belong with them. Spec
 * §4.1–§4.5, §5.2, §5.4.
 *
 * <p>The text atoms are asserted through their resolved roles and their document state (is the
 * content in an HTML view? does the atom offer to stretch?), never through rendered glyph pixels.
 */
@ExtendWith({EdtInterceptor.class, ThemeExtension.class})
class ElwhaCardAtomsTest {

  private static final int CANVAS = 32;

  /** The distinct fully-opaque colors an icon rasterizes to; AA edge pixels are excluded. */
  private static Set<Color> opaqueColors(final FlatSVGIcon icon) {
    final BufferedImage image = new BufferedImage(CANVAS, CANVAS, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = image.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      icon.paintIcon(new JPanel(), g, 4, 4);
    } finally {
      g.dispose();
    }
    final Set<Color> colors = new LinkedHashSet<>();
    for (int y = 0; y < CANVAS; y++) {
      for (int x = 0; x < CANVAS; x++) {
        final Color pixel = new Color(image.getRGB(x, y), true);
        if (pixel.getAlpha() == 255) {
          colors.add(new Color(pixel.getRGB(), false));
        }
      }
    }
    return colors;
  }

  /** True when Swing installed an HTML view for the label — the wrap machinery is live. */
  private static boolean rendersAsHtml(final JLabel label) {
    return label.getClientProperty("html") != null;
  }

  // ------------------------------------------------------------------ title

  @Test
  void titleCarriesTheM3TitleRoles() {
    final ElwhaCardTitle title = new ElwhaCardTitle("Recent activity");

    assertThat(title.getTypeRole())
        .as("the title slot is TITLE_MEDIUM per §4.1")
        .isEqualTo(TypeRole.TITLE_MEDIUM);
    assertThat(title.getColorRole())
        .as("and the primary-emphasis on-surface color")
        .isEqualTo(ColorRole.ON_SURFACE);
    assertThat(title.getFont())
        .as("the font resolves from the role rather than from a UIManager default")
        .isEqualTo(TypeRole.TITLE_MEDIUM.resolve());
    assertThat(title.getForeground())
        .as("the color resolves from the role too")
        .isEqualTo(ColorRole.ON_SURFACE.resolve());
  }

  @Test
  void subtitleCarriesTheM3SecondaryRoles() {
    final ElwhaCardSubtitle subtitle = new ElwhaCardSubtitle("Last 30 days");

    assertThat(subtitle.getTypeRole())
        .as("the subtitle slot is LABEL_MEDIUM per §4.2")
        .isEqualTo(TypeRole.LABEL_MEDIUM);
    assertThat(subtitle.getColorRole())
        .as("and the M3 de-emphasis color for secondary text")
        .isEqualTo(ColorRole.ON_SURFACE_VARIANT);
  }

  @Test
  void supportingTextCarriesTheM3BodyRoles() {
    final ElwhaCardSupportingText body = new ElwhaCardSupportingText("12 cycles found");

    assertThat(body.getTypeRole())
        .as("the body slot is BODY_MEDIUM per §4.3")
        .isEqualTo(TypeRole.BODY_MEDIUM);
    assertThat(body.getColorRole())
        .as("and the de-emphasis color")
        .isEqualTo(ColorRole.ON_SURFACE_VARIANT);
  }

  @Test
  void everyTextAtomResolvesItsRolesAgainstTheLiveTheme() {
    final ElwhaCardTitle title = new ElwhaCardTitle("x");
    final Color inLight = title.getForeground();

    ThemeExtension.install(Mode.DARK);

    assertThat(title.getForeground())
        .as("roles are read at paint time, so a mode flip re-skins with no consumer wiring")
        .isEqualTo(ColorRole.ON_SURFACE.resolve())
        .isNotEqualTo(inLight);
  }

  @Test
  void textRolesAreOverridableAndRejectNull() {
    final ElwhaCardTitle title = new ElwhaCardTitle("x");

    assertThat(title.setTypeRole(TypeRole.HEADLINE_SMALL).getTypeRole())
        .as("a caller may pick another type step for the title")
        .isEqualTo(TypeRole.HEADLINE_SMALL);
    assertThat(title.setColorRole(ColorRole.PRIMARY).getForeground())
        .as("and another color role, which the paint-time getter honors")
        .isEqualTo(ColorRole.PRIMARY.resolve());
    assertThatThrownBy(() -> title.setTypeRole(null))
        .as("a null type role is rejected")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> title.setColorRole(null))
        .as("a null color role is rejected")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void plainTextIsPromotedToAnHtmlViewSoItCanWrap() {
    final ElwhaCardSupportingText body = new ElwhaCardSupportingText("a plain sentence");

    assertThat(body.getText())
        .as("§4.3 — plain text is auto-wrapped so the label uses the wrapping HTML renderer")
        .startsWith("<html>")
        .endsWith("</html>");
    assertThat(rendersAsHtml(body)).as("and Swing actually installed that view").isTrue();
  }

  @Test
  void contentThatIsAlreadyHtmlIsLeftAlone() {
    final ElwhaCardSupportingText body = new ElwhaCardSupportingText("<html><b>bold</b></html>");

    assertThat(body.getText())
        .as("caller-authored markup is not double-wrapped")
        .doesNotStartWith("<html><html>");
  }

  @Test
  void plainTextIsEscapedSoItIsNotReadAsMarkup() {
    final ElwhaCardTitle title = new ElwhaCardTitle("Price < 5 & rising");

    assertThat(title.getText())
        .as("auto-wrapping is how the atom wraps lines, not an invitation to parse consumer prose")
        .isEqualTo("<html>Price &lt; 5 &amp; rising</html>");
  }

  @Test
  void anEntityLikeSequenceSurvivesAsLiteralText() {
    final ElwhaCardSupportingText body = new ElwhaCardSupportingText("Tom &amp; Jerry");

    assertThat(body.getText())
        .as("the ampersand is escaped first, so a literal entity is not silently substituted")
        .isEqualTo("<html>Tom &amp;amp; Jerry</html>");
  }

  @Test
  void callerAuthoredMarkupIsStillHonored() {
    final ElwhaCardSubtitle subtitle = new ElwhaCardSubtitle("<html><b>bold</b> & plain</html>");

    assertThat(subtitle.getText())
        .as("an explicit <html> prefix is the opt-in for markup and is passed through untouched")
        .isEqualTo("<html><b>bold</b> & plain</html>");
  }

  @Test
  void emptyAndNullTextAreLeftUnwrapped() {
    assertThat(new ElwhaCardTitle().getText())
        .as("the no-arg constructor leaves an empty title, not an empty HTML document")
        .isEmpty();
    final ElwhaCardTitle title = new ElwhaCardTitle("x");
    title.setText(null);
    assertThat(title.getText()).as("clearing the text does not wrap null").isNull();
  }

  @Test
  void everyTextAtomOffersToStretchHorizontally() {
    assertThat(new ElwhaCardTitle("x").getMaximumSize().width)
        .as("§3.4 rule 2 — the title must let a width-respecting parent narrow it so text wraps")
        .isEqualTo(Integer.MAX_VALUE);
    assertThat(new ElwhaCardSubtitle("x").getMaximumSize().width)
        .as("the subtitle too")
        .isEqualTo(Integer.MAX_VALUE);
    assertThat(new ElwhaCardSupportingText("x").getMaximumSize().width)
        .as("and supporting text, which is the one that wraps most often")
        .isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  void aTextAtomIsNoTallerThanItAsksToBe() {
    final ElwhaCardSupportingText body = new ElwhaCardSupportingText("x");

    assertThat(body.getMaximumSize().height)
        .as("height is pinned to preferred so a parent cannot stretch a single line vertically")
        .isEqualTo(body.getPreferredSize().height);
  }

  @Test
  void narrowingASupportingTextGrowsItsHeightRatherThanClippingIt() {
    final ElwhaCardSupportingText body =
        new ElwhaCardSupportingText(
            "A sentence long enough that it has somewhere to wrap when the column narrows.");
    body.setSize(400, 20);
    final int atWide = body.getPreferredSize().height;
    body.setSize(90, 20);

    assertThat(body.getPreferredSize().height)
        .as("§4.3 — supporting text word-wraps at narrow widths instead of ellipsizing")
        .isGreaterThan(atWide);
  }

  @Test
  void aTextAtomsPreferredWidthDoesNotChaseTheWidthItWasGiven() {
    final ElwhaCardSupportingText body =
        new ElwhaCardSupportingText("A sentence with enough words in it to wrap somewhere.");
    body.setSize(400, 40);
    final int reportedWide = body.getPreferredSize().width;
    body.setSize(120, 40);

    assertThat(body.getPreferredSize().width)
        .as("#305 — preferred width is the intrinsic single-line width, not a layout-cycle echo")
        .isEqualTo(reportedWide);
  }

  // ------------------------------------------------------------ leading icon

  @Test
  void leadingIconCarriesTheM3LeadingRoleAndSize() {
    final ElwhaCardLeadingIcon icon = new ElwhaCardLeadingIcon(MaterialIcons.pushPinFilled());

    assertThat(icon.getColorRole())
        .as("§4.4 — the card's leading glyph is the primary accent")
        .isEqualTo(ColorRole.PRIMARY);
    assertThat(ElwhaCardLeadingIcon.DEFAULT_ICON_SIZE_DP)
        .as("the canonical M3 card-leading glyph is 24 dp")
        .isEqualTo(24);
  }

  @Test
  void leadingIconTintsItsGlyphToTheColorRole() {
    final FlatSVGIcon glyph = MaterialIcons.pushPinFilled();
    new ElwhaCardLeadingIcon(glyph);

    assertThat(opaqueColors(glyph))
        .as("wrapping an SVG symbol remaps every painted pixel to the active role")
        .containsExactly(new Color(ColorRole.PRIMARY.resolve().getRGB(), false));
  }

  @Test
  void changingTheColorRoleRetintsTheGlyph() {
    final FlatSVGIcon glyph = MaterialIcons.pushPinFilled();
    final ElwhaCardLeadingIcon icon = new ElwhaCardLeadingIcon(glyph);

    icon.setColorRole(ColorRole.ERROR);

    assertThat(opaqueColors(glyph))
        .as("the filter is re-applied rather than left pointing at the old role")
        .containsExactly(new Color(ColorRole.ERROR.resolve().getRGB(), false));
  }

  @Test
  void installingANewGlyphLaterTintsThatOneToo() {
    final ElwhaCardLeadingIcon icon = new ElwhaCardLeadingIcon();
    icon.setColorRole(ColorRole.TERTIARY);
    final FlatSVGIcon glyph = MaterialIcons.pushPinFilled();

    icon.setIcon(glyph);

    assertThat(opaqueColors(glyph))
        .as("a deferred icon install picks up the role the slot was already carrying")
        .containsExactly(new Color(ColorRole.TERTIARY.resolve().getRGB(), false));
  }

  @Test
  void leadingIconRejectsANullColorRole() {
    assertThatThrownBy(() -> new ElwhaCardLeadingIcon().setColorRole(null))
        .as("a null color role is rejected")
        .isInstanceOf(NullPointerException.class);
  }

  // -------------------------------------------------------------- thumbnail

  @Test
  void thumbnailCarriesTheAvatarDefaults() {
    final ElwhaCardThumbnail thumb = new ElwhaCardThumbnail(blankImage());

    assertThat(thumb.getShape())
        .as("§4.5 — the thumbnail defaults to the round avatar treatment")
        .isEqualTo(ThumbnailShape.CIRCULAR);
    assertThat(thumb.getSizeDp()).as("at the canonical 40 dp").isEqualTo(40);
    assertThat(thumb.getPreferredSize())
        .as("and asks for a square of that size")
        .isEqualTo(new Dimension(40, 40));
  }

  @Test
  void everyDisplayOnlyAtomIsInertByConstruction() {
    assertThat(new ElwhaCardTitle("Title").isFocusable())
        .as("a card title is text, not a control")
        .isFalse();
    assertThat(new ElwhaCardSubtitle("Subtitle").isFocusable()).isFalse();
    assertThat(new ElwhaCardSupportingText("Body").isFocusable()).isFalse();
    assertThat(new ElwhaCardLeadingIcon().isFocusable()).isFalse();
    assertThat(new ElwhaCardDivider().isFocusable()).isFalse();
  }

  @Test
  void structuralSlotsAreInertButPassTheirChildrensStopsThrough() {
    final ElwhaCardHeader header = new ElwhaCardHeader();
    final ElwhaCardActions actions = new ElwhaCardActions();
    final ElwhaButton save = ElwhaButton.textButton("Save");
    actions.addTrailing(save);

    assertThat(header.isFocusable())
        .as("a slot that hosts controls must not become a stop in front of them")
        .isFalse();
    assertThat(actions.isFocusable()).isFalse();
    assertThat(save.isFocusable())
        .as("container focusability does not cascade — the hosted button keeps its stop")
        .isTrue();
  }

  @Test
  void thumbnailIsInertByConstruction() {
    assertThat(new ElwhaCardThumbnail(blankImage()).isFocusable())
        .as("a thumbnail is decoration and must never take a tab stop")
        .isFalse();
  }

  @Test
  void thumbnailSizeAndShapeRoundTripAndRejectBadInput() {
    final ElwhaCardThumbnail thumb = new ElwhaCardThumbnail(blankImage());

    assertThat(thumb.setShape(ThumbnailShape.SQUARE).getShape())
        .as("the photo treatment is selectable")
        .isEqualTo(ThumbnailShape.SQUARE);
    assertThat(thumb.setSize(64).getPreferredSize())
        .as("a resized thumbnail stays square")
        .isEqualTo(new Dimension(64, 64));
    assertThat(thumb.getMinimumSize())
        .as("a thumbnail refuses to be compressed below its own size")
        .isEqualTo(thumb.getPreferredSize());
    assertThatThrownBy(() -> thumb.setSize(0))
        .as("a zero size is rejected rather than rendering nothing")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> thumb.setShape(null))
        .as("a null shape is rejected")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new ElwhaCardThumbnail(null))
        .as("a thumbnail with no image is rejected at construction")
        .isInstanceOf(NullPointerException.class);
  }

  // ---------------------------------------------------------------- divider

  @Test
  void dividerIsAOneDpLineThatSpansWhateverItIsGiven() {
    final ElwhaCardDivider rule = new ElwhaCardDivider();

    assertThat(rule.getPreferredSize().height).as("§5.4 — the M3 divider is 1 dp").isEqualTo(1);
    assertThat(rule.getMaximumSize().width)
        .as("and stretches to whatever width the row offers")
        .isEqualTo(Integer.MAX_VALUE);
    assertThat(rule.getMaximumSize().height).as("without ever growing thicker").isEqualTo(1);
  }

  @Test
  void dividerStyleIsFixedAtConstruction() {
    assertThat(new ElwhaCardDivider().getStyle())
        .as("the bare constructor gives the common in-body separator")
        .isEqualTo(DividerStyle.INSET);
    assertThat(new ElwhaCardDivider(DividerStyle.FULL).getStyle())
        .as("the edge-to-edge treatment is opt-in")
        .isEqualTo(DividerStyle.FULL);
    assertThatThrownBy(() -> new ElwhaCardDivider(null))
        .as("a null style is rejected")
        .isInstanceOf(NullPointerException.class);
  }

  // ------------------------------------------------------------------ media

  @Test
  void mediaFactoriesRejectNullSources() {
    assertThatThrownBy(() -> ElwhaCardMedia.image(null))
        .as("an image slot with no image is rejected")
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> ElwhaCardMedia.painter(null))
        .as("a painter slot with no painter is rejected")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void mediaDefaultsToSixteenByNineAndDerivedHeight() {
    final ElwhaCardMedia cover = ElwhaCardMedia.painter((g, w, h) -> {});

    assertThat(cover.getAspectRatio())
        .as("§5.2 — media defaults to the 16:9 hero ratio")
        .isEqualTo(16.0 / 9.0);
    assertThat(cover.getPreferredHeight())
        .as("with no pinned height, so sizing is aspect-ratio driven")
        .isEqualTo(-1);
    assertThat(cover.isDecorative())
        .as("and is informative until a consumer says otherwise")
        .isFalse();
  }

  @Test
  void mediaRejectsANonPositiveAspectRatio() {
    final ElwhaCardMedia cover = ElwhaCardMedia.painter((g, w, h) -> {});

    assertThatThrownBy(() -> cover.setAspectRatio(0))
        .as("a zero ratio would divide the height computation by zero")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> cover.setAspectRatio(-2))
        .as("and a negative one would invert the slot")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void mediaHeightForSlotWidthFollowsTheDocumentedResolutionOrder() {
    final ElwhaCardMedia cover = ElwhaCardMedia.painter((g, w, h) -> {}).setAspectRatio(4.0);

    assertThat(cover.heightForSlotWidth(200))
        .as("with no pinned height the slot width drives it through the ratio")
        .isEqualTo(50);
    assertThat(cover.heightForSlotWidth(0))
        .as("with no slot width yet it falls back to the intrinsic preferred height")
        .isEqualTo(cover.getPreferredSize().height);
    cover.setPreferredHeight(90);
    assertThat(cover.heightForSlotWidth(200)).as("a pinned height wins over both").isEqualTo(90);
    cover.setPreferredHeight(-1);
    assertThat(cover.heightForSlotWidth(200))
        .as("and passing -1 hands sizing back to the aspect ratio")
        .isEqualTo(50);
  }

  @Test
  void mediaPreferredSizeIgnoresTheWidthItWasGiven() {
    final ElwhaCardMedia cover = ElwhaCardMedia.painter((g, w, h) -> {});
    final Dimension intrinsic = cover.getPreferredSize();

    cover.setSize(900, 400);

    assertThat(cover.getPreferredSize())
        .as("reading the laid-out width here would make every layout pass grow the card")
        .isEqualTo(intrinsic);
  }

  @Test
  void informativeMediaAnnouncesItsAltText() {
    final ElwhaCardMedia cover =
        ElwhaCardMedia.painter((g, w, h) -> {}).setAltText("A river delta");

    assertThat(cover.getAltText()).as("the alt text round-trips").isEqualTo("A river delta");
    assertThat(cover.getAccessibleContext().getAccessibleRole())
        .as("informative media reads as an icon to assistive technology")
        .isEqualTo(AccessibleRole.ICON);
    assertThat(cover.getAccessibleContext().getAccessibleDescription())
        .as("and verbalizes the alt text")
        .isEqualTo("A river delta");
  }

  @Test
  void decorativeMediaGoesSilent() {
    final ElwhaCardMedia cover =
        ElwhaCardMedia.painter((g, w, h) -> {}).setAltText("ignored").setDecorative(true);

    assertThat(cover.getAccessibleContext().getAccessibleRole())
        .as("decorative media drops to a nameless label so AT skips it")
        .isEqualTo(AccessibleRole.LABEL);
    assertThat(cover.getAccessibleContext().getAccessibleName())
        .as("with no name to announce")
        .isNull();
    assertThat(cover.getAccessibleContext().getAccessibleDescription())
        .as("and no description either, even though alt text was set")
        .isNull();
  }

  @Test
  void flippingTheDecorativeFlagBackRestoresTheAnnouncement() {
    final ElwhaCardMedia cover =
        ElwhaCardMedia.painter((g, w, h) -> {}).setAltText("A river delta").setDecorative(true);
    cover.getAccessibleContext();

    cover.setDecorative(false);

    assertThat(cover.getAccessibleContext().getAccessibleRole())
        .as("the accessible context re-resolves rather than caching the first answer")
        .isEqualTo(AccessibleRole.ICON);
  }

  @Test
  void mediaIsInertByConstruction() {
    assertThat(ElwhaCardMedia.painter((g, w, h) -> {}).isFocusable())
        .as("media is decoration per the V3 actionability doctrine — never a tab stop")
        .isFalse();
  }

  private static BufferedImage blankImage() {
    return new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
  }
}
