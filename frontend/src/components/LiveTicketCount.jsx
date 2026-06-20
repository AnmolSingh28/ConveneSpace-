import { useEffect, useState } from 'react';
import { Zap } from 'lucide-react';

export default function LiveTicketCount({ tierId, initial, liveCount }) {
  const [count, setCount] = useState(initial);
  const [flash, setFlash] = useState(false);

  useEffect(() => {
    if (liveCount !== undefined && liveCount !== count&& liveCount > 0) {
      setCount(liveCount);
      setFlash(true);
      const t = setTimeout(() => setFlash(false), 800);
      return () => clearTimeout(t);
    }
  }, [liveCount]);

  const isLow = count < 50;
  const isCritical = count < 10;

  return (
    <div
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium transition-all duration-300 ${
        flash ? 'scale-110' : 'scale-100'
      } ${
        isCritical
          ? 'bg-red-100 text-red-700'
          : isLow
          ? 'bg-orange-100 text-orange-700'
          : 'bg-green-100 text-green-700'
      }`}
    >
      <span
        className={`w-1.5 h-1.5 rounded-full animate-pulse ${
          isCritical ? 'bg-red-500' : isLow ? 'bg-orange-500' : 'bg-green-500'
        }`}
      />
      {isCritical ? (
        <>
          <Zap className="h-3 w-3" />
          Only {count} left!
        </>
      ) : isLow ? (
        `${count} tickets left`
      ) : (
        `${count} available`
      )}
    </div>
  );
}