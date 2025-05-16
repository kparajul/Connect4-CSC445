import React from 'react';

export default function Lobby({ playerName, setPlayerName, gameId, setGameId, setInGame }) {
  return (
    <div>
      <h1>Welcome to Connect Four</h1>
      <input value={playerName} onChange={(e) => setPlayerName(e.target.value)} placeholder="Your name" />
      <input value={gameId} onChange={(e) => setGameId(e.target.value)} placeholder="Game ID (for joining)" />
      <button onClick={() => setInGame(true)}>Start</button>
    </div>
  );
}