import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Random


class QueuedSpinLockSpec extends AnyWordSpec with Matchers {

  case class CriticalSection(var v: Int, mainLock: QueuedSpinLock) {
    def increment(): Unit ={
      val localLock = mainLock.lock()
      v += 1
      Thread.sleep(Random.between(0, 10))
      mainLock.unlock(localLock)
    }
  }

  "two threads" should {
    "sum up correctly" in {
      val times = 10000
      val criticalSection = CriticalSection(0, new QueuedSpinLock)
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
