// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.prim.etc

import org.nlogo.nvm.{ Context, Reporter }

class _maxpxcor extends Reporter {

  override def report(context: Context) =
    report_1(context)
  def report_1(context: Context): java.lang.Double =
    world.maxPxcorBoxed
  def report_2(context: Context): Double =
    world.maxPxcor
}
