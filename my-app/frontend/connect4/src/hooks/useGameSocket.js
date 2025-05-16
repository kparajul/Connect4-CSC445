// hooks/useGameSocket.js
import { useEffect, useRef } from 'react';

export function useGameSocket(serverUrl, onMessage) {
  const socketRef = useRef(null);

  useEffect(() => {
    socketRef.current = new WebSocket(serverUrl);

    socketRef.current.onopen = () => {
      console.log('Connected to game server');
    };

    socketRef.current.onmessage = (event) => {
      console.log('Message from server:', event.data);
      onMessage(event.data);
    };

    socketRef.current.onclose = () => {
      console.log('Disconnected');
    };

    socketRef.current.onerror = (err) => {
      console.error('WebSocket error:', err);
    };

    return () => socketRef.current?.close();
  }, [serverUrl]);

  const send = (msg) => {
    if (socketRef.current && socketRef.current.readyState === WebSocket.OPEN) {
      socketRef.current.send(`gameRequest:${msg}`);
    }
  };

  return send;
}