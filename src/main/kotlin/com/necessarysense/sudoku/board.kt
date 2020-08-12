package main.kotlin.com.necessarysense.sudoku

typealias SquarePosition = Pair<Char, Char>

sealed class SquarePossibility
data class Unresolved(val possibilities: Set<Char>): SquarePossibility() {
    override fun toString(): String {
        return possibilities.joinToString("")
    }
}
data class Resolved(val actuality: Char): SquarePossibility() {
    override fun toString(): String {
        return actuality.toString()
    }
}

sealed class BoardPossibility

object ImpossibleBoard: BoardPossibility()

class Board(private val board: Map<SquarePosition, SquarePossibility>): BoardPossibility() {
    private fun assign(square: SquarePosition, digit: Char): BoardPossibility {
        println("assigning $digit to $square")
        return when (val squareValues = board[square] ?: error("Could not find square $square in board")) {
            is Resolved -> {
                if (squareValues.actuality != digit) {
                    ImpossibleBoard
                } else {
                    this
                }
            }
            is Unresolved -> {
                val valuesToEliminate: Set<Char> = squareValues.possibilities - digit
                println("values to eliminate = $valuesToEliminate")
                val newBoard: BoardPossibility = Board(board + (square to Resolved(digit)))
                valuesToEliminate.fold(newBoard) { b: BoardPossibility, eliminationDigit: Char ->
                    when(b) {
                        is ImpossibleBoard -> ImpossibleBoard
                        is Board -> b.eliminate(square, eliminationDigit)
                    }
                }
            }
        }
    }

    /* Eliminate d from values[s]; propagate when values or places <= 2.
    Return values, except return False if a contradiction is detected. */
    fun eliminate(square: SquarePosition, digit: Char): BoardPossibility {
        println("eliminating $digit from $square")
        return when (val squareValues = board[square] ?: error("Could not find square $square in board")) {
            is Resolved -> this
            is Unresolved -> {
                println("got to this point")
//                if (!squareValues.possibilities.contains(digit)) {
//                    // the digit has already been eliminated, return the board
//                    return this
//                }
                val newPossibilities = squareValues.possibilities - digit
                println("new possibilities? $newPossibilities")
                val newBoardMap = board + (square to Unresolved(
                    newPossibilities
                ))
                // (1) If a square s is reduced to one value d2, then eliminate d2 from the peers.
                // (2) If a unit u is reduced to only one place for a value d, then put it there.
                when(val newBoard = eliminateFromPeers(newPossibilities, square, newBoardMap)) {
                    is Board -> assignForUnits(square, newBoard, digit)
                    is ImpossibleBoard -> ImpossibleBoard
                }
            }
        }
    }

    private fun assignForUnits(
        square: SquarePosition,
        newBoard: Board,
        digit: Char
    ): BoardPossibility {
        val dplaces = units[square]!!.flatMap { unitSquares: List<SquarePosition> ->
            unitSquares.filter { unitSquare ->
                when (val unitSquareValue = newBoard.board[unitSquare]!!) {
                    is Resolved -> {
                        digit == unitSquareValue.actuality
                    }
                    is Unresolved -> {
                        unitSquareValue.possibilities.contains(digit)
                    }
                }
            }
        }
        return when {
            dplaces.isEmpty() -> {
                ImpossibleBoard
            }
            dplaces.size == 1 -> {
                newBoard.assign(dplaces[0], digit)
            }
            else -> {
                newBoard
            }
        }
    }

    private fun eliminateFromPeers(
        newPossibilities: Set<Char>,
        square: SquarePosition,
        newBoardMap: Map<SquarePosition, SquarePossibility>
    ): BoardPossibility {
        println("eliminating from peers, new possibilities = $newPossibilities")
        return if (newPossibilities.size == 1) {
            val remainingDigit = newPossibilities.firstOrNull()!!
            PEERS[square]!!.fold(
                Board(newBoardMap) as BoardPossibility
            ) { b: BoardPossibility, peerSquare: SquarePosition ->
                println("eliminating $remainingDigit from peer $peerSquare")
                when (b) {
                    is ImpossibleBoard -> ImpossibleBoard
                    is Board -> {
                        b.eliminate(peerSquare, remainingDigit)
                    }
                }
            }
        } else {
            Board(newBoardMap)
        }
    }

    fun display() {
        val width = 1 + SQUARES.map { square -> board[square]!!.toString().length }.max()!!
        val splitLine = generateSequence { "-".repeat(width*3) }.take(3).joinToString("+")
        rows.forEach { row ->
            val rowLine = cols.flatMap { col ->
                val colSplit = if (col == '3' || col == '6') {
                    "|"
                } else {
                    ""
                }
                val currSquare = board[(row to col)]!!.toString()
                listOf(
                    center(currSquare, width),
                    colSplit
                )
            }.joinToString("")
            println(rowLine)
            if (row == 'C' || row == 'F') {
                println(splitLine)
            }
        }
    }

    companion object {
        val digits: List<Char> = ('1'..'9').toList()
        val rows: List<Char> = ('A'..'I').toList()
        val cols: List<Char> = digits
        val parsableChars: List<Char> = digits + listOf('0', '.')

        val SQUARES: List<SquarePosition> =
            crossProduct(
                rows,
                cols
            )
        val UNIT_LIST: List<List<SquarePosition>> =
            cols.map { col ->
                SQUARES.filter { square -> square.second == col }
            } +
                    rows.map { row ->
                        SQUARES.filter { square -> square.first == row }
                    } +
                    rows.chunked(3).flatMap { unitRows ->
                        cols.chunked(3).map { unitCols ->
                            crossProduct(
                                unitRows,
                                unitCols
                            )
                        }
                    }

        val units: Map<SquarePosition, List<List<SquarePosition>>> =
            SQUARES.fold(mapOf()) { acc, square ->
                val containingUnits = UNIT_LIST.filter { connectionList ->
                    connectionList.contains(square)
                }
                acc + (square to containingUnits)
            }

        val PEERS: Map<SquarePosition, Set<SquarePosition>> =
            SQUARES.fold(mapOf()) { peerMap, square ->
                peerMap + (square to determinePeers(
                    square
                ))
            }

        private fun entirelyPossibleBoard(): Board {
            val boardMap = SQUARES.fold(mapOf<SquarePosition, SquarePossibility>()) { bMap, currSquare ->
                val digitsSet = mutableSetOf<Char>()
                digitsSet.addAll(digits)
                bMap + (currSquare to Unresolved(digitsSet))
            }
            return Board(boardMap)
        }

        fun parseGrid(grid: String): BoardPossibility {
            val startingBoard: BoardPossibility = entirelyPossibleBoard()
            return gridValues(grid).fold(startingBoard) { b, squareAssignment ->
                println("board = $b squareAssignment = $squareAssignment")
                when (b) {
                    is Board ->
                        if (digits.contains(squareAssignment.second)) {
                            b.assign(squareAssignment.first, squareAssignment.second)
                        } else {
                            b
                        }
                    is ImpossibleBoard -> ImpossibleBoard
                }
            }
        }

        private fun gridValues(grid: String): List<Pair<SquarePosition, Char>> {
            val puzzleChars = grid.toCharArray().filter { c -> parsableChars.contains(c) }
            if (puzzleChars.size != 81) {
                throw IllegalArgumentException("Given grid had ${puzzleChars.size} valid characters in it; it should have exactly 81 valid characters")
            }
            return SQUARES.zip(puzzleChars)
        }

        private fun crossProduct(a: List<Char>, b: List<Char>): List<SquarePosition> {
            return a.flatMap { aEle ->
                b.map { bEle ->
                    (aEle to bEle)
                }
            }
        }

        // thanks to https://stackoverflow.com/questions/8154366/how-to-center-a-string-using-string-format
        fun center(text: String, len: Int): String {
            val out = String.format("%" + len + "s%s%" + len + "s", "", text, "")
            val mid = (out.length / 2).toFloat()
            val start = mid - len / 2
            val end = start + len
            return out.substring(start.toInt(), end.toInt())
        }

        /**
         * Find the set of squares which are parts of units with the current square,
         * but not including the current square
         */
        private fun determinePeers(square: SquarePosition): Set<SquarePosition> {
            val peerageUnits: List<List<SquarePosition>> = units[square] ?: error("Could not find units for square ${square.first}${square.second}")
            val allPeers = peerageUnits.fold(mutableSetOf<SquarePosition>()) { peerSet, peerList ->
                peerSet.addAll(peerList)
                peerSet
            }
            allPeers.remove(square)
            return allPeers
        }
    }
}
