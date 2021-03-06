package com.persist.uw.examples

import com.sun.management.VMOption.Origin

import scala.annotation.tailrec
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

// Introduce parallelism using futures
// Not required (but desirable) try to maximize parallelism
// Use only immutable data
// Do not use blocking (don't call Await)
// Don't worry about the cost of concatenating lists
// If you use recursion, to keep it simpler you are not required to
//    make every function tail recursive
// Hint: use flatMap rather than map to avoid Future[Future[...]]
//
// REQUIRED: parMap, parFilter and parFold (partial credit if you only do some)
// OPTIONAL: parSortBy and/or parGroupBy for extra credit

object Parallel {

  implicit class ParallelList[A](flist: Future[List[A]]) {

    private def split[B](l: List[B]): (List[B], List[B]) = {
      l.splitAt(l.size / 2)
    }

    // One possible approach (you are free to pick a different approach)
    //   Run f on each element in its own Future and use Future.sequence to combine
    def parMap[B](f: A => B): Future[List[B]] = {
     val mappedResults : List[Future[B]]=  for{
        element <- Await.result(flist,Duration.Inf)
        } yield Future{ f( element) }
      Future.sequence(mappedResults)
    }

    // One possible approach (you are free to pick a different approach)
    //   Split list into two parts, in parallel recursively filter each half
    //      and then combine the results
    def parFilter(f: A => Boolean): Future[List[A]] = {
      @tailrec
      def myFilter(original : List[A],acc: List[A]) : List[A] = original match {
        case Nil =>  acc
        case head :: tail => myFilter(tail,if (f(head)) {head :: acc} else acc )
      }
      Future { myFilter(Await.result(flist,Duration.Inf),List.empty[A]).reverse }
    }

    // One possible approach (you are free to pick a different approach)
    //   Split list into two parts, in parallel recursively fold each half
    //      and then combine the results
    def parFold(init: A)(f: (A, A) => A): Future[A] = {
      @tailrec
      def myFold(result: A, list: List[A]): Future[A] = list match {
        case Nil => Future { result }
        case head :: tail => myFold(f(result,head), tail)
      }
      myFold(init, Await.result(flist,Duration.Inf))
    }

    // One possible approach (you are free to pick a different approach)
    //   Split the list, recursively sort each half and then merge the result
    def parSortBy[B](f: A => B)(implicit order: Ordering[B]): Future[List[A]] = {
      import order._
      ???
    }

    // One possible approach (you are free to pick a different approach)
    //   Use parMap to produce pair (f(i),i)
    //   Pick an f(i) and split list of pairs into left(<) middle(=) right(>) parts
    //       using parFilter
    //   Use recursion to group the left and right parts in parallel
    def parGroupBy[B](f: A => B)(implicit order: Ordering[B]): Future[Map[B, List[A]]] = ???

  }

}
