import { useEffect, useState } from 'react';
import { Eye } from 'lucide-react';

export default function LiveViewerCount({ count }) {
  const [display, setDisplay] = useState(count);
  const [flash, setFlash] = useState(false);

  useEffect(() => {
    if (count !== display) {
      setDisplay(count);
      setFlash(true);
      const t = setTimeout(() => setFlash(false), 600);
      return () => clearTimeout(t);
    }
  }, [count]);

  if (!display || display === 0) return null;

  return (
    <div className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-red-50 border border-red-100 transition-all duration-300 ${flash ? 'scale-105' : 'scale-100'}`}>
      <span className="w-2 h-2 rounded-full bg-red-500 animate-pulse" />
      <Eye className="h-3.5 w-3.5 text-red-600" />
      <span className="text-xs font-semibold text-red-700">
        {display.toLocaleString('en-IN')} viewing now
      </span>
    </div>
  );
}