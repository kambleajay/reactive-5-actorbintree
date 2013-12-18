package actorbintree

import actorbintree.BinaryTreeSet._
import akka.actor._
import akka.event.LoggingReceive
import scala.Some

object BinaryTreeNode {

  trait Position

  case object Left extends Position

  case object Right extends Position

  case class CopyTo(treeNode: ActorRef)

  case object CopyFinished

  case object PoisonPill

  def props(elem: Int, initiallyRemoved: Boolean) = Props(classOf[BinaryTreeNode], elem, initiallyRemoved)
}

class BinaryTreeNode(val elem: Int, initiallyRemoved: Boolean) extends Actor with ActorLogging {

  import BinaryTreeNode._

  //var subtrees = Map[Position, ActorRef]()
  var removed = initiallyRemoved

  var left: Option[ActorRef] = None
  var right: Option[ActorRef] = None

  // optional
  def receive = LoggingReceive {
    normal
  }

  // optional
  /** Handles `Operation` messages and `CopyTo` requests. */
  val normal: Receive = LoggingReceive {

    case Insert(caller, id, newElem) => {
      if (newElem < elem) {
        insertInLeft(caller, id, newElem)
      } else if (newElem > elem) {
        insertInRight(caller, id, newElem)
      } else {
        caller ! OperationFinished(id)
      }
    }

    case Contains(caller, id, elemToMatch) => {
      if (elemToMatch == elem) {
        if (removed) caller ! ContainsResult(id, false)
        else if (!removed) caller ! ContainsResult(id, true)
      } else if (elemToMatch < elem) {
        containsInLeft(caller, id, elemToMatch)
      } else {
        containsInRight(caller, id, elemToMatch)
      }
    }

    case Remove(caller, id, elemToRemove) => {
      if (elemToRemove == elem) {
        removed = true
        caller ! OperationFinished(id)
      } else if (elemToRemove < elem) {
        removeFromLeft(caller, id, elemToRemove)
      } else {
        removeFromRight(caller, id, elemToRemove)
      }
    }

    case CopyTo(newRoot) => {
      (removed, left, right) match {
        case (true, None, None) => {
          sender ! CopyFinished
        }
        case (false, None, None) => {
          context.become(copying(sender, Set(), removed))
          newRoot ! Insert(self, elem, elem)
        }
        case (true, Some(left), Some(right)) => {
          context.become(copying(sender, Set(left, right), removed))
          left ! CopyTo(newRoot)
          right ! CopyTo(newRoot)
        }
        case (false, Some(left), Some(right)) => {
          context.become(copying(sender, Set(left, right), removed))
          left ! CopyTo(newRoot)
          right ! CopyTo(newRoot)
          newRoot ! Insert(self, elem, elem)
        }
        case (true, Some(left), None) => {
          context.become(copying(sender, Set(left), removed))
          left ! CopyTo(newRoot)
        }
        case (false, Some(left), None) => {
          context.become(copying(sender, Set(left), removed))
          left ! CopyTo(newRoot)
          newRoot ! Insert(self, elem, elem)
        }
        case (true, None, Some(right)) => {
          context.become(copying(sender, Set(right), removed))
          right ! CopyTo(newRoot)
        }
        case (false, None, Some(right)) => {
          context.become(copying(sender, Set(right), removed))
          right ! CopyTo(newRoot)
          newRoot ! Insert(self, elem, elem)
        }
      }
    }

    case PoisonPill => {
      if (left.isDefined) left.get ! PoisonPill
      if (right.isDefined) right.get ! PoisonPill
      context.stop(self)
    }

  }

  def insertInLeft(caller: ActorRef, id: Int, newElem: Int) {
    left match {
      case Some(lnode) => lnode ! Insert(caller, id, newElem)
      case None => {
        left = Some(context.actorOf(BinaryTreeNode.props(newElem, false)))
        caller ! OperationFinished(id)
      }
    }
  }

  def insertInRight(caller: ActorRef, id: Int, newElem: Int) {
    right match {
      case Some(rnode) => rnode ! Insert(caller, id, newElem)
      case None => {
        right = Some(context.actorOf(BinaryTreeNode.props(newElem, false)))
        caller ! OperationFinished(id)
      }
    }
  }

  def containsInLeft(caller: ActorRef, id: Int, elemToMatch: Int) {
    left match {
      case Some(lnode) => lnode ! Contains(caller, id, elemToMatch)
      case None => {
        caller ! ContainsResult(id, false)
      }
    }
  }

  def containsInRight(caller: ActorRef, id: Int, elemToMatch: Int) {
    right match {
      case Some(rnode) => rnode ! Contains(caller, id, elemToMatch)
      case None => {
        caller ! ContainsResult(id, false)
      }
    }
  }

  def removeFromLeft(caller: ActorRef, id: Int, elemToRemove: Int) {
    left match {
      case Some(lnode) => lnode ! Remove(caller, id, elemToRemove)
      case None => {
        caller ! OperationFinished(id)
      }
    }
  }

  def removeFromRight(caller: ActorRef, id: Int, elemToRemove: Int) {
    right match {
      case Some(rnode) => rnode ! Remove(caller, id, elemToRemove)
      case None => {
        caller ! OperationFinished(id)
      }
    }
  }

  // optional
  /** `expected` is the set of ActorRefs whose replies we are waiting for,
    * `insertConfirmed` tracks whether the copy of this node to the new tree has been confirmed.
    */
  def copying(caller: ActorRef, expected: Set[ActorRef], insertConfirmed: Boolean): Receive = LoggingReceive {

    case CopyFinished => {
      val currentExpected = expected - sender
      //println(s"copy finished -> $elem ($pendingCopy, $selfInsert)")
      if (currentExpected.isEmpty && insertConfirmed) {
        context.become(normal)
        caller ! CopyFinished
      } else {
        context.become(copying(caller, currentExpected, insertConfirmed))
      }
    }

    case OperationFinished(id) => {
      val currentInsertConfirmed = true
      //println(s"ops finished -> $elem ($pendingCopy, $selfInsert)")
      if (expected.isEmpty && currentInsertConfirmed) {
        context.become(normal)
        caller ! CopyFinished
      } else {
        context.become(copying(caller, expected, currentInsertConfirmed))
      }
    }
  }

}
