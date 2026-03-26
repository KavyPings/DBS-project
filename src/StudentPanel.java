import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;

public class StudentPanel extends JPanel {
    public StudentPanel() {
        setLayout(new BorderLayout());
        setBackground(MainDashboard.SECONDARY_GREY);
        
        // Title
        JLabel title = new JLabel("🎓 Student Dashboard", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(title, BorderLayout.NORTH);
        
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        tabs.addTab("My Courses", createCoursesPanel());
        tabs.addTab("Assignments & Submissions", createAssignmentsPanel());
        tabs.addTab("Grades", createGradesPanel());
        
        add(tabs, BorderLayout.CENTER);
    }
    
    private JPanel createCoursesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        String[] columns = {"Course ID", "Course Name", "Instructor"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        
        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
            // Get courses for Student ID = 1 (Alice)
            ResultSet rs = stmt.executeQuery(
                "SELECT C.course_id, C.course_name, I.name AS instructor_name " +
                "FROM Course C " +
                "JOIN Enrollment E ON C.course_id = E.course_id " +
                "JOIN Instructor I ON C.instructor_id = I.instructor_id " +
                "WHERE E.student_id = 1"
            );
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("course_id"),
                    rs.getString("course_name"),
                    rs.getString("instructor_name")
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createAssignmentsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        String[] columns = {"Assignment ID", "Title", "Course", "Deadline"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        
        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT A.assignment_id, A.title, C.course_name, A.deadline " +
                "FROM Assignment A JOIN Course C ON A.course_id = C.course_id"
            );
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("assignment_id"), rs.getString("title"), rs.getString("course_name"), rs.getString("deadline")
                });
            }
        } catch (Exception e) {}
        
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        
        // Submission Action Area
        JPanel submitPanel = new JPanel();
        submitPanel.add(new JLabel("Assignment ID:"));
        JTextField txtId = new JTextField(10);
        submitPanel.add(txtId);
        
        JButton btnSubmit = new JButton("Submit Assignment");
        btnSubmit.setBackground(MainDashboard.PRIMARY_BLUE);
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.addActionListener(e -> {
            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO Submission (submission_id, assignment_id, student_id, submission_date) VALUES (?, ?, ?, date('now'))"
                );
                // Simple random ID for submission
                int subId = (int)(Math.random() * 1000) + 100;
                stmt.setInt(1, subId);
                stmt.setInt(2, Integer.parseInt(txtId.getText()));
                stmt.setInt(3, 1); // Mock student ID
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Assignment Submitted successfully! Sub ID: " + subId);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });
        submitPanel.add(btnSubmit);
        panel.add(submitPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createGradesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        String[] columns = {"Submission ID", "Assignment", "Marks", "Feedback"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        
        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT S.submission_id, A.title, G.marks, G.feedback " +
                "FROM Submission S " +
                "JOIN Assignment A ON S.assignment_id = A.assignment_id " +
                "LEFT JOIN Grade G ON G.submission_id = S.submission_id " +
                "WHERE S.student_id = 1"
            );
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("submission_id"), rs.getString("title"), 
                    rs.getObject("marks") != null ? rs.getInt("marks") : "Not Graded",
                    rs.getString("feedback") != null ? rs.getString("feedback") : "-"
                });
            }
        } catch (Exception e) {}
        
        JTable table = new JTable(model);
        table.setRowHeight(30);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }
}
