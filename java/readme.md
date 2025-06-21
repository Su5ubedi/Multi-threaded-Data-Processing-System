# Java Data Processing System

A concurrent data processing system implemented in Java, demonstrating parallel task processing with ExecutorService, thread synchronization, and proper exception handling.

## Overview

This system simulates multiple worker threads processing tasks from a shared queue concurrently. It showcases Java's concurrency utilities including ExecutorService, CountDownLatch, ReentrantLock, and synchronized collections.

## Features

- **Concurrent Processing**: Multiple worker threads process tasks in parallel
- **Thread-Safe Queue**: ReentrantLock-protected task distribution with no race conditions
- **Result Storage**: Synchronized storage saving results to both memory and file
- **Exception Handling**: Comprehensive try-catch blocks for specific exception types
- **Logging**: Detailed logging with java.util.logging framework
- **Graceful Shutdown**: Clean termination ensuring all workers complete properly

## Architecture

### Core Components

1. **SharedQueue**: ReentrantLock-protected task queue
2. **ResultStorage**: Thread-safe result storage with file I/O
3. **WorkerThread**: Runnable threads that process tasks from the queue
4. **DataProcessingSystem**: Main coordinator managing all components

### Concurrency Model

- **ExecutorService**: Professional thread pool management
- **ReentrantLock**: Explicit locking for shared resources
- **CountDownLatch**: Task completion synchronization (Java's WaitGroup equivalent)
- **Collections.synchronizedList()**: Thread-safe collections

## Getting Started

### Prerequisites

- Java 8 or higher
- Any Java IDE or command line tools

### Running the System

```bash
# Navigate to java folder
cd java

# Compile the program
javac DataProcessingSystem.java

# Run the program
java DataProcessingSystem

# Check the logs
tail -f system.log

# View results
cat results.txt
```

### Configuration

The system can be configured by modifying these parameters in `main()`:

```java
// Number of workers, output file
system = new DataProcessingSystem(3, "results.txt");

// Number of tasks to process
system.addTasks(10); // Prepare for 10 tasks

// Add tasks in loop
for (int i = 1; i <= 10; i++) {
    system.addTask(new Task(i, "data_" + i));
}
```

## Code Structure

```
DataProcessingSystem.java
├── Task class                  # Work item definition
├── SharedQueue                 # ReentrantLock-protected task queue
├── ResultStorage              # Thread-safe result storage
├── WorkerThread               # Runnable task processor
├── DataProcessingSystem       # Main system coordinator
└── main method                # Entry point and demo
```

## Key Java Concepts Demonstrated

### ExecutorService
```java
// Create fixed thread pool
ExecutorService executor = Executors.newFixedThreadPool(numWorkers);

// Submit workers
executor.submit(worker);

// Graceful shutdown
executor.shutdown();
executor.awaitTermination(10, TimeUnit.SECONDS);
```

### ReentrantLock
```java
// Explicit locking for thread safety
private final ReentrantLock lock = new ReentrantLock();

lock.lock();
try {
    // Critical section
} finally {
    lock.unlock();
}
```

### CountDownLatch (Java's WaitGroup)
```java
// Initialize with task count
CountDownLatch latch = new CountDownLatch(10);

// Signal task completion
latch.countDown();

// Wait for all tasks
latch.await();
```

### Exception Handling
```java
// Specific exception types
try {
    processTask(task);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    break;
} catch (IOException e) {
    logger.severe("I/O error: " + e.getMessage());
    throw e;
}
```

### Thread Interruption
```java
// Proper interruption handling
catch (InterruptedException e) {
    logger.info("Thread interrupted");
    Thread.currentThread().interrupt(); // Restore interrupt status
    break;
}
```

## Output Files

- **`system.log`**: Detailed execution logs with timestamps and thread information
- **`results.txt`**: Processed task results from all workers

## Example Output

```
INFO: Starting system with 3 workers
INFO: Worker-1 started
INFO: Added task: 1
INFO: Retrieved task: 1
INFO: Worker-1 processing task: 1
INFO: Saved result: Worker-1 processed task 1: PROCESSED_DATA_1
INFO: Waiting for all tasks to complete...
INFO: All tasks completed
INFO: System shutdown complete
```

## Exception Handling Strategy

The system implements Java's comprehensive exception handling:

- **Checked Exceptions**: `IOException` for file operations, `InterruptedException` for thread operations
- **Try-catch-finally**: Proper resource cleanup with finally blocks
- **Exception Propagation**: Specific exception types thrown and caught appropriately
- **Thread Interruption**: Proper handling of thread interruption with status restoration

## Performance Characteristics

- **Scalable**: Easy to adjust number of workers and processing parameters
- **Efficient**: ReentrantLock provides better performance than synchronized blocks
- **Robust**: CountDownLatch ensures accurate task completion tracking
- **Professional**: Uses ExecutorService for proper thread lifecycle management