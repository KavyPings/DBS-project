import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainDashboard extends JFrame {
    
    private JPanel mainContainer;
    private CardLayout cardLayout;
    private JLabel userLabel;
    private String currentView = "STUDENT";

    // ── Design Tokens (from design.txt) ──────────────────────────────────────
    public static final Color PRIMARY_BLUE    = new Color(41, 128, 185);
    public static final Color SIDEBAR_DARK    = new Color(30, 39, 46);
    public static final Color SIDEBAR_HOVER   = new Color(20, 108, 168);
    public static final Color SECONDARY_GREY  = new Color(245, 246, 250);
    public static final Color TEXT_DARK       = new Color(44, 62, 80);
    public static final Color ACCENT_GREEN    = new Color(39, 174, 96);
    public static final Color CARD_WHITE      = Color.WHITE;
    public static final Color BORDER_COLOR    = new Color(220, 227, 235);

    public static final Font FONT_TITLE  = new Font("SansSerif", Font.BOLD,   24);
    public static final Font FONT_HEADER = new Font("SansSerif", Font.BOLD,   15);
    public static final Font FONT_BODY   = new Font("SansSerif", Font.PLAIN,  13);
    public static final Font FONT_SMALL  = new Font("SansSerif", Font.PLAIN,  12);

    // Panels stored so we can rebuild them on switch
    private StudentPanel   studentPanel;
    private InstructorPanel instructorPanel;

    public MainDashboard() {
        setTitle("Mini Course Management System");
        setSize(1100, 680);
        setMinimumSize(new Dimension(900, 550));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Init DB
        DBConnection.initDatabase();

        // Build UI
        add(createTopNav(), BorderLayout.NORTH);

        cardLayout     = new CardLayout();
        mainContainer  = new JPanel(cardLayout);
        mainContainer.setBackground(SECONDARY_GREY);

        studentPanel    = new StudentPanel();
        instructorPanel = new InstructorPanel();
        mainContainer.add(studentPanel,    "STUDENT");
        mainContainer.add(instructorPanel, "INSTRUCTOR");
        cardLayout.show(mainContainer, "STUDENT");

        add(createSidebar(), BorderLayout.WEST);
        add(mainContainer,   BorderLayout.CENTER);
    }

    // ── Top Navigation Bar ───────────────────────────────────────────────────
    private JPanel createTopNav() {
        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(PRIMARY_BLUE);
        nav.setPreferredSize(new Dimension(getWidth(), 64));
        nav.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        JLabel title = new JLabel("🎓  Educo Course Manager");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        userLabel = new JLabel("👤  Kavy Khilrani  |  Student");
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        rightPanel.add(userLabel);
        nav.add(title,      BorderLayout.WEST);
        nav.add(rightPanel, BorderLayout.EAST);
        return nav;
    }

    // ── Sidebar ──────────────────────────────────────────────────────────────
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR_DARK);
        sidebar.setPreferredSize(new Dimension(210, getHeight()));

        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        JLabel navLabel = new JLabel("  NAVIGATION");
        navLabel.setForeground(new Color(120, 144, 156));
        navLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        navLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(navLabel);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));

        JButton btnStudent    = createSidebarButton("🎓   Student View",    "STUDENT");
        JButton btnInstructor = createSidebarButton("👨‍🏫   Instructor View", "INSTRUCTOR");

        sidebar.add(btnStudent);
        sidebar.add(Box.createRigidArea(new Dimension(0, 6)));
        sidebar.add(btnInstructor);
        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    private JButton createSidebarButton(String text, String viewKey) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(210, 46));
        btn.setPreferredSize(new Dimension(210, 46));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setBackground(SIDEBAR_DARK);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e)  { btn.setBackground(SIDEBAR_HOVER); }
            public void mouseExited(MouseEvent e)   { btn.setBackground(SIDEBAR_DARK); }
        });

        btn.addActionListener(e -> switchView(viewKey));
        return btn;
    }

    // ── View Switching (updates profile label too) ───────────────────────────
    private void switchView(String viewKey) {
        currentView = viewKey;
        cardLayout.show(mainContainer, viewKey);
        if ("STUDENT".equals(viewKey)) {
            userLabel.setText("👤  Kavy Khilrani  |  Student");
        } else {
            userLabel.setText("👨‍🏫  Dr. Jashma Suresh  |  Instructor");
        }
    }

    // ── Entry Point ──────────────────────────────────────────────────────────
    public static void main(String[] args) {
        // Force metal look to avoid Windows theme overriding button colours
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception e) {}

        SwingUtilities.invokeLater(() -> new MainDashboard().setVisible(true));
    }
}
