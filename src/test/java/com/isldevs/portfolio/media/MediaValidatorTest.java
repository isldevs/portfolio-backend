package com.isldevs.portfolio.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.isldevs.portfolio.media.service.MediaValidator;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class MediaValidatorTest {
  private final MediaValidator validator = new MediaValidator();

  @Test
  void identifiesDecodedImagesRatherThanTrustingTheFilename() throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", output);
    byte[] png = output.toByteArray();
    assertThat(validator.validate("portrait.png", png).contentType()).isEqualTo("image/png");
    assertThatThrownBy(() -> validator.validate("portrait.jpg", png))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("does not match");
  }

  @Test
  void rejectsHtmlSvgTruncatedAndOversizedPayloads() {
    assertThatThrownBy(() -> validator.validate("payload.svg", "<svg/>".getBytes()))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(
            () -> validator.validate("payload.png", "<script>alert(1)</script>".getBytes()))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(
            () -> validator.validate("payload.png", new byte[(int) MediaValidator.MAX_SIZE + 1]))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> validator.validate("payload.pdf", "%PDF-1.7 invalid".getBytes()))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void acceptsRealPdfButRejectsAutomaticScriptActions() throws Exception {
    try (PDDocument pdf = new PDDocument()) {
      pdf.addPage(new PDPage());
      ByteArrayOutputStream clean = new ByteArrayOutputStream();
      pdf.save(clean);
      assertThat(validator.validate("resume.pdf", clean.toByteArray()).contentType())
          .isEqualTo("application/pdf");
      pdf.getDocumentCatalog().setOpenAction(new PDActionJavaScript("app.alert('unsafe')"));
      ByteArrayOutputStream scripted = new ByteArrayOutputStream();
      pdf.save(scripted);
      assertThatThrownBy(() -> validator.validate("resume.pdf", scripted.toByteArray()))
          .isInstanceOf(ResponseStatusException.class)
          .hasMessageContaining("scripts");
    }
  }
}
