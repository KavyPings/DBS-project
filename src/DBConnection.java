import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

public class DBConnection {
    private static final String URL = "jdbc:sqlite:course_management.db";
    
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void initDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            
            // 1. Create Tables
            stmt.execute("CREATE TABLE IF NOT EXISTS Student (student_id INTEGER PRIMARY KEY, name TEXT NOT NULL, email TEXT UNIQUE NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS Instructor (instructor_id INTEGER PRIMARY KEY, name TEXT NOT NULL, department TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS Course (course_id INTEGER PRIMARY KEY, course_name TEXT NOT NULL, instructor_id INTEGER, FOREIGN KEY(instructor_id) REFERENCES Instructor(instructor_id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS Enrollment (student_id INTEGER, course_id INTEGER, PRIMARY KEY (student_id, course_id), FOREIGN KEY(student_id) REFERENCES Student(student_id), FOREIGN KEY(course_id) REFERENCES Course(course_id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS Assignment (assignment_id INTEGER PRIMARY KEY, course_id INTEGER, title TEXT NOT NULL, deadline TEXT, FOREIGN KEY(course_id) REFERENCES Course(course_id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS Submission (submission_id INTEGER PRIMARY KEY, assignment_id INTEGER, student_id INTEGER, submission_date TEXT, FOREIGN KEY(assignment_id) REFERENCES Assignment(assignment_id), FOREIGN KEY(student_id) REFERENCES Student(student_id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS Grade (grade_id INTEGER PRIMARY KEY, submission_id INTEGER, marks INTEGER CHECK(marks BETWEEN 0 AND 100), feedback TEXT, FOREIGN KEY(submission_id) REFERENCES Submission(submission_id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS Discussion (discussion_id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, instructor_id INTEGER, message TEXT NOT NULL, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(student_id) REFERENCES Student(student_id), FOREIGN KEY(instructor_id) REFERENCES Instructor(instructor_id))");
            
            // 2. Insert Dummy Data if Empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM Student");
            if (rs.next() && rs.getInt("count") == 0) {
                stmt.execute("INSERT INTO Student (student_id, name, email) VALUES (1, 'Alice Smith', 'alice@edu.com')");
                stmt.execute("INSERT INTO Student (student_id, name, email) VALUES (2, 'Bob Johnson', 'bob@edu.com')");
                
                stmt.execute("INSERT INTO Instructor (instructor_id, name, department) VALUES (1, 'Dr. Alan Turing', 'Computer Science')");
                
                stmt.execute("INSERT INTO Course (course_id, course_name, instructor_id) VALUES (101, 'Intro to Databases', 1)");
                stmt.execute("INSERT INTO Course (course_id, course_name, instructor_id) VALUES (102, 'Data Structures', 1)");
                
                stmt.execute("INSERT INTO Enrollment (student_id, course_id) VALUES (1, 101)");
                stmt.execute("INSERT INTO Enrollment (student_id, course_id) VALUES (1, 102)");
                
                stmt.execute("INSERT INTO Assignment (assignment_id, course_id, title, deadline) VALUES (1001, 101, 'Normal Forms Assignment', '2026-05-15')");
                
                System.out.println("Initialized database with dummy data.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
