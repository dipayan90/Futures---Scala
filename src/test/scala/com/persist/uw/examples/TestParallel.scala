package com.persist.uw.examples

import com.fortysevendeg.lambdatest._

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import com.persist.uw.examples.Parallel._
import org.scalacheck.Prop._

//import org.scalacheck.Gen._
//import org.scalacheck.Test
import scala.concurrent.duration._


class TestParallel extends LambdaTest {
  def recover(body: => LambdaAct): LambdaAct = {
    SingleLambdaAct(t =>
      try {
        body.eval(t)
      } catch {
        case ex:Throwable => assert(false, s"failed ${ex.getMessage}").eval(t)
      }
    )
  }

  def act = {
    val s = List(5, 1, 4, 2, 3)
    val id = (i: Int) => i
    test(s"simple [$s]") {
      val f = Future(s)
      test("parMap"){ recover {
        def s1 = Await.result(f.parMap(_ + 1), Duration.Inf)
        val s2 = List(6, 2, 5, 3, 4)
        assertEq(s1, s2)
      }} + test("parFilter") { recover {
        val s1 = Await.result(f.parFilter(_ % 2 == 0), Duration.Inf)
        val s2 = List(4, 2)
        assertEq(s1, s2)
      }} + test("parFold") { recover {
        val v1 = Await.result(f.parFold(0)((i, j) => i + j), Duration.Inf)
        assertEq(v1, 15)
      }} + test("parSortBy") { recover {
        val s1 = Await.result(f.parSortBy(-_), Duration.Inf)
        val s2 = List(5, 4, 3, 2, 1)
        assertEq(s1, s2)
      }} +
      test("parGroupBy") { recover {
        val m1 = Await.result(f.parGroupBy(_ % 2), Duration.Inf).map {
          case (k, l) => (k, l.sortBy(id))
        }
        val m2 = Map(0 -> List(2, 4), 1 -> List(1, 3, 5))
        assertEq(m1, m2)

      }}
    } +
    test("ScalaCheck") {
      test("parMap") {
        assertSC() {
          forAll {
            (t: List[Int]) =>
              val f = (i: Int) => i + 1
              val s1 = Await.result(Future(t).parMap(f), Duration.Inf)
              val s2 = t.map(f)
              s1 == s2
          }
        }
      } +
      test("parFilter") {
        assertSC() {
          forAll {
            (t: List[Int]) =>
              val f = (i: Int) => i % 2 == 0
              val s1 = Await.result(Future(t).parFilter(f), Duration.Inf)
              val s2 = t.filter(f)
              s1 == s2
          }
        }
      } +
      test("parFold") {
        assertSC() {
          forAll {
            (t: List[Int]) =>
              val f = (i: Int, j: Int) => i + j
              val v1 = Await.result(Future(t).parFold(0)(f), Duration.Inf)
              val v2 = t.fold(0)(f)
              v1 == v2
          }
        }
      } +
      test("parSortBy") {
        assertSC() {
          forAll {
            (t: List[Int]) =>
              val f = (i: Int) => -i
              val s1 = Await.result(Future(t).parSortBy(f), Duration.Inf)
              val s2 = t.sortBy(f)
              s1 == s2
          }
        }
      } +
      test("parGroupBy") {
        assertSC() {
          forAll {
            (t: List[Int]) =>
              val f = (i: Int) => i % 2
              val m1 = Await.result(Future(t).parGroupBy(f), Duration.Inf).map {
                case (k, v) => (k, v.sortBy(id))
              }
              val m2 = t.groupBy(f).map {
                case (k, v) => (k, v.sortBy(id))
              }
              m1 == m2
          }
        }
      }

    }
  }
}

object TestParallel {
  def main(args: Array[String]): Unit = {
    run("parallel", new TestParallel)

  }
}