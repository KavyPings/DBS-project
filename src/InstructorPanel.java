import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class InstructorPanel extends JPanel {
    public InstructorPanel() {
        setLayout(new BorderLayout());
        setBackground(MainDashboard.SECONDARY_GREY);
        
        // Title
        JLabel title = new JLabel("👨‍🏫 Instructor Dashboard", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(title, BorderLayout.NORTH);
        
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        tabs.addTab("Manage Courses", createCoursesPanel());
        tabs.addTab("Grade Submissions", createGradingPanel());
        
        add(tabs, BorderLayout.CENTER);
    }
    
    private JPanel createCoursesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        String[] columns = {"Course ID", "Course Name"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        
        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
            // Get courses for Instructor ID = 1
            ResultSet rs = stmt.executeQuery("SELECT course_id, course_name FROM Course WHERE instructor_id = 1");
            while (rs.next()) {
                model.addRow(new Object[]{ rs.getInt("course_id"), rs.getString("course_name") });
            }
        } catch (Exception e) {}
        
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        
        // Add course form
        JPanel form = new JPanel();
        form.add(new JLabel("Course ID:"));
        JTextField txtId = new JTextField(5);
        form.add(txtId);
        form.add(new JLabel("Name:"));
        JTextField txtName = new JTextField(15);
        form.add(txtName);
        
        JButton btnAdd = new JButton("Add Course");
        btnAdd.addActionListener(e -> {
            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("INSERT INTO Course (course_id, course_name, instructor_id) VALUES (?, ?, ?)");
                stmt.setInt(1, Integer.parseInt(txtId.getText()));
                stmt.setString(2, txtName.getText());
                stmt.setInt(3, 1); // Instructor 1
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Course Added!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage());
            }
        });
        form.add(btnAdd);
        panel.add(form, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createGradingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        String[] columns = {"Sub ID", "Assignment ID", "Student ID", "Date"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        
        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM Submission");
            while (rs.next()) {
                model.addRow(new Object[]{ rs.getInt("submission_id"), rs.getInt("assignment_id"), rs.getInt("student_id"), rs.getString("submission_date") });
            }
        } catch (Exception e) {}
        
        panel.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        
        // Grading Form
        JPanel form = new JPanel();
        form.add(new JLabel("Sub ID:"));
        JTextField txtId = new JTextField(5);
        form.add(txtId);
        form.add(new JLabel("Marks (0-100):"));
        JTextField txtMarks = new JTextField(5);
        form.add(txtMarks);
        form.add(new JLabel("Feedback:"));
        JTextField txtFeedback = new JTextField(15);
        form.add(txtFeedback);
        
        JButton btnGrade = new JButton("Save Grade");
        btnGrade.setBackground(Color.decode("#27ae60"));
        btnGrade.setForeground(Color.WHITE);
        btnGrade.addActionListener(e -> {
            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("INSERT INTO Grade (grade_id, submission_id, marks, feedback) VALUES (?, ?, ?, ?)");
                stmt.setInt(1, (int)(Math.random()*1000)+100);
                stmt.setInt(2, Integer.parseInt(txtId.getText()));
                stmt.setInt(3, Integer.parseInt(txtMarks.getText()));
                stmt.setString(4, txtFeedback.getText());
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Grade Saved!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });
        form.add(btnGrade);
        panel.add(form, BorderLayout.SOUTH);
        
        return panel;
    }
}
