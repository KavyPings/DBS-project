import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class InstructorPanel extends JPanel {

    private static final int INSTRUCTOR_ID = 1; // Dr. Jashma Suresh

    // Reference to StudentPanel so we can push updates to it immediately
    private final StudentPanel studentPanel;

    // ── Instance-field models ────────────────────────────────────────────────
    private final DefaultTableModel coursesModel;
    private final DefaultTableModel assignmentsModel;
    private final DefaultTableModel submissionsModel;

    // Table references (needed by delete buttons)
    private JTable coursesTable;
    private JTable assignmentsTable;
    private JTable submissionsTable;

    // Assignment-tab course dropdown
    private JComboBox<String> cmbCourse;
    private final java.util.LinkedHashMap<String, Integer> courseMap = new java.util.LinkedHashMap<>();

    // Grading-tab form fields
    private JTextField txtSubId, txtMarks, txtFeedback;

    public InstructorPanel(StudentPanel studentPanel) {
        this.studentPanel = studentPanel;
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

        refreshCourses();  // initial load
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

    /** Fully custom-painted button — LAF-independent, always shows text + colour correctly */
    private JButton makeBtn(String label, Color bg) {
        JButton btn = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setFont(getFont());
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth()  - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(bg.darker());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        btn.setPreferredSize(new Dimension(190, 44));
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

    /** After any DB mutation — refresh both instructor tables and all student data. */
    private void syncAll() {
        refreshCourses();
        refreshCourseDropdown();
        refreshAssignments();
        refreshSubmissions();
        studentPanel.refreshAllModels();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tab 1 — Manage Courses
    // ══════════════════════════════════════════════════════════════════════════

    private JPanel buildCoursesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setBackground(MainDashboard.SECONDARY_GREY);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        coursesTable = new JTable(coursesModel);
        styleTable(coursesTable);
        panel.add(new JScrollPane(coursesTable), BorderLayout.CENTER);

        // ── Bottom card ────────────────────────────────────────────────────
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(MainDashboard.CARD_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MainDashboard.BORDER_COLOR),
            BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        // Add row
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        addRow.setOpaque(false);
        JLabel addTitle = new JLabel("\u2795  Add New Course");
        addTitle.setFont(MainDashboard.FONT_HEADER);
        addTitle.setForeground(MainDashboard.TEXT_DARK);
        addRow.add(addTitle);
        addRow.add(Box.createHorizontalStrut(10));
        addRow.add(new JLabel("Course Name:"));
        JTextField txtCourseName = new JTextField(26);
        txtCourseName.setFont(MainDashboard.FONT_BODY);
        addRow.add(txtCourseName);

        JButton btnAdd = makeBtn("Add Course", MainDashboard.PRIMARY_BLUE);
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
                pst.setInt(1, newId); pst.setString(2, name); pst.setInt(3, INSTRUCTOR_ID);
                pst.executeUpdate(); pst.close();
                conn.close();
                txtCourseName.setText("");
                syncAll();
                JOptionPane.showMessageDialog(this,
                    "Course \"" + name + "\" added (ID: " + newId + ")", "Done \u2705", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        addRow.add(btnAdd);

        // Delete row
        JPanel delRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        delRow.setOpaque(false);
        JLabel delTitle = new JLabel("\uD83D\uDDD1\uFE0F  Delete Course");
        delTitle.setFont(MainDashboard.FONT_HEADER);
        delTitle.setForeground(new Color(192, 57, 43));
        delRow.add(delTitle);
        delRow.add(Box.createHorizontalStrut(10));
        delRow.add(new JLabel("Select a row above, then:"));

        JButton btnDelete = makeBtn("Delete Selected Course", new Color(192, 57, 43));
        btnDelete.addActionListener(e -> {
            int row = coursesTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select a course row to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int courseId   = (int) coursesModel.getValueAt(row, 0);
            String cname   = (String) coursesModel.getValueAt(row, 1);
            int confirm = JOptionPane.showConfirmDialog(this,
                "<html>Delete course <b>" + cname + "</b>?<br>"
                + "This will also remove all its assignments, submissions, grades and enrollments.</html>",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            Connection conn = DBConnection.getConnection();
            if (!connOk(conn)) return;
            try {
                // Manual cascade (reliable across all SQLite configs)
                PreparedStatement s1 = conn.prepareStatement(
                    "DELETE FROM Grade WHERE submission_id IN (" +
                    "  SELECT s.submission_id FROM Submission s" +
                    "  JOIN Assignment a ON s.assignment_id = a.assignment_id" +
                    "  WHERE a.course_id = ?)");
                s1.setInt(1, courseId); s1.executeUpdate(); s1.close();

                PreparedStatement s2 = conn.prepareStatement(
                    "DELETE FROM Submission WHERE assignment_id IN " +
                    "(SELECT assignment_id FROM Assignment WHERE course_id = ?)");
                s2.setInt(1, courseId); s2.executeUpdate(); s2.close();

                PreparedStatement s3 = conn.prepareStatement("DELETE FROM Assignment WHERE course_id = ?");
                s3.setInt(1, courseId); s3.executeUpdate(); s3.close();

                PreparedStatement s4 = conn.prepareStatement("DELETE FROM Enrollment WHERE course_id = ?");
                s4.setInt(1, courseId); s4.executeUpdate(); s4.close();

                PreparedStatement s5 = conn.prepareStatement("DELETE FROM Course WHERE course_id = ?");
                s5.setInt(1, courseId); s5.executeUpdate(); s5.close();

                conn.close();
                syncAll();
                JOptionPane.showMessageDialog(this,
                    "Course \"" + cname + "\" and all related data deleted.", "Deleted \uD83D\uDDD1\uFE0F", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        delRow.add(btnDelete);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        rows.add(addRow);
        rows.add(new JSeparator());
        rows.add(delRow);
        card.add(rows, BorderLayout.CENTER);
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
    // Tab 2 — Manage Assignments
    // ══════════════════════════════════════════════════════════════════════════

    private JPanel buildAssignmentsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setBackground(MainDashboard.SECONDARY_GREY);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        assignmentsTable = new JTable(assignmentsModel);
        styleTable(assignmentsTable);
        panel.add(new JScrollPane(assignmentsTable), BorderLayout.CENTER);

        // ── Bottom card ────────────────────────────────────────────────────
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(MainDashboard.CARD_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MainDashboard.BORDER_COLOR),
            BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        // Add row
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        addRow.setOpaque(false);
        JLabel addTitle = new JLabel("\u2795  Create New Assignment");
        addTitle.setFont(MainDashboard.FONT_HEADER);
        addTitle.setForeground(MainDashboard.TEXT_DARK);
        addRow.add(addTitle);
        addRow.add(Box.createHorizontalStrut(6));

        addRow.add(new JLabel("Course:"));
        cmbCourse = new JComboBox<>();
        cmbCourse.setFont(MainDashboard.FONT_BODY);
        addRow.add(cmbCourse);

        addRow.add(new JLabel("  Title:"));
        JTextField txtTitle = new JTextField(18);
        txtTitle.setFont(MainDashboard.FONT_BODY);
        addRow.add(txtTitle);

        addRow.add(new JLabel("  Deadline (YYYY-MM-DD):"));
        JTextField txtDeadline = new JTextField(10);
        txtDeadline.setFont(MainDashboard.FONT_BODY);
        addRow.add(txtDeadline);

        JButton btnAdd = makeBtn("Add Assignment", MainDashboard.ACCENT_GREEN);
        btnAdd.addActionListener(e -> {
            String courseLbl = (String) cmbCourse.getSelectedItem();
            String title     = txtTitle.getText().trim();
            String deadline  = txtDeadline.getText().trim();
            if (courseLbl == null || courseLbl.isEmpty() || title.isEmpty() || deadline.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!deadline.matches("\\d{4}-\\d{2}-\\d{2}")) {
                JOptionPane.showMessageDialog(this, "Deadline must be YYYY-MM-DD.", "Validation", JOptionPane.WARNING_MESSAGE);
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
                pst.setInt(1, courseId); pst.setString(2, title); pst.setString(3, deadline);
                pst.executeUpdate(); pst.close();
                conn.close();
                txtTitle.setText(""); txtDeadline.setText("");
                syncAll();
                JOptionPane.showMessageDialog(this,
                    "Assignment \"" + title + "\" created!", "Done \u2705", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        addRow.add(Box.createHorizontalStrut(6));
        addRow.add(btnAdd);

        // Delete row
        JPanel delRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        delRow.setOpaque(false);
        JLabel delTitle = new JLabel("\uD83D\uDDD1\uFE0F  Delete Assignment");
        delTitle.setFont(MainDashboard.FONT_HEADER);
        delTitle.setForeground(new Color(192, 57, 43));
        delRow.add(delTitle);
        delRow.add(Box.createHorizontalStrut(10));
        delRow.add(new JLabel("Select a row above, then:"));

        JButton btnDelete = makeBtn("Delete Selected Assignment", new Color(192, 57, 43));
        btnDelete.addActionListener(e -> {
            int row = assignmentsTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select an assignment row to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int asgId  = (int) assignmentsModel.getValueAt(row, 0);
            String asgName = (String) assignmentsModel.getValueAt(row, 1);
            int confirm = JOptionPane.showConfirmDialog(this,
                "<html>Delete assignment <b>" + asgName + "</b>?<br>"
                + "All existing submissions and grades for this assignment will be removed.</html>",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            Connection conn = DBConnection.getConnection();
            if (!connOk(conn)) return;
            try {
                PreparedStatement s1 = conn.prepareStatement(
                    "DELETE FROM Grade WHERE submission_id IN " +
                    "(SELECT submission_id FROM Submission WHERE assignment_id = ?)");
                s1.setInt(1, asgId); s1.executeUpdate(); s1.close();

                PreparedStatement s2 = conn.prepareStatement("DELETE FROM Submission WHERE assignment_id = ?");
                s2.setInt(1, asgId); s2.executeUpdate(); s2.close();

                PreparedStatement s3 = conn.prepareStatement("DELETE FROM Assignment WHERE assignment_id = ?");
                s3.setInt(1, asgId); s3.executeUpdate(); s3.close();

                conn.close();
                syncAll();
                JOptionPane.showMessageDialog(this,
                    "Assignment \"" + asgName + "\" deleted.", "Deleted \uD83D\uDDD1\uFE0F", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        delRow.add(btnDelete);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        rows.add(addRow);
        rows.add(new JSeparator());
        rows.add(delRow);
        card.add(rows, BorderLayout.CENTER);
        panel.add(card, BorderLayout.SOUTH);
        return panel;
    }

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
    // Tab 3 — Grade Submissions
    // ══════════════════════════════════════════════════════════════════════════

    private JPanel buildGradingTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setBackground(MainDashboard.SECONDARY_GREY);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        submissionsTable = new JTable(submissionsModel);
        styleTable(submissionsTable);
        submissionsTable.getColumnModel().getColumn(6).setPreferredWidth(220);
        panel.add(new JScrollPane(submissionsTable), BorderLayout.CENTER);

        // ── Bottom card ────────────────────────────────────────────────────
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(MainDashboard.CARD_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MainDashboard.BORDER_COLOR),
            BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        // Grade entry row
        JPanel gradeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        gradeRow.setOpaque(false);
        JLabel gradeTitle = new JLabel("\uD83D\uDD8A\uFE0F  Enter / Update Grade  (click a row to auto-fill)");
        gradeTitle.setFont(MainDashboard.FONT_HEADER);
        gradeTitle.setForeground(MainDashboard.TEXT_DARK);
        gradeRow.add(gradeTitle);
        gradeRow.add(Box.createHorizontalStrut(6));

        gradeRow.add(new JLabel("Sub ID:"));
        txtSubId = new JTextField(5);
        txtSubId.setFont(MainDashboard.FONT_BODY);
        gradeRow.add(txtSubId);

        gradeRow.add(new JLabel("  Marks (0\u2013100):"));
        txtMarks = new JTextField(5);
        txtMarks.setFont(MainDashboard.FONT_BODY);
        gradeRow.add(txtMarks);

        gradeRow.add(new JLabel("  Feedback:"));
        txtFeedback = new JTextField(22);
        txtFeedback.setFont(MainDashboard.FONT_BODY);
        gradeRow.add(txtFeedback);

        JButton btnSave = makeBtn("\uD83D\uDCBE  Save Grade", new Color(39, 174, 96));
        btnSave.addActionListener(e -> {
            String subStr = txtSubId.getText().trim(), markStr = txtMarks.getText().trim();
            String feedback = txtFeedback.getText().trim();
            if (subStr.isEmpty() || markStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Sub ID and Marks are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int subId, marks;
            try { subId = Integer.parseInt(subStr); marks = Integer.parseInt(markStr); }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Sub ID and Marks must be numbers.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (marks < 0 || marks > 100) {
                JOptionPane.showMessageDialog(this, "Marks must be 0\u2013100.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Connection conn = DBConnection.getConnection();
            if (!connOk(conn)) return;
            try {
                PreparedStatement chkSub = conn.prepareStatement("SELECT 1 FROM Submission WHERE submission_id = ?");
                chkSub.setInt(1, subId);
                if (!chkSub.executeQuery().next()) {
                    JOptionPane.showMessageDialog(this, "Submission ID " + subId + " does not exist.", "Not Found", JOptionPane.WARNING_MESSAGE);
                    conn.close(); return;
                }
                chkSub.close();

                PreparedStatement chkG = conn.prepareStatement("SELECT grade_id FROM Grade WHERE submission_id = ?");
                chkG.setInt(1, subId);
                ResultSet rsG = chkG.executeQuery();
                boolean isUpdate = rsG.next();
                if (isUpdate) {
                    PreparedStatement upd = conn.prepareStatement(
                        "UPDATE Grade SET marks = ?, feedback = ? WHERE submission_id = ?");
                    upd.setInt(1, marks); upd.setString(2, feedback); upd.setInt(3, subId);
                    upd.executeUpdate(); upd.close();
                } else {
                    PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO Grade (submission_id, marks, feedback) VALUES (?, ?, ?)");
                    ins.setInt(1, subId); ins.setInt(2, marks); ins.setString(3, feedback);
                    ins.executeUpdate(); ins.close();
                }
                chkG.close();
                conn.close();

                txtSubId.setText(""); txtMarks.setText(""); txtFeedback.setText("");
                submissionsTable.clearSelection();
                refreshSubmissions();
                submissionsTable.repaint();
                studentPanel.refreshAllModels();  // push grade update to student view

                JOptionPane.showMessageDialog(this,
                    isUpdate ? "Grade updated to " + marks + "!" : "Grade of " + marks + " saved!",
                    isUpdate ? "Updated \u2705" : "Graded \u2705", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        gradeRow.add(Box.createHorizontalStrut(6));
        gradeRow.add(btnSave);

        // Row-click auto-fill
        submissionsTable.getSelectionModel().addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting() && submissionsTable.getSelectedRow() >= 0) {
                int r = submissionsTable.getSelectedRow();
                txtSubId.setText(submissionsModel.getValueAt(r, 0).toString());
                Object grade = submissionsModel.getValueAt(r, 5);
                txtMarks.setText("Not Graded".equals(grade) ? "" : grade.toString());
                Object fb = submissionsModel.getValueAt(r, 6);
                txtFeedback.setText("\u2014".equals(fb) ? "" : fb.toString());
            }
        });

        // Remove grade row
        JPanel delRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        delRow.setOpaque(false);
        JLabel delTitle = new JLabel("\uD83D\uDDD1\uFE0F  Remove Grade");
        delTitle.setFont(MainDashboard.FONT_HEADER);
        delTitle.setForeground(new Color(192, 57, 43));
        delRow.add(delTitle);
        delRow.add(Box.createHorizontalStrut(10));
        delRow.add(new JLabel("Select a row above, then:"));

        JButton btnRemoveGrade = makeBtn("Remove Grade (keep submission)", new Color(192, 57, 43));
        btnRemoveGrade.addActionListener(e -> {
            int row = submissionsTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select a submission row.", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Object gradeVal = submissionsModel.getValueAt(row, 5);
            if ("Not Graded".equals(gradeVal)) {
                JOptionPane.showMessageDialog(this, "This submission has no grade to remove.", "Nothing to Remove", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int subId = (int) submissionsModel.getValueAt(row, 0);
            String student = (String) submissionsModel.getValueAt(row, 1);
            int confirm = JOptionPane.showConfirmDialog(this,
                "<html>Remove the grade for <b>" + student + "</b>'s submission (ID " + subId + ")?<br>"
                + "The submission itself will remain; only the grade record is deleted.</html>",
                "Confirm Remove", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            Connection conn = DBConnection.getConnection();
            if (!connOk(conn)) return;
            try {
                PreparedStatement pst = conn.prepareStatement("DELETE FROM Grade WHERE submission_id = ?");
                pst.setInt(1, subId); pst.executeUpdate(); pst.close();
                conn.close();

                submissionsTable.clearSelection();
                txtSubId.setText(""); txtMarks.setText(""); txtFeedback.setText("");
                refreshSubmissions();
                submissionsTable.repaint();
                studentPanel.refreshAllModels();

                JOptionPane.showMessageDialog(this,
                    "Grade removed. Submission is now marked 'Not Graded'.",
                    "Removed \u2705", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        delRow.add(btnRemoveGrade);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        rows.add(gradeRow);
        rows.add(new JSeparator());
        rows.add(delRow);
        card.add(rows, BorderLayout.CENTER);
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
