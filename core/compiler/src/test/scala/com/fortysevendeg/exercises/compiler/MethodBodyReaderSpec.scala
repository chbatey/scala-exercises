package com.fortysevendeg.exercises
package compiler

import org.scalatest._

import java.lang.ClassLoader

class MethodBodyReaderSpec extends FunSpec with Matchers with MethodBodyReaderSpecUtilities {

  describe("code snippet extraction") {
    it("should extract a one line snippet and trim all whitespace") {
      val code = """
       |/** This is an example exercise.
       |  * What value returns two?
       |  */
       |def addOne(value: Int) = {
       |  value + value
       |}
       """.stripMargin

      extractSnippet(code) should equal(
        """value + value"""
      )
    }

    it("should extract a multi line snippet and trim all whitespace") {
      val code = """
       |/** This is an example exercise.
       |  * What value returns two?
       |  */
       |def addOne(value: Int) = {
       |
       |  val foo = value + 1
       |    println("we have a foo " + foo)
       |  1 + whatever
       |}
       """.stripMargin

      extractSnippet(code) should equal(
        """|val foo = value + 1
           |  println("we have a foo " + foo)
           |1 + whatever""".stripMargin
      )
    }

    it("should handle the closing block bracket on the last line of code") {
      val code = """
       |/** This is an example exercise.
       |  * What value returns two?
       |  */
       |def addOne(value: Int) = {
       |
       |  val foo = value + 1
       |    println("we have a foo " + foo)
       |  1 + whatever}
       """.stripMargin

      extractSnippet(code) should equal(
        """|val foo = value + 1
           |  println("we have a foo " + foo)
           |1 + whatever""".stripMargin
      )
    }

    it("should handle the opening block bracket on the first line of code") {
      val code = """
       |/** This is an example exercise.
       |  * What value returns two?
       |  */
       |def addOne(value: Int) = {val foo = value + 1
       |  println("we have a foo " + foo)
       |  1 + whatever
       |}
       """.stripMargin

      extractSnippet(code) should equal(
        """|val foo = value + 1
           |  println("we have a foo " + foo)
           |  1 + whatever""".stripMargin
      )
    }
  }

}

trait MethodBodyReaderSpecUtilities {
  val global = new DocExtractionGlobal() {
    locally { new Run() }
  }

  import global._

  def unwrapBody(tree: Tree): Tree = tree match {
    case q"def $tname(...$paramss): $tpt = $expr" ⇒ expr
    case DocDef(comment, defTree)                 ⇒ unwrapBody(defTree)
    case _                                        ⇒ EmptyTree
  }

  def compileMethod(code: String): Tree = {
    def wrap(code: String): String = s"""package Code { object Code { $code }}"""
    def unwrap(tree: Tree): Tree = tree match {
      case q"package Code { object Code { $statements }}" ⇒ statements
      case _ ⇒ EmptyTree
    }
    unwrap(global
      .newUnitParser(wrap(code))
      .compilationUnit())
  }

  def extractSnippet(code: String): String = {
    val method = compileMethod(code)
    val body = unwrapBody(method)
    MethodBodyReader.read(global)(body)
  }

}
