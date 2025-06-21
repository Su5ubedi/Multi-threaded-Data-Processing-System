# Go Data Processing System

A concurrent data processing system implemented in Go, demonstrating parallel task processing with goroutines, channels, and proper synchronization.

## Overview

This system simulates multiple worker goroutines processing tasks from a shared queue concurrently. It showcases Go's concurrency primitives including channels, goroutines, and sync mechanisms.

## Features

- **Concurrent Processing**: Multiple worker goroutines process tasks in parallel
- **Thread-Safe Queue**: Channel-based task distribution with no race conditions
- **Result Storage**: Mutex-protected storage saving results to both memory and file
- **Error Handling**: Comprehensive error checking with graceful failure handling
- **Logging**: Detailed logging of all operations, errors, and worker lifecycle
- **Graceful Shutdown**: Clean termination ensuring all workers complete properly

## Architecture

### Core Components

1. **SharedQueue**: Buffered channel-based task queue
2. **ResultStorage**: Thread-safe result storage with file I/O
3. **Worker**: Goroutines that process tasks from the queue
4. **DataProcessingSystem**: Main coordinator managing all components

### Concurrency Model

- **Channels**: Used for thread-safe task distribution
- **Goroutines**: Lightweight threads for concurrent task processing
- **Mutex**: Protects shared result storage from race conditions
- **WaitGroup**: Ensures proper worker synchronization and cleanup

## Getting Started

### Prerequisites

- Go 1.16 or higher

### Running the System

```bash
# Run the program
go run main.go

# Check the logs
tail -f system.log

# View results
cat results.txt
```

### Configuration

The system can be configured by modifying these parameters in `main()`:

```go
// Queue capacity, number of workers, output file
system := NewDataProcessingSystem(20, "results.txt")
system.Start(3) // Number of worker goroutines

// Number of tasks to process
for i := 1; i <= 10; i++ {
    // Add tasks
}
```

## Code Structure

```
main.go
├── Task struct                 # Work item definition
├── SharedQueue                 # Channel-based task queue
├── ResultStorage              # Thread-safe result storage
├── Worker function            # Goroutine task processor
├── DataProcessingSystem       # Main system coordinator
└── main function              # Entry point and demo
```

## Key Go Concepts Demonstrated

### Channels
```go
// Buffered channel acts as thread-safe queue
tasks: make(chan Task, capacity)

// Non-blocking send with select
select {
case q.tasks <- task:
    return nil
default:
    return fmt.Errorf("queue is full")
}
```

### Goroutines
```go
// Start worker goroutines
for i := 0; i < numWorkers; i++ {
    s.wg.Add(1)
    go Worker(name, queue, storage, &s.wg)
}
```

### Synchronization
```go
// Mutex for protecting shared resources
s.mutex.Lock()
defer s.mutex.Unlock()

// WaitGroup for coordinating goroutines
defer wg.Done()
s.wg.Wait()
```

### Error Handling
```go
// Error returns and checking
if err := storage.AddResult(result); err != nil {
    return fmt.Errorf("failed to save: %w", err)
}

// Panic recovery
defer func() {
    if r := recover(); r != nil {
        log.Printf("Panic recovered: %v", r)
    }
}()
```

## Output Files

- **`system.log`**: Detailed execution logs with timestamps
- **`results.txt`**: Processed task results

## Example Output

```
2025/06/21 15:58:11 Starting system with 3 workers
2025/06/21 15:58:11 Worker-1 started
2025/06/21 15:58:11 Added task: 1
2025/06/21 15:58:11 Retrieved task: 1
2025/06/21 15:58:11 Worker-1 processing task: 1
2025/06/21 15:58:11 Saved result: Worker-1 processed task 1: PROCESSED_DATA_1
2025/06/21 15:58:12 All tasks completed
2025/06/21 15:58:12 System shutdown complete
```

## Error Handling Strategy

The system implements Go's idiomatic error handling:

- **Error returns**: Functions return errors that are checked by callers
- **Defer statements**: Used for cleanup and panic recovery
- **Graceful degradation**: Individual task failures don't crash the system
- **Resource cleanup**: Files and goroutines are properly closed/terminated

## Performance Characteristics

- **Scalable**: Easy to adjust number of workers and queue size
- **Efficient**: Channel-based communication with minimal locking
- **Responsive**: Non-blocking operations where possible
- **Memory-safe**: No race conditions or memory leaks

## Customization

### Adding New Task Types

```go
type CustomTask struct {
    ID   int
    Data string
    Type string // Add new fields
}
```

### Custom Processing Logic

```go
func processTask(workerName string, task Task, storage *ResultStorage) error {
    // Add your custom processing logic here
    processedData := customProcessing(task.Data)
    // ... rest of function
}
```

### Different Storage Backends

```go
// Implement different storage mechanisms
type DatabaseStorage struct {
    // Database connection
}

func (d *DatabaseStorage) AddResult(result string) error {
    // Save to database instead of file
}
```