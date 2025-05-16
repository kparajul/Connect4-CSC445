import React, { useState } from 'react';
import Game from './Game';
import Lobby from './Lobby';

function App() {
  const [playerName, setPlayerName] = useState('');
  const [gameId, setGameId] = useState('');
  const [inGame, setInGame] = useState(false);

  return inGame ? (
    <Game
      playerName={playerName}
      gameId={gameId}
    />
  ) : (
    <Lobby
      playerName={playerName}
      setPlayerName={setPlayerName}
      gameId={gameId}
      setGameId={setGameId}
      setInGame={setInGame}
    />
  );
}

export default App;