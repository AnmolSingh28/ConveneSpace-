import { MapPin, ExternalLink } from 'lucide-react';
import { Button } from './ui/button';

export default function VenueMap({ venue }) {
  if (!venue) return null;

  const mapsQuery = encodeURIComponent(`${venue.name}, ${venue.city}`);
  const mapsUrl = venue.googleMapsURL || `https://www.google.com/maps/search/?api=1&query=${mapsQuery}`;
  const embedUrl = `https://maps.google.com/maps?q=${mapsQuery}&output=embed&z=15`;

  return (
    <div className="bg-card border rounded-xl overflow-hidden">
      <div className="h-48 bg-muted relative">
        <iframe
          title="venue-map"
          src={embedUrl}
          className="w-full h-full border-0"
          loading="lazy"
          referrerPolicy="no-referrer-when-downgrade"
        />
      </div>
      <div className="p-4">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-start gap-2">
            <MapPin className="h-4 w-4 text-primary mt-0.5 shrink-0" />
            <div>
              <p className="font-medium text-sm">{venue.name}</p>
              <p className="text-xs text-muted-foreground mt-0.5">{venue.address}</p>
              {venue.locationDescription && (
                <p className="text-xs text-muted-foreground mt-0.5">
                  {venue.locationDescription}
                </p>
              )}
            </div>
          </div>
          <a href={mapsUrl} target="_blank" rel="noopener noreferrer">
            <Button variant="outline" size="sm" className="gap-1.5 shrink-0">
              <ExternalLink className="h-3.5 w-3.5" />
              Directions
            </Button>
          </a>
        </div>
      </div>
    </div>
  );
}