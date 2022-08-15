import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import java.util.concurrent.locks.LockSupport
import scala.collection.mutable

class Semaphore(n: Int) {

  private val mutexCriticalSection: AtomicBoolean = new AtomicBoolean()
  private val semaphore: AtomicInteger = new AtomicInteger(n)
  private val mutexBlockedThreads: AtomicBoolean = new AtomicBoolean()
  @volatile private var blockedThread: mutable.Seq[Thread] = mutable.Seq.empty

  def up(): Unit = {
    while (mutexCriticalSection.getAndSet(true)) {
      // A thread has already entered the critical section
      // Put the current thread to sleep
    }
//    println(s"Thread ${Thread.currentThread()} has entered the critical section")

    if (semaphore.incrementAndGet() <= 0) {
//      while (mutexBlockedThreads.getAndSet(true)) {
//         Busy waiting
//      }
      val head = blockedThread.head
      blockedThread = blockedThread.drop(1)
//      mutexBlockedThreads.set(false)
      LockSupport.unpark(head)
      println(s"Unparked the thread $head")
    }

    mutexCriticalSection.set(false)
  }

  def downNanos(nanos: Long = 0): Unit = {
    while (mutexCriticalSection.getAndSet(true)) {}
//    println(s"Thread ${Thread.currentThread()} has entered the critical section")
    if (semaphore.decrementAndGet() < 0) {
//      while (mutexBlockedThreads.getAndSet(true)) {}
      println(s"Parked the current thread ${Thread.currentThread()}")
      blockedThread :+= Thread.currentThread()
//      mutexBlockedThreads.set(false)
      mutexCriticalSection.set(false)
      if (nanos > 0)
        LockSupport.parkNanos(nanos)
      else
        LockSupport.park()
    } else {
      mutexCriticalSection.set(false)
    }
  }
}
