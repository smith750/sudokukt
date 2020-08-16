package com.necessarysense.sudoku

import kotlinx.collections.immutable.*

sealed class BoardPossibility {
    abstract fun search(): BoardPossibility
    abstract fun display(): String
}

object ImpossibleBoard : BoardPossibility() {
    override fun search(): BoardPossibility = ImpossibleBoard
    override fun display(): String = "The board could not be assigned"
}

class Board(private val board: PersistentMap<SquarePosition, SquarePossibility>) : BoardPossibility() {
    fun assign(square: SquarePosition, digit: Char): BoardPossibility {
        return when (val squareValues = board[square] ?: error("Could not find square $square in board")) {
            is Resolved -> {
                if (squareValues.actuality != digit) {
                    ImpossibleBoard
                } else {
                    this
                }
            }
            is Unresolved -> {
                val valuesToEliminate: PersistentSet<Char> = squareValues.possibilities - digit
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
            is Resolved -> {
                if (squareValues.actuality == digit) {
                    // whoops! we're removing our resolved digit
                    ImpossibleBoard
                } else {
                    this
                }
            }
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
                Game.mapBoardPossibility(newBoard) { nb -> nb.assignForUnits(square, digit) }
            }
        }
    }

    private fun assignForUnits(
        square: SquarePosition,
        digit: Char
    ): BoardPossibility {
        val dPlaces = units[square]!!.flatMap { unitSquares: List<SquarePosition> ->
            unitSquares.filter { unitSquare ->
                when (val unitSquareValue = board[unitSquare]!!) {
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
                assign(dPlaces[0], digit)
            }
            else -> {
                this
            }
        }
    }

    private fun eliminateFromPeers(
        square: SquarePosition,
        remainingDigit: Char,
        newBoardMap: PersistentMap<SquarePosition, SquarePossibility>
    ): BoardPossibility {
        return PEERS[square]!!.fold(
            Board(newBoardMap) as BoardPossibility
        ) { b: BoardPossibility, peerSquare: SquarePosition ->
            Game.mapBoardPossibility(b) { brd -> brd.eliminate(peerSquare, remainingDigit) }
        }
    }

    override fun display(): String {
        val width = 1 + SQUARES.map { square -> board[square]!!.toString().length }.max()!!
        val splitLine = generateSequence { "-".repeat(width * 3) }.take(3).joinToString("+")
        return rows.joinToString("\n") { row ->
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
            if (row == 'C' || row == 'F') {
                rowLine + "\n" + splitLine
            } else {
                rowLine
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

    fun isSolved(): Boolean = SQUARES.all { square -> board[square]!! is Resolved }

    companion object {
        val digits: PersistentList<Char> = ('1'..'9').toPersistentList()
        private val rows: PersistentList<Char> = ('A'..'I').toPersistentList()
        private val cols: PersistentList<Char> = digits

        val SQUARES: PersistentList<SquarePosition> =
            crossProduct(
                rows,
                cols
            )
        private val UNIT_LIST: PersistentList<PersistentList<SquarePosition>> =
            (cols.map { col ->
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
                    }).map { l -> l.toPersistentList()}.toPersistentList()

        private val units: PersistentMap<SquarePosition, List<List<SquarePosition>>> =
            SQUARES.fold(mapOf<SquarePosition, List<List<SquarePosition>>>()) { acc, square ->
                val containingUnits = UNIT_LIST.filter { connectionList ->
                    square in connectionList
                }
                acc + (square to containingUnits)
            }.toPersistentMap()

        private val PEERS: PersistentMap<SquarePosition, PersistentSet<SquarePosition>> =
            SQUARES.fold(persistentMapOf()) { peerMap, square ->
                peerMap + (square to determinePeers(
                    square
                ))
            }

        fun entirelyPossibleBoard(): Board {
            val boardMap = SQUARES.fold(persistentMapOf<SquarePosition, SquarePossibility>()) { bMap, currSquare ->
                val digitsSet = mutableSetOf<Char>()
                digitsSet.addAll(digits)
                bMap + (currSquare to Unresolved(digitsSet.toPersistentSet()))
            }
            return Board(boardMap)
        }

        private fun crossProduct(a: List<Char>, b: List<Char>): PersistentList<SquarePosition> {
            return (a.flatMap { aEle ->
                b.map { bEle ->
                    (aEle to bEle)
                }
            }).toPersistentList()
        }

        // thanks to https://stackoverflow.com/questions/8154366/how-to-center-a-string-using-string-format
        private fun center(text: String, len: Int): String {
            val out = String.format("%" + len + "s%s%" + len + "s ", "", text, "")
            val mid = (out.length / 2).toFloat()
            val start = mid - len / 2
            val end = start + len
            return out.substring(start.toInt(), end.toInt())
        }

        /**
         * Find the set of squares which are parts of units with the current square,
         * but not including the current square
         */
        private fun determinePeers(square: SquarePosition): PersistentSet<SquarePosition> {
            val peerageUnits: List<List<SquarePosition>> =
                units[square] ?: error("Could not find units for square ${square.first}${square.second}")
            val allPeers = peerageUnits.fold(mutableSetOf<SquarePosition>()) { peerSet, peerList ->
                peerSet.addAll(peerList)
                peerSet
            }
            allPeers.remove(square)
            return allPeers.toPersistentSet()
        }
    }
}