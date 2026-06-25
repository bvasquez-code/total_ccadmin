package com.ccadmin.app.sale;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import java.nio.charset.Charset;
import java.util.Arrays;

public class TestEscPosPrint {

    // Cambia si tu impresora se llama distinto (usa exactamente el Name que viste en Get-Printer)
    private static final String PRINTER_NAME = "IMPCCADMIN";

    public static void main(String[] args) throws Exception {
        // 1) Buscar la impresora por nombre
        PrintService printer = Arrays.stream(PrintServiceLookup.lookupPrintServices(null, null))
                .filter(ps -> ps.getName().equalsIgnoreCase(PRINTER_NAME))
                .findFirst()
                .orElse(null);

        if (printer == null) {
            System.err.println("No encontré la impresora: " + PRINTER_NAME);
            System.out.println("Disponibles:");
            for (PrintService ps : PrintServiceLookup.lookupPrintServices(null, null)) {
                System.out.println(" - " + ps.getName());
            }
            return;
        }

        // 2) Construir bytes ESC/POS (init, texto, corte)
        // Consejo: muchas EPSON esperan codepage tipo CP437/858 para acentos; para la prueba usa ASCII simple
        byte[] init    = new byte[]{0x1B, '@'};           // ESC @ -> inicializa
        byte[] center  = new byte[]{0x1B, 'a', 0x01};     // ESC a 1 -> centrar
        byte[] left    = new byte[]{0x1B, 'a', 0x00};     // ESC a 0 -> izquierda
        byte[] boldOn  = new byte[]{0x1B, 'E', 0x01};     // negrita ON
        byte[] boldOff = new byte[]{0x1B, 'E', 0x00};     // negrita OFF
        byte[] cut     = new byte[]{0x1D, 'V', 66, 0};    // GS V 66 0 -> corte parcial

        String header  = "PRUEBA ESC/POS\n";
        String tienda  = "MI TIENDA\n";
        String cuerpo  = "Prod A      2 x 5.00\nProd B      1 x 3.50\n";
        String total   = "TOTAL           13.50\n";
        String footer  = "\nGracias por su compra\n\n";

        // Si deseas usar acentos, prueba con CP437 o CP858 (depende del driver)
        Charset cs = Charset.forName("US-ASCII");

        // concatena
        byte[] data = concat(
                init,
                center, boldOn, tienda.getBytes(cs), boldOff,
                left, header.getBytes(cs),
                cuerpo.getBytes(cs),
                total.getBytes(cs),
                footer.getBytes(cs),
                cut
        );

        // 3) Enviar como RAW (AUTOSENSE) a la impresora
        DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
        Doc doc = new SimpleDoc(data, flavor, null);

        DocPrintJob job = printer.createPrintJob();
        job.print(doc, new HashPrintRequestAttributeSet());

        System.out.println("Enviado a impresora: " + printer.getName());
    }

    private static byte[] concat(byte[]... arrays) {
        int len = 0; for (byte[] a : arrays) len += a.length;
        byte[] out = new byte[len];
        int p = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, out, p, a.length);
            p += a.length;
        }
        return out;
    }
}
