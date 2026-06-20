import { Badge } from '../components/ui/badge';
import { Zap, Users } from 'lucide-react';
import { formatCurrency } from '../lib/utils';

export default function TierCard({ tier, selected, onSelect }) {
  const now = new Date();
  const saleStarted = !tier.saleStart || new Date(tier.saleStart) <= now;
  const saleEnded = tier.saleEnd && new Date(tier.saleEnd) < now;
  
  const soldOut = tier.tierStatus === 'SOLD_OUT' || tier.availableQuantity === 0;
  const notStarted = !saleStarted;
  const ended = saleEnded;
  
  const disabled = soldOut || notStarted || ended;
  const lowStock = !soldOut && !notStarted && tier.availableQuantity < 50;

  return (
    <button
      onClick={() => !disabled && onSelect(tier)}
      disabled={disabled}
      className={`w-full text-left p-4 rounded-xl border-2 transition-all ${
        selected
          ? 'border-primary bg-primary/5 shadow-sm'
          : disabled
          ? 'border-border opacity-50 cursor-not-allowed bg-muted/30'
          : 'border-border hover:border-primary/40 hover:shadow-sm cursor-pointer'
      }`}
    >
      <div className="flex items-start justify-between gap-2 mb-2">
        <div className="flex-1">
          <span className="font-semibold text-sm">{tier.tierName}</span>
          {tier.sectionName && (
            <span className="text-xs text-muted-foreground ml-2">
              · {tier.sectionName}
            </span>
          )}
        </div>
        {soldOut ? (
          <Badge variant="destructive" className="text-xs shrink-0">Sold Out</Badge>
        ) : notStarted ? (
          <Badge variant="outline" className="text-xs shrink-0 border-blue-300 text-blue-600 bg-blue-50">
            Sale starts {new Date(tier.saleStart).toLocaleDateString()}
          </Badge>
        ) : ended ? (
          <Badge variant="outline" className="text-xs shrink-0 border-gray-300 text-gray-600">
            Sale Ended
          </Badge>
        ) : lowStock ? (
          <Badge variant="outline" className="text-xs shrink-0 border-orange-300 text-orange-600 bg-orange-50">
            <Zap className="h-2.5 w-2.5 mr-1" />
            {tier.availableQuantity} left
          </Badge>
        ) : (
          <Badge variant="secondary" className="text-xs shrink-0">
            {tier.availableQuantity} available
          </Badge>
        )}
      </div>

      <p className="text-xl font-bold text-primary mb-1">
        {formatCurrency(tier.price)}
      </p>

      <div className="flex items-center gap-1">
        <Users className="h-3 w-3 text-muted-foreground" />
        <span className="text-xs text-muted-foreground">
          Max {tier.maxPerUser} per person
        </span>
      </div>

      {selected && (
        <div className="mt-2 pt-2 border-t border-primary/20">
          <span className="text-xs text-primary font-medium">✓ Selected</span>
        </div>
      )}
    </button>
  );
}