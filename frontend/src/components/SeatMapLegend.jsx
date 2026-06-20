const LEGEND = [
  { color: 'bg-green-500', label: 'Available' },
  { color: 'bg-amber-500', label: 'Locked' },
  { color: 'bg-red-500', label: 'Booked' },
  { color: 'bg-blue-500', label: 'Selected' },
];

export default function SeatMapLegend() {
  return (
    <div className="flex flex-wrap gap-3">
      {LEGEND.map((item) => (
        <div key={item.label} className="flex items-center gap-1.5">
          <div className={`w-3 h-3 rounded-sm ${item.color}`} />
          <span className="text-xs text-muted-foreground">{item.label}</span>
        </div>
      ))}
    </div>
  );
}