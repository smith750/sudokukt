package com.necessarysense.sudoku

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList

object Game {
    private val parsableChars: PersistentList<Char> = Board.digits + persistentListOf('0', '.')

    fun solve(grid: String): BoardPossibility = parseGrid(grid).search()

    private fun parseGrid(grid: String): BoardPossibility {
        val startingBoard: BoardPossibility = Board.entirelyPossibleBoard()
        return gridValues(grid).fold(startingBoard) { b, squareAssignment ->
            when (b) {
                is Board ->
                    if ((squareAssignment.second) in Board.digits) {
                        b.assign(squareAssignment.first, squareAssignment.second)
                    } else {
                        b
                    }
                is ImpossibleBoard -> ImpossibleBoard
            }
        }
    }

    private fun gridValues(grid: String): PersistentList<Pair<SquarePosition, Char>> {
        val puzzleChars = grid.toCharArray().filter { c -> c in parsableChars }
        if (puzzleChars.size != 81) {
            throw IllegalArgumentException("Given grid had ${puzzleChars.size} valid characters in it; it should have exactly 81 valid characters")
        }
        return (Board.SQUARES.zip(puzzleChars)).toPersistentList()
    }

    fun mapBoardPossibility(bp: BoardPossibility, transformation: (Board) -> BoardPossibility): BoardPossibility {
        return when (bp) {
            is ImpossibleBoard -> ImpossibleBoard
            is Board -> transformation(bp)
        }
    }
}
