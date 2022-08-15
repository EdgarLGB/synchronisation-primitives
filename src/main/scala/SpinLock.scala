import java.util.concurrent.atomic.AtomicBoolean

class SpinLock {

  private val isLocked = new AtomicBoolean()

  def lock(): Unit = {
    while (!isLocked.compareAndSet(false, true)){}  // Unlimited busy-waiting
  }

  def unlock(): Unit = {
    isLocked.set(false)
  }
}
