import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class StudentPanel extends JPanel {

    private static final int STUDENT_ID = 1; // Kavy Khilrani

    public StudentPanel() {
        setLayout(new BorderLayout());
        setBackground(MainDashboard.SECONDARY_GREY);

        // ── Section Header ──────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(MainDashboard.CARD_WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, MainDashboard.BORDER_COLOR));
        header.setPreferredSize(new Dimension(0, 64));

        JLabel title = new JLabel("   🎓  Student Dashboard");
        title.setFont(MainDashboard.FONT_TITLE);
        title.setForeground(MainDashboard.TEXT_DARK);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // ── Tabbed Panes ────────────────────────────────────────────────────
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(MainDashboard.FONT_HEADER);
        tabs.setBackground(MainDashboard.SECONDARY_GREY);

        tabs.addTab("📚  My Courses",            createCoursesPanel());
        tabs.addTab("📝  Assignments",            createAssignmentsPanel());
        tabs.addTab("📤  Submit Assignment",      createSubmitPanel());
        tabs.addTab("📊  My Grades",              createGradesPanel());

        add(tabs, BorderLayout.CENTER);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** Style a JTable to look clean and card-like */
    private void styleTable(JTable table) {
        table.setFont(MainDashboard.FONT_BODY);
        table.setRowHeight(32);
        table.setGridColor(MainDashboard.BORDER_COLOR);
        table.setShowGrid(true);
        table.setSelectionBackground(new Color(210, 231, 252));
        table.setSelectionForeground(MainDashboard.TEXT_DARK);
        table.setBackground(MainDashboard.CARD_WHITE);

        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setFont(MainDashboard.FONT_HEADER);
        tableHeader.setBackground(MainDashboard.PRIMARY_BLUE);
        tableHeader.setForeground(Color.WHITE);
        tableHeader.setPreferredSize(new Dimension(0, 38));
    }

    /** Create a styled action button */
    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bg.darker(), 1),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        return btn;
    }

    // ── Tab 1: My Courses ───────────────────────────────────────────────────
    private JPanel createCoursesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(MainDashboard.SECONDARY_GREY);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String[] columns = {"Course ID", "Course Name", "Instructor", "Department"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT C.course_id, C.course_name, I.name, I.department " +
                "FROM Course C " +
                "JOIN Enrollment E  ON C.course_id    = E.course_id " +
                "JOIN Instructor I  ON C.instructor_id = I.instructor_id " +
                "WHERE E.student_id = " + STUDENT_ID
            );
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)
                });
            }
        } catch (Exception e) { e.printStackTrace(); }

        JTable table = new JTable(model);
        styleTable(table);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel info = new JLabel("  Showing all courses you are enrolled in.");
        info.setFont(MainDashboard.FONT_SMALL);
        info.setForeground(Color.GRAY);
        panel.add(info, BorderLayout.SOUTH);

        return panel;
    }

    // ── Tab 2: Assignments ──────────────────────────────────────────────────
    private JPanel createAssignmentsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(MainDashboard.SECONDARY_GREY);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String[] columns = {"Assignment ID", "Title", "Course", "Deadline", "Submitted?"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT A.assignment_id, A.title, C.course_name, A.deadline, " +
                "  CASE WHEN S.submission_id IS NOT NULL THEN 'Yes ✅' ELSE 'No ❌' END AS submitted " +
                "FROM Assignment A " +
                "JOIN Course C ON A.course_id = C.course_id " +
                "JOIN Enrollment E ON E.course_id = C.course_id AND E.student_id = " + STUDENT_ID +
                " LEFT JOIN Submission S ON S.assignment_id = A.assignment_id AND S.student_id = " + STUDENT_ID +
                " ORDER BY A.deadline"
            );
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5)
                });
            }
        } catch (Exception e) { e.printStackTrace(); }

        JTable table = new JTable(model);
        styleTable(table);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // ── Tab 3: Submit Assignment ─────────────────────────────────────────────
    private JPanel createSubmitPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(MainDashboard.SECONDARY_GREY);

        // Card-style inner panel
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(MainDashboard.CARD_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MainDashboard.BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(30, 40, 30, 40)
        ));

        JLabel heading = new JLabel("📤  Submit an Assignment");
        heading.setFont(MainDashboard.FONT_TITLE);
        heading.setForeground(MainDashboard.TEXT_DARK);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Select from your pending assignments and click Submit.");
        sub.setFont(MainDashboard.FONT_SMALL);
        sub.setForeground(Color.GRAY);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(heading);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(sub);
        card.add(Box.createRigidArea(new Dimension(0, 24)));

        // Dropdown of pending assignments
        JLabel dropLabel = new JLabel("Choose Assignment:");
        dropLabel.setFont(MainDashboard.FONT_HEADER);
        dropLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(dropLabel);
        card.add(Box.createRigidArea(new Dimension(0, 8)));

        JComboBox<String> assignmentDropdown = new JComboBox<>();
        assignmentDropdown.setFont(MainDashboard.FONT_BODY);
        assignmentDropdown.setMaximumSize(new Dimension(500, 36));
        assignmentDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Map to store display text -> assignment_id
        java.util.LinkedHashMap<String, Integer> assignmentMap = new java.util.LinkedHashMap<>();

        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT A.assignment_id, A.title, C.course_name, A.deadline " +
                "FROM Assignment A " +
                "JOIN Course C ON A.course_id = C.course_id " +
                "JOIN Enrollment E ON E.course_id = C.course_id AND E.student_id = " + STUDENT_ID +
                " LEFT JOIN Submission S ON S.assignment_id = A.assignment_id AND S.student_id = " + STUDENT_ID +
                " WHERE S.submission_id IS NULL ORDER BY A.deadline"
            );
            while (rs.next()) {
                String display = "[" + rs.getInt("assignment_id") + "] "
                    + rs.getString("title") + " — " + rs.getString("course_name")
                    + "  (Due: " + rs.getString("deadline") + ")";
                assignmentMap.put(display, rs.getInt("assignment_id"));
                assignmentDropdown.addItem(display);
            }
        } catch (Exception e) { e.printStackTrace(); }

        if (assignmentDropdown.getItemCount() == 0) {
            assignmentDropdown.addItem("No pending assignments 🎉");
        }

        card.add(assignmentDropdown);
        card.add(Box.createRigidArea(new Dimension(0, 24)));

        JButton btnSubmit = makeButton("  ✅  Submit Assignment", MainDashboard.PRIMARY_BLUE);
        btnSubmit.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSubmit.addActionListener(e -> {
            String selected = (String) assignmentDropdown.getSelectedItem();
            if (selected == null || !assignmentMap.containsKey(selected)) {
                JOptionPane.showMessageDialog(this, "No valid assignment selected.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int assignmentId = assignmentMap.get(selected);
            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO Submission (assignment_id, student_id, submission_date) VALUES (?, ?, date('now'))"
                );
                stmt.setInt(1, assignmentId);
                stmt.setInt(2, STUDENT_ID);
                stmt.executeUpdate();

                JOptionPane.showMessageDialog(this,
                    "Assignment submitted successfully!\n" + selected,
                    "Submitted ✅", JOptionPane.INFORMATION_MESSAGE);

                // Refresh dropdown
                assignmentDropdown.removeItem(selected);
                assignmentMap.remove(selected);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        card.add(btnSubmit);
        outer.add(card);
        return outer;
    }

    // ── Tab 4: Grades ───────────────────────────────────────────────────────
    private JPanel createGradesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(MainDashboard.SECONDARY_GREY);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String[] columns = {"Assignment", "Course", "Submitted On", "Marks / 100", "Feedback"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT A.title, C.course_name, S.submission_date, " +
                "  CASE WHEN G.marks IS NOT NULL THEN CAST(G.marks AS TEXT) ELSE 'Pending' END, " +
                "  COALESCE(G.feedback, '—') " +
                "FROM Submission S " +
                "JOIN Assignment A ON S.assignment_id = A.assignment_id " +
                "JOIN Course C     ON A.course_id      = C.course_id " +
                "LEFT JOIN Grade G ON G.submission_id  = S.submission_id " +
                "WHERE S.student_id = " + STUDENT_ID +
                " ORDER BY S.submission_date DESC"
            );
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5)
                });
            }
        } catch (Exception e) { e.printStackTrace(); }

        JTable table = new JTable(model);
        styleTable(table);
        table.getColumnModel().getColumn(4).setPreferredWidth(260);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel info = new JLabel("  Grades marked 'Pending' have not yet been evaluated by your instructor.");
        info.setFont(MainDashboard.FONT_SMALL);
        info.setForeground(Color.GRAY);
        panel.add(info, BorderLayout.SOUTH);

        return panel;
    }
}
