import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.*;
import java.io.*;
import java.util.logging.*;

// Represents a unit of work to be processed
class Task {
    private final int id;
    private final String data;

    public Task(int id, String data) {
        this.id = id;
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public String getData() {
        return data;
    }
}

// Thread-safe queue for distributing tasks to workers
class SharedQueue {
    private final Queue<Task> queue = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private static final Logger logger = Logger.getLogger(SharedQueue.class.getName());

    public void addTask(Task task) {
        lock.lock();
        try {
            queue.offer(task);
            logger.info("Added task: " + task.getId());
        } finally {
            lock.unlock();
        }
    }

    public Task getTask() {
        lock.lock();
        try {
            Task task = queue.poll();
            if (task != null) {
                logger.info("Retrieved task: " + task.getId());
            }
            return task;
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        lock.lock();
        try {
            return queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }
}

// Thread-safe storage for processing results
class ResultStorage {
    private final List<String> results = Collections.synchronizedList(new ArrayList<>());
    private final PrintWriter fileWriter;
    private final ReentrantLock fileLock = new ReentrantLock();
    private static final Logger logger = Logger.getLogger(ResultStorage.class.getName());

    public ResultStorage(String filename) throws IOException {
        try {
            File file = new File(filename);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            this.fileWriter = new PrintWriter(new FileWriter(filename, true));
        } catch (IOException e) {
            logger.severe("Failed to create file writer: " + e.getMessage());
            throw e;
        }
    }

    public void addResult(String result) throws IOException {
        // Add to memory
        results.add(result);

        // Write to file with synchronization
        fileLock.lock();
        try {
            fileWriter.println(result);
            fileWriter.flush();
            logger.info("Saved result: " + result);
        } catch (Exception e) {
            logger.severe("File I/O error: " + e.getMessage());
            throw new IOException("Failed to write to file", e);
        } finally {
            fileLock.unlock();
        }
    }

    public void close() {
        fileLock.lock();
        try {
            if (fileWriter != null) {
                fileWriter.close();
            }
        } finally {
            fileLock.unlock();
        }
    }
}

// Worker thread that processes tasks from the shared queue
class WorkerThread implements Runnable {
    private final String name;
    private final SharedQueue queue;
    private final ResultStorage storage;
    private static final Logger logger = Logger.getLogger(WorkerThread.class.getName());
    private volatile boolean running = true;
    private CountDownLatch taskLatch; // Reference to current task latch

    public WorkerThread(String name, SharedQueue queue, ResultStorage storage) {
        this.name = name;
        this.queue = queue;
        this.storage = storage;
    }

    public void setTaskLatch(CountDownLatch taskLatch) {
        this.taskLatch = taskLatch;
    }

    @Override
    public void run() {
        logger.info(name + " started");

        try {
            while (running) {
                Task task = queue.getTask();

                if (task == null) {
                    if (queue.isEmpty()) {
                        Thread.sleep(100); // Wait before checking again
                        continue;
                    }
                    continue;
                }

                try {
                    processTask(task);
                    // Signal that a task is completed (like Go's WaitGroup.Done())
                    if (taskLatch != null) {
                        taskLatch.countDown();
                    }
                } catch (InterruptedException e) {
                    logger.info(name + " interrupted");
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    logger.severe(name + " I/O error processing task " + task.getId() + ": " + e.getMessage());
                    // Still count down even on error to avoid hanging
                    if (taskLatch != null) {
                        taskLatch.countDown();
                    }
                } catch (Exception e) {
                    logger.severe(name + " error processing task " + task.getId() + ": " + e.getMessage());
                    // Still count down even on error to avoid hanging
                    if (taskLatch != null) {
                        taskLatch.countDown();
                    }
                }
            }
        } catch (InterruptedException e) {
            logger.info(name + " was interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.severe(name + " fatal error: " + e.getMessage());
        } finally {
            logger.info(name + " completed");
        }
    }

    // Simulates actual work being done on a task
    private void processTask(Task task) throws InterruptedException, IOException {
        logger.info(name + " processing task: " + task.getId());

        try {
            // Simulate processing delay
            Thread.sleep(200 + (int) (Math.random() * 300));

            // Process data
            String processedData = "PROCESSED_" + task.getData().toUpperCase();
            String result = name + " processed task " + task.getId() + ": " + processedData;

            storage.addResult(result);

        } catch (InterruptedException e) {
            logger.warning(name + " interrupted while processing task: " + task.getId());
            throw e;
        } catch (IOException e) {
            logger.severe(name + " failed to save result for task " + task.getId());
            throw e;
        }
    }

    public void stop() {
        running = false;
    }
}

// Main system that coordinates queue, workers, and result storage
public class DataProcessingSystem {
    private static final Logger logger = Logger.getLogger(DataProcessingSystem.class.getName());
    private final SharedQueue queue;
    private final ResultStorage storage;
    private final ExecutorService executor;
    private final List<WorkerThread> workers;
    private CountDownLatch currentTaskLatch = new CountDownLatch(0);

    public DataProcessingSystem(int numWorkers, String outputFile) throws IOException {
        this.queue = new SharedQueue();
        this.storage = new ResultStorage(outputFile);
        this.executor = Executors.newFixedThreadPool(numWorkers);
        this.workers = new ArrayList<>();

        // Create worker threads
        for (int i = 0; i < numWorkers; i++) {
            WorkerThread worker = new WorkerThread("Worker-" + (i + 1), queue, storage);
            workers.add(worker);
        }

        setupLogging();
    }

    private void setupLogging() {
        try {
            FileHandler handler = new FileHandler("system.log", true);
            handler.setFormatter(new SimpleFormatter());
            Logger.getLogger("").addHandler(handler);
        } catch (IOException e) {
            System.err.println("Failed to setup logging: " + e.getMessage());
        }
    }

    // Prepare for a specific number of tasks (like Go's WaitGroup.Add())
    public void addTasks(int numberOfTasks) {
        // Create new latch for this batch of tasks
        currentTaskLatch = new CountDownLatch(numberOfTasks);

        // Update workers to use this latch
        for (WorkerThread worker : workers) {
            worker.setTaskLatch(currentTaskLatch);
        }

        logger.info("Prepared to wait for " + numberOfTasks + " tasks");
    }

    public void addTask(Task task) {
        queue.addTask(task);
    }

    // Wait for all tasks to complete (like Go's WaitGroup.Wait())
    public void waitForTasks() throws InterruptedException {
        logger.info("Waiting for all tasks to complete...");
        currentTaskLatch.await();
        logger.info("All tasks completed");
    }

    public void start() {
        logger.info("Starting system with " + workers.size() + " workers");
        for (WorkerThread worker : workers) {
            executor.submit(worker);
        }
    }

    // Gracefully shutdown all components
    public void shutdown() {
        logger.info("Shutting down system");

        try {
            // Signal workers to stop
            for (WorkerThread worker : workers) {
                worker.stop();
            }

            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warning("Forcing shutdown");
                executor.shutdownNow();
            }

            storage.close();
            logger.info("System shutdown complete");

        } catch (InterruptedException e) {
            logger.severe("Shutdown interrupted");
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        DataProcessingSystem system = null;

        try {
            // Initialize with 2 workers
            system = new DataProcessingSystem(2, "results.txt");
            system.start();

            // Prepare for 10 tasks (like Go's WaitGroup.Add())
            system.addTasks(4);

            // Add sample tasks
            for (int i = 1; i <= 4; i++) {
                system.addTask(new Task(i, "data_" + i));
                Thread.sleep(50);
            }

            // Wait for all tasks to complete (like Go's WaitGroup.Wait())
            system.waitForTasks();

        } catch (Exception e) {
            logger.severe("System error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (system != null) {
                system.shutdown();
            }
        }
    }
}