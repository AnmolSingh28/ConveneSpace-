import { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { CreditCard, CheckCircle, Loader2 } from 'lucide-react';

import ExpiryTimerAlert from '../components/ExpiryTimerAlert';
import SeatMap from '../components/SeatMap';
import { useWebSocket } from '../hooks/useWebSocket';
import { Button } from '../components/ui/button';
import { Separator } from '../components/ui/separator';
import { Badge } from '../components/ui/badge';
import api from '../lib/axios';
import useAuthStore from '../store/authStore';
import useBookingStore from '../store/bookingStore';
import { formatCurrency, generateIdempotencyKey } from '../lib/utils';
import toast from 'react-hot-toast';

export default function BookingPage() {
  const { tierId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const {
    selectedConcert, clearBooking, setSelectedConcert,
    setActiveLock, clearActiveLock,
    selectedSeatIds: storedSeatIds, setSelectedSeatIds,
    lockTimerEnd, setLockTimerEnd,
  } = useBookingStore();

  // Always derive from store — never local state for these
  const selectedSeatIds = Array.isArray(storedSeatIds) ? storedSeatIds : [];

  // step is purely local — never persist it
  const [step, setStep] = useState('select');

  const [canBook, setCanBook] = useState(true);
  const [canBookReason, setCanBookReason] = useState(null);
  const [tier, setTier] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(true);
  const [locking, setLocking] = useState(false);
  const [locked, setLocked] = useState(false);
  const [lockTimer, setLockTimer] = useState(0);
  const [booking, setBooking] = useState(null);
  const [seats, setSeats] = useState([]);
  const [estimate, setEstimate] = useState(null);
  const [remainingSlots, setRemainingSlots] = useState(4);

  const { seatUpdates } = useWebSocket(selectedConcert?.id);


  useEffect(() => {
    fetchTierInfo();

    console.log("BEFORE", useBookingStore.getState());

    if (!lockTimerEnd || Date.now() > lockTimerEnd) {
        clearBooking();
    }
    console.log("AFTER", useBookingStore.getState());
}, [tierId]);

  useEffect(() => {
    if (!lockTimerEnd) return;
    const remaining = Math.max(0, Math.floor((lockTimerEnd - Date.now()) / 1000));
    if (remaining > 0) {
      setLockTimer(remaining);
      setLocked(true);
     
      if (selectedSeatIds.length > 0) {
        setStep('locked');
      }
    } else {
      handleReleaseAll();
    }
  }, []); 

useEffect(() => {
    if (!tier || !tierId) return;
    if (tier.sectionType === 'ASSIGNED' && selectedSeatIds.length === 0) {
        setEstimate(null);
        return;
    }
    
    const selectedSeat = seats.find(s => selectedSeatIds.includes(s.id));
    const actualTierId = selectedSeat?.tierId || tierId;
    const qty = tier.sectionType === 'ASSIGNED' ? selectedSeatIds.length : quantity;
    
    api.get('/api/v1/bookings/estimate', {
        params: { tierId: actualTierId, quantity: qty }
    })
    .then(res => setEstimate(res.data.data))
    .catch(() => {});
}, [tier, quantity, selectedSeatIds, seats]);

useEffect(() => {
    if (!tier) return;

    const now = new Date();

    if (tier.saleEnd && new Date(tier.saleEnd) < now) {
        toast.error('Ticket sale has ended');
        navigate(`/concert/${selectedConcert?.id}`);
    }
}, [tier]);
  // Can book check
  useEffect(() => {
    if (!tierId || !user) return;
    api.get('/api/v1/bookings/can-book', { params: { tierId } })
      .then(res => {
        setCanBook(res.data.data.canBook);
        setCanBookReason(res.data.data.reason);
        setRemainingSlots(res.data.data.remainingSlots);
      })
      .catch(() => {});
  }, [tierId, user]);

  // Load seats for ASSIGNED
useEffect(() => {
    if (tier?.sectionType === 'ASSIGNED') {
      const assignedTiers = selectedConcert?.ticketTiers?.filter(t => t.sectionType === 'ASSIGNED');
      if (assignedTiers?.length) {
        Promise.all(assignedTiers.map(t => api.get(`/api/v1/seats/tier/${t.id}`)))
          .then(responses => {
            const allSeats = responses.flatMap(r => r.data.data || []);
            setSeats(allSeats);
          });
      }
    }
  }, [tier, selectedConcert]);

  // Timer countdown
  useEffect(() => {
    if (!locked || lockTimer <= 0) return;
    const interval = setInterval(() => {
      setLockTimer(t => {
        if (t <= 1) { handleReleaseAll(); return 0; }
        return t - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [locked, lockTimer]);

  // Heartbeat for assigned seats
  useEffect(() => {
    if (!locked || selectedSeatIds.length === 0) return;
    const ping = async () => {
      try {
        await Promise.all(selectedSeatIds.map(seatId => api.post(`/api/v1/seats/presence/${seatId}`)));
      } catch {}
    };
    ping();
    const interval = setInterval(ping, 30000);
    return () => clearInterval(interval);
  }, [locked, selectedSeatIds]);

  const handleReleaseAll = () => {
    setLocked(false);
    setStep('select');
    setSelectedSeatIds([]);
    setLockTimerEnd(null);
    clearActiveLock();
    toast.error('Session expired. Seats released.');
  };

const baseAmount = useMemo(() => {
    if (booking?.baseAmount) return booking.baseAmount;
    if (estimate?.baseAmount) return estimate.baseAmount; // estimate se lo
    if (tier?.sectionType === 'ASSIGNED') {
        return seats.filter(s => selectedSeatIds.includes(s.id))
            .reduce((sum, s) => sum + (s.price || 0), 0);
    }
    return (tier?.price || 0) * quantity;
}, [booking, estimate, tier, selectedSeatIds, seats, quantity]);

  const availableCount = useMemo(() => {
    if (!seats?.length) return tier?.availableQuantity || 0;
    return seats.filter(s => {
      const status = seatUpdates?.[s.id] || s.status;
      return status === 'AVAILABLE';
    }).length;
  }, [seats, seatUpdates]);

  const fetchTierInfo = async () => {
    try {
      let tierData = selectedConcert?.ticketTiers?.find(t => t.id === tierId);
      if (!tierData) {
        const res = await api.get(`/api/v1/queue/concert-id/${tierId}`);
        const concertId = res.data.data;
        const concertRes = await api.get(`/api/v1/concerts/${concertId}`);
        const concert = concertRes.data.data;
        setSelectedConcert(concert);
        tierData = concert.ticketTiers?.find(t => t.id === tierId);
      }
      setTier(tierData);
    } catch {
      navigate(-1);
    } finally {
      setLoading(false);
    }
  };

 const handleSeatToggle = async (seat) => {
    console.log("CLICKED", seat.id);

    if (step === 'locked' || step === 'payment') return;

    const isSelected = selectedSeatIds.includes(seat.id);

    if (isSelected) {
        try {
            const res = await api.post(
                '/api/v1/seats/assigned/release',
                { seatId: seat.id }
            );

            console.log("RELEASE RESPONSE", res.data);

            setSelectedSeatIds(
                selectedSeatIds.filter(id => id !== seat.id)
            );
        } catch (err) {
            console.log("RELEASE ERROR", err);
            toast.error('Failed to release');
        }
    } else {
        if (selectedSeatIds.length >= remainingSlots) {
            toast.error(`Max ${remainingSlots} seat(s) allowed`);
            return;
        }

        try {
            const res = await api.post(
                '/api/v1/seats/assigned/lock',
                { seatId: seat.id }
            );

            console.log("LOCK RESPONSE", res.data);

            setSelectedSeatIds([
                ...selectedSeatIds,
                seat.id
            ]);

            console.log(
                "STORE AFTER LOCK",
                useBookingStore.getState()
            );

        } catch (err) {
            console.log(
                "LOCK ERROR",
                err.response?.status,
                err.response?.data
            );

            toast.error(
                err.response?.data?.message || 'Seat Taken'
            );
        }
    }
};

  const handleLock = async () => {
    if (tier.sectionType === 'ASSIGNED') {
      setQuantity(selectedSeatIds.length);
      setLocked(true);
      const expiresAt = Date.now() + tier.lockTtlMinutes * 60 * 1000;
      setActiveLock(tierId, expiresAt);
      setLockTimerEnd(expiresAt);
      setLockTimer(tier.lockTtlMinutes * 60);
      setStep('locked');
    } else {
      setLocking(true);
      try {
        await api.post(`/api/v1/seats/ga/${tierId}/lock`, { quantity });
        setLocked(true);
        const expiresAt = Date.now() + tier.lockTtlMinutes * 60 * 1000;
        setActiveLock(tierId, expiresAt);
        setLockTimerEnd(expiresAt);
        setLockTimer(tier.lockTtlMinutes * 60);
        setStep('locked');
      } catch (err) {
        const status = err.response?.status;
        if (status === 409) {
          try {
            const queueRes = await api.get(`/api/v1/queue/status/${tierId}`);
            if (queueRes.data.data.queueActive) { navigate(`/queue/${tierId}`); return; }
          } catch {}
          toast.error('Tickets are currently in high demand. Please try again shortly.');
        } else {
          toast.error('Lock failed');
        }
      } finally { setLocking(false); }
    }
  };

  const handleCreateBooking = async () => {
    setLocking(true);
    try {
      const res = await api.post('/api/v1/bookings', {
        tierId,
        quantity: tier?.sectionType === 'ASSIGNED' ? selectedSeatIds.length : quantity,
        seatIds: tier?.sectionType === 'ASSIGNED' ? selectedSeatIds : undefined,
        idempotencyKey: generateIdempotencyKey(),
      });
      setBooking(res.data.data);
      setStep('payment');
    } catch (err) { toast.error(err.response?.data?.message || 'Error'); }
    finally { setLocking(false); }
  };

  const handlePayment = async () => {
    setLocking(true);
    try {
      const res = await api.post('/api/v1/payments/create-order', { bookingId: booking.id });
      const { razorpayOrderId, razorpayKeyId, amount } = res.data.data;
      const options = {
        key: razorpayKeyId,
        amount: amount * 100,
        currency: 'INR',
        name: 'ConveneSpace',
        order_id: razorpayOrderId,
        handler: () => {
          setStep('confirmed');
          setSelectedSeatIds([]);
          setLockTimerEnd(null);
          clearActiveLock();
          clearBooking();
        },
       modal: {
  ondismiss: async () => {
    try {
      await api.post(`/api/v1/payments/cancel/${booking.id}`);
    } catch (e) {
      console.error(e);
    }

    setSelectedSeatIds([]);
    setLockTimerEnd(null);
    clearActiveLock();
    clearBooking();

    toast.error('Payment cancelled');
    navigate(`/concerts/${selectedConcert.id}`);
  }
}
      };
      new window.Razorpay(options).open();
    } catch { toast.error('Payment failed'); }
    finally { setLocking(false); }
  };

  if (loading || !tier) return <div className="py-20 text-center">Loading...</div>;

  if (step === 'confirmed') {
    return (
      <div className="max-w-md mx-auto py-20 text-center">
        <CheckCircle className="h-16 w-16 text-green-500 mx-auto mb-4" />
        <h1 className="text-2xl font-bold">Confirmed!</h1>
        <Button className="mt-6" onClick={() => navigate('/my-bookings')}>View Tickets</Button>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8 space-y-6">
      {locked && (
        <ExpiryTimerAlert
          seconds={lockTimer}
          totalSeconds={tier.lockTtlMinutes * 60}
          onRelease={handleReleaseAll}
        />
      )}

      <div className="bg-card border rounded-2xl p-6 shadow-sm">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-xl font-bold">{selectedConcert?.title}</h1>
            <p className="text-sm text-muted-foreground">{tier.tierName}</p>
          </div>
          <Badge variant="secondary">{availableCount} Available</Badge>
        </div>

        {tier?.sectionType === 'ASSIGNED' ? (
          <SeatMap
            seats={seats}
            selectedSeats={selectedSeatIds}
            onSeatToggle={handleSeatToggle}
            liveUpdates={seatUpdates}
            canBook={canBook}
            canBookReason={canBookReason}
             remainingSlots={remainingSlots}
          />
        ) : (
          <div className="p-10 text-center border-2 border-dashed rounded-xl">
            <p className="text-muted-foreground mb-4">General Admission</p>
            <div className="flex items-center justify-center gap-3">
              <Button variant="outline" size="sm" onClick={() => setQuantity(q => Math.max(1, q - 1))} disabled={locked}>-</Button>
              <span className="text-2xl font-bold w-8 text-center">{quantity}</span>
              <Button variant="outline" size="sm" onClick={() => setQuantity(q => Math.min(tier.maxPerUser, q + 1))} disabled={locked}>+</Button>
            </div>
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-card border rounded-2xl p-6 shadow-sm space-y-4">
          <h2 className="font-bold text-sm uppercase tracking-wider text-muted-foreground">Price Details</h2>
          <div className="space-y-3">
            <div className="flex justify-between text-sm">
              <span>Tickets Selected</span>
             <span className="font-mono">{tier?.sectionType === 'ASSIGNED' ? selectedSeatIds.length : quantity}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span>Base Amount</span>
              <span className="font-mono">{formatCurrency(baseAmount)}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span>Platform Fee</span>
              <span className="font-mono">{formatCurrency(booking?.platformFee ?? estimate?.platformFee ?? 0)}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span>Gateway Fee</span>
              <span className="font-mono">{formatCurrency(booking?.paymentGatewayFee ?? estimate?.gatewayFee ?? 0)}</span>
            </div>
            <Separator />
            <div className="flex justify-between font-bold text-xl text-primary">
              <span>Total Pay</span>
              <span>{formatCurrency(booking?.totalAmount ?? estimate?.totalAmount ?? baseAmount)}</span>
            </div>
          </div>
        </div>

        <div className="bg-card border rounded-2xl p-6 shadow-sm flex flex-col justify-center space-y-4">
          {step === 'select' && (
            <Button className="w-full h-14 text-lg gap-2" onClick={handleLock}
              disabled={locking || !canBook || (tier?.sectionType === 'ASSIGNED' && selectedSeatIds.length === 0)}>
              {locking ? <Loader2 className="h-5 w-5 animate-spin" /> : null}
              {!canBook ? canBookReason : locking ? 'Locking Seats...' : 'Lock Seats & Continue'}
            </Button>
          )}
          {step === 'locked' && (
            <Button className="w-full h-14 text-lg" onClick={handleCreateBooking} disabled={locking}>
              {locking ? <Loader2 className="h-5 w-5 animate-spin mr-2" /> : null}
              Confirm Booking
            </Button>
          )}
          {step === 'payment' && (
            <Button className="w-full h-14 text-lg" onClick={handlePayment} variant="default">
              <CreditCard className="mr-2 h-5 w-5" />
              Pay {formatCurrency(booking.totalAmount)}
            </Button>
          )}
          <p className="text-[10px] text-center text-muted-foreground uppercase tracking-widest">
            Payments are encrypted via Razorpay
          </p>
        </div>
      </div>
    </div>
  );
}