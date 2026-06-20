import { useEffect, useState } from 'react';
import { Users, TrendingUp, Bell } from 'lucide-react';

export default function LiveQueuePosition({ position, peopleAhead, total, estimatedWait, status, expiresAt }) {
  const [timeLeft, setTimeLeft] = useState('');

  useEffect(() => {
    if (!expiresAt) return;
    const interval = setInterval(() => {
      const remaining = Math.max(0, new Date(expiresAt) - new Date());
      const minutes = Math.floor(remaining / 60000);
      const seconds = Math.floor((remaining % 60000) / 1000);
      setTimeLeft(`${minutes}:${seconds.toString().padStart(2, '0')}`);
      if (remaining <= 0) clearInterval(interval);
    }, 1000);
    return () => clearInterval(interval);
  }, [expiresAt]);

  if (!position) return null;

  const isNotified = status === 'NOTIFIED';

  return (
    <div className={`rounded-xl border p-4 ${
      isNotified ? 'bg-green-50 border-green-200' : 'bg-blue-50 border-blue-200'
    }`}>
      <div className="flex items-center gap-2 mb-3">
        {isNotified ? (
          <Bell className="h-4 w-4 text-green-600" />
        ) : (
          <Users className="h-4 w-4 text-blue-600" />
        )}
        <span className={`text-sm font-semibold ${
          isNotified ? 'text-green-700' : 'text-blue-700'
        }`}>
          {isNotified ? 'A seat is available for you!' : 'Waitlist Position'}
        </span>
      </div>

      {isNotified ? (
        <div>
          <p className="text-sm text-green-700 mb-1">
            You have a limited window to book this seat.
          </p>
          {timeLeft && (
            <p className="text-2xl font-bold font-mono text-green-600">
              {timeLeft} remaining
            </p>
          )}
        </div>
      ) : (
        <div className="flex items-end gap-4">
          <div>
            <p className="text-4xl font-bold text-blue-700">#{position}</p>
            <p className="text-xs text-blue-600 mt-0.5">Your position</p>
          </div>
          <div className="pb-1 space-y-1">
            {total && (
              <div className="flex items-center gap-1 text-blue-600">
                <TrendingUp className="h-3.5 w-3.5" />
                <span className="text-xs">{total} total waiting</span>
              </div>
            )}
            {peopleAhead !== undefined && (
              <p className="text-xs text-blue-600">{peopleAhead} people ahead</p>
            )}
            {estimatedWait !== undefined && (
              <p className="text-xs text-blue-500">~{estimatedWait} min estimated wait</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}