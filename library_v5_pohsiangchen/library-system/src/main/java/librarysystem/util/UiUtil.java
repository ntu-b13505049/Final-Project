package librarysystem.util;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Dimension;
import java.awt.Font;

public final class UiUtil {
    private UiUtil() {
    }

    public static void applyDefaultTableStyle(JTable table) {
        table.setRowHeight(26);
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD, 13f));
        table.setAutoCreateRowSorter(true);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setPreferredWidth(70);
        }
    }

    public static void pad(JComponent component) {
        component.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    public static Dimension buttonSize() {
        return new Dimension(140, 36);
    }
}
