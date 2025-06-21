package main

import (
	"fmt"
	"log"
	"math/rand"
	"os"
	"strings"
	"sync"
	"time"
)

// Task represents work to be processed
type Task struct {
	ID   int
	Data string
}

// SharedQueue manages task distribution using channels
type SharedQueue struct {
	tasks chan Task
}

// NewSharedQueue creates a new shared queue
func NewSharedQueue(capacity int) *SharedQueue {
	return &SharedQueue{
		tasks: make(chan Task, capacity),
	}
}

// AddTask adds a task to the queue
func (q *SharedQueue) AddTask(task Task) error {
	select {
	case q.tasks <- task:
		log.Printf("Added task: %d", task.ID)
		return nil
	default:
		return fmt.Errorf("queue is full")
	}
}

// GetTask retrieves a task from the queue
func (q *SharedQueue) GetTask() (Task, bool) {
	task, ok := <-q.tasks
	if ok {
		log.Printf("Retrieved task: %d", task.ID)
	}
	return task, ok
}

// Close closes the queue
func (q *SharedQueue) Close() {
	close(q.tasks)
}

// ResultStorage manages saving results with thread safety
type ResultStorage struct {
	results []string
	mutex   sync.Mutex
	file    *os.File
}

// NewResultStorage creates new result storage
func NewResultStorage(filename string) (*ResultStorage, error) {
	file, err := os.OpenFile(filename, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0644)
	if err != nil {
		return nil, fmt.Errorf("failed to open file: %w", err)
	}

	return &ResultStorage{
		results: make([]string, 0),
		file:    file,
	}, nil
}

// AddResult adds a result to storage
func (s *ResultStorage) AddResult(result string) error {
	s.mutex.Lock()
	defer s.mutex.Unlock()

	// Add to memory
	s.results = append(s.results, result)

	// Write to file
	if s.file != nil {
		_, err := fmt.Fprintln(s.file, result)
		if err != nil {
			log.Printf("File I/O error: %v", err)
			return err
		}
	}

	log.Printf("Saved result: %s", result)
	return nil
}

// Close closes the file
func (s *ResultStorage) Close() error {
	s.mutex.Lock()
	defer s.mutex.Unlock()

	if s.file != nil {
		return s.file.Close()
	}
	return nil
}

// Worker processes tasks from the queue
func Worker(name string, queue *SharedQueue, storage *ResultStorage, taskWg *sync.WaitGroup, wg *sync.WaitGroup) {
	defer wg.Done()
	defer log.Printf("%s completed", name)

	log.Printf("%s started", name)

	for {
		task, ok := queue.GetTask()
		if !ok {
			// Channel closed, exit
			log.Printf("%s: queue closed, exiting", name)
			break
		}

		// Process the task
		err := processTask(name, task, storage)
		if err != nil {
			log.Printf("%s error processing task %d: %v", name, task.ID, err)
		}

		// Mark task as done
		taskWg.Done()
	}
}

// processTask simulates task processing
func processTask(workerName string, task Task, storage *ResultStorage) error {
	defer func() {
		if r := recover(); r != nil {
			log.Printf("%s panic during task %d: %v", workerName, task.ID, r)
		}
	}()

	log.Printf("%s processing task: %d", workerName, task.ID)

	// Simulate processing delay
	delay := time.Duration(200+rand.Intn(300)) * time.Millisecond
	time.Sleep(delay)

	// Process data
	processedData := "PROCESSED_" + strings.ToUpper(task.Data)
	result := fmt.Sprintf("%s processed task %d: %s", workerName, task.ID, processedData)

	// Save result
	return storage.AddResult(result)
}

// DataProcessingSystem manages the system
type DataProcessingSystem struct {
	queue   *SharedQueue
	storage *ResultStorage
	wg      sync.WaitGroup
	taskWg  sync.WaitGroup // Track individual tasks
}

// NewDataProcessingSystem creates a new system
func NewDataProcessingSystem(queueSize int, outputFile string) (*DataProcessingSystem, error) {
	queue := NewSharedQueue(queueSize)

	storage, err := NewResultStorage(outputFile)
	if err != nil {
		return nil, fmt.Errorf("failed to create storage: %w", err)
	}

	return &DataProcessingSystem{
		queue:   queue,
		storage: storage,
	}, nil
}

// AddTask adds a task to the system
func (s *DataProcessingSystem) AddTask(task Task) error {
	s.taskWg.Add(1) // Add to task counter
	return s.queue.AddTask(task)
}

// WaitForTasks waits for all tasks to complete
func (s *DataProcessingSystem) WaitForTasks() {
	log.Println("Waiting for all tasks to complete...")
	s.taskWg.Wait()
	log.Println("All tasks completed")
}

// Start begins processing with workers
func (s *DataProcessingSystem) Start(numWorkers int) {
	log.Printf("Starting system with %d workers", numWorkers)

	for i := 0; i < numWorkers; i++ {
		s.wg.Add(1)
		go Worker(fmt.Sprintf("Worker-%d", i+1), s.queue, s.storage, &s.taskWg, &s.wg)
	}
}

// Shutdown gracefully shuts down the system
func (s *DataProcessingSystem) Shutdown() {
	log.Println("Shutting down system")

	// Close queue to signal no more tasks
	s.queue.Close()

	// Wait for workers to complete
	s.wg.Wait()

	// Close storage
	if err := s.storage.Close(); err != nil {
		log.Printf("Error closing storage: %v", err)
	}

	log.Println("System shutdown complete")
}

func main() {
	// Setup logging
	logFile, err := os.OpenFile("system.log", os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0644)
	if err != nil {
		log.Printf("Failed to open log file: %v", err)
	} else {
		defer logFile.Close()
		log.SetOutput(logFile)
	}

	// Initialize system
	system, err := NewDataProcessingSystem(20, "results.txt")
	if err != nil {
		log.Fatalf("Failed to create system: %v", err)
	}

	// Start workers
	system.Start(3)

	// Add tasks
	rand.Seed(time.Now().UnixNano())
	for i := 1; i <= 10; i++ {
		task := Task{
			ID:   i,
			Data: fmt.Sprintf("data_%d", i),
		}

		if err := system.AddTask(task); err != nil {
			log.Printf("Error adding task %d: %v", i, err)
		}

		time.Sleep(50 * time.Millisecond)
	}

	// Wait for all tasks to complete
	system.WaitForTasks()

	// Shutdown
	system.Shutdown()
}