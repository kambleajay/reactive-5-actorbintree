package actorbintree

import akka.actor._
import actorbintree.BinaryTreeSet._
import actorbintree.BinaryTreeNode._
import akka.event.LoggingReceive

class Test extends Actor {

  val binTree1 = context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = false), "TREE1")
  val binTree2 = context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true), "TREE2")

  binTree1 ! Insert(self, 1, 10)
  binTree1 ! Insert(self, 2, 20)
  binTree1 ! Contains(self, 3, 10)
  binTree1 ! Contains(self, 4, 20)

  binTree1 ! CopyTo(binTree2)

  def receive: Receive = LoggingReceive {
    case OperationFinished(id) => println(s"finished $id")
    case ContainsResult(id, answer) => println(s"contains $id is $answer")
    case CopyFinished => {
      println("copy finish")
      binTree2 ! Contains(self, 5, 10)
      binTree2 ! Contains(self, 6, 20)
    }
  }

}