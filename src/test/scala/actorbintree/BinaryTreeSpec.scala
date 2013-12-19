package actorbintree

import org.specs2.mutable.Specification
import akka.actor.Props
import org.specs2.time.NoTimeConversions
import scala.concurrent.duration._
import BinaryTreeSet._
import BinaryTreeNode._

class BinaryTreeSpec extends Specification with NoTimeConversions {
  sequential

  "Binary tree" should {

    "insert given element" in new AkkaTestkitSpecs2Support {
      within(1 second) {
        val binTree = system.actorOf(Props[BinaryTreeSet])
        binTree ! Insert(self, 1, 10)
        expectMsgType[OperationFinished] must be equalTo OperationFinished(1)

        binTree ! Insert(self, 2, 10)
        expectMsgType[OperationFinished] must be equalTo OperationFinished(2)
      }
    }

    "give correct answer for contains test" in new AkkaTestkitSpecs2Support {
      within(1 second) {
        val binTree = system.actorOf(Props[BinaryTreeSet])
        binTree ! Insert(self, 1, 20)
        expectMsgType[OperationFinished] must be equalTo OperationFinished(1)

        binTree ! BinaryTreeSet.Contains(self, 3, 21)
        expectMsgType[ContainsResult] must be equalTo ContainsResult(3, false)

        binTree ! BinaryTreeSet.Contains(self, 5, 20)
        expectMsgType[ContainsResult] must be equalTo ContainsResult(5, true)
      }
    }

    "remove given element" in new AkkaTestkitSpecs2Support {
      within(1 second) {
        val binTree = system.actorOf(Props[BinaryTreeSet])
        binTree ! Insert(self, 1, 30)
        expectMsgType[OperationFinished] must be equalTo OperationFinished(1)

        binTree ! Remove(self, 2, 31)
        expectMsgType[OperationFinished] must be equalTo OperationFinished(2)

        binTree ! BinaryTreeSet.Contains(self, 3, 30)
        expectMsgType[ContainsResult] must be equalTo ContainsResult(3, true)

        binTree ! Remove(self, 5, 30)
        expectMsgType[OperationFinished] must be equalTo OperationFinished(5)

        binTree ! BinaryTreeSet.Contains(self, 8, 30)
        expectMsgType[ContainsResult] must be equalTo ContainsResult(8, false)
      }
    }

    "do a proper copy - 2 nodes" in new AkkaTestkitSpecs2Support {
        within(1 second) {
            val binTree1 = system.actorOf(BinaryTreeNode.props(0, true))
            binTree1 ! Insert(self, 1, 1)
            expectMsgType[OperationFinished] must be equalTo OperationFinished(1)
            binTree1 ! Insert(self, 2, -1)
            expectMsgType[OperationFinished] must be equalTo OperationFinished(2)

            binTree1 ! Contains(self, 3, 1)
            expectMsgType[ContainsResult] must be equalTo ContainsResult(3, true)

            binTree1 ! Contains(self, 4, -1)
            expectMsgType[ContainsResult] must be equalTo ContainsResult(4, true)

            val binTree2 = system.actorOf(BinaryTreeNode.props(1, true))
            binTree1 ! CopyTo(binTree2)
            expectMsgType[CopyFinished.type] must be equalTo CopyFinished

            binTree2 ! Contains(self, 5, 1)
            expectMsgType[ContainsResult] must be equalTo ContainsResult(5, true)

            binTree2 ! Contains(self, 6, -1)
            expectMsgType[ContainsResult] must be equalTo ContainsResult(6, true)
        }
    }

    "do a proper copy - right node" in new AkkaTestkitSpecs2Support {
        within(1 second) {
            val binTree1 = system.actorOf(BinaryTreeNode.props(0, true))
            binTree1 ! Insert(self, 1, 1)
            expectMsgType[OperationFinished] must be equalTo OperationFinished(1)

            binTree1 ! Contains(self, 3, 1)
            expectMsgType[ContainsResult] must be equalTo ContainsResult(3, true)

            val binTree2 = system.actorOf(BinaryTreeNode.props(10, true))
            binTree1 ! CopyTo(binTree2)
            expectMsgType[CopyFinished.type] must be equalTo CopyFinished

            binTree2 ! Contains(self, 5, 1)
            expectMsgType[ContainsResult] must be equalTo ContainsResult(5, true)
        }
    }

    "do a proper copy - left node" in new AkkaTestkitSpecs2Support {
        within(1 second) {
            val binTree1 = system.actorOf(BinaryTreeNode.props(0, true))
            binTree1 ! Insert(self, 1, -1)
            expectMsgType[OperationFinished] must be equalTo OperationFinished(1)

            binTree1 ! Contains(self, 3, -1)
            expectMsgType[ContainsResult] must be equalTo ContainsResult(3, true)

            val binTree2 = system.actorOf(BinaryTreeNode.props(10, true))
            binTree1 ! CopyTo(binTree2)
            expectMsgType[CopyFinished.type] must be equalTo CopyFinished

            binTree2 ! Contains(self, 5, -1)
            expectMsgType[ContainsResult] must be equalTo ContainsResult(5, true)
        }
    }

    "properly handle GC" in new AkkaTestkitSpecs2Support {
      within(1 second) {
        val binTree = system.actorOf(Props[BinaryTreeSet])
        binTree ! Insert(self, 1, 30)
        expectMsgType[OperationFinished] must be equalTo OperationFinished(1)

        binTree ! Insert(self, 2, 40)
        expectMsgType[OperationFinished] must be equalTo OperationFinished(2)

        binTree ! Insert(self, 2, 50)
        expectMsgType[OperationFinished] must be equalTo OperationFinished(2)

        binTree ! BinaryTreeSet.Contains(self, 3, 30)
        expectMsgType[ContainsResult] must be equalTo ContainsResult(3, true)

        binTree ! Remove(self, 5, 30)
        expectMsgType[OperationFinished] must be equalTo OperationFinished(5)

        binTree ! BinaryTreeSet.Contains(self, 8, 30)
        expectMsgType[ContainsResult] must be equalTo ContainsResult(8, false)

        binTree ! BinaryTreeSet.GC

        binTree ! BinaryTreeSet.Contains(self, 10, 30)
        binTree ! Insert(self, 11, 77)
        binTree ! Remove(self, 12, 77)
        binTree ! BinaryTreeSet.Contains(self, 13, 77)

        expectMsgType[ContainsResult] must be equalTo ContainsResult(10, false)
        expectMsgType[OperationFinished] must be equalTo OperationFinished(11)
        expectMsgType[OperationFinished] must be equalTo OperationFinished(12)
        expectMsgType[ContainsResult] must be equalTo ContainsResult(13, false)        
      }
    }

  }

}
