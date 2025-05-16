import React, { useState, useEffect } from 'react';
import { useGameSocket } from './hooks/useGameSocket';
import Board from './Board';

const ROWS = 6;
const COLS = 7;

export default function Game({ playerName, gameId }) {
  const [messages, setMessages] = useState([]);
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

  const send = useGameSocket('ws://localhost:26960', addMessage);

  useEffect(() => {
    if (gameId) {
      send(`JOIN ${playerName} ${gameId}`);
    } else {
      send(`NEW ${playerName}`);
    }
  }, [playerName, gameId]);

  const move = (col) => {
    send(`MOVE ${col}`);
  };

  return (
    <div>
      <h1>Connect Four</h1>
      <p><strong>Status:</strong> {status}</p>
      <Board board={board} move={move} />
      <pre>{messages.join('\n')}</pre>
    </div>
  );
}
