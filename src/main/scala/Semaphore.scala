import com.typesafe.scalalogging.StrictLogging

import java.util.concurrent.locks.LockSupport

class Semaphore(@volatile var count: Int) extends StrictLogging {

  private val lock = new SpinLock
  @volatile private var waiterList: Seq[Waiter] = Seq.empty

  def down(nanos: Long): Unit = {
    lock.lock()
    if (count > 0) {
      count -= 1
      logger.info(s"Thread ${Thread.currentThread().getId} acquired the lock")
      lock.unlock()
    } else {
      val localWaiter = Waiter(Thread.currentThread(), isUp = false)
      waiterList :+= localWaiter
      logger.info(s"Thread ${Thread.currentThread().getId} gets appended to the waiter list")
      lock.unlock()
      while (!localWaiter.isUp) {
        if (nanos == 0) {
          LockSupport.park()
        } else {
          val parkStartTime = System.nanoTime()
          LockSupport.parkNanos(nanos)
          if (System.nanoTime() - parkStartTime >= nanos) {
            logger.info(s"Thread ${Thread.currentThread().getId} timed out for lock acquirement")
            return
          }
        }

      }
      logger.info(s"Was waken up, thread ${Thread.currentThread().getId} acquired the lock")
    }
  }

  def up(): Unit = {
    lock.lock()
    if (waiterList.nonEmpty) {
      val waiter = waiterList.head
      waiter.isUp = true
      LockSupport.unpark(waiter.thread)
      waiterList = waiterList.drop(1)
      logger.info(s"Thread ${Thread.currentThread().getId} released the lock and woke up thread ${waiter.thread.getId}")
    } else {
      count += 1
      logger.info(s"Thread ${Thread.currentThread().getId} released the lock")
    }
    lock.unlock()
  }
}

case class Waiter(thread: Thread, @volatile var isUp: Boolean)