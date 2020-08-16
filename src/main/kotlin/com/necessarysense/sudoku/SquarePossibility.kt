package com.necessarysense.sudoku

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.minus

sealed class SquarePossibility {
    abstract fun hasDigit(digit: Char): Boolean
    abstract fun assignValue(digit: Char, squarePosition: SquarePosition, board: Board): BoardPossibility
    abstract fun eliminateValue(digit: Char, squarePosition: SquarePosition, board: Board): BoardPossibility
}

data class Unresolved(val possibilities: PersistentSet<Char>) : SquarePossibility() {
    override fun toString(): String {
        return possibilities.joinToString("")
    }

    override fun hasDigit(digit: Char): Boolean = (digit in possibilities)

    override fun assignValue(digit: Char, squarePosition: SquarePosition, board: Board): BoardPossibility {
        val valuesToEliminate: PersistentSet<Char> = possibilities - digit
        return valuesToEliminate.fold(board as BoardPossibility) { b: BoardPossibility, eliminationDigit: Char ->
            when (b) {
                is ImpossibleBoard -> ImpossibleBoard
                is Board -> b.eliminate(squarePosition, eliminationDigit)
            }
        }
    }

    override fun eliminateValue(digit: Char, squarePosition: SquarePosition, board: Board): BoardPossibility {
        val newPossibilities = possibilities - digit
        // (1) If a square s is reduced to one value d2, then eliminate d2 from the peers.
        val newBoard = when {
            newPossibilities.isEmpty() -> {
                // we removed the remaining digit! impossible board
                ImpossibleBoard
            }
            newPossibilities.size == 1 -> {
                board.newResolvedBoard(squarePosition, newPossibilities.firstOrNull()!!)
            }
            else -> {
                board.newUnresolvedBoard(squarePosition, Unresolved(newPossibilities))
            }
        }
        // (2) If a unit u is reduced to only one place for a value d, then put it there.
        return Game.mapBoardPossibility(newBoard) { it.assignForUnits(squarePosition, digit) }
    }
}

data class Resolved(val actuality: Char) : SquarePossibility() {
    override fun toString(): String {
        return actuality.toString()
    }

    override fun hasDigit(digit: Char): Boolean = (actuality == digit)

    override fun assignValue(digit: Char, squarePosition: SquarePosition, board: Board): BoardPossibility {
        return if (actuality != digit) {
            ImpossibleBoard
        } else {
            board
        }
    }

    override fun eliminateValue(digit: Char, squarePosition: SquarePosition, board: Board): BoardPossibility {
        return if (actuality == digit) {
            // whoops! we're removing our resolved digit
            ImpossibleBoard
        } else {
            board
        }
    }
}
