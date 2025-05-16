import React, { useState } from 'react';
import { useGameSocket } from './hooks/useGameSocket';

export default function Game() {
  const [messages, setMessages] = useState([]);
  const [gameId, setGameId] = useState('');
  const [playerName, setPlayerName] = useState('');

  const addMessage = (msg) => setMessages((prev) => [...prev, msg]);

  const send = useGameSocket('ws://localhost:26960', addMessage); // use your Raft leader host

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
      <div>
        {[...Array(7)].map((_, i) => (
          <button key={i} onClick={() => move(i)}>Drop in column {i}</button>
        ))}
      </div>
      <pre>{messages.join('\n')}</pre>
    </div>
  );
}