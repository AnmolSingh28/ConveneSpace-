import useAuthStore from '../store/authStore';
import { useState, useEffect } from 'react';
import { Users, Bell, BellOff } from 'lucide-react';
import { Button } from './ui/button';
import { toast } from 'react-hot-toast';
import api from '../lib/axios';
import LiveQueuePosition from './LiveQueuePosition';

export default function WaitlistSection({ tierId, tierName, queueSize: liveQueueSize, myPosition: livePosition }) {
  const { accessToken } = useAuthStore();
  const [status, setStatus] = useState(null);
  const [localQueueSize, setLocalQueueSize] = useState(0);
  const [loading, setLoading] = useState(false);

  const queueSize = (liveQueueSize !== undefined && liveQueueSize !== null)
    ? liveQueueSize
    : localQueueSize;

  // Sync localQueueSize with WebSocket updates
  useEffect(() => {
    if (liveQueueSize !== undefined && liveQueueSize !== null) {
      setLocalQueueSize(liveQueueSize);
    }
  }, [liveQueueSize]);

  useEffect(() => {
    fetchQueueSize();
    if (accessToken) fetchMyStatus();
  }, [tierId]);

  useEffect(() => {
    if (livePosition && status) {
      setStatus(prev => ({
        ...prev,
        position: livePosition.position,
      }));
    }
  }, [livePosition]);

  const fetchQueueSize = async () => {
    try {
      const res = await api.get(`/api/v1/waitlist/queue-size/${tierId}`);
      setLocalQueueSize(res.data.data);
    } catch {}
  };

  const fetchMyStatus = async () => {
    try {
      const res = await api.get(`/api/v1/waitlist/position/${tierId}`);
      setStatus(res.data.data);
    } catch (err) {
      if (err.response?.status !== 404) {
        console.error('fetchMyStatus error:', err.response?.data);
      }
    }
  };

  const handleJoin = async () => {
    if (!accessToken) {
      toast.error('Please login to join waitlist');
      return;
    }
    setLoading(true);
    try {
      const res = await api.post('/api/v1/waitlist/join', { tierId, autoBook: false });
      setStatus(res.data.data);
      setLocalQueueSize((q) => q + 1);
      toast.success(`You are #${res.data.data.position} in the waitlist`);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to join waitlist');
    } finally {
      setLoading(false);
    }
  };

  const handleLeave = async () => {
    setLoading(true);
    try {
      await api.delete(`/api/v1/waitlist/leave/${tierId}`);
      setStatus(null);
      setLocalQueueSize((q) => Math.max(0, q - 1));
      toast.success('Left waitlist');
      await fetchQueueSize();
    } catch (err) {
      toast.error('Failed to leave waitlist');
    } finally {
      setLoading(false);
    }
  };

  const onWaitlist = status?.status === 'WAITING' || status?.status === 'NOTIFIED';

  return (
    <div className="border rounded-xl p-4 bg-muted/30">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <Users className="h-4 w-4 text-muted-foreground" />
          <span className="text-sm font-medium">Waitlist</span>
        </div>
        <span className="text-xs text-muted-foreground">{queueSize} people waiting</span>
      </div>

      {status?.status === 'NOTIFIED' && (
        <div className="mb-3 p-2 bg-amber-50 border border-amber-200 rounded-lg">
          <p className="text-xs text-amber-700 font-medium">
            A seat is available! You have a limited window to book.
          </p>
        </div>
      )}

      {onWaitlist ? (
        <div className="space-y-2">
          <LiveQueuePosition
            position={livePosition?.position ?? status?.position}
            peopleAhead={livePosition?.peopleAhead}
            total={livePosition?.totalInQueue ?? queueSize}
            estimatedWait={livePosition?.estimatedWaitMinutes}
            status={status?.status}
            expiresAt={status?.expiresAt}
          />
          <Button
            variant="outline"
            size="sm"
            className="w-full gap-2 text-muted-foreground"
            onClick={handleLeave}
            disabled={loading}
          >
            <BellOff className="h-3.5 w-3.5" />
            Leave Waitlist
          </Button>
        </div>
      ) : (
        <Button
          variant="outline"
          size="sm"
          className="w-full gap-2"
          onClick={handleJoin}
          disabled={loading}
        >
          <Bell className="h-3.5 w-3.5" />
          {loading ? 'Joining...' : 'Join Waitlist'}
        </Button>
      )}
    </div>
  );
}