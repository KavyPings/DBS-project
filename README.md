# Mini Course Management System

A database-driven application built using Java Swing and SQL for a college-level DBMS project. The application manages academic activities such as course registration, assignment handling, grading, and student-instructor communication.

## Features Built
- **Role-based Dashboards:** Dedicated panels and actions for Students and Instructors.
- **Course Management:** Instructors can create courses, and students can view them.
- **Assignment Handling:** Instructors assign tasks with deadlines; students can track and submit them.
- **Grading Pipeline:** Instructors can grade submissions and give feedback.
- **Discussions System:** A message board connected to database tracking.

## Technology Stack
- **Frontend:** Java Swing
- **Backend:** Java Database Connectivity (JDBC)
- **Database:** SQLite (Embedded DB for Zero-Setup runtime), SQL standard queries.

---

## 🚀 How to Run the Project Easily
The project has been configured with an embedded database, meaning **you do not need to install an Oracle or MySQL server on your computer** just to run this program.

### Prerequisites:
- Ensure you have **Java (JDK)** installed.
- To check if you have Java, open Command Prompt and type `java -version`. 

### Running on Windows:
Just double-click the **`run.bat`** file included in this folder. 
This batch file will automatically:
1. Download the required JDBC driver (if missing).
2. Compile all the `.java` files.
3. Launch the Application window.

### First Boot Note:
The system will automatically initialize a database file called `course_management.db` in this folder when it first runs. It will populate it with 5 sample dummy records to get you started immediately!

---

## 📝 For Your Final Report (`schema.sql`)
Although the runnable app defaults to a zero-configuration SQLite database, your PRD asks for MySQL / Oracle. You can test your pure Oracle/MySQL SQL statements using the `schema.sql` file included in this directory. Just copy and paste the contents into your MySQL Workbench or SQL Developer environment.
