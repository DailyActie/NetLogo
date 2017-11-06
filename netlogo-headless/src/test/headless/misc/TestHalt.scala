// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.headless
package misc

import org.scalatest.FunSuite
import org.nlogo.util.SlowTestTag
import org.nlogo.{ core, api, nvm }
import org.nlogo.workspace.WorkspaceDependencies

object TestHalt {
  // This is ugly, but since we use reflection to instantiate HeadlessWorkspace it's hard to
  // subclass.  Oh well, this is only test code. - ST 3/4/09
  var finalized = false
  class MyWorkspace(deps: WorkspaceDependencies, renderer: api.RendererInterface)
  extends HeadlessWorkspace(deps, renderer) {
    override def finalize() {
      finalized = true
      super.finalize()
    }
  }
}

class TestHalt extends FunSuite  {

  // I've had weird Heisenbug-type problems with the workspace not getting GC'ed if
  // it's a local variable rather than a top-level class member - ST 1/8/13
  var workspace: HeadlessWorkspace = null

  def withWorkspace(body: => Unit) {
    import TestHalt._
    finalized = false
    workspace = HeadlessWorkspace.newInstance(classOf[MyWorkspace])
    body
    workspace.halt()
    workspace.dispose()
    workspace = null
    for (i <- 1 to 20)
      if (!finalized) { Thread.sleep(200); System.gc() }
    assert(finalized)
  }

  test("halt 0", SlowTestTag) {
    withWorkspace { }
  }

  test("halt 1", SlowTestTag) {
    withWorkspace {
      // multiply possible memory leaks
      workspace.compileCommands("")
    }
  }

  test("halt 2", SlowTestTag) {
    var ex: api.LogoException = null
    val thread = new Thread("TestHalt.testHalt") {
      override def run() {
        try workspace.command("loop [ set x x + 1 ]")
        catch { case e: api.LogoException => ex = e }
      }
    }
    withWorkspace {
      workspace.openModel(core.Model(code = "globals [x]"))
      thread.start()
      def loop() {
        if (ex != null) throw ex
        if (workspace.report("x").asInstanceOf[Double] < 10) {
          Thread.sleep(5)
          loop()
        }
      }
      loop()
    }
    thread.join()
    if (ex != null)
      assert(ex.isInstanceOf[nvm.HaltException])
  }

}
