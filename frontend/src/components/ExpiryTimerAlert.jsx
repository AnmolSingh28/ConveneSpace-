import { useEffect, useState } from 'react';
import { AlertTriangle, Clock } from 'lucide-react';
import { Button } from './ui/button';

export default function ExpiryTimerAlert({ seconds, totalSeconds, onExtend, onRelease }) {
  const [showWarning, setShowWarning] = useState(false);
  const percent = (seconds / totalSeconds) * 100;
  const minutes = Math.floor(seconds / 60);
  const secs = seconds % 60;
  const isUrgent = seconds <= 120;
  const isCritical = seconds <= 30;

  useEffect(() => {
    if (seconds === 120) setShowWarning(true);
  }, [seconds]);

  if (seconds <= 0) return null;

  return (
    <>
      {/* Persistent timer bar */}
      <div className={`rounded-xl p-4 border transition-all ${
        isCritical
          ? 'bg-red-50 border-red-200 animate-pulse'
          : isUrgent
          ? 'bg-amber-50 border-amber-200'
          : 'bg-blue-50 border-blue-200'
      }`}>
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-2">
            <Clock className={`h-4 w-4 ${
              isCritical ? 'text-red-600' : isUrgent ? 'text-amber-600' : 'text-blue-600'
            }`} />
            <span className={`text-sm font-medium ${
              isCritical ? 'text-red-700' : isUrgent ? 'text-amber-700' : 'text-blue-700'
            }`}>
              {isCritical ? 'Expiring soon!' : isUrgent ? 'Running out of time' : 'Tickets reserved'}
            </span>
          </div>
          <span className={`text-xl font-bold font-mono ${
            isCritical ? 'text-red-700' : isUrgent ? 'text-amber-700' : 'text-blue-700'
          }`}>
            {minutes}:{secs.toString().padStart(2, '0')}
          </span>
        </div>

        {/* Progress bar */}
        <div className="w-full h-2 bg-black/10 rounded-full overflow-hidden">
          <div
            className={`h-full rounded-full transition-all duration-1000 ${
              isCritical ? 'bg-red-500' : isUrgent ? 'bg-amber-500' : 'bg-blue-500'
            }`}
            style={{ width: `${percent}%` }}
          />
        </div>

        {isUrgent && (
          <p className="text-xs mt-2 text-amber-700">
            Complete your booking before time runs out or your seats will be released.
          </p>
        )}
      </div>

      {showWarning && seconds <= 120 && seconds > 0 && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 px-4">
          <div className="bg-white rounded-2xl p-6 max-w-sm w-full shadow-2xl">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-10 h-10 bg-amber-100 rounded-full flex items-center justify-center">
                <AlertTriangle className="h-5 w-5 text-amber-600" />
              </div>
              <div>
                <h3 className="font-bold">Time is running out!</h3>
                <p className="text-sm text-muted-foreground">
                  2 minutes left to complete booking
                </p>
              </div>
            </div>
            <p className="text-sm text-muted-foreground mb-4">
              Your selected seats are reserved for{' '}
              <span className="font-bold text-amber-600">
                {minutes}:{secs.toString().padStart(2, '0')}
              </span>
              . Complete payment now to secure your tickets.
            </p>
            <div className="flex gap-2">
              <Button
                className="flex-1"
                onClick={() => setShowWarning(false)}
              >
                Continue Booking
              </Button>
              {onRelease && (
                <Button
                  variant="outline"
                  onClick={() => {
                    setShowWarning(false);
                    onRelease();
                  }}
                >
                  Release
                </Button>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
}