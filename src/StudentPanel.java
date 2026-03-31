import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class StudentPanel extends JPanel {

    private static final int STUDENT_ID = 1; // Kavy Khilrani

    // ── Instance-field models so any method can refresh them ────────────────
    private final DefaultTableModel coursesModel;
    private final DefaultTableModel assignmentsModel;
    private final DefaultTableModel gradesModel;

    // Submit-tab state (needs refresh when tab is opened)
    private JComboBox<String> submitDropdown;
    private final java.util.LinkedHashMap<String, Integer> submitMap = new java.util.LinkedHashMap<>();

    // Enroll-in-course state (available courses not yet enrolled)
    private JComboBox<String> enrollDropdown;
    private final java.util.LinkedHashMap<String, Integer> enrollMap = new java.util.LinkedHashMap<>();

    // Cross-panel sync — wired by MainDashboard after both panels are constructed
    private InstructorPanel instructorPanel;

    public StudentPanel() {
        setLayout(new BorderLayout());
        setBackground(MainDashboard.SECONDARY_GREY);

        // ── Section Header ──────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(MainDashboard.CARD_WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, MainDashboard.BORDER_COLOR));
        header.setPreferredSize(new Dimension(0, 64));
        JLabel title = new JLabel("   \uD83C\uDF93  Student Dashboard");
        title.setFont(MainDashboard.FONT_TITLE);
        title.setForeground(MainDashboard.TEXT_DARK);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // ── Initialise models ───────────────────────────────────────────────
        coursesModel     = makeModel("Course ID", "Course Name", "Instructor", "Department");
        assignmentsModel = makeModel("Assignment ID", "Title", "Course", "Deadline", "Submitted?");
        gradesModel      = makeModel("Assignment", "Course", "Submitted On", "Marks / 100", "Feedback");

        // ── Build tabs ──────────────────────────────────────────────────────
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(MainDashboard.FONT_HEADER);
        tabs.setBackground(MainDashboard.SECONDARY_GREY);

        tabs.addTab("\uD83D\uDCDA  My Courses",         buildCoursesTab());
        tabs.addTab("\uD83D\uDCDD  Assignments",         buildAssignmentsTab());
        tabs.addTab("\uD83D\uDCE4  Submit Assignment",   buildSubmitTab());
        tabs.addTab("\uD83D\uDCCA  My Grades",           buildGradesTab());

        // ── ChangeListener: refresh the tab that's about to be shown ────────
        tabs.addChangeListener((ChangeEvent e) -> {
            switch (tabs.getSelectedIndex()) {
                case 0 -> refreshCourses();
                case 1 -> refreshAssignments();
                case 2 -> refreshSubmitDropdown();
                case 3 -> refreshGrades();
            }
        });

        // Load initial data for the default (first) tab
        refreshCourses();

        add(tabs, BorderLayout.CENTER);
    }

    // ── Model factory ────────────────────────────────────────────────────────
    private DefaultTableModel makeModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    /** Called by InstructorPanel after any DB mutation to keep student data in sync */
    public void refreshAllModels() {
        refreshCourses();           // also rebuilds the enroll dropdown
        refreshAssignments();
        refreshSubmitDropdown();
        refreshGrades();
    }

    /** Wired by MainDashboard once both panels are constructed */
    public void setInstructorPanel(InstructorPanel ip) { this.instructorPanel = ip; }

    // ── Styling helpers ──────────────────────────────────────────────────────
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
        th.setBackground(MainDashboard.PRIMARY_BLUE);
        th.setForeground(Color.WHITE);
        th.setPreferredSize(new Dimension(0, 38));
    }

    private JButton makeButton(String label, Color bg) {
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
        btn.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        btn.setPreferredSize(new Dimension(200, 44));
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tab builders
    // ══════════════════════════════════════════════════════════════════════════

    // ── Tab 1: My Courses ────────────────────────────────────────────────────
    private JPanel buildCoursesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(MainDashboard.SECONDARY_GREY);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTable table = new JTable(coursesModel);
        styleTable(table);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // ── Enroll in a new course card ────────────────────────────────────
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(MainDashboard.CARD_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MainDashboard.BORDER_COLOR),
            BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        JPanel enrollRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        enrollRow.setOpaque(false);

        JLabel enrollTitle = new JLabel("\uD83D\uDCDA  Enroll in a New Course");
        enrollTitle.setFont(MainDashboard.FONT_HEADER);
        enrollTitle.setForeground(MainDashboard.TEXT_DARK);
        enrollRow.add(enrollTitle);
        enrollRow.add(Box.createHorizontalStrut(12));

        enrollDropdown = new JComboBox<>();
        enrollDropdown.setFont(MainDashboard.FONT_BODY);
        enrollDropdown.setPreferredSize(new Dimension(400, 32));
        enrollRow.add(enrollDropdown);
        enrollRow.add(Box.createHorizontalStrut(10));

        JButton btnEnroll = makeButton("  \u2795  Enroll Now", MainDashboard.ACCENT_GREEN);
        btnEnroll.addActionListener(e -> {
            String selected = (String) enrollDropdown.getSelectedItem();
            if (selected == null || !enrollMap.containsKey(selected)) {
                JOptionPane.showMessageDialog(this,
                    "Please select an available course to enroll in.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int courseId = enrollMap.get(selected);
            Connection conn = DBConnection.getConnection();
            if (conn == null) return;
            try {
                PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO Enrollment (student_id, course_id) VALUES (?, ?)");
                pst.setInt(1, STUDENT_ID);
                pst.setInt(2, courseId);
                pst.executeUpdate();
                pst.close();
                conn.close();

                JOptionPane.showMessageDialog(this,
                    "Successfully enrolled in: " + selected.split(" \u2014 ")[0],
                    "Enrolled \u2705", JOptionPane.INFORMATION_MESSAGE);

                // Refresh all student views immediately
                refreshAllModels();

                // Also refresh the instructor-side tables so new
                // enrollment/submissions appear in Grade Submissions
                if (instructorPanel != null) instructorPanel.refreshInstructorViews();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        enrollRow.add(btnEnroll);
        card.add(enrollRow, BorderLayout.CENTER);
        panel.add(card, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshCourses() {
        coursesModel.setRowCount(0);
        Connection conn = DBConnection.getConnection();
        if (conn == null) return;
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT C.course_id, C.course_name, I.name, I.department " +
                "FROM Course C " +
                "JOIN Enrollment E ON C.course_id     = E.course_id " +
                "JOIN Instructor I ON C.instructor_id = I.instructor_id " +
                "WHERE E.student_id = " + STUDENT_ID + " ORDER BY C.course_name");
            while (rs.next())
                coursesModel.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)});
            conn.close();
        } catch (Exception e) { e.printStackTrace(); }
        refreshEnrollDropdown();
    }

    /** Repopulates the enroll dropdown with courses the student is NOT yet enrolled in */
    private void refreshEnrollDropdown() {
        if (enrollDropdown == null) return;
        enrollMap.clear();
        enrollDropdown.removeAllItems();
        Connection conn = DBConnection.getConnection();
        if (conn == null) return;
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT C.course_id, C.course_name, I.name, I.department " +
                "FROM Course C " +
                "JOIN Instructor I ON C.instructor_id = I.instructor_id " +
                "WHERE C.course_id NOT IN (" +
                "  SELECT course_id FROM Enrollment WHERE student_id = " + STUDENT_ID + ") " +
                "ORDER BY C.course_name");
            while (rs.next()) {
                String lbl = rs.getString(2)
                    + " \u2014 " + rs.getString(3)
                    + " (" + rs.getString(4) + ")";
                enrollMap.put(lbl, rs.getInt(1));
                enrollDropdown.addItem(lbl);
            }
            conn.close();
        } catch (Exception e) { e.printStackTrace(); }
        if (enrollDropdown.getItemCount() == 0)
            enrollDropdown.addItem("All available courses already enrolled \uD83C\uDF89");
    }

    // ── Tab 2: Assignments ───────────────────────────────────────────────────
    private JPanel buildAssignmentsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(MainDashboard.SECONDARY_GREY);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTable table = new JTable(assignmentsModel);
        styleTable(table);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void refreshAssignments() {
        assignmentsModel.setRowCount(0);
        Connection conn = DBConnection.getConnection();
        if (conn == null) return;
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT A.assignment_id, A.title, C.course_name, A.deadline, " +
                "  CASE WHEN S.submission_id IS NOT NULL THEN 'Yes \u2705' ELSE 'No \u274C' END " +
                "FROM Assignment A " +
                "JOIN Course C     ON A.course_id  = C.course_id " +
                "JOIN Enrollment E ON E.course_id  = C.course_id AND E.student_id = " + STUDENT_ID +
                " LEFT JOIN Submission S ON S.assignment_id = A.assignment_id AND S.student_id = " + STUDENT_ID +
                " ORDER BY A.deadline");
            while (rs.next())
                assignmentsModel.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5)});
            conn.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Tab 3: Submit Assignment ─────────────────────────────────────────────
    private JPanel buildSubmitTab() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(MainDashboard.SECONDARY_GREY);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(MainDashboard.CARD_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MainDashboard.BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(30, 40, 30, 40)));

        JLabel heading = new JLabel("\uD83D\uDCE4  Submit an Assignment");
        heading.setFont(MainDashboard.FONT_TITLE);
        heading.setForeground(MainDashboard.TEXT_DARK);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Select from your pending assignments and click Submit.");
        sub.setFont(MainDashboard.FONT_SMALL);
        sub.setForeground(Color.GRAY);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dropLabel = new JLabel("Choose Assignment:");
        dropLabel.setFont(MainDashboard.FONT_HEADER);
        dropLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        submitDropdown = new JComboBox<>();
        submitDropdown.setFont(MainDashboard.FONT_BODY);
        submitDropdown.setMaximumSize(new Dimension(600, 36));
        submitDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnSubmit = makeButton("  \u2705  Submit Assignment", MainDashboard.PRIMARY_BLUE);
        btnSubmit.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSubmit.addActionListener(e -> {
            String selected = (String) submitDropdown.getSelectedItem();
            if (selected == null || !submitMap.containsKey(selected)) {
                JOptionPane.showMessageDialog(this, "No valid assignment selected.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int assignmentId = submitMap.get(selected);
            Connection conn = DBConnection.getConnection();
            if (conn == null) return;
            try {
                PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO Submission (assignment_id, student_id, submission_date) VALUES (?, ?, date('now'))");
                pst.setInt(1, assignmentId);
                pst.setInt(2, STUDENT_ID);
                pst.executeUpdate();
                pst.close();
                conn.close();

                JOptionPane.showMessageDialog(this,
                    "Assignment submitted successfully!\n" + selected,
                    "Submitted \u2705", JOptionPane.INFORMATION_MESSAGE);

                // Immediately remove from dropdown + map
                submitDropdown.removeItem(selected);
                submitMap.remove(selected);

                // Also refresh the assignments table in the background
                refreshAssignments();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        card.add(heading);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(sub);
        card.add(Box.createRigidArea(new Dimension(0, 24)));
        card.add(dropLabel);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(submitDropdown);
        card.add(Box.createRigidArea(new Dimension(0, 24)));
        card.add(btnSubmit);

        outer.add(card);
        return outer;
    }

    /** Repopulate the submit dropdown from the DB (called every time the tab opens) */
    private void refreshSubmitDropdown() {
        submitMap.clear();
        submitDropdown.removeAllItems();

        Connection conn = DBConnection.getConnection();
        if (conn == null) return;
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT A.assignment_id, A.title, C.course_name, A.deadline " +
                "FROM Assignment A " +
                "JOIN Course C     ON A.course_id = C.course_id " +
                "JOIN Enrollment E ON E.course_id = C.course_id AND E.student_id = " + STUDENT_ID +
                " LEFT JOIN Submission S ON S.assignment_id = A.assignment_id AND S.student_id = " + STUDENT_ID +
                " WHERE S.submission_id IS NULL ORDER BY A.deadline");
            while (rs.next()) {
                String display = "[" + rs.getInt("assignment_id") + "] "
                    + rs.getString("title") + " \u2014 " + rs.getString("course_name")
                    + "  (Due: " + rs.getString("deadline") + ")";
                submitMap.put(display, rs.getInt("assignment_id"));
                submitDropdown.addItem(display);
            }
            conn.close();
        } catch (Exception e) { e.printStackTrace(); }

        if (submitDropdown.getItemCount() == 0)
            submitDropdown.addItem("No pending assignments \uD83C\uDF89");
    }

    // ── Tab 4: Grades ────────────────────────────────────────────────────────
    private JPanel buildGradesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(MainDashboard.SECONDARY_GREY);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTable table = new JTable(gradesModel);
        styleTable(table);
        table.getColumnModel().getColumn(4).setPreferredWidth(260);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel info = new JLabel("  Grades marked 'Pending' have not yet been evaluated by your instructor.");
        info.setFont(MainDashboard.FONT_SMALL);
        info.setForeground(Color.GRAY);
        panel.add(info, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshGrades() {
        gradesModel.setRowCount(0);
        Connection conn = DBConnection.getConnection();
        if (conn == null) return;
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT A.title, C.course_name, S.submission_date, " +
                "  CASE WHEN G.marks IS NOT NULL THEN CAST(G.marks AS TEXT) ELSE 'Pending' END, " +
                "  COALESCE(G.feedback, '\u2014') " +
                "FROM Submission S " +
                "JOIN Assignment A ON S.assignment_id = A.assignment_id " +
                "JOIN Course C     ON A.course_id     = C.course_id " +
                "LEFT JOIN Grade G ON G.submission_id = S.submission_id " +
                "WHERE S.student_id = " + STUDENT_ID +
                " ORDER BY S.submission_date DESC");
            while (rs.next())
                gradesModel.addRow(new Object[]{
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5)});
            conn.close();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
