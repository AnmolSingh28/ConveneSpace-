import { QrCode, CheckCircle, Clock } from 'lucide-react';
import { Badge } from './ui/badge';
import { formatDateTime } from '../lib/utils';

export default function TicketQR({ item }) {
  return (
    <div className="bg-card border rounded-xl overflow-hidden">
      <div className="p-4 border-b bg-muted/30">
        <div className="flex justify-between items-start">
          <div>
            <p className="font-semibold text-sm">{item.tierName}</p>
            <p className="text-xs text-muted-foreground">{item.sectionName}</p>
          </div>
          {item.checkedIn ? (
            <Badge className="bg-green-100 text-green-800 border-green-200 gap-1">
              <CheckCircle className="h-3 w-3" />
              Used
            </Badge>
          ) : (
            <Badge variant="secondary" className="gap-1">
              <Clock className="h-3 w-3" />
              Valid
            </Badge>
          )}
        </div>
      </div>

      <div className="p-4 flex flex-col items-center">
        {item.qrCodeUrl ? (
          <img
            src={item.qrCodeUrl}
            alt="QR Code"
            className="w-32 h-32 rounded-lg"
          />
        ) : (
          <div className="w-32 h-32 bg-muted rounded-lg flex items-center justify-center">
            <QrCode className="h-12 w-12 text-muted-foreground" />
          </div>
        )}
        <p className="text-xs text-muted-foreground mt-2 text-center">
          Show this QR code at the venue entrance
        </p>
        {item.checkedInAt && (
          <p className="text-xs text-green-600 mt-1">
            Scanned at {formatDateTime(item.checkedInAt)}
          </p>
        )}
      </div>
    </div>
  );
}