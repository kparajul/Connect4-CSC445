import React from 'react';
import Cell from './Cell';
import './styles.css';

const Board = ({ board, move }) => {
  return (
    <div className="board">
      {board.map((row, rowIndex) => (
        <div className="row" key={rowIndex}>
          {row.map((cell, colIndex) => (
            <Cell
              key={`${rowIndex}-${colIndex}`}
              value={cell}
              onClick={() => move(colIndex)}
            />
          ))}
        </div>
      ))}
    </div>
  );
};

export default Board;
