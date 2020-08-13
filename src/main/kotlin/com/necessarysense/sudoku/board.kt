package com.necessarysense.sudoku

typealias SquarePosition = Pair<Char, Char>

sealed class SquarePossibility
data class Unresolved(val possibilities: Set<Char>) : SquarePossibility() {
    override fun toString(): String {
        return possibilities.joinToString("")
    }
}

data class Resolved(val actuality: Char) : SquarePossibility() {
    override fun toString(): String {
        return actuality.toString()
    }
}

sealed class BoardPossibility {
    abstract fun search(): BoardPossibility
}

object ImpossibleBoard : BoardPossibility() {
    override fun search(): BoardPossibility = ImpossibleBoard
}

class Board(private val board: Map<SquarePosition, SquarePossibility>) : BoardPossibility() {
    private fun assign(square: SquarePosition, digit: Char): BoardPossibility {
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
                valuesToEliminate.fold(this as BoardPossibility) { b: BoardPossibility, eliminationDigit: Char ->
                    when (b) {
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
        return when (val squareValues = board[square] ?: error("Could not find square $square in board")) {
            is Resolved -> this
            is Unresolved -> {
                val newPossibilities = squareValues.possibilities - digit
                // (1) If a square s is reduced to one value d2, then eliminate d2 from the peers.
                val newBoard = when {
                    newPossibilities.isEmpty() -> {
                        // we removed the remaining digit! impossible board
                        ImpossibleBoard
                    }
                    newPossibilities.size == 1 -> {
                        val remainingDigit = newPossibilities.firstOrNull()!!
                        val newBoardMap = board + (square to Resolved(remainingDigit))
                        eliminateFromPeers(square, remainingDigit, newBoardMap)
                    }
                    else -> {
                        Board(
                            board + (square to Unresolved(
                                newPossibilities
                            ))
                        )
                    }
                }
                // (2) If a unit u is reduced to only one place for a value d, then put it there.
                when (newBoard) {
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
        val dPlaces = units[square]!!.flatMap { unitSquares: List<SquarePosition> ->
            unitSquares.filter { unitSquare ->
                when (val unitSquareValue = newBoard.board[unitSquare]!!) {
                    is Resolved -> {
                        digit == unitSquareValue.actuality
                    }
                    is Unresolved -> {
                        digit in unitSquareValue.possibilities
                    }
                }
            }
        }
        return when {
            dPlaces.isEmpty() -> {
                ImpossibleBoard
            }
            dPlaces.size == 1 -> {
                newBoard.assign(dPlaces[0], digit)
            }
            else -> {
                newBoard
            }
        }
    }

    private fun eliminateFromPeers(
        square: SquarePosition,
        remainingDigit: Char,
        newBoardMap: Map<SquarePosition, SquarePossibility>
    ): BoardPossibility {
        return PEERS[square]!!.fold(
            Board(newBoardMap) as BoardPossibility
        ) { b: BoardPossibility, peerSquare: SquarePosition ->
            when (b) {
                is ImpossibleBoard -> ImpossibleBoard
                is Board -> {
                    b.eliminate(peerSquare, remainingDigit)
                }
            }
        }
    }

    fun display() {
        val width = 1 + SQUARES.map { square -> board[square]!!.toString().length }.max()!!
        val splitLine = generateSequence { "-".repeat(width * 3) }.take(3).joinToString("+")
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

    override fun search(): BoardPossibility {
        return if (isSolved()) {
            this
        } else {
            val firstSmallSquare = board.entries.
                filter { boardEntry -> boardEntry.value is Unresolved }.
                map { boardEntry -> (boardEntry.key to (boardEntry.value as Unresolved).possibilities.size ) }.
                minBy { entryPair -> entryPair.second }
            val firstSmallSquareValues = (board[firstSmallSquare!!.first] as Unresolved)
            firstSmallSquareValues.possibilities.asSequence().map { possibleDigit ->
                Board(board).assign(firstSmallSquare.first, possibleDigit).search()
            }.find { newBoard -> newBoard is Board && newBoard.isSolved() } ?: ImpossibleBoard
        }
    }

    private fun isSolved(): Boolean = SQUARES.all { square -> board[square]!! is Resolved }

    companion object {
        private val digits: List<Char> = ('1'..'9').toList()
        val rows: List<Char> = ('A'..'I').toList()
        val cols: List<Char> = digits
        private val parsableChars: List<Char> = digits + listOf('0', '.')

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
                    square in connectionList
                }
                acc + (square to containingUnits)
            }

        val PEERS: Map<SquarePosition, Set<SquarePosition>> =
            SQUARES.fold(mapOf()) { peerMap, square ->
                peerMap + (square to determinePeers(
                    square
                ))
            }

        fun solve(grid: String): BoardPossibility = parseGrid(grid).search()

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
                when (b) {
                    is Board ->
                        if ((squareAssignment.second) in digits) {
                            b.assign(squareAssignment.first, squareAssignment.second)
                        } else {
                            b
                        }
                    is ImpossibleBoard -> ImpossibleBoard
                }
            }
        }

        private fun gridValues(grid: String): List<Pair<SquarePosition, Char>> {
            val puzzleChars = grid.toCharArray().filter { c -> c in parsableChars }
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
            val peerageUnits: List<List<SquarePosition>> =
                units[square] ?: error("Could not find units for square ${square.first}${square.second}")
            val allPeers = peerageUnits.fold(mutableSetOf<SquarePosition>()) { peerSet, peerList ->
                peerSet.addAll(peerList)
                peerSet
            }
            allPeers.remove(square)
            return allPeers
        }
    }
}
