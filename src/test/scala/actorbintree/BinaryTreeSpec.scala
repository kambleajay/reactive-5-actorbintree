package actorbintree

import org.specs2.mutable.Specification
import akka.actor.Props
import org.specs2.time.NoTimeConversions
import scala.concurrent.duration._

class BinaryTreeSpec extends Specification with NoTimeConversions {
  sequential

  "Binary tree" should {

    "insert given element" in new AkkaTestkitSpecs2Support {
      within(1 second) {
        val binTree = system.actorOf(Props[BinaryTreeSet])
        binTree ! BinaryTreeSet.Insert(self, 1, 10)
        expectMsgType[BinaryTreeSet.OperationFinished] must be equalTo BinaryTreeSet.OperationFinished(1)

        binTree ! BinaryTreeSet.Insert(self, 2, 10)
        expectMsgType[BinaryTreeSet.OperationFinished] must be equalTo BinaryTreeSet.OperationFinished(2)
      }
    }

    "give correct answer for contains test" in new AkkaTestkitSpecs2Support {
      within(1 second) {
        val binTree = system.actorOf(Props[BinaryTreeSet])
        binTree ! BinaryTreeSet.Insert(self, 1, 20)
        expectMsgType[BinaryTreeSet.OperationFinished] must be equalTo BinaryTreeSet.OperationFinished(1)

        binTree ! BinaryTreeSet.Contains(self, 3, 21)
        expectMsgType[BinaryTreeSet.ContainsResult] must be equalTo BinaryTreeSet.ContainsResult(3, false)

        binTree ! BinaryTreeSet.Contains(self, 5, 20)
        expectMsgType[BinaryTreeSet.ContainsResult] must be equalTo BinaryTreeSet.ContainsResult(5, true)
      }
    }

    "remove given element" in new AkkaTestkitSpecs2Support {
      within(1 second) {
        val binTree = system.actorOf(Props[BinaryTreeSet])
        binTree ! BinaryTreeSet.Insert(self, 1, 30)
        expectMsgType[BinaryTreeSet.OperationFinished] must be equalTo BinaryTreeSet.OperationFinished(1)

        binTree ! BinaryTreeSet.Remove(self, 2, 31)
        expectMsgType[BinaryTreeSet.OperationFinished] must be equalTo BinaryTreeSet.OperationFinished(2)

        binTree ! BinaryTreeSet.Contains(self, 3, 30)
        expectMsgType[BinaryTreeSet.ContainsResult] must be equalTo BinaryTreeSet.ContainsResult(3, true)

        binTree ! BinaryTreeSet.Remove(self, 5, 30)
        expectMsgType[BinaryTreeSet.OperationFinished] must be equalTo BinaryTreeSet.OperationFinished(5)

        binTree ! BinaryTreeSet.Contains(self, 8, 30)
        expectMsgType[BinaryTreeSet.ContainsResult] must be equalTo BinaryTreeSet.ContainsResult(8, false)
      }
    }

    "properly handle GC" in new AkkaTestkitSpecs2Support {
      within(1 second) {
        val binTree = system.actorOf(Props[BinaryTreeSet])
        binTree ! BinaryTreeSet.Insert(self, 1, 30)
        expectMsgType[BinaryTreeSet.OperationFinished] must be equalTo BinaryTreeSet.OperationFinished(1)

        binTree ! BinaryTreeSet.Insert(self, 2, 40)
        expectMsgType[BinaryTreeSet.OperationFinished] must be equalTo BinaryTreeSet.OperationFinished(2)

        binTree ! BinaryTreeSet.Insert(self, 2, 50)
        expectMsgType[BinaryTreeSet.OperationFinished] must be equalTo BinaryTreeSet.OperationFinished(2)

        binTree ! BinaryTreeSet.Contains(self, 3, 30)
        expectMsgType[BinaryTreeSet.ContainsResult] must be equalTo BinaryTreeSet.ContainsResult(3, true)

        binTree ! BinaryTreeSet.Remove(self, 5, 30)
        expectMsgType[BinaryTreeSet.OperationFinished] must be equalTo BinaryTreeSet.OperationFinished(5)

        binTree ! BinaryTreeSet.Contains(self, 8, 30)
        expectMsgType[BinaryTreeSet.ContainsResult] must be equalTo BinaryTreeSet.ContainsResult(8, false)

        binTree ! BinaryTreeSet.GC

        binTree ! BinaryTreeSet.Contains(self, 10, 30)
        binTree ! BinaryTreeSet.Insert(self, 11, 77)
        binTree ! BinaryTreeSet.Remove(self, 12, 77)
        binTree ! BinaryTreeSet.Contains(self, 13, 77)

        expectMsgType[BinaryTreeSet.ContainsResult] must be equalTo BinaryTreeSet.ContainsResult(10, false)
        expectMsgType[BinaryTreeSet.OperationFinished] must be equalTo BinaryTreeSet.OperationFinished(11)
        expectMsgType[BinaryTreeSet.OperationFinished] must be equalTo BinaryTreeSet.OperationFinished(12)
        expectMsgType[BinaryTreeSet.ContainsResult] must be equalTo BinaryTreeSet.ContainsResult(13, false)        
      }
    }

  }

}
