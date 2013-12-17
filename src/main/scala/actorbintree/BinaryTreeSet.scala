/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package actorbintree

import akka.actor._
import scala.collection.mutable
import actorbintree.BinaryTreeSet._
import actorbintree.BinaryTreeSet.Contains
import actorbintree.BinaryTreeSet.OperationFinished
import actorbintree.BinaryTreeSet.ContainsResult
import scala.Some
import actorbintree.BinaryTreeSet.Insert
import actorbintree.BinaryTreeNode.{CopyTo, CopyFinished}

object BinaryTreeSet {

  trait Operation {
    def requester: ActorRef

    def id: Int

    def elem: Int
  }

  trait OperationReply {
    def id: Int
  }

  /** Request with identifier `id` to insert an element `elem` into the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Insert(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to check whether an element `elem` is present
    * in the tree. The actor at reference `requester` should be notified when
    * this operation is completed.
    */
  case class Contains(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to remove the element `elem` from the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Remove(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request to perform garbage collection */
  case object GC

  /** Holds the answer to the Contains request with identifier `id`.
    * `result` is true if and only if the element is present in the tree.
    */
  case class ContainsResult(id: Int, result: Boolean) extends OperationReply

  /** Message to signal successful completion of an insert or remove operation. */
  case class OperationFinished(id: Int) extends OperationReply

}


class BinaryTreeSet extends Actor with ActorLogging {

  import BinaryTreeSet._

  def createRoot: ActorRef = context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))

  var root = createRoot

  // optional
  var pendingQueue: mutable.Queue[Operation] = mutable.Queue.empty[Operation]

  // optional
  def receive = normal

  // optional
  /** Accepts `Operation` and `GC` messages. */
  val normal: Receive = {
    case Insert(caller, id, elem) => root ! Insert(caller, id, elem)
    case Contains(caller, id, elem) => root ! Contains(caller, id, elem)
    case Remove(caller, id, elem) => root ! Remove(caller, id, elem)
    case GC => {
      val newRoot = createRoot
      context.become(garbageCollecting(newRoot))
      root ! CopyTo(newRoot)
    }
  }

  // optional
  /** Handles messages while garbage collection is performed.
    * `newRoot` is the root of the new binary tree where we want to copy
    * all non-removed elements into.
    */
  def garbageCollecting(newRoot: ActorRef): Receive = {
    case Insert(caller, id, elem) => pendingQueue.enqueue(Insert(caller, id, elem))
    case Contains(caller, id, elem) => pendingQueue.enqueue(Contains(caller, id, elem))
    case Remove(caller, id, elem) => pendingQueue.enqueue(Remove(caller, id, elem))
    case CopyFinished => {
      //println("tree copy finished!")

      while (!pendingQueue.isEmpty) {
        //println(pendingQueue)
        newRoot ! pendingQueue.dequeue
      }
      assert(pendingQueue.isEmpty)

      context.stop(root)
      root = newRoot
      context.become(normal)
    }
    case GC => ()
  }

}

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

  //var subtrees = Map[Position, ActorRef]()
  var removed = initiallyRemoved

  var left: Option[ActorRef] = None
  var right: Option[ActorRef] = None

  // optional
  def receive = normal

  // optional
  /** Handles `Operation` messages and `CopyTo` requests. */
  val normal: Receive = {

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
      if (elemToMatch == elem && !removed) {
        caller ! ContainsResult(id, true)
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
      (left, right) match {
        case (None, None) => {
          if(removed) {
            sender ! CopyFinished
          } else {
            context.become(copying(sender, Set(), removed))
            newRoot ! Insert(self, elem, elem)
          }
        }
        case (Some(left), Some(right)) => {
          context.become(copying(sender, Set(left, right), removed))
          left ! CopyTo(newRoot)
          right ! CopyTo(newRoot)
          insertSelf(newRoot)
        }
        case (Some(left), None) => {
          context.become(copying(sender, Set(left), removed))
          left ! CopyTo(newRoot)
          insertSelf(newRoot)
        }
        case (None, Some(right)) => {
          context.become(copying(sender, Set(right), removed))
          right ! CopyTo(newRoot)
          insertSelf(newRoot)
        }
      }
    }

    case PoisonPill => {
      if (left.isDefined) left.get ! PoisonPill
      if (right.isDefined) right.get ! PoisonPill
      context.stop(self)
    }

  }

  def insertSelf(newRoot: ActorRef) {
    if (!removed) {
      newRoot ! Insert(self, elem, elem)
    } else {
      self ! OperationFinished(0)
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
  def copying(caller: ActorRef, expected: Set[ActorRef], insertConfirmed: Boolean): Receive = {

    case CopyFinished => {
      val currentExpected = expected - sender
      //println(s"copy finished -> $elem ($pendingCopy, $selfInsert)")
      if (currentExpected.isEmpty && insertConfirmed) {
        caller ! CopyFinished
        context.become(normal)
      } else {
        context.become(copying(caller, currentExpected, insertConfirmed))
      }
    }

    case OperationFinished(id) => {

      //println(s"ops finished -> $elem ($pendingCopy, $selfInsert)")
      if (expected.isEmpty) {
        caller ! CopyFinished
        context.become(normal)
      } else {
        context.become(copying(caller, expected, true))
      }
    }
  }

}
