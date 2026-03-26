-- ==========================================================
-- 📘 Mini Course Management System Schema (MySQL / Oracle)
-- Use this file for your final academic report submission.
-- ==========================================================

-- 1. Create Tables (DDL)

CREATE TABLE Student (
    student_id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE Instructor (
    instructor_id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100)
);

CREATE TABLE Course (
    course_id INT PRIMARY KEY,
    course_name VARCHAR(200) NOT NULL,
    instructor_id INT,
    FOREIGN KEY (instructor_id) REFERENCES Instructor(instructor_id)
);

CREATE TABLE Enrollment (
    student_id INT,
    course_id INT,
    PRIMARY KEY (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES Student(student_id),
    FOREIGN KEY (course_id) REFERENCES Course(course_id)
);

CREATE TABLE Assignment (
    assignment_id INT PRIMARY KEY,
    course_id INT,
    title VARCHAR(200) NOT NULL,
    deadline DATE,
    FOREIGN KEY (course_id) REFERENCES Course(course_id)
);

CREATE TABLE Submission (
    submission_id INT PRIMARY KEY,
    assignment_id INT,
    student_id INT,
    submission_date DATE,
    FOREIGN KEY (assignment_id) REFERENCES Assignment(assignment_id),
    FOREIGN KEY (student_id) REFERENCES Student(student_id)
);

CREATE TABLE Grade (
    grade_id INT PRIMARY KEY,
    submission_id INT,
    marks INT CHECK (marks BETWEEN 0 AND 100),
    feedback VARCHAR(500),
    FOREIGN KEY (submission_id) REFERENCES Submission(submission_id)
);

CREATE TABLE Discussion (
    discussion_id INT PRIMARY KEY,
    student_id INT,
    instructor_id INT,
    message VARCHAR(1000) NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES Student(student_id),
    FOREIGN KEY (instructor_id) REFERENCES Instructor(instructor_id)
);

-- ==========================================================
-- 2. Insert Dummy Data (DML)
-- ==========================================================

INSERT INTO Student VALUES (1, 'Alice Smith', 'alice@edu.com');
INSERT INTO Student VALUES (2, 'Bob Johnson', 'bob@edu.com');

INSERT INTO Instructor VALUES (1, 'Dr. Alan Turing', 'Computer Science');
INSERT INTO Instructor VALUES (2, 'Dr. Grace Hopper', 'Mathematics');

INSERT INTO Course VALUES (101, 'Intro to Databases', 1);
INSERT INTO Course VALUES (102, 'Data Structures', 1);
INSERT INTO Course VALUES (103, 'Linear Algebra', 2);

INSERT INTO Enrollment VALUES (1, 101);
INSERT INTO Enrollment VALUES (1, 103);
INSERT INTO Enrollment VALUES (2, 101);

INSERT INTO Assignment VALUES (1001, 101, 'Normal Forms Assignment', '2026-05-15');
INSERT INTO Assignment VALUES (1002, 101, 'SQL Joins Practice', '2026-06-01');

INSERT INTO Submission VALUES (5001, 1001, 1, '2026-05-10');
INSERT INTO Submission VALUES (5002, 1001, 2, '2026-05-14');

INSERT INTO Grade VALUES (9001, 5001, 95, 'Excellent work Alice.');
INSERT INTO Grade VALUES (9002, 5002, 85, 'Watch out for 3NF.');

-- ==========================================================
-- 3. PL/SQL Procedures & Triggers 
-- Note: Oracle syntax
-- ==========================================================

-- Procedure: Add an enrollment record easily
/*
CREATE OR REPLACE PROCEDURE Enroll_Student (
    p_student_id IN INT,
    p_course_id IN INT
) AS
BEGIN
    INSERT INTO Enrollment (student_id, course_id)
    VALUES (p_student_id, p_course_id);
    COMMIT;
END;
*/

-- Trigger: Validate Marks to be <= 100
/*
CREATE OR REPLACE TRIGGER Check_Valid_Grade
BEFORE INSERT OR UPDATE ON Grade
FOR EACH ROW
BEGIN
    IF :NEW.marks > 100 OR :NEW.marks < 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Marks must be between 0 and 100.');
    END IF;
END;
*/
