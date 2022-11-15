/*
 * Dungeons Guide - The most intelligent Hypixel Skyblock Dungeons Mod
 * Copyright (C) 2021  cyoung06
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package kr.syeyoung.dungeonsguide.dungeon.pathfinding.impl

import kr.syeyoung.dungeonsguide.dungeon.pathfinding.DungeonRoomAccessor
import kr.syeyoung.dungeonsguide.dungeon.pathfinding.IPathfinderStrategy
import lombok.Getter
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import org.joml.Vector3d
import java.util.*
import kotlin.math.roundToInt

class AStarCornerCut(private val roomAccessor: DungeonRoomAccessor): IPathfinderStrategy(roomAccessor) {
    private var dx: Int = 0
    private var dy: Int = 0
    private var dz: Int = 0

    @Getter
    private lateinit var destinationBB: AxisAlignedBB
    private val nodeMap: MutableMap<Coordinate, Node> = HashMap()

    @Getter
    private val open = PriorityQueue(
        Comparator.comparing { a: Node? -> a?.f ?: Float.MAX_VALUE }
        .thenComparing { _, a: Node? -> if (a == null) Int.MAX_VALUE else a.coordinate!!.x }
        .thenComparing { _, a: Node? -> if (a == null) Int.MAX_VALUE else a.coordinate!!.y }
        .thenComparing { _, a: Node? -> if (a == null) Int.MAX_VALUE else a.coordinate!!.z }
    )



    private var lastSx = 0
    private var lastSy = 0
    private var lastSz = 0

    private var pfindIdx = 0


    private fun openNode(x: Int, y: Int, z: Int): Node {
        val coordinate = Coordinate(x, y, z)
        var node = nodeMap[coordinate]
        if (node == null) {
            node = Node()
            nodeMap[coordinate] = node
        }
        return node
    }

    private fun distSq(x: Float, y: Float, z: Float): Float {
        return MathHelper.sqrt_float(x * x + y * y + z * z)
    }

    class Coordinate (val x:Int, val y:Int, val z: Int)

    class Node {
        val coordinate: Coordinate? = null
        var f = Float.MAX_VALUE
        var g = Float.MAX_VALUE
        var lastVisited = 0

        var parent: Node? = null



        companion object {
            fun makeHash(x: Int, y: Int, z: Int): Long {
                return y.toLong() and 32767L or (x.toShort().toLong() and 32767L shl 16) or (z.toShort()
                    .toLong() and 32767L shl 32)
            }
        }
    }

    override fun pathfind(from: Vector3d, to: Vector3d, timeout: Float):Boolean {

        dx = (to.x * 2).toInt()
        dy = (to.y * 2).toInt()
        dz = (to.z * 2).toInt()
        destinationBB = AxisAlignedBB.fromBounds(
            (dx - 2).toDouble(),
            (dy - 2).toDouble(),
            (dz - 2).toDouble(),
            (dx + 2).toDouble(),
            (dy + 2).toDouble(),
            (dz + 2).toDouble()
        )



        pfindIdx++
        if (lastSx != (from.x * 2).roundToInt() || lastSy != (from.y * 2).roundToInt() || lastSz != (from.z * 2).roundToInt()
        ) open.clear()
        lastSx = Math.round(from.x * 2).toInt()
        lastSy = Math.round(from.y * 2).toInt()
        lastSz = Math.round(from.z * 2).toInt()
        val startNode = openNode(dx, dy, dz)
        val goalNode = openNode(lastSx, lastSy, lastSz)
        if (goalNode.parent != null) {
            val route = LinkedList<Vec3>()
            var curr: Node? = goalNode
            while (curr!!.parent != null) {
                route.addLast(
                    Vec3(
                        curr.coordinate!!.x / 2.0,
                        curr.coordinate!!.y / 2.0 + 0.1,
                        curr.coordinate!!.z / 2.0
                    )
                )
                curr = curr.parent
            }
            route.addLast(Vec3(curr.coordinate!!.x / 2.0, curr.coordinate!!.y / 2.0 + 0.1, curr.coordinate!!.z / 2.0))
            this.route = route
            return true
        }
        startNode.g = 0f
        startNode.f = 0f
        goalNode.g = Int.MAX_VALUE.toFloat()
        goalNode.f = Int.MAX_VALUE.toFloat()
        open.add(startNode)
        val end = System.currentTimeMillis() + timeout
        while (!open.isEmpty()) {
            if (System.currentTimeMillis() > end) {
                return false
            }
            val n = open.poll()
            if (n != null) {
                if (n.lastVisited == pfindIdx) continue
                n.lastVisited = pfindIdx
            }
            if (n === goalNode) {
                // route = reconstructPath(startNode)
                val route = LinkedList<Vec3>()
                var curr: Node? = goalNode
                while (curr!!.parent != null) {
                    route.addLast(
                        Vec3(
                            curr.coordinate!!.x / 2.0,
                            curr.coordinate!!.y / 2.0 + 0.1,
                            curr.coordinate!!.z / 2.0
                        )
                    )
                    curr = curr.parent
                }
                route.addLast(
                    Vec3(
                        curr.coordinate!!.x / 2.0,
                        curr.coordinate!!.y / 2.0 + 0.1,
                        curr.coordinate!!.z / 2.0
                    )
                )
                this.route = route
                return true
            }
            for (z in -1..1) {
                for (y in -1..1) {
                    for (x in -1..1) {
                        if (x == 0 && y == 0 && z == 0) continue
                        val neighbor = openNode(
                            n?.coordinate!!.x + x, n.coordinate.y + y, n.coordinate.z + z
                        )

                        // check blocked.
                        if (!(destinationBB.minX <= neighbor.coordinate!!.x && neighbor.coordinate.x <= destinationBB.maxX && destinationBB.minY <= neighbor.coordinate.y && neighbor.coordinate.y <= destinationBB.maxY && destinationBB.minZ <= neighbor.coordinate.z && neighbor.coordinate.z <= destinationBB.maxZ // near destination
                                    || !roomAccessor.isBlocked(
                                neighbor.coordinate.x,
                                neighbor.coordinate.y,
                                neighbor.coordinate.z
                            ))
                        ) { // not blocked
                            continue
                        }
                        if (neighbor.lastVisited == pfindIdx) continue
                        val gScore =
                            n.g.plus(MathHelper.sqrt_float((x * x + y * y + z * z).toFloat())) // altho it's sq, it should be fine
                        if (gScore < neighbor.g) {
                            neighbor.parent = n
                            neighbor.g = gScore
                            neighbor.f = gScore + distSq(
                                (goalNode.coordinate!!.x - neighbor.coordinate.x).toFloat(),
                                (goalNode.coordinate.y - neighbor.coordinate.y).toFloat(),
                                (goalNode.coordinate.z - neighbor.coordinate.z).toFloat()
                            )
                            open.add(neighbor)
                        } else if (neighbor.lastVisited != pfindIdx) {
                            neighbor.f = gScore + distSq(
                                (goalNode.coordinate!!.x - neighbor.coordinate.x).toFloat(),
                                (goalNode.coordinate.y - neighbor.coordinate.y).toFloat(),
                                (goalNode.coordinate.z - neighbor.coordinate.z).toFloat()
                            )
                            open.add(neighbor)
                        }
                    }
                }
            }
        }
        return true
    }
}