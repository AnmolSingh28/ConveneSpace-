import { useMemo } from 'react';
import { Badge } from './ui/badge';
const TIER_COLOR_PALETTE = [
  'bg-yellow-500/20 border-yellow-500/50 text-yellow-400 hover:bg-yellow-500/40',
  'bg-purple-500/20 border-purple-500/50 text-purple-400 hover:bg-purple-500/40',
  'bg-sky-500/20 border-sky-500/50 text-sky-400 hover:bg-sky-500/40',
  'bg-pink-500/20 border-pink-500/50 text-pink-400 hover:bg-pink-500/40',
  'bg-orange-500/20 border-orange-500/50 text-orange-400 hover:bg-orange-500/40',
];

const STATUS_STYLES = {
  LOCKED:   'bg-amber-500/20 border-amber-500/50 text-amber-500 cursor-not-allowed opacity-80',
  BOOKED:   'bg-red-500/20 border-red-500/50 text-red-500 cursor-not-allowed opacity-80',
  SELECTED: 'bg-blue-600 border-blue-400 text-white scale-110 shadow-lg shadow-blue-500/30 cursor-pointer',
};

export default function SeatMap({ seats, selectedSeats, onSeatToggle, liveUpdates,canBook = true,remainingSlots = 0   }) {
  if (!seats?.length) return null;

  // 1. Group by Tier, then by Row
  const structuredData = useMemo(() => {
    const map = {};
    seats.forEach((seat) => {
      const tier = seat.tierName || 'Standard';
      const row = seat.rowLabel || 'A';
      
      if (!map[tier]) map[tier] = {};
      if (!map[tier][row]) map[tier][row] = [];
      
      map[tier][row].push(seat);
    });
    return map;
  }, [seats]);

  // 2. Sort Tiers (Front to Back - usually based on Price)
  const sortedTierNames = useMemo(() => {
    return Object.keys(structuredData).sort((a, b) => {
        // Find first seat in each tier to compare price
        const priceA = Object.values(structuredData[a])[0][0].price;
        const priceB = Object.values(structuredData[b])[0][0].price;
        return priceB - priceA; // Higher price first (at the top/stage)
    });
  }, [structuredData]);

  const getStatus = (seat) => {
    if (selectedSeats?.includes(seat.id)) return 'SELECTED';
    return liveUpdates?.[seat.id] || seat.status || 'AVAILABLE';
  };


const getStyle = (seat, status, tierIndex) => {
    if (status !== 'AVAILABLE') return STATUS_STYLES[status] || STATUS_STYLES.LOCKED;
    const colorClass = TIER_COLOR_PALETTE[tierIndex % TIER_COLOR_PALETTE.length];
    return `${colorClass} hover:scale-110 cursor-pointer transition-all duration-150`;
};

  return (
    <div className="bg-card border rounded-2xl p-8 overflow-x-auto min-w-[600px]">
      
      {/* STAGE VISUALIZATION */}
      <div className="w-full mb-16 flex flex-col items-center">
        <div className="w-2/3 h-2 bg-muted-foreground/20 rounded-full shadow-[0_15px_30px_rgba(0,0,0,0.1)] mb-2" />
        <span className="text-[10px] font-bold tracking-[0.3em] text-muted-foreground uppercase">Stage / Screen</span>
      </div>

      <div className="flex flex-col gap-12">
        {sortedTierNames.map((tierName, tierIndex) => (
          <div key={tierName} className="flex flex-col items-center">
            {/* TIER LABEL */}
            <div className="mb-4 flex items-center gap-3 w-full">
                <div className="h-[1px] flex-1 bg-border" />
                <Badge variant="outline" className="px-4 py-1 text-[10px] tracking-widest uppercase font-bold bg-muted/50">
                    {tierName} — ₹{Object.values(structuredData[tierName])[0][0].price}
                </Badge>
                <div className="h-[1px] flex-1 bg-border" />
            </div>

            {/* ROWS IN THIS TIER */}
            <div className="space-y-2">
              {Object.keys(structuredData[tierName])
                .sort() // Sort A, B, C
                .map((rowLabel) => (
                  <div key={rowLabel} className="flex items-center gap-4 group">
                    <span className="text-[10px] font-bold text-muted-foreground w-4 text-right opacity-50 group-hover:opacity-100 transition-opacity">
                      {rowLabel}
                    </span>
                    
                    <div className="flex gap-1.5 no-wrap">
                      {structuredData[tierName][rowLabel]
                        .sort((a, b) => parseInt(a.seatNumber) - parseInt(b.seatNumber))
                        .map((seat) => {
                          const status = getStatus(seat);
                        //  const isClickable = status === 'AVAILABLE' || status === 'SELECTED';
                        const isClickable = (status === 'AVAILABLE' || status === 'SELECTED') 
                                 && canBook 
                                  && (status === 'SELECTED' || selectedSeats.length < remainingSlots);
                          return (
                            <button
                              key={seat.id}
                              onClick={() => isClickable && onSeatToggle?.(seat)}
                              disabled={!isClickable}
                               title={!canBook ? 'You already have an active booking' : undefined}
                              className={`
                                w-7 h-7 rounded-md text-[9px] font-bold border transition-all
                                ${getStyle(seat, status, tierIndex)}
                              `}
                            >
                             {seat.seatNumber}
                            </button>
                          );
                        })}
                    </div>

                    <span className="text-[10px] font-bold text-muted-foreground w-4 opacity-50 group-hover:opacity-100 transition-opacity">
                      {rowLabel}
                    </span>
                  </div>
                ))}
            </div>
          </div>
        ))}
      </div>

      {/* FOOTER LEGEND */}
      <div className="mt-12 pt-6 border-t flex flex-wrap justify-center gap-6">
        <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded bg-amber-500/20 border border-amber-500/50" />
            <span className="text-[10px] font-medium text-muted-foreground uppercase">Locked</span>
        </div>
        <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded bg-red-500/20 border border-red-500/50" />
            <span className="text-[10px] font-medium text-muted-foreground uppercase">Booked</span>
        </div>
        <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded bg-blue-600 border border-blue-400 shadow-md shadow-blue-500/20" />
            <span className="text-[10px] font-medium text-muted-foreground uppercase text-blue-500">Your Selection</span>
        </div>
      </div>
    </div>
  );
}