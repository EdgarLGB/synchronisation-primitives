import com.typesafe.scalalogging.StrictLogging

import java.util.concurrent.atomic.AtomicReference

class QueuedSpinLock extends StrictLogging{
  private val next: AtomicReference[QueuedSpinLock] = new AtomicReference[QueuedSpinLock]()
  @volatile private var isLocked = false

  def lock(): QueuedSpinLock = {
    val localLock = new QueuedSpinLock
    val currentLockHolder = next.getAndSet(localLock) // Append a new spin lock at the end of the waiters queue
    if (currentLockHolder == null) {
      localLock.isLocked = true // Acquire the lock when there is no previous waiter
      logger.info(s"Thread ${Thread.currentThread().getId} acquired the lock")
    } else {
      currentLockHolder.next.set(localLock) // Append the new lock at the end of the last waiter
      logger.info(s"Thread ${Thread.currentThread().getId} is spinning")
      while (!localLock.isLocked) {}  // Spin on the locally-accessible flag
      logger.info(s"Having quited the spinning, thread ${Thread.currentThread().getId} acquired the lock")
    }
    localLock
  }

  def unlock(localLock: QueuedSpinLock): Unit = {
    // If the current thread is the only waiter, no need to wake up others
    if (next.compareAndSet(localLock, null)) {
      logger.info(s"No previous holders, thread ${Thread.currentThread().getId} released the lock")
      return
    }
    // Wake up the next waiter and disconnect itself
    localLock.next.get().isLocked = true
    localLock.next.set(null)
    logger.info(s"Thread ${Thread.currentThread().getId} released the lock")
  }

}