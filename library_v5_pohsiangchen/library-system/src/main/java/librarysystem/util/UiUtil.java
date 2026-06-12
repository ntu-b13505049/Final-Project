package librarysystem.util;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public final class UiUtil {
    private static final Dimension MAIN_WINDOW_SIZE = new Dimension(1400, 860);

    private UiUtil() {
    }

    public static Dimension mainWindowSize() {
        return new Dimension(MAIN_WINDOW_SIZE);
    }

    public static void applyGlobalFont(float size) {
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource fontResource) {
                UIManager.put(key, new FontUIResource(fontResource.deriveFont(size)));
            }
        }
    }

    public static void applyDefaultTableStyle(JTable table) {
        table.setRowHeight(34);
        table.setFont(table.getFont().deriveFont(15f));
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD, 15f));
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setPreferredWidth(86);
        }
    }

    public static void applyOverdueRowHighlight(JTable table, int overdueColumnIndex, String... overdueMarkers) {
        Set<String> markers = new HashSet<>();
        for (String marker : overdueMarkers) {
            if (marker != null) {
                markers.add(marker.trim());
            }
        }
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                boolean overdue = false;
                try {
                    int modelRow = table.convertRowIndexToModel(row);
                    Object markerValue = table.getModel().getValueAt(modelRow, overdueColumnIndex);
                    String markerText = markerValue == null ? "" : markerValue.toString().trim();
                    overdue = markers.contains(markerText) || markerText.contains("逾期");
                } catch (Exception ignored) {
                    overdue = false;
                }

                if (!isSelected) {
                    if (overdue) {
                        component.setBackground(new Color(255, 225, 225));
                        component.setForeground(new Color(160, 0, 0));
                    } else {
                        component.setBackground(Color.WHITE);
                        component.setForeground(Color.BLACK);
                    }
                }
                component.setFont(component.getFont().deriveFont(overdue ? Font.BOLD : Font.PLAIN, 15f));
                return component;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
    }

    public static void pad(JComponent component) {
        component.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    public static Dimension buttonSize() {
        return new Dimension(150, 40);
    }
}
