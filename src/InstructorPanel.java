import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class InstructorPanel extends JPanel {

    private static final int INSTRUCTOR_ID = 1; // Dr. Jashma Suresh

    // ── Instance-field models ────────────────────────────────────────────────
    private final DefaultTableModel coursesModel;
    private final DefaultTableModel assignmentsModel;
    private final DefaultTableModel submissionsModel;

    // Assignment-tab course dropdown (needs to stay up-to-date with new courses)
    private JComboBox<String> cmbCourse;
    private final java.util.LinkedHashMap<String, Integer> courseMap = new java.util.LinkedHashMap<>();

    // Grading-tab fields (referenced in the row-click listener)
    private JTextField txtSubId, txtMarks, txtFeedback;

    public InstructorPanel() {
        setLayout(new BorderLayout());
        setBackground(MainDashboard.SECONDARY_GREY);

        // ── Section Header ──────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(MainDashboard.CARD_WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, MainDashboard.BORDER_COLOR));
        header.setPreferredSize(new Dimension(0, 64));
        JLabel title = new JLabel("   \uD83D\uDC68\u200D\uD83C\uDFEB  Instructor Dashboard");
        title.setFont(MainDashboard.FONT_TITLE);
        title.setForeground(MainDashboard.TEXT_DARK);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // ── Initialise models ───────────────────────────────────────────────
        coursesModel     = makeModel("Course ID", "Course Name", "Enrolled Students");
        assignmentsModel = makeModel("Assignment ID", "Title", "Course", "Deadline");
        submissionsModel = makeModel("Sub ID", "Student", "Assignment", "Course", "Submitted On", "Grade", "Feedback");

        // ── Tabs ────────────────────────────────────────────────────────────
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(MainDashboard.FONT_HEADER);

        tabs.addTab("\uD83D\uDCCB  Manage Courses",    buildCoursesTab());
        tabs.addTab("\u270F\uFE0F  Manage Assignments", buildAssignmentsTab());
        tabs.addTab("\uD83D\uDCCA  Grade Submissions",  buildGradingTab());

        // ── ChangeListener: refresh whichever tab is opened ─────────────────
        tabs.addChangeListener((ChangeEvent e) -> {
            switch (tabs.getSelectedIndex()) {
                case 0 -> refreshCourses();
                case 1 -> { refreshCourseDropdown(); refreshAssignments(); }
                case 2 -> refreshSubmissions();
            }
        });

        // Initial load for first tab
        refreshCourses();

        add(tabs, BorderLayout.CENTER);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private DefaultTableModel makeModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private void styleTable(JTable table) {
        table.setFont(MainDashboard.FONT_BODY);
        table.setRowHeight(32);
        table.setGridColor(MainDashboard.BORDER_COLOR);
        table.setShowGrid(true);
        table.setSelectionBackground(new Color(210, 231, 252));
        table.setSelectionForeground(MainDashboard.TEXT_DARK);
        table.setBackground(MainDashboard.CARD_WHITE);
        JTableHeader th = table.getTableHeader();
        th.setFont(MainDashboard.FONT_HEADER);
        th.setBackground(new Color(44, 62, 80));
        th.setForeground(Color.WHITE);
        th.setPreferredSize(new Dimension(0, 38));
    }

    /** Styled button using BasicButtonUI to prevent Metal LAF from hiding text */
    private JButton makeBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bg.darker(), 1),
            BorderFactory.createEmptyBorder(8, 18, 8, 18)));
        return btn;
    }

    private boolean connOk(Connection conn) {
        if (conn == null) {
            JOptionPane.showMessageDialog(this,
                "<html>Database connection failed.<br>Make sure you launched via <b>run.bat</b>.</html>",
                "Connection Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tab 1: Manage Courses
    // ══════════════════════════════════════════════════════════════════════════

    private JPanel buildCoursesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setBackground(MainDashboard.SECONDARY_GREY);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTable table = new JTable(coursesModel);
        styleTable(table);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // ── Add Course Card ────────────────────────────────────────────────
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(MainDashboard.CARD_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MainDashboard.BORDER_COLOR),
            BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        JLabel cardTitle = new JLabel("\u2795  Add New Course");
        cardTitle.setFont(MainDashboard.FONT_HEADER);
        cardTitle.setForeground(MainDashboard.TEXT_DARK);
        card.add(cardTitle, BorderLayout.NORTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        row.setOpaque(false);
        row.add(new JLabel("Course Name:"));
        JTextField txtCourseName = new JTextField(26);
        txtCourseName.setFont(MainDashboard.FONT_BODY);
        row.add(txtCourseName);

        JButton btnAdd = makeBtn("  Add Course", MainDashboard.PRIMARY_BLUE);
        btnAdd.addActionListener(e -> {
            String name = txtCourseName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a Course Name.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Connection conn = DBConnection.getConnection();
            if (!connOk(conn)) return;
            try {
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(course_id), 100) + 1 FROM Course");
                int newId = rs.next() ? rs.getInt(1) : 200;
                st.close();

                PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO Course (course_id, course_name, instructor_id) VALUES (?, ?, ?)");
                pst.setInt(1, newId);
                pst.setString(2, name);
                pst.setInt(3, INSTRUCTOR_ID);
                pst.executeUpdate();
                pst.close();
                conn.close();

                txtCourseName.setText("");
                refreshCourses();          // instant table update
                JOptionPane.showMessageDialog(this,
                    "Course \"" + name + "\" added! (ID: " + newId + ")",
                    "Done \u2705", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        row.add(btnAdd);
        card.add(row, BorderLayout.CENTER);
        panel.add(card, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshCourses() {
        coursesModel.setRowCount(0);
        Connection conn = DBConnection.getConnection();
        if (conn == null) return;
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT C.course_id, C.course_name, COUNT(E.student_id) " +
                "FROM Course C LEFT JOIN Enrollment E ON C.course_id = E.course_id " +
                "WHERE C.instructor_id = " + INSTRUCTOR_ID +
                " GROUP BY C.course_id, C.course_name ORDER BY C.course_id");
            while (rs.next())
                coursesModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getInt(3)});
            conn.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tab 2: Manage Assignments
    // ══════════════════════════════════════════════════════════════════════════

    private JPanel buildAssignmentsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setBackground(MainDashboard.SECONDARY_GREY);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTable table = new JTable(assignmentsModel);
        styleTable(table);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // ── Add Assignment Card ────────────────────────────────────────────
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(MainDashboard.CARD_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MainDashboard.BORDER_COLOR),
            BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        JLabel cardTitle = new JLabel("\u2795  Create New Assignment");
        cardTitle.setFont(MainDashboard.FONT_HEADER);
        cardTitle.setForeground(MainDashboard.TEXT_DARK);
        card.add(cardTitle, BorderLayout.NORTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        row.setOpaque(false);

        // Course dropdown
        row.add(new JLabel("Course:"));
        cmbCourse = new JComboBox<>();
        cmbCourse.setFont(MainDashboard.FONT_BODY);
        row.add(cmbCourse);

        row.add(new JLabel("  Title:"));
        JTextField txtTitle = new JTextField(20);
        txtTitle.setFont(MainDashboard.FONT_BODY);
        row.add(txtTitle);

        row.add(new JLabel("  Deadline (YYYY-MM-DD):"));
        JTextField txtDeadline = new JTextField(10);
        txtDeadline.setFont(MainDashboard.FONT_BODY);
        row.add(txtDeadline);

        JButton btnAdd = makeBtn("  Add Assignment", MainDashboard.ACCENT_GREEN);
        btnAdd.addActionListener(e -> {
            String courseLbl = (String) cmbCourse.getSelectedItem();
            String title     = txtTitle.getText().trim();
            String deadline  = txtDeadline.getText().trim();

            if (courseLbl == null || courseLbl.isEmpty() || title.isEmpty() || deadline.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!deadline.matches("\\d{4}-\\d{2}-\\d{2}")) {
                JOptionPane.showMessageDialog(this,
                    "Deadline must be in YYYY-MM-DD format (e.g. 2026-06-30).", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!courseMap.containsKey(courseLbl)) {
                JOptionPane.showMessageDialog(this, "Please select a valid course.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int courseId = courseMap.get(courseLbl);
            Connection conn = DBConnection.getConnection();
            if (!connOk(conn)) return;
            try {
                PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO Assignment (course_id, title, deadline) VALUES (?, ?, ?)");
                pst.setInt(1, courseId);
                pst.setString(2, title);
                pst.setString(3, deadline);
                pst.executeUpdate();
                pst.close();
                conn.close();

                txtTitle.setText("");
                txtDeadline.setText("");
                refreshAssignments();    // instant table update
                JOptionPane.showMessageDialog(this,
                    "Assignment \"" + title + "\" created!", "Done \u2705", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        row.add(Box.createHorizontalStrut(6));
        row.add(btnAdd);
        card.add(row, BorderLayout.CENTER);
        panel.add(card, BorderLayout.SOUTH);
        return panel;
    }

    /** Reload the course dropdown — run every time the Assignments tab opens */
    private void refreshCourseDropdown() {
        courseMap.clear();
        if (cmbCourse == null) return;
        cmbCourse.removeAllItems();
        Connection conn = DBConnection.getConnection();
        if (conn == null) return;
        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery(
                "SELECT course_id, course_name FROM Course WHERE instructor_id = " + INSTRUCTOR_ID + " ORDER BY course_id");
            while (rs.next()) {
                String label = rs.getString(2) + " [" + rs.getInt(1) + "]";
                courseMap.put(label, rs.getInt(1));
                cmbCourse.addItem(label);
            }
            conn.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void refreshAssignments() {
        assignmentsModel.setRowCount(0);
        Connection conn = DBConnection.getConnection();
        if (conn == null) return;
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT A.assignment_id, A.title, C.course_name, A.deadline " +
                "FROM Assignment A JOIN Course C ON A.course_id = C.course_id " +
                "WHERE C.instructor_id = " + INSTRUCTOR_ID + " ORDER BY A.deadline");
            while (rs.next())
                assignmentsModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)});
            conn.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tab 3: Grade Submissions
    // ══════════════════════════════════════════════════════════════════════════

    private JPanel buildGradingTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setBackground(MainDashboard.SECONDARY_GREY);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTable table = new JTable(submissionsModel);
        styleTable(table);
        table.getColumnModel().getColumn(6).setPreferredWidth(220);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // ── Grading Card ───────────────────────────────────────────────────
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(MainDashboard.CARD_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MainDashboard.BORDER_COLOR),
            BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        JLabel cardTitle = new JLabel("\uD83D\uDD8A\uFE0F  Enter / Update a Grade  (click a row to auto-fill)");
        cardTitle.setFont(MainDashboard.FONT_HEADER);
        cardTitle.setForeground(MainDashboard.TEXT_DARK);
        card.add(cardTitle, BorderLayout.NORTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        row.setOpaque(false);

        row.add(new JLabel("Submission ID:"));
        txtSubId = new JTextField(6);
        txtSubId.setFont(MainDashboard.FONT_BODY);
        row.add(txtSubId);

        row.add(new JLabel("  Marks (0\u2013100):"));
        txtMarks = new JTextField(6);
        txtMarks.setFont(MainDashboard.FONT_BODY);
        row.add(txtMarks);

        row.add(new JLabel("  Feedback:"));
        txtFeedback = new JTextField(26);
        txtFeedback.setFont(MainDashboard.FONT_BODY);
        row.add(txtFeedback);

        // Row-click auto-fill
        table.getSelectionModel().addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int r = table.getSelectedRow();
                txtSubId.setText(submissionsModel.getValueAt(r, 0).toString());
                Object grade = submissionsModel.getValueAt(r, 5);
                txtMarks.setText("Not Graded".equals(grade) ? "" : grade.toString());
                Object fb = submissionsModel.getValueAt(r, 6);
                txtFeedback.setText("\u2014".equals(fb) ? "" : fb.toString());
            }
        });

        JButton btnSave = makeBtn("  \uD83D\uDCBE  Save Grade", new Color(39, 174, 96));
        btnSave.addActionListener(e -> {
            String subStr   = txtSubId.getText().trim();
            String markStr  = txtMarks.getText().trim();
            String feedback = txtFeedback.getText().trim();

            if (subStr.isEmpty() || markStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Submission ID and Marks are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int subId, marks;
            try {
                subId = Integer.parseInt(subStr);
                marks = Integer.parseInt(markStr);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Submission ID and Marks must be numbers.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (marks < 0 || marks > 100) {
                JOptionPane.showMessageDialog(this, "Marks must be between 0 and 100.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Connection conn = DBConnection.getConnection();
            if (!connOk(conn)) return;
            try {
                // Validate submission exists
                PreparedStatement chkSub = conn.prepareStatement("SELECT 1 FROM Submission WHERE submission_id = ?");
                chkSub.setInt(1, subId);
                if (!chkSub.executeQuery().next()) {
                    JOptionPane.showMessageDialog(this, "Submission ID " + subId + " does not exist.", "Not Found", JOptionPane.WARNING_MESSAGE);
                    conn.close(); return;
                }
                chkSub.close();

                // Upsert
                PreparedStatement chkGrade = conn.prepareStatement("SELECT grade_id FROM Grade WHERE submission_id = ?");
                chkGrade.setInt(1, subId);
                ResultSet rsG = chkGrade.executeQuery();
                if (rsG.next()) {
                    PreparedStatement upd = conn.prepareStatement(
                        "UPDATE Grade SET marks = ?, feedback = ? WHERE submission_id = ?");
                    upd.setInt(1, marks); upd.setString(2, feedback); upd.setInt(3, subId);
                    upd.executeUpdate(); upd.close();
                    JOptionPane.showMessageDialog(this, "Grade updated to " + marks + "!", "Updated \u2705", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO Grade (submission_id, marks, feedback) VALUES (?, ?, ?)");
                    ins.setInt(1, subId); ins.setInt(2, marks); ins.setString(3, feedback);
                    ins.executeUpdate(); ins.close();
                    JOptionPane.showMessageDialog(this, "Grade of " + marks + " saved!", "Graded \u2705", JOptionPane.INFORMATION_MESSAGE);
                }
                chkGrade.close();
                conn.close();

                txtSubId.setText(""); txtMarks.setText(""); txtFeedback.setText("");
                table.clearSelection();
                refreshSubmissions();   // instant table update
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        row.add(Box.createHorizontalStrut(8));
        row.add(btnSave);
        card.add(row, BorderLayout.CENTER);
        panel.add(card, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshSubmissions() {
        submissionsModel.setRowCount(0);
        Connection conn = DBConnection.getConnection();
        if (conn == null) return;
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT S.submission_id, ST.name, A.title, C.course_name, S.submission_date, " +
                "  COALESCE(CAST(G.marks AS TEXT), 'Not Graded'), COALESCE(G.feedback, '\u2014') " +
                "FROM Submission S " +
                "JOIN Student ST   ON S.student_id    = ST.student_id " +
                "JOIN Assignment A ON S.assignment_id = A.assignment_id " +
                "JOIN Course C     ON A.course_id     = C.course_id " +
                "LEFT JOIN Grade G ON G.submission_id = S.submission_id " +
                "WHERE C.instructor_id = " + INSTRUCTOR_ID + " ORDER BY S.submission_date DESC");
            while (rs.next())
                submissionsModel.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)});
            conn.close();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
