import { Clock } from 'lucide-react';
import { Progress } from './ui/progress';

export default function BookingTimer({ seconds, totalSeconds }) {
  const percent = (seconds / totalSeconds) * 100;
  const minutes = Math.floor(seconds / 60);
  const secs = seconds % 60;
  const isUrgent = seconds < 60;

  return (
    <div className={`rounded-lg p-3 border ${isUrgent ? 'bg-red-50 border-red-200' : 'bg-amber-50 border-amber-200'}`}>
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-2">
          <Clock className={`h-4 w-4 ${isUrgent ? 'text-red-600' : 'text-amber-600'}`} />
          <span className={`text-sm font-medium ${isUrgent ? 'text-red-700' : 'text-amber-700'}`}>
            Tickets reserved
          </span>
        </div>
        <span className={`text-sm font-bold font-mono ${isUrgent ? 'text-red-700' : 'text-amber-700'}`}>
          {minutes}:{secs.toString().padStart(2, '0')}
        </span>
      </div>
      <Progress
        value={percent}
        className={`h-1.5 ${isUrgent ? '[&>div]:bg-red-500' : '[&>div]:bg-amber-500'}`}
      />
    </div>
  );
}