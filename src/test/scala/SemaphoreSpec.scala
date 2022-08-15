import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.mutable

class SemaphoreSpec extends AnyWordSpec with Matchers {

  class MyMonitor {
    private val mutex = new Semaphore(1)
    private val empty = new Semaphore(2)
    private val full = new Semaphore(0)
    @volatile private var items = mutable.Seq.empty[Int]

    def produce(i: Int): Unit = {
      empty.downNanos()
      mutex.downNanos()
      items :+= i
//      println(s"Produced $i")
      mutex.up()
      full.up()
    }

    def consume(): Option[Int] = {
      try {
        full.downNanos(1 * Math.pow(10, 9).toLong)
        mutex.downNanos()
        if (items.isEmpty) {
          None
        } else {
          val result = items.head
          items = items.drop(1)
//          println(s"${Thread.currentThread()}: Consumed $result")
          Some(result)
        }
      } finally {
        mutex.up()
        empty.up()
      }
    }
  }

  def binarySemaphoreFixture = new {
    val mutex = new Semaphore(1)
    var i = 0
    val n = 10000
    var result = mutable.Seq.empty[Int]
  }
  "MySemaphore" when {
    "having 1 consumer and 1 producer" should {
      "work" in {
        val m = new MyMonitor
        val n = 10000
        val producer = new Thread(() => {
          for (i <- 1 to n) {
            m.produce(i)
          }
        }, "producer")
        var result = mutable.Seq.empty[Int]
        val consumer = new Thread(() => {
          for (_ <- 1 to n) {
            m.consume() match {
              case Some(i) =>
                result :+= i
              case None =>
            }
          }
        }, "consumer")
        producer.start()
        consumer.start()
        producer.join()
        consumer.join()

        result should contain theSameElementsInOrderAs(1 to n)
      }
    }
    "having 2 consumers and 1 producer" should {
      "work" in {
        val m = new MyMonitor
        val n = 10000
        val producer = new Thread(() => {
          for (i <- 1 to n) {
            m.produce(i)
          }
        }, "producer")
        var result = Seq.empty[Int]
        val mutex = new Object
        val consumer = new Thread(() => {
          while (result.size < n) {
            m.consume() match {
              case Some(i) =>
                mutex.synchronized {
                  result :+= i
                }
              case None =>
            }
          }
        }, "consumer1")
        val consumer2 = new Thread(() => {
          while (result.size < n) {
            m.consume() match {
              case Some(i) =>
                mutex.synchronized {
                  result :+= i
                }
              case None =>
            }
          }
        }, "consumer2")
        producer.start()
        consumer.start()
        consumer2.start()
        producer.join()
        consumer.join()
        consumer2.join()

        result should contain theSameElementsAs (1 to n)
      }
      "having 1 consumer and 2 producers" should {
        "work" in {
          val m = new MyMonitor
          val n = 10000
          val producer = new Thread(() => {
            for (i <- 1 to n) {
              m.produce(i)
            }
          }, "producer")
          val producer2 = new Thread(() => {
            for (i <- n + 1 to 2 * n) {
              m.produce(i)
            }
          }, "producer2")
          var result = Seq.empty[Int]
          val mutex = new Object
          val consumer = new Thread(() => {
            while (result.size < 2 * n) {
              m.consume() match {
                case Some(i) =>
                  mutex.synchronized {
                    result :+= i
                  }
                case None =>
              }
            }
          }, "consumer1")
          producer.start()
          producer2.start()
          consumer.start()
          producer.join()
          producer2.join()
          consumer.join()

          result should contain theSameElementsAs (1 to 2 * n)
        }
      }
      "having 2 consumers and 2 producers" should {
        "work" in {
          val m = new MyMonitor
          val n = 10000
          val producer = new Thread(() => {
            for (i <- 1 to n) {
              m.produce(i)
            }
          }, "producer")
          val producer2 = new Thread(() => {
            for (i <- n + 1 to 2 * n) {
              m.produce(i)
            }
          }, "producer2")
          var result = Seq.empty[Int]
          val mutex = new Object
          val consumer = new Thread(() => {
            while (result.size < 2 * n) {
              m.consume() match {
                case Some(i) =>
                  mutex.synchronized {
                    result :+= i
                  }
                case None =>
              }
            }
          }, "consumer1")
          val consumer2 = new Thread(() => {
            while (result.size < 2 * n) {
              m.consume() match {
                case Some(i) =>
                  mutex.synchronized {
                    result :+= i
                  }
                case None =>
              }
            }
          }, "consumer2")
          producer.start()
          producer2.start()
          consumer.start()
          consumer2.start()
          producer.join()
          producer2.join()
          consumer.join()
          consumer2.join()

          result should contain theSameElementsAs (1 to 2 * n)
        }
      }
    }

    "being a binary semaphore" should {
      "work with one thread" in {
        val f = binarySemaphoreFixture
        val t = new Thread(() => {
          for (_ <- 1 to f.n) {
            f.mutex.downNanos()
            f.i += 1
            f.result :+= f.i
            f.mutex.up()
          }
        })
        t.start()
        t.join()
        f.result should contain theSameElementsInOrderAs(1 to f.n)
      }
      "work with two threads" in {
        val f = binarySemaphoreFixture
        val t = new Thread(() => {
          for (_ <- 1 to f.n) {
            f.mutex.downNanos()
            f.i += 1
            f.result :+= f.i
            f.mutex.up()
          }
        })
        val t2 = new Thread(() => {
          for (_ <- 1 to f.n) {
            f.mutex.downNanos()
            f.i += 1
            f.result :+= f.i
            f.mutex.up()
          }
        })
        t.start()
        t2.start()

        t.join()
        t2.join()
        f.result should contain theSameElementsInOrderAs(1 to 2 * f.n)
      }
    }
  }
}
