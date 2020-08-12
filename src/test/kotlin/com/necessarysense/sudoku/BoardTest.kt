package test.kotlin.com.necessarysense.sudoku

import main.kotlin.com.necessarysense.sudoku.Board
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

object BoardTest: Spek({
    describe("Board") {
        describe("Board Object") {
            describe("squares") {
                it("should have a length of 81") {
                    assertEquals(81, Board.SQUARES.size)
                }
            }

            describe("unitList") {
                it("should have a length of 27") {
                    assertEquals(27, Board.UNIT_LIST.size)
                }
            }

            describe("units") {
                it("should have a size 3 for each square") {
                    Board.SQUARES.forEach { square ->
                        assertNotNull(Board.units[square])
                        assertEquals(3, Board.units[square]!!.size)
                    }
                }

                it("should contain the correct contents for square C2") {
                    val c2Unit = Board.units[('C' to '2')]
                    assertEquals(
                        listOf(
                            listOf(('A' to '2'), ('B' to '2'), ('C' to '2'), ('D' to '2'), ('E' to '2'), ('F' to '2'), ('G' to '2'), ('H' to '2'), ('I' to '2')),
                            listOf(('C' to '1'), ('C' to '2'), ('C' to '3'), ('C' to '4'), ('C' to '5'), ('C' to '6'), ('C' to '7'), ('C' to '8'), ('C' to '9')),
                            listOf(('A' to '1'), ('A' to '2'), ('A' to '3'), ('B' to '1'), ('B' to '2'), ('B' to '3'), ('C' to '1'), ('C' to '2'), ('C' to '3'))
                        ),
                        c2Unit
                    )
                }
            }

            describe("peers") {
                it("should have a size 20 for each square") {
                    Board.SQUARES.forEach { square ->
                        assertNotNull(Board.PEERS[square])
                        assertEquals(20, Board.PEERS[square]!!.size)
                    }
                }

                it("should contain the correct contents for square C2") {
                    val c2Peers = Board.PEERS[('C' to '2')]
                    assertEquals(
                        setOf(('A' to '2'), ('B' to '2'), ('D' to '2'), ('E' to '2'), ('F' to '2'),
                            ('G' to '2'), ('H' to '2'), ('I' to '2'), ('C' to '1'), ('C' to '3'),
                            ('C' to '4'), ('C' to '5'), ('C' to '6'), ('C' to '7'), ('C' to '8'),
                            ('C' to '9'), ('A' to '1'), ('A' to '3'), ('B' to '1'), ('B' to '3')),
                        c2Peers
                    )
                }
            }
        }
    }
})
