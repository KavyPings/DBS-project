import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;

public class DBConnection {
    private static final String URL = "jdbc:sqlite:course_management.db";
    private static boolean driverLoaded = false;

    private static boolean loadDriver() {
        if (driverLoaded) return true;
        try {
            Class.forName("org.sqlite.JDBC");
            driverLoaded = true;
            return true;
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null,
                "<html><b>SQLite JDBC driver not found!</b><br><br>"
                + "Please run the application using <b>run.bat</b><br>"
                + "so that the required library in <b>lib\\sqlite-jdbc.jar</b> is loaded.<br><br>"
                + "<i>Do NOT launch directly from VS Code / IDE.</i></html>",
                "Missing Driver", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public static Connection getConnection() {
        if (!loadDriver()) return null;
        try {
            return DriverManager.getConnection(URL);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void initDatabase() {
        Connection conn = getConnection();
        if (conn == null) {
            System.err.println("Cannot initialize database — no connection.");
            return;
        }
        try (Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON");

            // Create tables
            stmt.execute("CREATE TABLE IF NOT EXISTS Student (student_id INTEGER PRIMARY KEY, name TEXT NOT NULL, email TEXT UNIQUE NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS Instructor (instructor_id INTEGER PRIMARY KEY, name TEXT NOT NULL, department TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS Course (course_id INTEGER PRIMARY KEY, course_name TEXT NOT NULL, instructor_id INTEGER, FOREIGN KEY(instructor_id) REFERENCES Instructor(instructor_id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS Enrollment (student_id INTEGER, course_id INTEGER, PRIMARY KEY (student_id, course_id), FOREIGN KEY(student_id) REFERENCES Student(student_id), FOREIGN KEY(course_id) REFERENCES Course(course_id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS Assignment (assignment_id INTEGER PRIMARY KEY AUTOINCREMENT, course_id INTEGER, title TEXT NOT NULL, deadline TEXT, FOREIGN KEY(course_id) REFERENCES Course(course_id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS Submission (submission_id INTEGER PRIMARY KEY AUTOINCREMENT, assignment_id INTEGER, student_id INTEGER, submission_date TEXT, FOREIGN KEY(assignment_id) REFERENCES Assignment(assignment_id), FOREIGN KEY(student_id) REFERENCES Student(student_id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS Grade (grade_id INTEGER PRIMARY KEY AUTOINCREMENT, submission_id INTEGER UNIQUE, marks INTEGER CHECK(marks BETWEEN 0 AND 100), feedback TEXT, FOREIGN KEY(submission_id) REFERENCES Submission(submission_id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS Discussion (discussion_id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, instructor_id INTEGER, message TEXT NOT NULL, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(student_id) REFERENCES Student(student_id), FOREIGN KEY(instructor_id) REFERENCES Instructor(instructor_id))");

            // Seed data only if empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM Student");
            if (rs.next() && rs.getInt("count") == 0) {
                stmt.execute("INSERT INTO Student VALUES (1, 'Kavy Khilrani', 'kavy@edu.com')");
                stmt.execute("INSERT INTO Student VALUES (2, 'Ruchi Pawar', 'ruchi@edu.com')");
                stmt.execute("INSERT INTO Student VALUES (3, 'Aarjica Talati', 'aarjica@edu.com')");

                // Updated instructor name
                stmt.execute("INSERT INTO Instructor VALUES (1, 'Dr. Jashma Suresh', 'Computer Science')");
                stmt.execute("INSERT INTO Instructor VALUES (2, 'Dr. Grace Hopper', 'Mathematics')");

                stmt.execute("INSERT INTO Course VALUES (101, 'Intro to Databases', 1)");
                stmt.execute("INSERT INTO Course VALUES (102, 'Data Structures', 1)");
                stmt.execute("INSERT INTO Course VALUES (103, 'Linear Algebra', 2)");
                stmt.execute("INSERT INTO Course VALUES (104, 'Operating Systems', 1)");

                stmt.execute("INSERT INTO Enrollment VALUES (1, 101)");
                stmt.execute("INSERT INTO Enrollment VALUES (1, 102)");
                stmt.execute("INSERT INTO Enrollment VALUES (1, 103)");
                stmt.execute("INSERT INTO Enrollment VALUES (2, 101)");
                stmt.execute("INSERT INTO Enrollment VALUES (3, 102)");

                stmt.execute("INSERT INTO Assignment (course_id, title, deadline) VALUES (101, 'ER Diagram Design', '2026-04-10')");
                stmt.execute("INSERT INTO Assignment (course_id, title, deadline) VALUES (101, 'Normal Forms Assignment', '2026-05-15')");
                stmt.execute("INSERT INTO Assignment (course_id, title, deadline) VALUES (101, 'SQL Joins Practice', '2026-06-01')");
                stmt.execute("INSERT INTO Assignment (course_id, title, deadline) VALUES (102, 'Linked List Implementation', '2026-04-20')");
                stmt.execute("INSERT INTO Assignment (course_id, title, deadline) VALUES (102, 'Binary Trees Quiz', '2026-05-25')");
                stmt.execute("INSERT INTO Assignment (course_id, title, deadline) VALUES (103, 'Matrix Operations', '2026-04-30')");

                stmt.execute("INSERT INTO Submission (assignment_id, student_id, submission_date) VALUES (1, 1, '2026-04-08')");
                stmt.execute("INSERT INTO Submission (assignment_id, student_id, submission_date) VALUES (2, 1, '2026-05-12')");
                stmt.execute("INSERT INTO Submission (assignment_id, student_id, submission_date) VALUES (4, 1, '2026-04-18')");
                stmt.execute("INSERT INTO Submission (assignment_id, student_id, submission_date) VALUES (1, 2, '2026-04-09')");

                stmt.execute("INSERT INTO Grade (submission_id, marks, feedback) VALUES (1, 88, 'Great ER Diagram! Minor notations to fix.')");
                stmt.execute("INSERT INTO Grade (submission_id, marks, feedback) VALUES (3, 92, 'Excellent Linked List implementation.')");

                System.out.println("Database initialized with sample data.");
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
