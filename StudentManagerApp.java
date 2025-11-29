import java.io.*;
import java.util.*;

// minimal comments

abstract class Person {
    protected String name;
    protected String email;
    public Person() {}
    public Person(String name, String email) { this.name = name; this.email = email; }
    public abstract void displayInfo();
}

class Student extends Person {
    private int rollNo;
    private String course;
    private double marks;
    private char grade;

    public Student() {}

    public Student(int rollNo, String name, String email, String course, double marks) {
        super(name, email);
        this.rollNo = rollNo;
        this.course = course;
        this.marks = marks;
        calculateGrade();
    }

    public void inputDetails(Scanner sc) throws InvalidInputException {
        try {
            System.out.print("Enter Roll No: ");
            String rline = sc.nextLine().trim();
            if (rline.isEmpty()) throw new InvalidInputException("Roll no empty");
            rollNo = Integer.parseInt(rline);

            System.out.print("Enter Name: ");
            name = sc.nextLine().trim();
            if (name.isEmpty()) throw new InvalidInputException("Name empty");

            System.out.print("Enter Email: ");
            email = sc.nextLine().trim();
            if (email.isEmpty()) throw new InvalidInputException("Email empty");

            System.out.print("Enter Course: ");
            course = sc.nextLine().trim();
            if (course.isEmpty()) throw new InvalidInputException("Course empty");

            System.out.print("Enter Marks: ");
            String mline = sc.nextLine().trim();
            if (mline.isEmpty()) throw new InvalidInputException("Marks empty");
            marks = Double.parseDouble(mline);
            if (marks < 0 || marks > 100) throw new InvalidInputException("Marks must be 0-100");

            calculateGrade();
        } catch (NumberFormatException ex) {
            throw new InvalidInputException("Invalid number format");
        }
    }

    private void calculateGrade() {
        if (marks >= 90) grade = 'A';
        else if (marks >= 75) grade = 'B';
        else if (marks >= 60) grade = 'C';
        else grade = 'D';
    }

    @Override
    public void displayInfo() {
        System.out.println("Roll No: " + rollNo);
        System.out.println("Name: " + name);
        System.out.println("Email: " + email);
        System.out.println("Course: " + course);
        System.out.println("Marks: " + marks);
        System.out.println("Grade: " + grade);
        System.out.println("-------------------------");
    }

    public int getRollNo() { return rollNo; }
    public double getMarks() { return marks; }
    public String getName() { return name; }

    public String toCSV() {
        return rollNo + "," + name + "," + email + "," + course + "," + marks;
    }

    public static Student fromCSV(String csv) throws Exception {
        String[] p = csv.split(",", -1);
        if (p.length != 5) throw new Exception("Invalid record");
        int r = Integer.parseInt(p[0]);
        String n = p[1];
        String e = p[2];
        String c = p[3];
        double m = Double.parseDouble(p[4]);
        return new Student(r, n, e, c, m);
    }
}

interface RecordActions {
    void addStudent(Student s) throws InvalidInputException;
    void deleteStudentByName(String name) throws StudentNotFoundException;
    void updateStudent(int rollNo, Student newData) throws StudentNotFoundException, InvalidInputException;
    Student searchByName(String name) throws StudentNotFoundException;
    List<Student> getAllStudents();
    void loadFromFile() throws IOException;
    void saveToFile() throws IOException;
}

class StudentNotFoundException extends Exception {
    public StudentNotFoundException(String msg) { super(msg); }
}

class InvalidInputException extends Exception {
    public InvalidInputException(String msg) { super(msg); }
}

class Loader implements Runnable {
    private String message;
    public Loader(String message) { this.message = message; }
    public void run() {
        try {
            System.out.print(message);
            for (int i = 0; i < 5; i++) {
                Thread.sleep(200);
                System.out.print(".");
            }
            System.out.println();
        } catch (InterruptedException e) {
            System.out.println("\nInterrupted.");
        }
    }
}

class StudentManager implements RecordActions {
    private final Map<Integer, Student> map = new HashMap<>();
    private final String filename;

    public StudentManager(String filename) {
        this.filename = filename;
    }

    public synchronized void addStudent(Student s) throws InvalidInputException {
        if (s == null) throw new InvalidInputException("Student null");
        Integer key = Integer.valueOf(s.getRollNo());
        if (map.containsKey(key)) throw new InvalidInputException("Duplicate roll no");
        Thread t = new Thread(new Loader("Adding student"));
        t.start();
        try { t.join(); } catch (InterruptedException e) {}
        map.put(key, s);
        System.out.println("Added.");
    }

    public synchronized void deleteStudentByName(String name) throws StudentNotFoundException {
        Iterator<Map.Entry<Integer, Student>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Student> en = it.next();
            if (en.getValue().getName().equalsIgnoreCase(name)) {
                it.remove();
                System.out.println("Student record deleted.");
                return;
            }
        }
        throw new StudentNotFoundException("No student with name: " + name);
    }

    public synchronized void updateStudent(int rollNo, Student newData) throws StudentNotFoundException, InvalidInputException {
        Integer key = Integer.valueOf(rollNo);
        if (!map.containsKey(key)) throw new StudentNotFoundException("Roll no not found");
        if (newData == null) throw new InvalidInputException("New data null");
        Thread t = new Thread(new Loader("Updating student"));
        t.start();
        try { t.join(); } catch (InterruptedException e) {}
        map.put(key, newData);
        System.out.println("Updated.");
    }

    public synchronized Student searchByName(String name) throws StudentNotFoundException {
        for (Student s : map.values()) {
            if (s.getName().equalsIgnoreCase(name)) return s;
        }
        throw new StudentNotFoundException("Student not found: " + name);
    }

    public synchronized List<Student> getAllStudents() {
        return new ArrayList<>(map.values());
    }

    public synchronized void loadFromFile() throws IOException {
        File f = new File(filename);
        if (!f.exists()) {
            f.createNewFile();
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    Student s = Student.fromCSV(line);
                    map.put(Integer.valueOf(s.getRollNo()), s);
                } catch (Exception ex) {
                    // skip bad record
                }
            }
        }
    }

    public synchronized void saveToFile() throws IOException {
        Thread t = new Thread(new Loader("Saving records"));
        t.start();
        try { t.join(); } catch (InterruptedException e) {}
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename, false))) {
            for (Student s : map.values()) {
                bw.write(s.toCSV());
                bw.newLine();
            }
        }
    }

    // demonstrate RandomAccessFile reading first record (if exists)
    public synchronized void randomAccessDemo() {
        try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
            if (raf.length() > 0) {
                raf.seek(0);
                String line = raf.readLine();
                if (line != null) {
                    System.out.println("RandomAccessFile read (first line): " + line);
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }
}

public class StudentManagerApp {
    private static final String FILE_NAME = "students.txt";
    private static final Scanner sc = new Scanner(System.in);

    private static void printMenu() {
        System.out.println("\n===== Capstone Student Menu =====");
        System.out.println("1. Add Student");
        System.out.println("2. View All Students");
        System.out.println("3. Search by Name");
        System.out.println("4. Delete by Name");
        System.out.println("5. Sort by Marks (desc)");
        System.out.println("6. Update Student by RollNo");
        System.out.println("7. Save and Exit");
        System.out.print("Enter choice: ");
    }

    public static void main(String[] args) {
        StudentManager manager = new StudentManager(FILE_NAME);
        try {
            manager.loadFromFile();
            System.out.println("Loaded students from file.");
        } catch (IOException e) {
            System.out.println("Could not load file.");
        }

        manager.randomAccessDemo();

        boolean running = true;
        while (running) {
            printMenu();
            String choiceLine = sc.nextLine().trim();
            int choice;
            try { choice = Integer.parseInt(choiceLine); } catch (NumberFormatException ex) { System.out.println("Enter number"); continue; }

            switch (choice) {
                case 1:
                    try {
                        Student s = new Student();
                        s.inputDetails(sc);
                        manager.addStudent(s);
                    } catch (InvalidInputException ex) {
                        System.out.println("Input error: " + ex.getMessage());
                    } catch (Exception ex) {
                        System.out.println("Error: " + ex.getMessage());
                    }
                    break;

                case 2:
                    List<Student> all = manager.getAllStudents();
                    if (all.isEmpty()) { System.out.println("No records."); break; }
                    Iterator<Student> it = all.iterator();
                    while (it.hasNext()) it.next().displayInfo();
                    break;

                case 3:
                    try {
                        System.out.print("Enter name to search: ");
                        String name = sc.nextLine().trim();
                        Student found = manager.searchByName(name);
                        System.out.println("Student Info:");
                        found.displayInfo();
                    } catch (StudentNotFoundException ex) {
                        System.out.println(ex.getMessage());
                    }
                    break;

                case 4:
                    try {
                        System.out.print("Enter name to delete: ");
                        String nameDel = sc.nextLine().trim();
                        manager.deleteStudentByName(nameDel);
                    } catch (StudentNotFoundException ex) {
                        System.out.println(ex.getMessage());
                    }
                    break;

                case 5:
                    List<Student> list = manager.getAllStudents();
                    if (list.isEmpty()) { System.out.println("No records."); break; }
                    list.sort(Comparator.comparingDouble(Student::getMarks).reversed());
                    System.out.println("Sorted Student List by Marks:");
                    Iterator<Student> it2 = list.iterator();
                    while (it2.hasNext()) it2.next().displayInfo();
                    break;

                case 6:
                    try {
                        System.out.print("Enter roll no to update: ");
                        String rline = sc.nextLine().trim();
                        int roll = Integer.parseInt(rline);
                        // get existing to show
                        Student existing = null;
                        for (Student s : manager.getAllStudents()) if (s.getRollNo() == roll) { existing = s; break; }
                        if (existing == null) throw new StudentNotFoundException("Roll not found");
                        System.out.println("Existing record:");
                        existing.displayInfo();
                        Student newS = new Student();
                        System.out.println("Enter new details:");
                        newS.inputDetails(sc);
                        manager.updateStudent(roll, newS);
                    } catch (NumberFormatException ex) {
                        System.out.println("Invalid roll no.");
                    } catch (StudentNotFoundException | InvalidInputException ex) {
                        System.out.println(ex.getMessage());
                    }
                    break;

                case 7:
                    try {
                        manager.saveToFile();
                        System.out.println("Saved and exiting.");
                    } catch (IOException e) {
                        System.out.println("Error saving: " + e.getMessage());
                    }
                    running = false;
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
        }

        sc.close();
    }
}
