import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.DoubleStream;

/* ---------------------------
   Security / User wrapper
   --------------------------- */
class User {
    private final String userId;
    private final Set<String> roles;

    public User(String userId, String... roles) {
        this.userId = Objects.requireNonNull(userId);
        this.roles = new HashSet<>(Arrays.asList(roles));
    }

    public boolean hasPermission(String perm) {
        // simple role-based permission model; expand as needed
        if (perm.equals("VIEW_PERSONAL")) {
            return roles.contains("ADMIN") || roles.contains("STAFF");
        }
        return false;
    }
}

/* ---------------------------
   Student with encapsulation
   --------------------------- */
class Student {
    private final String id;
    private String firstName;
    private String lastName;
    private String personalData; // sensitive (e.g., national ID, DOB, address)

    public Student(String id, String firstName, String lastName, String personalData) {
        this.id = Objects.requireNonNull(id);
        this.firstName = Objects.requireNonNull(firstName);
        this.lastName = Objects.requireNonNull(lastName);
        this.personalData = personalData;
    }

    public String getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }

    public void setFirstName(String fn) { this.firstName = fn; }
    public void setLastName(String ln) { this.lastName = ln; }

    // Sensitive accessor: checks permission
    public String getPersonalData(User currentUser) {
        if (currentUser != null && currentUser.hasPermission("VIEW_PERSONAL")) {
            return personalData;
        }
        throw new SecurityException("Insufficient permissions to view personal data");
    }

    @Override
    public String toString() {
        return String.format("%s: %s %s", id, firstName, lastName);
    }
}

/* ---------------------------
   Student Registry
   HashMap + LinkedList
   --------------------------- */
class StudentRegistry {
    private final Map<String, Student> studentTable = new HashMap<>();
    private final List<Student> studentList = new LinkedList<>();

    public synchronized void addStudent(Student s) {
        Objects.requireNonNull(s);
        if (studentTable.containsKey(s.getId())) {
            throw new IllegalArgumentException("Student already exists: " + s.getId());
        }
        studentTable.put(s.getId(), s);
        studentList.add(s);
    }

    public synchronized Student findStudent(String id) {
        return studentTable.get(id);
    }

    public synchronized List<Student> listAllStudents() {
        return Collections.unmodifiableList(new ArrayList<>(studentList));
    }

    public synchronized int size() {
        return studentTable.size();
    }
}

/* ---------------------------
   Course Scheduler
   ArrayDeque / Queue
   --------------------------- */
class CourseScheduler {
    private final Map<String, ArrayDeque<Student>> courseQueues = new HashMap<>();
    private final Map<String, Integer> courseCapacities = new HashMap<>();

    public synchronized void createCourse(String code, int capacity) {
        if (code == null || capacity < 0) throw new IllegalArgumentException("Invalid course or capacity");
        courseQueues.putIfAbsent(code, new ArrayDeque<>());
        courseCapacities.put(code, capacity);
    }

    public synchronized boolean registerStudent(String code, Student s) {
        Objects.requireNonNull(s);
        ArrayDeque<Student> queue = courseQueues.get(code);
        Integer cap = courseCapacities.get(code);
        if (queue == null || cap == null) throw new NoSuchElementException("Course not found: " + code);
        if (queue.size() < cap) {
            return queue.offer(s);
        }
        return false;
    }

    public synchronized Student dequeueNext(String code) {
        ArrayDeque<Student> queue = courseQueues.get(code);
        if (queue == null) throw new NoSuchElementException("Course not found: " + code);
        return queue.poll();
    }

    public synchronized int queuedCount(String code) {
        ArrayDeque<Student> queue = courseQueues.get(code);
        if (queue == null) throw new NoSuchElementException("Course not found: " + code);
        return queue.size();
    }
}

/* ---------------------------
   Fee Tracking
   TreeMap (NavigableMap)
   --------------------------- */
class FeeRecord {
    private final String paymentId;
    private final String studentId;
    private final double amount;
    private final long timestamp;

    public FeeRecord(String paymentId, String studentId, double amount, long timestamp) {
        this.paymentId = Objects.requireNonNull(paymentId);
        this.studentId = Objects.requireNonNull(studentId);
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public String getPaymentId() { return paymentId; }
    public String getStudentId() { return studentId; }
    public double getAmount() { return amount; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("%s | %s | %.2f | %d", paymentId, studentId, amount, timestamp);
    }
}

class FeeTracker {
    private final NavigableMap<String, FeeRecord> feeTree = new TreeMap<>();

    private String makeKey(long timestamp, String paymentId) {
        return String.format("%019d_%s", timestamp, paymentId);
    }

    public synchronized void recordPayment(String paymentId, String studentId, double amount, long timestamp) {
        if (amount < 0) throw new IllegalArgumentException("Amount cannot be negative");
        String key = makeKey(timestamp, paymentId);
        feeTree.put(key, new FeeRecord(paymentId, studentId, amount, timestamp));
    }

    public synchronized SortedMap<String, FeeRecord> getPaymentsInRange(long fromTime, long toTime) {
        String fromKey = makeKey(fromTime, "");
        String toKey = makeKey(toTime, Character.toString(Character.MAX_VALUE));
        return feeTree.subMap(fromKey, true, toKey, true);
    }

    public synchronized int count() { return feeTree.size(); }
}

/* ---------------------------
   Library System
   HashMap + Deque (stack)
   --------------------------- */
class Book {
    private final String isbn;
    private final String title;
    private boolean available;

    public Book(String isbn, String title) {
        this.isbn = Objects.requireNonNull(isbn);
        this.title = title;
        this.available = true;
    }

    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean v) { this.available = v; }

    @Override
    public String toString() {
        return String.format("%s - %s (%s)", isbn, title, available ? "available" : "borrowed");
    }
}

class BorrowRecord {
    private final String isbn;
    private final long borrowTime;
    private Long returnTime;

    public BorrowRecord(String isbn) {
        this.isbn = Objects.requireNonNull(isbn);
        this.borrowTime = System.currentTimeMillis();
    }

    public String getIsbn() { return isbn; }
    public long getBorrowTime() { return borrowTime; }
    public Long getReturnTime() { return returnTime; }
    public void markReturned() { this.returnTime = System.currentTimeMillis(); }

    @Override
    public String toString() {
        return String.format("Borrow[%s at %d, returned=%s]", isbn, borrowTime, returnTime == null ? "N" : returnTime.toString());
    }
}

class LibrarySystem {
    private final Map<String, Book> bookCatalog = new HashMap<>();
    private final Map<String, Deque<BorrowRecord>> studentBorrowHistory = new HashMap<>();

    public synchronized void addBook(Book b) {
        Objects.requireNonNull(b);
        bookCatalog.put(b.getIsbn(), b);
    }

    public synchronized Book findBook(String isbn) {
        return bookCatalog.get(isbn);
    }

    public synchronized boolean borrowBook(String studentId, String isbn) {
        Book book = bookCatalog.get(isbn);
        if (book == null) throw new NoSuchElementException("Book not found: " + isbn);
        if (!book.isAvailable()) return false;
        Deque<BorrowRecord> stack = studentBorrowHistory.computeIfAbsent(studentId, k -> new ArrayDeque<>());
        stack.push(new BorrowRecord(isbn)); // LIFO history
        book.setAvailable(false);
        return true;
    }

    public synchronized boolean returnBook(String studentId, String isbn) {
        Book book = bookCatalog.get(isbn);
        if (book == null) throw new NoSuchElementException("Book not found: " + isbn);
        Deque<BorrowRecord> stack = studentBorrowHistory.get(studentId);
        if (stack == null || stack.isEmpty()) {
            book.setAvailable(true);
            return true;
        }
        // find matching record (more robust than strict LIFO match)
        BorrowRecord found = null;
        for (BorrowRecord br : stack) {
            if (br.getIsbn().equals(isbn)) { found = br; break; }
        }
        if (found != null) {
            stack.remove(found);
            found.markReturned();
        }
        book.setAvailable(true);
        return true;
    }

    public synchronized List<BorrowRecord> getBorrowHistory(String studentId) {
        Deque<BorrowRecord> stack = studentBorrowHistory.get(studentId);
        if (stack == null) return Collections.emptyList();
        return new ArrayList<>(stack);
    }

    public synchronized Collection<Book> listAllBooks() {
        return Collections.unmodifiableCollection(bookCatalog.values());
    }
}

/* ---------------------------
   Performance Analytics
   2D array matrix + PriorityQueue
   Includes anonymized stats for a course
   --------------------------- */
class ClassStatistics {
    private final double average;
    private final double median;
    private final double stdDev;
    private final int count;

    public ClassStatistics(double average, double median, double stdDev, int count) {
        this.average = average;
        this.median = median;
        this.stdDev = stdDev;
        this.count = count;
    }

    public double getAverage() { return average; }
    public double getMedian() { return median; }
    public double getStdDev() { return stdDev; }
    public int getCount() { return count; }

    @Override
    public String toString() {
        return String.format("ClassStats{avg=%.2f, median=%.2f, stdDev=%.2f, n=%d}", average, median, stdDev, count);
    }
}

class StudentPerformance {
    private final String studentId;
    private final double average;

    public StudentPerformance(String studentId, double average) {
        this.studentId = studentId;
        this.average = average;
    }

    public String getStudentId() { return studentId; }
    public double getAverage() { return average; }

    @Override
    public String toString() {
        return String.format("%s -> %.2f", studentId, average);
    }
}

class PerformanceAnalyzer {
    // mapping studentId -> row index
    private final Map<String, Integer> studentIndex = new HashMap<>();
    private final List<String> indexToStudent = new ArrayList<>();
    private double[][] performanceMatrix; // rows = students, cols = assessments
    private final int columns; // number of assessments
    private final AtomicInteger nextIndex = new AtomicInteger(0);

    public PerformanceAnalyzer(int assessments) {
        if (assessments <= 0) throw new IllegalArgumentException("Assessments must be > 0");
        this.columns = assessments;
        this.performanceMatrix = new double[0][columns];
    }

    private synchronized void ensureCapacity(int rows) {
        if (performanceMatrix.length >= rows) return;
        int newSize = Math.max(rows, performanceMatrix.length * 2 + 1);
        double[][] newMat = new double[newSize][columns];
        for (int i = 0; i < performanceMatrix.length; i++) {
            System.arraycopy(performanceMatrix[i], 0, newMat[i], 0, columns);
        }
        performanceMatrix = newMat;
    }

    public synchronized void addStudent(String studentId) {
        if (studentIndex.containsKey(studentId)) return;
        int idx = nextIndex.getAndIncrement();
        ensureCapacity(idx + 1);
        studentIndex.put(studentId, idx);
        indexToStudent.add(studentId);
    }

    public synchronized void recordMark(String studentId, int assessmentIndex, double mark) {
        if (assessmentIndex < 0 || assessmentIndex >= columns) throw new IndexOutOfBoundsException("Invalid assessment index");
        if (!studentIndex.containsKey(studentId)) addStudent(studentId);
        int row = studentIndex.get(studentId);
        performanceMatrix[row][assessmentIndex] = mark;
    }

    public synchronized double getAverage(String studentId) {
        Integer idx = studentIndex.get(studentId);
        if (idx == null) throw new NoSuchElementException("Student not tracked: " + studentId);
        double sum = 0;
        for (int c = 0; c < columns; c++) sum += performanceMatrix[idx][c];
        return sum / columns;
    }

    // Top-K performers (returns studentId and average). O(n log k).
    public synchronized List<StudentPerformance> topK(int k) {
        if (k <= 0) return Collections.emptyList();
        PriorityQueue<StudentPerformance> minHeap = new PriorityQueue<>(Comparator.comparingDouble(StudentPerformance::getAverage));
        for (int i = 0; i < indexToStudent.size(); i++) {
            String sid = indexToStudent.get(i);
            double sum = 0;
            for (int c = 0; c < columns; c++) sum += performanceMatrix[i][c];
            double avg = sum / columns;
            StudentPerformance sp = new StudentPerformance(sid, avg);
            if (minHeap.size() < k) {
                minHeap.offer(sp);
            } else if (minHeap.peek().getAverage() < avg) {
                minHeap.poll();
                minHeap.offer(sp);
            }
        }
        List<StudentPerformance> out = new ArrayList<>(minHeap);
        out.sort(Comparator.comparingDouble(StudentPerformance::getAverage).reversed());
        return out;
    }

    // Anonymized class statistics for a course (course code -> list of student IDs is passed in)
    // Returns average, median, std dev, and count without exposing student identities.
    public synchronized ClassStatistics getAnonymizedClassStats(Collection<String> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) return new ClassStatistics(0, 0, 0, 0);
        List<Double> averages = new ArrayList<>();
        for (String sid : studentIds) {
            Integer idx = studentIndex.get(sid);
            if (idx == null) continue; // skip untracked
            double sum = 0;
            for (int c = 0; c < columns; c++) sum += performanceMatrix[idx][c];
            averages.add(sum / columns);
        }
        int n = averages.size();
        if (n == 0) return new ClassStatistics(0, 0, 0, 0);
        DoubleStream ds = averages.stream().mapToDouble(Double::doubleValue);
        double avg = ds.average().orElse(0.0);
        // median
        Collections.sort(averages);
        double median;
        if (n % 2 == 1) median = averages.get(n / 2);
        else median = (averages.get(n / 2 - 1) + averages.get(n / 2)) / 2.0;
        // std dev
        double variance = 0;
        for (double a : averages) variance += (a - avg) * (a - avg);
        variance /= n;
        double stdDev = Math.sqrt(variance);
        return new ClassStatistics(avg, median, stdDev, n);
    }
}

/* ---------------------------
   Central Controller (Singleton)
   --------------------------- */
class SchoolManagementSystem {
    private final StudentRegistry studentRegistry = new StudentRegistry();
    private final CourseScheduler courseScheduler = new CourseScheduler();
    private final FeeTracker feeTracker = new FeeTracker();
    private final LibrarySystem librarySystem = new LibrarySystem();
    private final PerformanceAnalyzer performanceAnalyzer;

    private static volatile SchoolManagementSystem instance;

    private SchoolManagementSystem(int assessments) {
        this.performanceAnalyzer = new PerformanceAnalyzer(assessments);
    }

    public static SchoolManagementSystem getInstance(int assessments) {
        if (instance == null) {
            synchronized (SchoolManagementSystem.class) {
                if (instance == null) {
                    instance = new SchoolManagementSystem(assessments);
                }
            }
        }
        return instance;
    }

    public StudentRegistry getStudentRegistry() { return studentRegistry; }
    public CourseScheduler getCourseScheduler() { return courseScheduler; }
    public FeeTracker getFeeTracker() { return feeTracker; }
    public LibrarySystem getLibrarySystem() { return librarySystem; }
    public PerformanceAnalyzer getPerformanceAnalyzer() { return performanceAnalyzer; }
}

/* ---------------------------
   Demo Main
   --------------------------- */
public class SchoolManagementSystemDemo {
    public static void main(String[] args) {
        SchoolManagementSystem sms = SchoolManagementSystem.getInstance(3); // 3 assessments

        // Users
        User admin = new User("u_admin", "ADMIN");
        User staff = new User("u_staff", "STAFF");
        User studentUser = new User("u_student", "STUDENT");

        // Create students
        Student a = new Student("S001", "Alice", "Mwangi", "DOB:1999-01-01;NID:1234");
        Student b = new Student("S002", "Brian", "Otieno", "DOB:2000-02-02;NID:2345");
        Student c = new Student("S003", "Catherine", "Kariuki", "DOB:1998-03-03;NID:3456");

        sms.getStudentRegistry().addStudent(a);
        sms.getStudentRegistry().addStudent(b);
        sms.getStudentRegistry().addStudent(c);

        // Safe personal data access
        try {
            System.out.println("Admin can view: " + a.getPersonalData(admin));
            System.out.println("Student user view attempt:");
            System.out.println(c.getPersonalData(studentUser)); // will throw
        } catch (SecurityException se) {
            System.out.println("SecurityException: " + se.getMessage());
        }

        // Courses
        sms.getCourseScheduler().createCourse("CS101", 2);
        sms.getCourseScheduler().registerStudent("CS101", a);
        sms.getCourseScheduler().registerStudent("CS101", b);
        boolean regC = sms.getCourseScheduler().registerStudent("CS101", c); // false, capacity full
        System.out.println("Registration for C succeeded? " + regC);

        // Fees
        sms.getFeeTracker().recordPayment("P100", a.getId(), 5000.0, System.currentTimeMillis());
        sms.getFeeTracker().recordPayment("P101", b.getId(), 4500.0, System.currentTimeMillis() + 1000);

        // Library
        Book book1 = new Book("978-001", "Intro to Java");
        sms.getLibrarySystem().addBook(book1);
        sms.getLibrarySystem().borrowBook(a.getId(), book1.getIsbn());

        // Performance: record marks
        PerformanceAnalyzer pa = sms.getPerformanceAnalyzer();
        pa.recordMark(a.getId(), 0, 80);
        pa.recordMark(a.getId(), 1, 90);
        pa.recordMark(a.getId(), 2, 85);

        pa.recordMark(b.getId(), 0, 70);
        pa.recordMark(b.getId(), 1, 75);
        pa.recordMark(b.getId(), 2, 72);

        pa.recordMark(c.getId(), 0, 95);
        pa.recordMark(c.getId(), 1, 92);
        pa.recordMark(c.getId(), 2, 94);

        // Top-K
        List<StudentPerformance> top2 = pa.topK(2);
        System.out.println("Top 2 performers:");
        top2.forEach(System.out::println);

        // Anonymized class stats for CS101 (pass only student IDs)
        Collection<String> cs101Students = Arrays.asList(a.getId(), b.getId(), c.getId());
        ClassStatistics stats= pa.getAnonymizedClassStats(cs101Students);
        System.out.println("CS101 anonymized stats:"+ stats);

        //Borrow history
        List<BorrowRecord> hist = sms.getLibrarySystem().getBorrowHistory(a.getId());
        System.out.println("Borrow History for " + a.getId() + ":" + hist);

        //Return Book
        sms.getLibrarySystem().returnBook(a.getId(),book1.getIsbn());
        System.out.println("Book after  return :"+ sms.getLibrarySystem().findBook(book1.getIsbn()));

    }

}

