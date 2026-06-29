import { Link } from 'react-router-dom';
import { Calendar, MapPin, Tag, Navigation, Clock } from 'lucide-react';
import { Badge } from './ui/badge';
import { formatDate, formatCurrency } from '../lib/utils';

const CATEGORY_CONFIG = {
  CONCERT_LIVE_MUSIC:     { label: 'Live Music',      color: 'bg-purple-100 text-purple-700', emoji: '🎵' },
  COMEDY_SHOW:            { label: 'Comedy',           color: 'bg-yellow-100 text-yellow-700', emoji: '😂' },
  STANDUP_SPECIAL:        { label: 'Stand-up',         color: 'bg-orange-100 text-orange-700', emoji: '🎤' },
  THEATRE_DRAMA:          { label: 'Theatre',          color: 'bg-red-100 text-red-700',       emoji: '🎭' },
  FOOD_DRINK_FESTIVAL:    { label: 'Food & Drink',     color: 'bg-green-100 text-green-700',   emoji: '🍻' },
  ART_EXHIBITION:         { label: 'Art',              color: 'bg-pink-100 text-pink-700',     emoji: '🎨' },
  SPORTS_EVENT:           { label: 'Sports',           color: 'bg-blue-100 text-blue-700',     emoji: '🏆' },
  WORKSHOP_MASTERCLASS:   { label: 'Workshop',         color: 'bg-teal-100 text-teal-700',     emoji: '📚' },
  FILM_SCREENING:         { label: 'Film',             color: 'bg-indigo-100 text-indigo-700', emoji: '🎬' },
  CULTURAL_FESTIVAL:      { label: 'Festival',         color: 'bg-amber-100 text-amber-700',   emoji: '🎪' },
};

export default function ConcertCard({ concert }) {
  const now = new Date();
  const rawDate = concert.saleStartTime || concert.saleStart;
  const parseKingDate = (dateStr) => {
    if (!dateStr || typeof dateStr !== 'string') return null;
    try {
      const [dPart, tPart] = dateStr.split(' ');
      const [day, month, year] = dPart.split('-').map(Number);
      const [hour, min, sec] = tPart.split(':').map(Number); 
      return new Date(year, month - 1, day, hour, min, sec);
    } catch (e) {
      return null;
    }
  };

  const saleStartDate = parseKingDate(rawDate);
  const saleStarted = saleStartDate && saleStartDate <= now;
  const isSaleFuture = saleStartDate && !isNaN(saleStartDate.getTime()) && saleStartDate > now;

  const cat = CATEGORY_CONFIG[concert.category] || {
    label: concert.category, color: 'bg-gray-100 text-gray-700', emoji: '🎫',
  };

  return (
    <Link to={`/concerts/${concert.id}`}>
      <div className="group rounded-xl border bg-card overflow-hidden hover:shadow-lg transition-all duration-300 hover:-translate-y-1">
        <div className="relative h-48 overflow-hidden bg-muted">
          {concert.bannerImageUrl ? (
            <img src={concert.bannerImageUrl} alt={concert.title} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
          ) : (
            <div className="w-full h-full bg-gradient-to-br from-primary/20 to-primary/5 flex items-center justify-center">
              <span className="text-4xl">{cat.emoji}</span>
            </div>
          )}
        
          {isSaleFuture && (
            <div className="absolute inset-0 bg-black/60 backdrop-blur-[2px] flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-300 z-10">
               <div className="bg-white px-4 py-2 rounded-full flex items-center gap-2 shadow-2xl border-2 border-amber-500">
                  <Clock className="h-4 w-4 text-amber-600 animate-pulse" />
                  <span className="text-[10px] font-black text-black uppercase">
                    Starts: {rawDate.split(' ')[0]}
                  </span>
               </div>
            </div>
          )}

          {concert.isFeatured && <Badge className="absolute top-3 left-3 bg-amber-500 hover:bg-amber-500 border-none z-20">Featured</Badge>}

          <span className={`absolute bottom-3 left-3 text-[10px] font-bold uppercase px-2 py-1 rounded-md shadow-sm z-20 ${cat.color}`}>
            {cat.emoji} {cat.label}
          </span>
        </div>

        <div className="p-4">
          <h3 className="font-bold text-base leading-tight line-clamp-2 mb-1 group-hover:text-primary transition-colors">{concert.title}</h3>
          <p className="text-sm text-muted-foreground mb-3 truncate">{concert.artistName}</p>
          {concert.organizerName && (
    <div className="flex items-center justify-between mb-3">
        <span className="text-xs text-muted-foreground">
            by {concert.organizerName}
        </span>
        {concert.organizerRating && (
            <span className="text-xs font-bold bg-amber-100 text-amber-700 px-2 py-0.5 rounded-full">
                ⭐ {concert.organizerRating.toFixed(1)}
            </span>
        )}
    </div>
)}
          <div className="space-y-1.5">
            <div className="flex items-center gap-2 text-xs text-muted-foreground"><Calendar className="h-3.5 w-3.5 shrink-0 text-primary" /><span>{formatDate(concert.concertDate)}</span></div>
            <div className="flex items-center gap-2 text-xs text-muted-foreground"><MapPin className="h-3.5 w-3.5 shrink-0 text-primary" /><span className="line-clamp-1">{concert.venueName}, {concert.venueCity}</span></div>
          </div>

          <div className="mt-4 pt-3 border-t flex items-center justify-between gap-2">
            <div className="flex flex-col">
              <span className="text-[9px] uppercase font-black text-muted-foreground tracking-tighter leading-none mb-1">From</span>
              <span className="text-sm font-black text-foreground">{formatCurrency(concert.startingPrice)}</span>
            </div>
            {concert.requiresPreRegistration && !saleStarted && (
                <Badge variant="outline" className="text-[10px] font-black border-purple-500 bg-purple-50 text-purple-700">
                  🎟 PRE-REG
                </Badge>
            )}
            {isSaleFuture ? (
              <Badge variant="outline" className="text-[10px] font-black border-amber-500 bg-amber-50 text-amber-700 animate-pulse">
                COMING SOON
              </Badge>
            ) : (
                <Badge className={`text-[10px] font-black uppercase px-3 py-1 border-none ${
                    concert.status === 'PUBLISHED' ? 'bg-emerald-500 text-white' :
                        concert.status === 'POSTPONED' ? 'bg-amber-500 text-white' :
                            concert.status === 'CANCELLED' ? 'bg-red-500 text-white' :
                                concert.status === 'SOLD_OUT'  ? 'bg-red-600 text-white' :
                                    concert.status === 'COMPLETED' ? 'bg-blue-500 text-white' :
                                        'bg-secondary text-secondary-foreground'
                }`}>
                  {concert.status === 'PUBLISHED' ? 'Available' :
                      concert.status === 'POSTPONED' ? 'Postponed' :
                          concert.status === 'CANCELLED' ? 'Cancelled' :
                              concert.status === 'SOLD_OUT'  ? 'Sold Out'  :
                                  concert.status}
                </Badge>
            )}
          </div>
        </div>
      </div>
    </Link>
  );
}