# KeyLock

## Overview

**KeyLock** is a lightweight Kotlin utility for managing locks on numerical keys using PostgreSQL's 
built-in advisory lock mechanism. It ensures **mutual exclusion** for critical sections of code 
identified by key pairs (e.g., `(resourceType, keyId)`), 
while **reusing a single database connection** to minimize overhead.

This is ideal for cases where multiple applications, threads or services need to coordinate access to 
shared resources, like processing the same task, row, or entity.

---

## Features

- **Key-based locking** using `(resourceType, keyId)`
- **PostgreSQL advisory lock** under the hood (`pg_advisory_lock`)
- **Single connection reuse** across multiple locks
- **Automatic connection management**: opens on first lock, closes on last unlock
- **Thread-safe** and suitable for in-process coordination

---

## Example Usage

```kotlin
// Setup
val lockProvider = KeyLockConnProviderDb(dataSource, KeyLockManagerPgDao())
val lockManager = KeyLockManager(lockProvider)

// Run task with lock
val resourceTypeId = 1  
val resourceId = 101
val result = lockManager.runLocked(resourceTypeId, resourceId) {
    // critical section - only one thread/process at a time can run this block for key (1, 101)
    "ok"
}
```

---

## How It Works

- Acquires a PostgreSQL **advisory session-level lock** (`pg_try_advisory_lock`) for the given key.
- If the lock is already held by another session, the task is skipped or throws an exception (`LockedResourceException`).
- Automatically releases the lock after the task completes.
- Reuses the same DB connection across multiple locks for performance.
- Cleans up all held locks on the final release.

---

