import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec


class SpinLockSpec extends AnyWordSpec with Matchers {

  case class CriticalSection(var v: Int, lock: SpinLock) {
    def increment(): Unit ={
      lock.lock()
      v += 1
      lock.unlock()
    }
  }

  "two threads" should {
    "sum up to 20000 correctly" in {
      val times = 10000
      val criticalSection = CriticalSection(0, new SpinLock)
      val t1 = new Thread(() => {
        for (_ <- 1 to times) {
          criticalSection.increment()
        }
      })
      val t2 = new Thread(() => {
        for (_ <- 1 to times) {
          criticalSection.increment()
        }
      })
      t1.start()
      t2.start()
      t1.join()
      t2.join()
      criticalSection.v should equal(times * 2)
    }
  }
}
