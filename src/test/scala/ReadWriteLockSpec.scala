import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ReadWriteLockSpec extends AnyWordSpec with Matchers{
  class ReadWriteLockFixture {
    private val lock = new ReadWriteLock
    @volatile private var v = 0
    val loop = 10000

    def write(i: Int): Unit = {
      lock.acquireWriteLock()
      try {
        for (_ <- 1 to i) {
          v += 1
        }
        println(s"Incremented to $v")
      } finally {
        lock.releaseWriteLock()
      }
    }

    def read(): Int = {
      lock.acquireReadLock()
      try {
        v
      } finally {
        lock.releaseReadLock()
      }
    }
  }

  "ReadWriteLock" when {
    "having 1 writer and 1 reader" should {
      "not see intermediate changing value" in {
        val f = new ReadWriteLockFixture
        val writer = new Thread(() => {
          for (i <- 1 to f.loop) {
            f.write(i * 10)
          }
        })
        var result = Seq.empty[Int]
        val reader = new Thread(() => {
          while (writer.isAlive) {
            result.synchronized{
              result :+= f.read()
            }
          }
        })
        writer.start()
        reader.start()
        writer.join()
        reader.join()
        result.exists(_ % 10 != 0) should be(false)
      }
    }
  }
  "having 1 writer and 2 readers" should {
    "not see intermediate changing value" in {
      val f = new ReadWriteLockFixture
      val writer = new Thread(() => {
        for (i <- 1 to f.loop) {
          f.write(i * 10)
        }
      })
      var result = Seq.empty[Int]
      val reader = new Thread(() => {
        while (writer.isAlive) {
          result.synchronized{
            result :+= f.read()
          }
        }
      })
      val reader2 = new Thread(() => {
        while (writer.isAlive) {
          result.synchronized{
            result :+= f.read()
          }
        }
      })
      writer.start()
      reader.start()
      reader2.start()
      writer.join()
      reader.join()
      reader2.join()
      result.exists(_ % 10 != 0) should be(false)
    }
  }
  "having 2 writers and 1 reader" should {
    "not see intermediate changing value" in {
      val f = new ReadWriteLockFixture
      val writer = new Thread(() => {
        for (i <- 1 to f.loop) {
          f.write(i * 10)
        }
      })
      val writer2 = new Thread(() => {
        for (i <- 1 to f.loop) {
          f.write(i * 10)
        }
      })
      var result = Seq.empty[Int]
      val reader = new Thread(() => {
        while (writer.isAlive || writer2.isAlive) {
          result.synchronized{
            result :+= f.read()
          }
        }
      })
      writer.start()
      writer2.start()
      reader.start()
      writer.join()
      writer2.join()
      reader.join()
      result.exists(_ % 10 != 0) should be(false)
    }
  }
  "having 2 writers and 2 readers" should {
    "not see intermediate changing value" in {
      val f = new ReadWriteLockFixture
      val writer = new Thread(() => {
        for (i <- 1 to f.loop) {
          f.write(i * 10)
        }
      })
      val writer2 = new Thread(() => {
        for (i <- 1 to f.loop) {
          f.write(i * 10)
        }
      })
      var result = Seq.empty[Int]
      val reader = new Thread(() => {
        while (writer.isAlive || writer2.isAlive) {
          result.synchronized{
            result :+= f.read()
          }
        }
      })
      val reader2 = new Thread(() => {
        while (writer.isAlive || writer2.isAlive) {
          result.synchronized{
            result :+= f.read()
          }
        }
      })
      writer.start()
      writer2.start()
      reader.start()
      reader2.start()
      writer.join()
      writer2.join()
      reader.join()
      reader2.join()
      result.exists(_ % 10 != 0) should be(false)
    }
  }
}
