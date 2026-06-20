import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Users, Clock, CheckCircle, Loader2, Wifi } from 'lucide-react';
import { Button } from '../components/ui/button';
import { useWebSocket } from '../hooks/useWebSocket';
import api from '../lib/axios';
import toast from 'react-hot-toast';
import useBookingStore from '../store/bookingStore';
import useAuthStore from '../store/authStore';

export default function VirtualQueuePage() {
  const { tierId } = useParams();
  const navigate = useNavigate();
  const { selectedConcert } = useBookingStore();
  const { user } = useAuthStore();

  const [joining, setJoining] = useState(true);
  const [position, setPosition] = useState(null);
  const [peopleAhead, setPeopleAhead] = useState(null);
  const [totalInQueue, setTotalInQueue] = useState(null);
  const [estimatedWait, setEstimatedWait] = useState(null);
  const [isAdmitted, setIsAdmitted] = useState(false);
  const [concertId, setConcertId] = useState(selectedConcert?.id || null);
  const [wsKey, setWsKey] = useState(0);

  // Step 1 — Join queue
  useEffect(() => {
    const join = async () => {
      try {
        const res = await api.post(`/api/v1/queue/join/${tierId}`);
        const data = res.data.data;
        setPosition(data.position);
        setPeopleAhead(data.peopleAhead);
        setTotalInQueue(data.totalInQueue);
        setEstimatedWait(data.estimatedWaitMinutes);
        setJoining(false);

        if (!concertId) {
          const concertRes = await api.get(`/api/v1/queue/concert-id/${tierId}`);
          setConcertId(concertRes.data.data);
        }
      } catch (err) {
        toast.error(err.response?.data?.message || 'Failed to join queue');
        navigate(-1);
      }
    };
    join();
  }, [tierId]);

  // Step 2 — Force WebSocket reconnect when concertId loads
  useEffect(() => {
    if (concertId) {
      setWsKey(prev => prev + 1);
    }
  }, [concertId]);

  const { queuePosition, admitted } = useWebSocket(concertId, tierId, user?.id, wsKey);

  // Step 3 — Live position updates
  useEffect(() => {
    if (!queuePosition) return;
    setPosition(queuePosition.position);
    setPeopleAhead(queuePosition.peopleAhead);
    setTotalInQueue(queuePosition.queueSize);
    setEstimatedWait(queuePosition.estimatedWaitMinutes);
  }, [queuePosition]);

  // Step 4 — WebSocket admission
useEffect(() => {
  if (!admitted) return;
  setIsAdmitted(true);
  setTimeout(() => {
    navigate(`/book/${tierId}`);
  }, 2500);
}, [admitted]);

  // Step 5 — Polling fallback
  useEffect(() => {
  const interval = setInterval(async () => {
    try {
      const res = await api.get(`/api/v1/queue/has-token/${tierId}`);
      if (res.data.data === true) {
        setIsAdmitted(true);
        clearInterval(interval);
        setTimeout(() => {
          navigate(`/book/${tierId}`);
        }, 2500);
      }
    } catch {}
  }, 3000);
  return () => clearInterval(interval);
}, [tierId]);

  const handleLeave = async () => {
    try {
      await api.delete(`/api/v1/queue/leave/${tierId}`);
    } catch {}
    navigate(-1);
  };

  if (isAdmitted) {
    return (
      <div className="max-w-md mx-auto py-20 px-4 text-center space-y-6">
        <div className="flex justify-center">
          <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center">
            <CheckCircle className="h-10 w-10 text-green-500" />
          </div>
        </div>
        <div>
          <h1 className="text-2xl font-bold">You're In!</h1>
          <p className="text-muted-foreground mt-1">Taking you to booking page...</p>
        </div>
        <Loader2 className="h-5 w-5 animate-spin mx-auto text-primary" />
      </div>
    );
  }

  if (joining) {
    return (
      <div className="max-w-md mx-auto py-20 px-4 text-center space-y-4">
        <Loader2 className="h-10 w-10 animate-spin mx-auto text-primary" />
        <p className="text-muted-foreground">Joining queue...</p>
      </div>
    );
  }

  return (
    <div className="max-w-md mx-auto py-16 px-4 space-y-6">
      <div className="text-center space-y-3">
        <div className="inline-flex items-center gap-2 bg-amber-100 text-amber-800 px-3 py-1.5 rounded-full text-sm font-medium">
          <span className="w-2 h-2 bg-amber-500 rounded-full animate-pulse" />
          High Demand Event
        </div>
        <h1 className="text-2xl font-bold">Virtual Queue</h1>
        {selectedConcert?.title && (
          <p className="text-sm text-muted-foreground">{selectedConcert.title}</p>
        )}
      </div>

      <div className="bg-card border rounded-2xl p-8 shadow-sm space-y-6">
        <div className="text-center">
          <div className="flex justify-center mb-3">
            <div className="w-14 h-14 bg-primary/10 rounded-full flex items-center justify-center">
              <Users className="h-7 w-7 text-primary" />
            </div>
          </div>
          <p className="text-7xl font-bold text-primary">#{position}</p>
          <p className="text-sm text-muted-foreground mt-2">Your position in queue</p>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div className="bg-muted rounded-xl p-4 text-center">
            <p className="text-2xl font-bold">{peopleAhead ?? '—'}</p>
            <p className="text-xs text-muted-foreground mt-1">People ahead</p>
          </div>
          <div className="bg-muted rounded-xl p-4 text-center">
            <p className="text-2xl font-bold">{totalInQueue ?? '—'}</p>
            <p className="text-xs text-muted-foreground mt-1">Total waiting</p>
          </div>
        </div>

        {estimatedWait !== null && estimatedWait !== undefined && (
          <div className="flex items-center justify-center gap-2 text-sm text-muted-foreground bg-muted rounded-xl py-3">
            <Clock className="h-4 w-4" />
            <span>~{estimatedWait} min estimated wait</span>
          </div>
        )}
      </div>

      <div className="flex items-center justify-center gap-2 text-xs text-muted-foreground">
        <Wifi className="h-3.5 w-3.5 text-green-500" />
        <span>Live updates active — position refreshes automatically</span>
      </div>

      <div className="bg-blue-50 border border-blue-200 rounded-xl p-4 space-y-1">
        <p className="text-sm font-medium text-blue-800">Keep this page open</p>
        <p className="text-xs text-blue-700">
          You'll be automatically redirected when it's your turn. Closing this tab will remove you from the queue.
        </p>
      </div>

      <Button variant="outline" className="w-full" onClick={handleLeave}>
        Leave Queue
      </Button>
    </div>
  );
}