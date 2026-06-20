import { Info } from 'lucide-react';

const POLICY = [
  { time: '48+ hours before', refund: '100%', color: 'text-green-600' },
  { time: '24–48 hours before', refund: '50%', color: 'text-amber-600' },
  { time: 'Under 24 hours', refund: 'No refund', color: 'text-red-600' },
];

export default function RefundPolicyInfo() {
  return (
    <div className="bg-blue-50 border border-blue-100 rounded-xl p-4">
      <div className="flex items-center gap-2 mb-3">
        <Info className="h-4 w-4 text-blue-600 shrink-0" />
        <span className="text-sm font-medium text-blue-800">Refund Policy</span>
      </div>
      <div className="space-y-1.5">
        {POLICY.map((p) => (
          <div key={p.time} className="flex justify-between text-xs">
            <span className="text-blue-700">{p.time}</span>
            <span className={`font-semibold ${p.color}`}>{p.refund}</span>
          </div>
        ))}
      </div>
    </div>
  );
}