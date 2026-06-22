import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import toast from "react-hot-toast";
import { useNavigate } from 'react-router-dom';

export function useWebSocket(concertId, tierId, userId,wsKey=0) {
  const [seatUpdates, setSeatUpdates] = useState({});
  const [availabilityUpdates, setAvailabilityUpdates] = useState({});
  const [viewerCount, setViewerCount] = useState(0);
  const [queueSize, setQueueSize] = useState(0);
  const [myPosition, setMyPosition] = useState(null);
  const [queuePosition, setQueuePosition] = useState(null);
  const [admitted, setAdmitted] = useState(false);
  const [connected, setConnected] = useState(false);
  const clientRef = useRef(null);
const navigate = useNavigate();
  useEffect(() => {
    if (!concertId && !tierId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS('https://convenespace.space/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);

        if (concertId) {
          client.subscribe(`/topic/concert.${concertId}.seats`, (message) => {
            const data = JSON.parse(message.body);
            setSeatUpdates((prev) => ({ ...prev, [data.seatId]: data.status }));
          });

          client.subscribe(`/topic/concert.${concertId}.availability`, (message) => {
            const data = JSON.parse(message.body);
            setAvailabilityUpdates((prev) => ({ ...prev, [data.tierId]: data.availableQuantity }));
          });

          client.subscribe(`/topic/concert.${concertId}.viewers`, (message) => {
            const data = JSON.parse(message.body);
            setViewerCount(data.viewerCount ?? 0);
          });

          client.subscribe(`/topic/concert.${concertId}.activeUsers`, (message) => {
            const data = JSON.parse(message.body);
            setViewerCount(data.activeUsers ?? 0);
          });
        }

        if (tierId) {
          client.subscribe(`/topic/waitlist.${tierId}.queue`, (message) => {
            const data = JSON.parse(message.body);
            setQueueSize(data.queueSize ?? 0);
          });

          const token = localStorage.getItem('accessToken');
          
            client.subscribe(`/user/queue/waitlist-position`, (message) => {
              const data = JSON.parse(message.body);
              setMyPosition(data);
            });
          
            client.subscribe(`/user/queue/waitlist`, (message) => {
              const data = JSON.parse(message.body);
              if (data.type === 'PROMOTED') {
                console.log('PROMOTED', data);
              }
            });
          
     if (userId) {
    client.subscribe(`/topic/queue.${tierId}.position.${userId}`, (message) => {
        const data = JSON.parse(message.body);
        setQueuePosition(data);
    });
}


  client.subscribe(`/topic/queue.${tierId}.admission`, (message) => {
  const data = JSON.parse(message.body);
    console.log("Admitted User:", data.admittedUserId);
  console.log("Current User:", userId);

  if (
      data.type === 'QUEUE_ADMISSION' &&
      data.admittedUserId === userId
  ) {
      setAdmitted(true);
  }
}); 
        }
      },
      onDisconnect: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;

    return () => client.deactivate();
  }, [concertId, tierId, userId, wsKey]);

  return { seatUpdates, availabilityUpdates, viewerCount, connected, queueSize, myPosition, queuePosition, admitted };
}
