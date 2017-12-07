// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.agent;

import org.nlogo.api.AgentException;
import org.nlogo.core.AgentKindJ;
import scala.Array;
import scala.Tuple2;
import shapeless.Tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;

import static jdk.nashorn.internal.objects.Global.println;

public strictfp class InRadiusOrCone
  implements World.InRadiusOrCone {
  private final World2D world;
  private Patch patches[] = null;

  InRadiusOrCone(World2D world) {
    this.world = world;
  }

  public List<Agent> inRadiusSimple(Agent agent, AgentSet sourceSet,
      double radius, boolean wrap) {
    return InRadiusSimple.apply(world, agent, sourceSet, radius, wrap);
  }

  public List<Agent> inRadius(Agent agent, AgentSet sourceSet,
                              double radius, boolean wrap) {

    List<Agent> result = new ArrayList<Agent>();
    Patch startPatch;
    double startX, startY, gRoot;
    int dx, dy;

    if (agent instanceof Turtle) {
      Turtle startTurtle = (Turtle) agent;
      startX = startTurtle.xcor();
      startY = startTurtle.ycor();
    } else {
      startPatch = (Patch) agent;
      startX = startPatch.pxcor;
      startY = startPatch.pycor;
    }

    HashSet<Long> cachedIDs = null;
    if (! sourceSet.isBreedSet()) {
      cachedIDs = new HashSet<Long>(sourceSet.count());
      AgentIterator sourceTurtles = sourceSet.iterator();
      while (sourceTurtles.hasNext()) {
        Agent t = sourceTurtles.next();
        cachedIDs.add(new Long(t.id()));
      }
    } else {
      cachedIDs = new HashSet<Long>(0);
    }

    ArrayList<Tuple2<Object, Object>> regions = world.topology().getRegion(startX, startY, radius);
    Patch patches[] = new Patch[world.patches().count()];
    Agent worldPatches[] = ((ArrayAgentSet) world.patches()).array();
    int curr = 0;
    int length;
    for (Tuple2<Object, Object> region : regions) {
      int r1 = (int)region._1();
      int r2 = (int)region._2();
      length = r2 - r1;
      System.arraycopy(worldPatches, r1, patches, curr, length);

      curr += length;
    }

    for (int i = 0; i < curr; i++) {
        Patch patch = patches[i];

        if (sourceSet.kind() == AgentKindJ.Patch()) {
          if (world.protractor().distance(patch.pxcor, patch.pycor, startX, startY, wrap) <= radius &&
              (sourceSet == world.patches() || cachedIDs.contains(new Long(patch.id())))) {
            result.add(patch);
          }
        } else if (sourceSet.kind() == AgentKindJ.Turtle()) {
          // Only check patches that might have turtles within the radius on them.
          // The 1.415 (square root of 2) adjustment is necessary because it is
          // possible for portions of a patch to be within the circle even though
          // the center of the patch is outside the circle.  Both turtles, the
          // turtle in the center and the turtle in the agentset, can be as much
          // as half the square root of 2 away from its patch center.  If they're
          // away from the patch centers in opposite directions, that makes a total
          // of square root of 2 additional distance we need to take into account.
          // TODO fix this:

          dx = Math.abs(patch.pxcor - (int)startX);
          if (dx > world.worldWidth()/2)
            dx = world.worldWidth() - dx;

          dy = Math.abs(patch.pycor - (int)startY);
          if (dy > world.worldHeight()/2)
            dy = world.worldHeight() - dy;

          gRoot = world.rootsTable().gridRoot(dx * dx + dy * dy);

          if (gRoot > radius + 1.415) {
            continue;
          }

          if (gRoot <= radius - 1.415) {
            patch.turtlesHere().forEach(t -> result.add(t));
          } else {
            for (Turtle turtle : patch.turtlesHere()) {
              if ((sourceSet == world.turtles()
                      || (sourceSet.isBreedSet() && sourceSet == turtle.getBreed())
                      || cachedIDs.contains(new Long(turtle.id())))
                      && world.protractor().distance(turtle.xcor(), turtle.ycor(), startX, startY, wrap) <= radius) {
                result.add(turtle);
              }
            }
          }
        }
    }
    return result;
  }

  public List<Agent> inCone(Turtle startTurtle, AgentSet sourceSet,
                            double radius, double angle, boolean wrap) {
    int worldWidth = world.worldWidth();
    int worldHeight = world.worldHeight();

    int m;
    int n;
    // If wrap is true and the radius is large enough, the cone
    // may wrap around the edges of the world.  We handle this by
    // enlarging the coordinate system in which we search beyond
    // the world edges and then filling the enlarged coordinate
    // system with "copies" of the world.  At least, you can
    // imagine it that way; we don't actually copy anything.  m
    // and n are the maximum number of times the cone might wrap
    // around the edge of the world in the X and Y directions, so
    // that's how many world copies we will need to make.  The
    // copies will range from -m to +m on the x axis and -n to +n
    // on the y axis.
    if (wrap) {
      m = world.wrappingAllowedInX() ? (int) StrictMath.ceil(radius / worldWidth) : 0;
      n = world.wrappingAllowedInY() ? (int) StrictMath.ceil(radius / worldHeight) : 0;
    } else {
      // in the nonwrapping case, we don't need any world copies besides
      // the original, so we have only one pair of offsets and both of
      // them are 0
      m = 0;
      n = 0;
    }

    List<Agent> result = new ArrayList<Agent>();
    double half = angle / 2;

    double gRoot;
    int dx, dy;

    HashSet<Long> cachedIDs = null;
    if (! sourceSet.isBreedSet()) {
      cachedIDs = new HashSet<Long>(sourceSet.count());
      AgentIterator sourceTurtles = sourceSet.iterator();
      while (sourceTurtles.hasNext()) {
        Agent t = sourceTurtles.next();
        cachedIDs.add(new Long(t.id()));
      }
    } else {
      cachedIDs = new HashSet<Long>(0);
    }

    ArrayList<Tuple2<Object, Object>> regions = world.topology().getRegion(startTurtle.xcor(), startTurtle.ycor(), radius);
    Patch patches[] = new Patch[world.patches().count()];
    Agent worldPatches[] = ((ArrayAgentSet) world.patches()).array();
    int curr = 0;
    int length;
    for (Tuple2<Object, Object> region : regions) {
      int r1 = (int)region._1();
      int r2 = (int)region._2();
      length = r2 - r1;
      System.arraycopy(worldPatches, r1, patches, curr, length);

      curr += length;
    }

    // loop through the patches in the rectangle.  (it doesn't matter what
    // order we check them in.)
    for (int i = 0; i < curr; i++) {
      Patch patch = patches[i];
      // incone is optimized assuming a torus world making incone use the topology properly
      // will require a significant re-write. maybe it's  candidate for optimizations
      // for each topology.  ev 9/5/05
      if (patch != null) {
        if (sourceSet.kind() == AgentKindJ.Patch()) {
          // loop through our world copies
          outer:
          for (int worldOffsetX = -m; worldOffsetX <= m; worldOffsetX++) {
            for (int worldOffsetY = -n; worldOffsetY <= n; worldOffsetY++) {
              if ((sourceSet == world.patches() || cachedIDs.contains(new Long(patch.id())))
                  && isInCone(patch.pxcor + worldWidth * worldOffsetX,
                  patch.pycor + worldHeight * worldOffsetY,
                  startTurtle.xcor(), startTurtle.ycor(),
                  radius, half, startTurtle.heading())) {
                result.add(patch);
                break outer;
              }
            }
          }
        } else {
          // Only check patches that might have turtles within the radius on them.
          // The 1.415 (square root of 2) adjustment is necessary because it is
          // possible for portions of a patch to be within the circle even though
          // the center of the patch is outside the circle.  Both turtles, the
          // turtle in the center and the turtle in the agentset, can be as much
          // as half the square root of 2 away from its patch center.  If they're
          // away from the patch centers in opposite directions, that makes a total
          // of square root of 2 additional distance we need to take into account.
          // TODO fix this:

          dx = Math.abs(patch.pxcor - (int)startTurtle.xcor());
          if (dx > world.worldWidth()/2)
            dx = world.worldWidth() - dx;

          dy = Math.abs(patch.pycor - (int)startTurtle.ycor());
          if (dy > world.worldHeight()/2)
            dy = world.worldHeight() - dy;

          gRoot = world.rootsTable().gridRoot(dx * dx + dy * dy);

          if (gRoot <= radius + 1.415) {

            for (Turtle turtle : patch.turtlesHere()) {
              // loop through our world copies
              outer:
              for (int worldOffsetX = -m; worldOffsetX <= m; worldOffsetX++) {
                for (int worldOffsetY = -n; worldOffsetY <= n; worldOffsetY++) {
                  // any turtle set with a non-null print name is either
                  // the set of all turtles, or a breed agentset - ST 2/19/04
                  if ((sourceSet == world.turtles()
                          || (sourceSet.isBreedSet() && sourceSet == turtle.getBreed())
                          || cachedIDs.contains(new Long(turtle.id())))
                          && isInCone(turtle.xcor() + worldWidth * worldOffsetX,
                          turtle.ycor() + worldHeight * worldOffsetY,
                          startTurtle.xcor(), startTurtle.ycor(),
                          radius, half, startTurtle.heading())) {
                    result.add(turtle);
                    break outer;
                  }
                }
              }
            }

          }
        }
      }
    }

    return result;
  }

  // helper method for inCone().
  // check if (x, y) is in the cone with center (cx, cy) , radius r, half-angle half, and central
  // line of the cone having heading h.
  private boolean isInCone(double x, double y,
                           double cx, double cy,
                           double r, double half, double h) {
    if (x == cx && y == cy) {
      return true;
    }
    if (world.protractor().distance(cx, cy, x, y, false) > r) // false = don't wrap, since inCone()
    // handles wrapping its own way
    {
      return false;
    }
    double theta;
    try {
      theta = world.protractor().towards(cx, cy, x, y, false);
    } catch (AgentException e) {
      // this should never happen because towards() only throws an AgentException
      // when the distance is 0, but we already ruled out that case above
      throw new IllegalStateException(e.toString());
    }
    double diff = StrictMath.abs(theta - h);
    // we have to be careful here because e.g. the difference between 5 and 355
    // is 10 not 350... hence the 360 thing
    return (diff <= half) || ((360 - diff) <= half);
  }
}
