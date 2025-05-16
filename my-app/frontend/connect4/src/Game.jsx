import React, { useState } from 'react';
import { useGameSocket } from './hooks/useGameSocket';
import Board from './Board';

const ROWS = 6;
const COLS = 7;

export default function Game() {
  const [messages, setMessages] = useState([]);
  const [gameId, setGameId] = useState('');
  const [playerName, setPlayerName] = useState('');
  const [board, setBoard] = useState(Array(ROWS).fill().map(() => Array(COLS).fill(null)));
  const [status, setStatus] = useState('');

  const addMessage = (msg) => {
    setMessages((prev) => [...prev, msg]);

    // Parse board from message
    if (msg.includes('Board:')) {
      const lines = msg.split('\n').filter(l => l.startsWith('|'));
      const parsed = lines.map(line =>
        line.replace(/\|/g, '').split('').map(cell => {
          if (cell === 'R') return 'R';
          if (cell === 'Y') return 'Y';
          return null;
        })
      );
      setBoard(parsed);
    }

    // Optional status updates
    if (msg.includes('win') || msg.includes('draw') || msg.includes('turn')) {
      setStatus(msg.split('\n')[0]);
    }
  };

  const send = useGameSocket('ws://localhost:26960', addMessage); // Point to leader server

  const createGame = () => {
    send(`NEW ${playerName}`);
  };

  const joinGame = () => {
    send(`JOIN ${playerName} ${gameId}`);
  };

  const move = (col) => {
    send(`MOVE ${col}`);
  };

  return (
    <div>
      <h1>Connect Four</h1>
      <input value={playerName} onChange={(e) => setPlayerName(e.target.value)} placeholder="Your name" />
      <input value={gameId} onChange={(e) => setGameId(e.target.value)} placeholder="Game ID" />
      <button onClick={createGame}>Create Game</button>
      <button onClick={joinGame}>Join Game</button>
      <p><strong>Status:</strong> {status}</p>
      <Board board={board} move={move} />
      <pre>{messages.join('\n')}</pre>
    </div>
  );
}
