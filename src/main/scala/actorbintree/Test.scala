package actorbintree

import akka.actor._
import actorbintree.BinaryTreeSet._
import actorbintree.BinaryTreeNode._

class Test extends Actor {

  val binTree = context.actorOf(Props[BinaryTreeSet])

  binTree ! Insert(self, 1, 10)
  binTree ! Insert(self, 2, 20)
  binTree ! Contains(self, 3, 40)
  binTree ! Remove(self, 4, 20)
  binTree ! Contains(self, 5, 10)
  binTree ! GC
  binTree ! Insert(self, 6, 50)
  binTree ! Contains(self, 7, 10)
  binTree ! Contains(self, 8, 50)

  def receive: Receive = {
    case OperationFinished(id) => println(s"finished $id")
    case ContainsResult(id, answer) => println(s"contains $id is $answer")
  }

}