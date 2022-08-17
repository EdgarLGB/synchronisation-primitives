import com.typesafe.scalalogging.StrictLogging

import java.util.concurrent.locks.LockSupport

class ReadWriteLock extends StrictLogging {
  @volatile private var readers = 0
  @volatile private var writers = 0
  @volatile private var waiters = Seq.empty[ReadWriteWaiter]
  private val lock = new SpinLock

  def acquireReadLock(): Unit = {
    lock.lock()
    if (writers == 0) {
      readers += 1
      logger.info(s"${Thread.currentThread()} acquired read lock")
      lock.unlock()
    } else {
      val readWaiter = ReadWriteWaiter(Thread.currentThread(), isReader = true, isUp = false)
      waiters :+= readWaiter
      lock.unlock()
      logger.info(s"${Thread.currentThread()} waits for the read lock")
      while (!readWaiter.isUp) {
        LockSupport.park()
      }
      logger.info(s"${Thread.currentThread()} acquired read lock after woke up")
    }
  }

  def acquireWriteLock(): Unit = {
    lock.lock()
    if (writers == 0 && readers == 0) {
      writers += 1
      logger.info(s"${Thread.currentThread()} acquired write lock")
      lock.unlock()
    } else {
      val writeWaiter = ReadWriteWaiter(Thread.currentThread(), isReader = false, isUp = false)
      waiters :+= writeWaiter
      lock.unlock()
      logger.info(s"${Thread.currentThread()} waits for write lock")
      while (!writeWaiter.isUp) {
        LockSupport.park()
      }
      logger.info(s"${Thread.currentThread()} acquired write lock after woke up")
    }
  }

  def releaseReadLock(): Unit = {
    lock.lock()
    try {
      readers -= 1
      logger.info(s"${Thread.currentThread()} released read lock")
      if (readers == 0 && waiters.nonEmpty) {
        val writeWaiter = waiters.head
        writeWaiter.isUp = true
        LockSupport.unpark(writeWaiter.thread)
        logger.info(s"${Thread.currentThread()} woke up writer ${writeWaiter.thread}")
        waiters = waiters.drop(1)
        writers += 1
      }
    } finally {
      lock.unlock()
    }
  }

  def releaseWriteLock(): Unit = {
    lock.lock()
    var dropped = 0
    try {
      writers -= 1
      logger.info(s"${Thread.currentThread()} released write lock")
      if (waiters.nonEmpty) {
        var shouldExit = false
        for (i <- waiters.indices) {
          val readWriteWaiter = waiters(i)
          readWriteWaiter.isUp = true
          LockSupport.unpark(readWriteWaiter.thread)
          dropped += 1
          if (readWriteWaiter.isReader) {
            readers += 1
            logger.info(s"${Thread.currentThread()} woke up reader ${readWriteWaiter.thread}")
            if (i + 1 < waiters.size && !waiters(i + 1).isReader) {
              shouldExit = true // Wake up all sequential read waiters
            }
          } else {
            writers += 1
            logger.info(s"${Thread.currentThread()} woke up writer ${readWriteWaiter.thread}")
            shouldExit = true  // Stop waking up waiters when a writer is waken up
          }
          if (shouldExit) {
            return
          }
        }
      }
    } finally {
      waiters = waiters.drop(dropped)
      lock.unlock()
    }
  }
}

case class ReadWriteWaiter(thread: Thread, isReader: Boolean, @volatile var isUp: Boolean)
