package com.necessarysense.sudoku

import kotlinx.collections.immutable.*

sealed class BoardPossibility {
    abstract fun display(): String
}

object ImpossibleBoard : BoardPossibility() {
    override fun display(): String = "The board could not be assigned"
}

class Board(private val board: PersistentMap<SquarePosition, SquarePossibility>) : BoardPossibility() {
    fun assign(square: SquarePosition, digit: Char): BoardPossibility {
        return squareValue(square).assignValue(digit, square, this)
    }

    /* Eliminate d from values[s]; propagate when values or places <= 2.
    Return values, except return False if a contradiction is detected. */
    fun eliminate(square: SquarePosition, digit: Char): BoardPossibility {
        return squareValue(square).eliminateValue(digit, square, this)
    }

    fun newUnresolvedBoard(updatedPosition: SquarePosition, updatedPossibilities: Unresolved): Board =
        Board(board + (updatedPosition to updatedPossibilities))

    fun newResolvedBoard(resolvedPosition: SquarePosition, actualDigit: Char): BoardPossibility {
        val newBoardMap = board + (resolvedPosition to Resolved(actualDigit))
        return eliminateFromPeers(resolvedPosition, actualDigit, newBoardMap)
    }

    internal fun assignForUnits(
        square: SquarePosition,
        digit: Char
    ): BoardPossibility {
        val dPlaces = units[square]!!.flatMap { unitSquares: List<SquarePosition> ->
            unitSquares.filter { squareValue(it).hasDigit(digit) }
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
        val width = 1 + SQUARES.map { square -> squareValue(square).toString().length }.max()!!
        val splitLine = generateSequence { "-".repeat(width * 3) }.take(3).joinToString("+")
        return rows.joinToString("\n") { row ->
            val rowLine = cols.flatMap { col ->
                val colSplit = if (col == '3' || col == '6') {
                    "|"
                } else {
                    ""
                }
                val currSquare = squareValue(row to col).toString()
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

    fun search(): BoardPossibility {
        return if (isSolved()) {
            this
        } else {
            val firstSmallSquare = board.entries.
                filter { it.value is Unresolved }.
                map { (it.key to (it.value as Unresolved).possibilities.size) }.
                minBy { it.second }
            val firstSmallSquareValues = (board[firstSmallSquare!!.first] as Unresolved)
            firstSmallSquareValues.possibilities.asSequence().map { possibleDigit ->
                Game.mapBoardPossibility(
                    Game.mapBoardPossibility(Board(board)) { it.assign(firstSmallSquare.first, possibleDigit) })
                { it.search() }
            }.find { it is Board && it.isSolved() } ?: ImpossibleBoard
        }
    }

    fun isSolved(): Boolean = SQUARES.all { square -> squareValue(square) is Resolved }

    private fun squareValue(square: SquarePosition): SquarePossibility =
        board[square] ?: error("Could not find a value for $square")

    companion object {
        val digits: PersistentList<Char> = ('1'..'9').toPersistentList()
        private val rows: PersistentList<Char> = ('A'..'I').toPersistentList()
        private val cols: PersistentList<Char> = digits

        val SQUARES: PersistentList<SquarePosition> =
            crossProduct(
                rows,
                cols
            )
        internal val UNIT_LIST: PersistentList<PersistentList<SquarePosition>> =
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

        internal val units: PersistentMap<SquarePosition, List<List<SquarePosition>>> =
            SQUARES.fold(mapOf<SquarePosition, List<List<SquarePosition>>>()) { acc, square ->
                val containingUnits = UNIT_LIST.filter { connectionList ->
                    square in connectionList
                }
                acc + (square to containingUnits)
            }.toPersistentMap()

        internal val PEERS: PersistentMap<SquarePosition, PersistentSet<SquarePosition>> =
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

        /**
         * Create a random solved puzzle
         */
        tailrec fun generateRandom(): Board {
            tailrec fun assignUnresolved(newBoard: Board, currSquare: SquarePosition, currSquareValue: Unresolved): BoardPossibility {
                println("$currSquare possibilities ${currSquareValue.possibilities}")
                val choice = currSquareValue.possibilities.random()
                println("assigning $choice to $currSquare")
                val newNewBoard = newBoard.assign(currSquare, choice)
                return if (newNewBoard is Board) {
                    newNewBoard
                } else {
                    if (currSquareValue.possibilities.size == 1) {
                        return ImpossibleBoard
                    } else {
                        assignUnresolved(newBoard, currSquare, Unresolved(currSquareValue.possibilities - choice))
                    }
                }
            }

            val generatedBoard = SQUARES.fold(entirelyPossibleBoard() as BoardPossibility) { newBoard, currentSquare ->
                when (newBoard) {
                    is ImpossibleBoard -> ImpossibleBoard
                    is Board -> when (val sv = newBoard.squareValue(currentSquare)) {
                        is Resolved -> newBoard
                        is Unresolved -> assignUnresolved(newBoard, currentSquare, sv)
                    }
                }
            }
            return when (generatedBoard) {
                is ImpossibleBoard -> generateRandom()
                is Board -> generatedBoard
            }
        }
    }
}
