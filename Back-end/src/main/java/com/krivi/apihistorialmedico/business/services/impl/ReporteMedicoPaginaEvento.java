package com.krivi.apihistorialmedico.business.services.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

final class ReporteMedicoPaginaEvento extends PdfPageEventHelper {
  private final Font fuentePie;

  ReporteMedicoPaginaEvento(BaseFont fuente) {
    this.fuentePie = new Font(fuente, 8, Font.NORMAL);
  }

  @Override
  public void onEndPage(PdfWriter writer, Document document) {
    Phrase pagina = new Phrase("Página " + writer.getPageNumber(), fuentePie);
    ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER, pagina,
        (document.left() + document.right()) / 2, document.bottom() - 18, 0);
  }
}
