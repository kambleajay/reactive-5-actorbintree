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
       }
    }
  }
}
