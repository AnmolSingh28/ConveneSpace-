import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Calendar, MapPin, Clock, ChevronRight, AlertCircle } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Skeleton } from '../components/ui/skeleton';
import LiveTicketCount from '../components/LiveTicketCount';
import LiveViewerCount from '../components/LiveViewerCount';
import VenueMap from '../components/VenueMap';
import TierCard from '../components/TierCard';
import WaitlistSection from '../components/WaitlistSection';
import { useWebSocket } from '../hooks/useWebSocket';
import api from '../lib/axios';
import useAuthStore from '../store/authStore';
import useBookingStore from '../store/bookingStore';
import { formatDate, formatTime, formatCurrency } from '../lib/utils';
import toast from 'react-hot-toast';


export default function ConcertDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { accessToken } = useAuthStore();
  const { setSelectedConcert } = useBookingStore();
  const [concert, setConcert] = useState(null);
  const [loading, setLoading] = useState(true);
  const { user } = useAuthStore();
  const [selectedTier, setSelectedTier] = useState(null);

const gaTierId = concert?.ticketTiers?.find(t => t.sectionType === 'GA')?.id;
const { availabilityUpdates, viewerCount, queueSize, myPosition } = useWebSocket(concert?.id, gaTierId, user?.id);

  useEffect(() => {
    if (id) fetchConcert();
  }, [id]);

 
  useEffect(() => {
    if (!concert?.id) return;
   const sessionId = sessionStorage.getItem('sessionId') ||
  Math.random().toString(36).substring(2);
sessionStorage.setItem('sessionId', sessionId);
    api.post(`/api/v1/seats/concert/${concert.id}/viewing`, null, {
      headers: { 'X-Session-Id': sessionId },
    }).catch(() => {});
    const interval = setInterval(() => {
      api.post(`/api/v1/seats/concert/${concert.id}/viewing`, null, {
        headers: { 'X-Session-Id': sessionId },
      }).catch(() => {});
    }, 30000);
    return () => clearInterval(interval);
  }, [concert?.id]);

  const fetchConcert = async () => {
    try {
      const res = await api.get(`/api/v1/concerts/${id}`);
      const concertData = res.data.data;
      setConcert(concertData);
      setSelectedConcert(concertData);
    } catch (err) {
      console.error('FETCH CONCERT ERROR:', err);
      toast.error('Concert not found');
      navigate('/');
    } finally {
      setLoading(false);
    }
  };

const handleBookNow = async () => {
  if (!accessToken) {
    toast.error('Please login to book tickets');
    navigate('/login');
    return;
  }
  if (!selectedTier) {
    toast.error('Please select a ticket tier');
    return;
  }
   const now = new Date();
  if (selectedTier.saleStart && new Date(selectedTier.saleStart) > now) {
    toast.error('Sale has not started yet');
    return;
  }
  if (selectedTier.saleEnd && new Date(selectedTier.saleEnd) < now) {
    toast.error('Sale has ended');
    return;
  }

  try {
    const res = await api.get(`/api/v1/queue/status/${selectedTier.id}`);
    const { queueActive } = res.data.data;
    if (queueActive) {
      navigate(`/queue/${selectedTier.id}`);
    } else {
      navigate(`/book/${selectedTier.id}`);
    }
  } catch {

    navigate(`/book/${selectedTier.id}`);
  }
};

  if (loading) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-8">
        <Skeleton className="h-80 w-full rounded-xl mb-6" />
        <Skeleton className="h-8 w-2/3 mb-3" />
        <Skeleton className="h-4 w-1/3 mb-6" />
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-3">
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-3/4" />
          </div>
          <div className="space-y-3">
            <Skeleton className="h-32 w-full rounded-xl" />
            <Skeleton className="h-32 w-full rounded-xl" />
          </div>
        </div>
      </div>
    );
  }
const salesEnded =
  concert?.saleEndTime &&
  new Date(concert.saleEndTime) < new Date();

  if (!concert) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-8">
        <div className="bg-card border rounded-xl p-6 text-center">
          <p className="text-muted-foreground">Concert not found</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Banner */}
      <div className="relative h-64 sm:h-80 rounded-2xl overflow-hidden mb-6 bg-muted">
        {concert?.bannerImageUrl ? (
          <img
            src={concert.bannerImageUrl}
            alt={concert?.title || 'Concert'}
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="w-full h-full bg-gradient-to-br from-primary/30 to-primary/10 flex items-center justify-center">
            <span className="text-6xl">🎵</span>
          </div>
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
        <div className="absolute bottom-4 left-4 right-4">
          <Badge className="mb-2 bg-white/20 backdrop-blur text-white border-white/30">
            {concert?.status || 'Upcoming'}
          </Badge>
          <h1 className="text-2xl sm:text-3xl font-bold text-white leading-tight">
            {concert?.title || 'Concert'}
          </h1>
          <p className="text-white/80 mt-1">{concert?.artistName || 'Unknown Artist'}</p>
          {/* NEW — live viewer count */}
          <LiveViewerCount count={viewerCount} />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* LEFT */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-card border rounded-xl p-5 space-y-3">
            <h2 className="font-semibold text-base">Event Details</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="flex items-start gap-3">
                <Calendar className="h-4 w-4 text-primary mt-0.5 shrink-0" />
                <div>
                  <p className="text-xs text-muted-foreground">Date</p>
                  <p className="text-sm font-medium">
                    {concert?.concertDate ? formatDate(concert.concertDate) : 'Date unavailable'}
                  </p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <Clock className="h-4 w-4 text-primary mt-0.5 shrink-0" />
                <div>
                  <p className="text-xs text-muted-foreground">Show Time</p>
                  <p className="text-sm font-medium">
                    {concert?.concertDate ? formatTime(concert.concertDate) : 'Time unavailable'}
                  </p>
                  {concert?.doorsOpenTime && (
                    <p className="text-xs text-muted-foreground">
                      Doors open: {formatTime(concert.doorsOpenTime)}
                    </p>
                  )}
                </div>
              </div>
              <div className="flex items-start gap-3">
                <MapPin className="h-4 w-4 text-primary mt-0.5 shrink-0" />
                <div>
                  <p className="text-xs text-muted-foreground">Venue</p>
                  <p className="text-sm font-medium">{concert?.venue?.name || 'Venue unavailable'}</p>
                  <p className="text-xs text-muted-foreground">{concert?.venue?.city || ''}</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <Clock className="h-4 w-4 text-primary mt-0.5 shrink-0" />
                <div>
                  <p className="text-xs text-muted-foreground">Sale Ends</p>
                  <p className="text-sm font-medium">
                    {concert?.saleEndTime ? formatDate(concert.saleEndTime) : 'On the day'}
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-card border rounded-xl p-5">
            <h2 className="font-semibold text-base mb-3">About the Event</h2>
            <p className="text-sm text-muted-foreground leading-relaxed">
              {concert?.description || 'No description available'}
            </p>
          </div>

          <VenueMap venue={concert?.venue} />
          {concert?.organizerName && (
    <div className="flex items-start gap-3">
        <div className="h-4 w-4 mt-0.5 shrink-0 text-primary">👤</div>
        <div>
            <p className="text-xs text-muted-foreground">Organizer</p>
            <button
                onClick={() => navigate(`/organizer/${concert.organizerId}`)}
                className="text-sm font-medium text-primary hover:underline flex items-center gap-1"
            >
                {concert.organizerName}
                {concert.organizerRating && (
                    <span className="text-xs bg-amber-100 text-amber-700 px-1.5 py-0.5 rounded-full ml-1">
                        ⭐ {concert.organizerRating.toFixed(1)}
                    </span>
                )}
            </button>
        </div>
    </div>
)}

          {concert?.requiresPreRegistration && (
            <div className="flex gap-3 p-4 bg-amber-50 border border-amber-200 rounded-xl">
              <AlertCircle className="h-5 w-5 text-amber-600 shrink-0 mt-0.5" />
              <div>
                <p className="text-sm font-medium text-amber-800">Pre-registration Required</p>
                <p className="text-xs text-amber-700 mt-0.5">
                  This is a high-demand event. You must pre-register to participate in the fair ticket queue.
                </p>
              </div>
            </div>
          )}
        </div>

        {/* RIGHT */}
<div className="space-y-4">
  <h2 className="font-semibold text-base">Select Tickets</h2>

  {(() => {
   
    const allAssigned = concert?.ticketTiers?.every(
      t => t.sectionType === 'ASSIGNED'
    );
    const hasAssigned = concert?.ticketTiers?.some(
      t => t.sectionType === 'ASSIGNED'
    );
    const hasGa = concert?.ticketTiers?.some(
      t => t.sectionType === 'GA'
    );

    return (
      <>
        {/* GA tiers */}
        {hasGa && concert.ticketTiers
          .filter(t => t.sectionType === 'GA')
          .map((tier) => {
            const liveCount = availabilityUpdates?.[tier.id];
          const soldOut = tier.tierStatus === 'SOLD_OUT';
            return (
              <div key={tier.id} className="space-y-2">
                <TierCard
                  tier={{
                    ...tier,
                    availableQuantity: liveCount !== undefined ? liveCount : tier.availableQuantity,
                  }}
                  selected={selectedTier?.id === tier?.id}
                  onSelect={setSelectedTier}
                />
                <LiveTicketCount
                  tierId={tier.id}
                  initial={tier.availableQuantity}
                  liveCount={liveCount}
                />
                {soldOut && (
                  <WaitlistSection tierId={tier.id} tierName={tier.tierName} />
                )}
              </div>
            );
          })}

        {/* Assigned tiers */}
        {hasAssigned && (
          <button
            onClick={() => {
              if (salesEnded) {
  toast.error('Ticket sales have ended');
  return;
}
              if (!accessToken) {
                toast.error('Please login to book tickets');
                navigate('/login');
                return;
              }
              
              const firstAssignedTier = concert.ticketTiers.find(
                t => t.sectionType === 'ASSIGNED'
              );
              const now = new Date();

     if (
      firstAssignedTier.saleEnd &&
       new Date(firstAssignedTier.saleEnd) < now
        ) {
        toast.error('Ticket sale has ended');
        return;
            }
              navigate(`/book/${firstAssignedTier.id}`);
            }}
            className="w-full p-4 rounded-xl border-2 border-dashed border-primary/40 hover:border-primary hover:bg-primary/5 transition-all text-left"
          >
            <p className="font-semibold text-sm text-primary">Select Your Seats</p>
            <p className="text-xs text-muted-foreground mt-1">
              {concert.ticketTiers
                .filter(t => t.sectionType === 'ASSIGNED')
                .map(t => t.tierName)
                .join(' · ')}
            </p>
            <p className="text-xs text-muted-foreground mt-0.5">
              Choose your exact seat from the interactive map
            </p>
          </button>
        )}

        {/* Book Now button */}
        {hasGa && (
          <>
            <Button
              className="w-full"
              size="lg"
              onClick={handleBookNow}
              disabled={!selectedTier || salesEnded}
            >
              {selectedTier ? (
                <>
                  Book Now — {formatCurrency(selectedTier?.price || 0)}
                  <ChevronRight className="h-4 w-4 ml-1" />
                </>
              ) : (
                'Select a ticket tier'
              )}
            </Button>
            <p className="text-xs text-center text-muted-foreground">
              Tickets are held for a limited time after selection
            </p>
          </>
        )}
      </>
    );
  })()}
</div>
      </div>
    </div>
  );
}