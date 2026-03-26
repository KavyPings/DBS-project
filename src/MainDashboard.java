import javax.swing.*;
import java.awt.*;

public class MainDashboard extends JFrame {
    
    private JPanel mainContainer;
    private CardLayout cardLayout;
    
    // UI Colors defined in design.txt
    public static final Color PRIMARY_BLUE = new Color(41, 128, 185); 
    public static final Color SECONDARY_GREY = new Color(236, 240, 241);
    public static final Color TEXT_DARK = new Color(44, 62, 80);

    public MainDashboard() {
        setTitle("Mini Course Management System");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Initialize Database
        DBConnection.initDatabase();

        // 1. Top Navigation Bar
        add(createTopNav(), BorderLayout.NORTH);

        // 2. Sidebar + 3. Main Dashboard Area
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        
        mainContainer.add(new StudentPanel(), "STUDENT");
        mainContainer.add(new InstructorPanel(), "INSTRUCTOR");
        // Default View
        cardLayout.show(mainContainer, "STUDENT");

        add(createSidebar(), BorderLayout.WEST);
        add(mainContainer, BorderLayout.CENTER);
    }
    
    private JPanel createTopNav() {
        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(PRIMARY_BLUE);
        nav.setPreferredSize(new Dimension(getWidth(), 60));
        
        JLabel title = new JLabel("  🎓 Educo Course Manager");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        rightPanel.setOpaque(false);
        JLabel userLabel = new JLabel("👤 Alice (Student) ");
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        rightPanel.add(userLabel);
        
        nav.add(title, BorderLayout.WEST);
        nav.add(rightPanel, BorderLayout.EAST);
        return nav;
    }
    
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(TEXT_DARK);
        sidebar.setPreferredSize(new Dimension(200, getHeight()));
        
        sidebar.add(Box.createRigidArea(new Dimension(0, 20)));
        
        JButton btnStudent = createSidebarButton("🎓 Student View");
        btnStudent.addActionListener(e -> cardLayout.show(mainContainer, "STUDENT"));
        
        JButton btnInstructor = createSidebarButton("👨‍🏫 Instructor View");
        btnInstructor.addActionListener(e -> cardLayout.show(mainContainer, "INSTRUCTOR"));
        
        sidebar.add(btnStudent);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(btnInstructor);
        
        return sidebar;
    }
    
    private JButton createSidebarButton(String text) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(180, 40));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setBackground(TEXT_DARK);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}
        
        SwingUtilities.invokeLater(() -> {
            new MainDashboard().setVisible(true);
        });
    }
}
