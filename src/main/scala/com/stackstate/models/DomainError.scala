package com.stackstate.models

import scala.annotation.tailrec

sealed trait DomainError {
  def code: String

  def message: String
}

case class InvalidComponent(override val message: String,
                            override val code: String = InvalidComponent.INVALID_COMPONENT) extends DomainError

case class InvalidState(override val message: String,
                        override val code: String = InvalidComponent.INVALID_STATE) extends DomainError

case class UnknownError(override val message: String,
                        override val code: String = InvalidComponent.UNKNOWN_ERROR) extends DomainError

object InvalidComponent {
  val INVALID_COMPONENT = "INVALID_COMPONENT"
  val INVALID_STATE = "INVALID_STATE"
  val UNKNOWN_ERROR = "UNKNOWN_ERROR"
}

trait OptionLike[+A] {

  def pure[B](b: B): OptionLike[B] = SomeLike(b)

  def map[B](func: A => B): OptionLike[B] = {
    this match {
      case SomeLike(value) => pure(func(value))
      case NoneLike => NoneLike
    }
  }

  def flatMap[B](func: A => OptionLike[B]): OptionLike[B] = {
    this match {
      case SomeLike(value) => func(value)
      case NoneLike => NoneLike
    }
  }

  def fold[B](ifEmpty: => B)(func: A => B): B = {
    this match {
      case SomeLike(value) => func(value)
      case NoneLike => ifEmpty
    }
  }


}

case class SomeLike[A](value: A) extends OptionLike[A]

case object NoneLike extends OptionLike[Nothing]


trait Functor[F[_]] {
  def map[A, B](a: F[A])(func: A => B): F[B]

  def ap[A, B](a: F[A])(func: F[A => B]): F[B]
}

object Test extends App {

  implicit val optionFunctor = new Functor[Option] {
    override def map[A, B](a: Option[A])(func: A => B): Option[B] = a match {
      case Some(value) => Some(func(value))
      case None => None
    }

    override def ap[A, B](a: Option[A])(func: Option[A => B]): Option[B] = a match {
      case Some(value) => func match {
        case Some(value1) => Some(value1(value))
        case None => None
      }
      case None => None
    }
  }

  case class Node(value: Int, left: Option[Node] = None, right: Option[Node] = None)

  @tailrec
  def search(element: Int, rootNode: Node): Option[Node] = {
    if (rootNode.value == element) {
      Option(rootNode)
    } else if (rootNode.value < element) {
      if (rootNode.right.isDefined) {
        search(element, rootNode.right.get)
      } else {
        None
      }
    } else {
      if (rootNode.left.isDefined) {
        search(element, rootNode.left.get)
      } else {
        None
      }
    }
  }

  val grandChild1 = Node(7)
  val grandchild2 = Node(9)
  val child1 = Node(6, None, Some(grandChild1))
  val child2 = Node(10, Some(grandchild2))

  val root = Node(8, Some(child1), Some(child2))

  println(search(11, root))

  case class Factor(
                     name: String,
                     age: Int,
                     address: String,
                     result: Int
                   )


  def groupAll(factors: Vector[Factor], commands: Vector[String]): Vector[Factor] = {
    if (commands.isEmpty) {
      factors
    } else {
      val result = groupOneField(factors, commands.head)
      groupAll(result, commands.tail)
    }
  }

  def groupOneField(factors: Vector[Factor], command: String): Vector[Factor] = {
    command match {
      case "name" => factors.groupBy(_.name).values.flatten.toVector
      case "age" => factors.groupBy(_.age).values.flatten.toVector
      case "address" => factors.groupBy(_.address).values.flatten.toVector
      case "result" => factors.groupBy(_.result).values.flatten.toVector
    }
  }


  //  sealed trait Something
  //
  //  case class Students(names: List[String]) extends Something
  //
  //  case class Ages(nums: List[String]) extends Something
  //
  //  import cats.implicits._
  //
  //  def calculate(incoming: Vector[String], studentMap: Map[String, String]): Either[Something, Something] = {
  //    val initial: Either[Something, Something] = Students(List.empty).asRight
  //    incoming.foldLeft(initial)((res1, res2) =>
  //      res1 match {
  //        case Right(Students(names)) if studentMap.contains(res2) => Students(names :+ res2).asRight
  //        case Right(Students(names)) if !studentMap.contains(res2) => Ages(List(res2)).asLeft
  //        case Left(Ages(nums)) if !studentMap.contains(res2) => Ages(nums :+ res2).asLeft
  //      }
  //    )
  //  }
  //
  //  val incoming = Vector("hari", "ram", "syam", "dam", "ham")
  //  val map = Map("hari" -> "smart", "ram" -> "n-smart", "syam" -> "smart")
  //
  //  val result: Either[Something, Something] = calculate(incoming, map)
  //
  //  println(result)

}