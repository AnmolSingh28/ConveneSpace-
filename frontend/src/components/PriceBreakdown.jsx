import { Separator } from './ui/separator';
import { formatCurrency } from '../lib/utils';
import { Info } from 'lucide-react';

export default function PriceBreakdown({ baseAmount, platformFee, gatewayFee, total }) {
  return (
    <div className="bg-card border rounded-xl p-5">
      <div className="flex items-center gap-2 mb-4">
        <h3 className="font-semibold text-sm">Price Breakdown</h3>
        <Info className="h-3.5 w-3.5 text-muted-foreground" />
      </div>

      <div className="space-y-2.5 text-sm">
        <div className="flex justify-between">
          <span className="text-muted-foreground">Ticket price</span>
          <span>{formatCurrency(baseAmount)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-muted-foreground">Platform fee (5%)</span>
          <span>{formatCurrency(platformFee)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-muted-foreground">Payment gateway (2%)</span>
          <span>{formatCurrency(gatewayFee)}</span>
        </div>
        <Separator />
        <div className="flex justify-between font-bold text-base">
          <span>Total</span>
          <span className="text-primary">{formatCurrency(total)}</span>
        </div>
      </div>

      <p className="text-xs text-muted-foreground mt-3">
        No hidden charges. What you see is what you pay.
      </p>
    </div>
  );
}