package ec.dfgranda.crm.service;

import ec.dfgranda.crm.config.SellerProperties;
import ec.dfgranda.crm.domain.Company;
import ec.dfgranda.crm.domain.Contact;
import ec.dfgranda.crm.domain.Quote;
import ec.dfgranda.crm.domain.QuoteItem;
import ec.dfgranda.crm.dto.TaxLine;
import ec.dfgranda.crm.mapper.QuoteMapper;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Genera el PDF de la cotización con OpenPDF. No es un comprobante electrónico: el pie lo aclara e incluye el
 * aviso de privacidad mínimo (responsable y canal para ejercer derechos, LOPDP Art. 12).
 */
@Component
public class QuotePdfRenderer {

    private static final Color PRIMARY = new Color(0x1F, 0x3A, 0x5F);
    private static final Color HEADER_BG = new Color(0xE8, 0xEE, 0xF5);
    private static final Color BORDER = new Color(0xC9, 0xD3, 0xDF);
    private static final Font TITLE = new Font(Font.HELVETICA, 16, Font.BOLD, PRIMARY);
    private static final Font BOLD = new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK);
    private static final Font NORMAL = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
    private static final Font SMALL = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, Color.DARK_GRAY);
    private static final Font TOTAL = new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SellerProperties seller;

    public QuotePdfRenderer(SellerProperties seller) {
        this.seller = seller;
    }

    public byte[] render(Quote quote) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(document, out);
            document.addTitle("Cotización " + quote.getNumber());
            document.addCreator("CRM Ventas");
            document.open();
            document.add(header(quote));
            document.add(customer(quote));
            document.add(items(quote));
            document.add(totals(quote));
            if (quote.getNotes() != null) {
                Paragraph notes = new Paragraph("Observaciones: " + quote.getNotes(), NORMAL);
                notes.setSpacingBefore(10);
                document.add(notes);
            }
            document.add(footer());
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF de la cotización.", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private PdfPTable header(Quote quote) {
        PdfPTable table = new PdfPTable(new float[]{3, 2});
        table.setWidthPercentage(100);
        PdfPCell sellerCell = new PdfPCell();
        sellerCell.setBorder(Rectangle.NO_BORDER);
        sellerCell.addElement(new Paragraph(seller.legalName(), TITLE));
        sellerCell.addElement(new Paragraph("RUC " + seller.ruc(), NORMAL));
        sellerCell.addElement(new Paragraph(seller.address() + " · " + seller.city(), NORMAL));
        sellerCell.addElement(new Paragraph(seller.phone() + " · " + seller.email(), NORMAL));
        table.addCell(sellerCell);

        PdfPCell quoteCell = new PdfPCell();
        quoteCell.setBorderColor(BORDER);
        quoteCell.setPadding(8);
        quoteCell.setBackgroundColor(HEADER_BG);
        Paragraph title = new Paragraph("COTIZACIÓN", TITLE);
        title.setAlignment(Element.ALIGN_RIGHT);
        quoteCell.addElement(title);
        quoteCell.addElement(right("N.º " + quote.getNumber(), BOLD));
        quoteCell.addElement(right("Emisión: " + DATE.format(quote.getIssueDate()), NORMAL));
        quoteCell.addElement(right("Válida hasta: " + DATE.format(quote.getValidUntil()), NORMAL));
        table.addCell(quoteCell);
        table.setSpacingAfter(12);
        return table;
    }

    private PdfPTable customer(Quote quote) {
        Company company = quote.getOpportunity().getCompany();
        PdfPTable table = new PdfPTable(new float[]{1.2f, 4});
        table.setWidthPercentage(100);
        labelValue(table, "Cliente", company.getLegalName());
        labelValue(table, "RUC", company.getRuc());
        if (company.getAddress() != null) {
            labelValue(table, "Dirección", company.getAddress() + (company.getCity() == null ? "" : ", " + company.getCity()));
        }
        Contact contact = quote.getContact();
        if (contact != null) {
            labelValue(table, "Atención", contact.fullName() + (contact.getEmail() == null ? "" : " · " + contact.getEmail()));
        }
        labelValue(table, "Referencia", quote.getOpportunity().getTitle());
        table.setSpacingAfter(12);
        return table;
    }

    private PdfPTable items(Quote quote) {
        PdfPTable table = new PdfPTable(new float[]{0.5f, 5, 1, 1.4f, 1, 0.9f, 1.5f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (String title : new String[]{"#", "Descripción", "Cant.", "P. unitario", "Desc. %", "IVA %", "Subtotal"}) {
            PdfPCell cell = cell(title, BOLD, Element.ALIGN_CENTER);
            cell.setBackgroundColor(HEADER_BG);
            table.addCell(cell);
        }
        for (QuoteItem item : quote.getItems()) {
            table.addCell(cell(String.valueOf(item.getLineNumber()), NORMAL, Element.ALIGN_CENTER));
            table.addCell(cell(item.getDescription(), NORMAL, Element.ALIGN_LEFT));
            table.addCell(cell(number(item.getQuantity()), NORMAL, Element.ALIGN_RIGHT));
            table.addCell(cell(money(item.getUnitPrice()), NORMAL, Element.ALIGN_RIGHT));
            table.addCell(cell(number(item.getDiscountPercent()), NORMAL, Element.ALIGN_RIGHT));
            table.addCell(cell(number(item.getVatPercentage()), NORMAL, Element.ALIGN_RIGHT));
            table.addCell(cell(money(item.netAmount()), NORMAL, Element.ALIGN_RIGHT));
        }
        table.setSpacingAfter(8);
        return table;
    }

    private PdfPTable totals(Quote quote) {
        PdfPTable table = new PdfPTable(new float[]{3, 1.5f});
        table.setWidthPercentage(45);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalRow(table, "Subtotal", money(quote.getSubtotal()), NORMAL);
        totalRow(table, "Descuento", "-" + money(quote.getDiscountTotal()), NORMAL);
        for (TaxLine line : QuoteMapper.taxBreakdown(quote)) {
            String rate = number(line.percentage()) + " %";
            totalRow(table, "Base imponible IVA " + rate, money(line.taxableBase()), NORMAL);
            if (line.percentage().signum() > 0) {
                totalRow(table, "IVA " + rate, money(line.tax()), NORMAL);
            }
        }
        totalRow(table, "TOTAL " + quote.getCurrency(), money(quote.getTotal()), TOTAL);
        return table;
    }

    private Paragraph footer() {
        Paragraph footer = new Paragraph(
                "Documento comercial sin validez tributaria: no reemplaza a un comprobante electrónico autorizado por el SRI. "
                        + "Sus datos personales son tratados por " + seller.legalName() + " para gestionar esta cotización, "
                        + "conforme a la Ley Orgánica de Protección de Datos Personales. Para ejercer sus derechos de acceso, "
                        + "rectificación, eliminación u oposición escriba a " + seller.email() + ".", SMALL);
        footer.setSpacingBefore(24);
        return footer;
    }

    private static void labelValue(PdfPTable table, String label, String value) {
        PdfPCell labelCell = cell(label, BOLD, Element.ALIGN_LEFT);
        labelCell.setBackgroundColor(HEADER_BG);
        table.addCell(labelCell);
        table.addCell(cell(value, NORMAL, Element.ALIGN_LEFT));
    }

    private static void totalRow(PdfPTable table, String label, String value, Font font) {
        table.addCell(cell(label, font, Element.ALIGN_LEFT));
        table.addCell(cell(value, font, Element.ALIGN_RIGHT));
    }

    private static PdfPCell cell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(BORDER);
        cell.setPadding(4);
        return cell;
    }

    private static Paragraph right(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_RIGHT);
        return paragraph;
    }

    private static String money(BigDecimal value) {
        return "$" + number(value);
    }

    private static String number(BigDecimal value) {
        return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
    }
}
