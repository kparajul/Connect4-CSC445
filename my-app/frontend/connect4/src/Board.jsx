import React, { useState } from 'react';
import Cell from './Cell';

const ROWS = 6;
const COLS = 7;

const Board = ({ currentPlayer, setCurrentPlayer, setWinner, winner }) => {
  const [grid, setGrid] = useState(Array(ROWS).fill().map(() => Array(COLS).fill(null)));

  const checkWinner = (grid) => {
    const directions = [
      [0, 1], [1, 0], [1, 1], [1, -1]
    ];

    for (let r = 0; r < ROWS; r++) {
      for (let c = 0; c < COLS; c++) {
        const player = grid[r][c];
        if (!player) continue;

        for (let [dr, dc] of directions) {
          let win = true;
          for (let k = 1; k < 4; k++) {
            const nr = r + dr * k;
            const nc = c + dc * k;
            if (nr < 0 || nr >= ROWS || nc < 0 || nc >= COLS || grid[nr][nc] !== player) {
              win = false;
              break;
            }
          }
          if (win) return player;
        }
      }
    }

    return null;
  };

  const handleClick = (col) => {
    if (winner) return;

    const newGrid = [...grid.map(row => [...row])];

    for (let row = ROWS - 1; row >= 0; row--) {
      if (!newGrid[row][col]) {
        newGrid[row][col] = currentPlayer;
        const winner = checkWinner(newGrid);
        if (winner) {
          setWinner(winner);
        } else {
          setCurrentPlayer(currentPlayer === 'R' ? 'Y' : 'R');
        }
        setGrid(newGrid);
        return;
      }
    }
  };

  return (
    <div className="board">
      {grid.map((row, rowIndex) =>
        <div key={rowIndex} className="row">
          {row.map((cell, colIndex) =>
            <Cell
              key={colIndex}
              value={cell}
              onClick={() => handleClick(colIndex)}
            />
          )}
        </div>
      )}
    </div>
  );
};

export default Board;
