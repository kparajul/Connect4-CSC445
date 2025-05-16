import React from 'react';
import Game from './Game';

function App() {
  return <Game />;
}

export default App;

/*import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';

const ROWS = 6;
const COLS = 7;

const Cell = ({ value, onClick }) => (
  <div className="cell" onClick={onClick}>
    {value && <div className={`disc ${value === 'R' ? 'red' : 'yellow'}`} />}
  </div>
);

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
    const newGrid = grid.map((row) => [...row]);
    for (let row = ROWS - 1; row >= 0; row--) {
      if (!newGrid[row][col]) {
        newGrid[row][col] = currentPlayer;
        const foundWinner = checkWinner(newGrid);
        if (foundWinner) {
          setWinner(foundWinner);
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
      {grid.map((row, rowIndex) => (
        <div key={rowIndex} className="row">
          {row.map((cell, colIndex) => (
            <Cell key={colIndex} value={cell} onClick={() => handleClick(colIndex)} />
          ))}
        </div>
      ))}
    </div>
  );
};

const App = () => {
  const [screen, setScreen] = useState('home'); 
  const [currentPlayer, setCurrentPlayer] = useState('R');
  const [winner, setWinner] = useState(null);
  const [username, setUsername] = useState('');
  const [showUsernameModal, setShowUsernameModal] = useState(false); // start as false
  const [selectedOpponent, setSelectedOpponent] = useState('');

  // Mocked list of usernames for now
  const availableUsernames = ['Alice', 'Bob', 'Charlie', 'Dana'];

  const handleUsernameSubmit = (e) => {
    e.preventDefault();
    if (username.trim()) {
      setShowUsernameModal(false);
      setScreen('select-opponent');
    }
  };

  const handleOpponentSelect = (opponent) => {
    setSelectedOpponent(opponent);
    setScreen('home');
  };

  const startGame = () => {
    setCurrentPlayer('R');
    setWinner(null);
    setScreen('game');
  };

  const returnHome = () => {
    setScreen('home');
  };

  // Username modal
  if (showUsernameModal) {
    return (
      <div className="modal-overlay">
        <div className="modal-box">
          <h2>Enter your username</h2>
          <form onSubmit={handleUsernameSubmit}>
            <input
              type="text"
              value={username}
              onChange={e => setUsername(e.target.value)}
              placeholder="Username"
              autoFocus
            />
            <button type="submit">Continue</button>
          </form>
        </div>
      </div>
    );
  }

  // Opponent selection screen
  if (screen === 'select-opponent') {
    return (
      <div className="app select-opponent-screen">
        <h2>Welcome, {username}!</h2>
        <h3>Select a player to play against:</h3>
        <ul className="opponent-list">
          {availableUsernames.filter(name => name !== username).map(name => (
            <li key={name}>
              <button onClick={() => handleOpponentSelect(name)}>{name}</button>
            </li>
          ))}
        </ul>
      </div>
    );
  }

  // Home screen with Join Game button
  if (screen === 'home') {
    return (
      <div className="app home-screen">
        <h1>Connect Four</h1>
        <button className="start-button" onClick={() => setShowUsernameModal(true)}>
          Join Game
        </button>
      </div>
    );
  }

  return (
    <div className="app">
      <h1>Connect Four</h1>
      {winner ? (
        <h2>🎉 Player {winner} wins!</h2>
      ) : (
        <h2>
          Current Player:{' '}
          <span className={currentPlayer === 'R' ? 'red' : 'yellow'}>
            {currentPlayer}
          </span>
        </h2>
      )}
      <Board
        currentPlayer={currentPlayer}
        setCurrentPlayer={setCurrentPlayer}
        setWinner={setWinner}
        winner={winner}
      />
      {winner && (
        <button className="home-button" onClick={returnHome}>Back to Home</button>
      )}
    </div>
  );
  
};

export default App;
*/