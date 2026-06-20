import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

import {
  Calendar,
  MapPin,
  Ticket,
  X,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';

import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Skeleton } from '../components/ui/skeleton';

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '../components/ui/dialog';

import BookingStatusBadge from '../components/BookingStatusBadge';
import EmptyState from '../components/EmptyState';
import PageLoader from '../components/PageLoader';
import TicketQR from '../components/TicketQR';
import RefundPolicyInfo from '../components/RefundPolicyInfo';

import api from '../lib/axios';
import { formatDate, formatCurrency } from '../lib/utils';
import toast from 'react-hot-toast';

export default function MyBookingsPage() {
  const navigate = useNavigate();
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cancelDialog, setCancelDialog] = useState(null);
  const [cancelling, setCancelling] = useState(false);
  const [expanded, setExpanded] = useState({});
  const [reviewDialog, setReviewDialog] = useState(null);
const [reviewForm, setReviewForm] = useState({ rating: 5, reviewText: '', wouldAttendAgain: true });
const [submittingReview, setSubmittingReview] = useState(false);

  useEffect(() => {
    fetchBookings();
  }, []);

  const fetchBookings = async () => {
    try {
      const res = await api.get('/api/v1/bookings/my-bookings', {
        params: {
          size: 20,
          sort: 'createdAt,desc',
        },
      });
      const responseData = res?.data?.data || res?.data;
      setBookings(responseData?.content || []);
    } catch (err) {
      console.error(err);
      toast.error('Failed to load bookings');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = async () => {
    if (!cancelDialog) return;
    setCancelling(true); 
    try {
      await api.delete(`/api/v1/bookings/${cancelDialog.id}`, {
        params: {
          reason: 'User requested cancellation',
        },
      });
      toast.success('Booking cancelled. Refund will be processed shortly.');
      setCancelDialog(null);
      fetchBookings();
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Failed to cancel');
    } finally {
      setCancelling(false); 
    }
  };
  const handleSubmitReview = async () => {
  setSubmittingReview(true);
  try {
    await api.post(`/api/v1/reviews/${reviewDialog.id}`, reviewForm);
    toast.success('Review submitted!');
    setReviewDialog(null);
    setReviewForm({ rating: 5, reviewText: '', wouldAttendAgain: true });
  } catch (err) {
    toast.error(err?.response?.data?.message || 'Failed to submit review');
  } finally {
    setSubmittingReview(false);
  }
};

  const toggleExpand = (id) => {
    setExpanded((prev) => ({
      ...prev,
      [id]: !prev[id],
    }));
  };

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-8 space-y-4">
        <Skeleton className="h-8 w-40 mb-6" />
        {[1, 2, 3].map((i) => (
          <Skeleton key={i} className="h-32 w-full rounded-xl" />
        ))}
      </div>
    );
  }

  return (
    <div className="relative min-h-screen">
      
      {/* 1. SCREEN-TYPE SPINNER OVERLAY */}
      {cancelling && (
        <div className="fixed inset-0 bg-background/80 backdrop-blur-sm z-[100] flex items-center justify-center">
          <PageLoader message="Processing your refund and cancelling booking..." />
        </div>
      )}

      <div className={`max-w-3xl mx-auto px-4 sm:px-6 py-8 transition-all duration-300 ${cancelling ? 'blur-md grayscale' : ''}`}>
        <h1 className="text-2xl font-bold mb-6">My Bookings</h1>

        {bookings.length === 0 ? (
          <EmptyState
            icon={Ticket}
            title="No bookings yet"
            description="Browse concerts and book your first ticket"
            action={<Button onClick={() => navigate('/')}>Browse Concerts</Button>}
          />
        ) : (
          <div className="space-y-4">
            {bookings.map((booking) => (
              <div key={booking.id} className="border rounded-xl overflow-hidden bg-card shadow-sm">
                <div className="p-5">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap mb-1">
                        <BookingStatusBadge status={booking.status} />
                        <span className="text-xs text-muted-foreground font-mono">
                          #{booking.bookingReference}
                        </span>
                      </div>
                      <h3 className="font-semibold text-base leading-tight">
                        {booking.concertTitle}
                      </h3>
                      <p className="text-sm text-muted-foreground">
                        {booking.artistName}
                      </p>
                    </div>
                    <div className="text-right shrink-0">
                      <p className="font-bold text-primary">
                        {formatCurrency(booking.totalAmount)}
                      </p>
                    </div>
                  </div>

                  <div className="mt-3 flex flex-wrap gap-3 text-sm text-muted-foreground">
                    <div className="flex items-center gap-1.5">
                      <Calendar className="h-3.5 w-3.5" />
                      {formatDate(booking.concertDate)}
                    </div>
                    <div className="flex items-center gap-1.5">
                      <MapPin className="h-3.5 w-3.5" />
                      {booking.venueName}, {booking.venueCity}
                    </div>
                  </div>

                  <div className="mt-4 flex items-center justify-between">
                    <button
                      onClick={() => toggleExpand(booking.id)}
                      className="flex items-center gap-1 text-sm text-primary hover:underline font-medium"
                    >
                      {expanded[booking.id] ? (
                        <>
                          <ChevronUp className="h-3.5 w-3.5" />
                          Hide details
                        </>
                      ) : (
                        <>
                          <ChevronDown className="h-3.5 w-3.5" />
                          View details
                        </>
                      )}
                    </button>

                    {(booking.status === 'PENDING' || booking.status === 'CONFIRMED') && (
                      <Button
                        variant="outline"
                        size="sm"
                        className="text-destructive border-destructive/30 hover:bg-destructive/10"
                        onClick={() => setCancelDialog(booking)}
                      >
                        <X className="h-3.5 w-3.5 mr-1" />
                        Cancel
                      </Button>
                    )}
                    {(booking.status === 'CONFIRMED' || booking.status === 'ATTENDED') &&
                new Date(booking.concertDate) < new Date() &&
                    booking.items?.some(i => i.checkedIn) && (
                        <Button size="sm" variant="outline"
                        className="text-amber-600 border-amber-300 hover:bg-amber-50"
                         onClick={() => setReviewDialog(booking)}>
                         ⭐ Write Review
                         </Button>
                            )}
                  </div>
                </div>

                {/* EXPANDED DETAILS */}
                {expanded[booking.id] && (
                  <div className="border-t bg-muted/30 p-5">
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-sm mb-6">
                      <div>
                        <p className="text-xs text-muted-foreground">Base Amount</p>
                        <p className="font-medium">{formatCurrency(booking.baseAmount)}</p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground">Platform Fee</p>
                        <p className="font-medium">{formatCurrency(booking.platformFee)}</p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground">Gateway Fee</p>
                        <p className="font-medium">{formatCurrency(booking.paymentGatewayFee)}</p>
                      </div>
                      {booking.refundAmount && (
                        <div>
                          <p className="text-xs text-muted-foreground">Refund Amount</p>
                          <p className="font-medium text-green-600">{formatCurrency(booking.refundAmount)}</p>
                        </div>
                      )}
                      <div>
                        <p className="text-xs text-muted-foreground">Booked On</p>
                        <p className="font-medium">{formatDate(booking.createdAt)}</p>
                      </div>
                    </div>

                    {booking.items?.length > 0 && (
                      <div className="space-y-4">
                        <p className="text-xs font-bold text-muted-foreground uppercase tracking-wider">
                          Ticket Information
                        </p>
                        <div className="space-y-3">
                          {booking.items.map((item) => (
                            <div key={item.id} className="bg-background rounded-xl p-4 border shadow-sm">
                              <div className="flex justify-between items-start text-sm">
                                <div>
                                  <p className="font-bold text-foreground">{item.tierName}</p>
                                  <p className="text-xs text-muted-foreground mt-1">
                                    {item.sectionName} · {item.quantity > 1 
                                      ? `${item.quantity} tickets` 
                                      : item.rowLabel 
                                        ? `Row ${item.rowLabel}, Seat ${item.seatNumber}` 
                                        : 'General Admission'}
                                  </p>
                                </div>
                                <div className="text-right">
                                  <p className="font-bold">{formatCurrency(item.priceAtBooking)}</p>
                                  {item.checkedIn && (
                                    <Badge variant="secondary" className="text-[10px] mt-1 bg-green-100 text-green-700 border-green-200">
                                      Checked In
                                    </Badge>
                                  )}
                                </div>
                              </div>

                              {/* TICKET QR SECTION */}
                              {booking.status === 'CONFIRMED' && (
                                <div className="mt-4 flex justify-center bg-white p-3 rounded-lg border border-dashed border-primary/20">
                                  <TicketQR item={item} />
                                </div>
                              )}
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* CANCEL DIALOG */}
      <Dialog open={!!cancelDialog} onOpenChange={() => !cancelling && setCancelDialog(null)}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Cancel Booking</DialogTitle>
          </DialogHeader>
          <div className="py-4">
            <p className="text-sm text-muted-foreground mb-4">
              Are you sure you want to cancel this booking? This action cannot be undone.
            </p>
            <div className="bg-muted/50 rounded-xl p-4 text-sm space-y-2 border">
              <p className="font-bold text-foreground">{cancelDialog?.concertTitle}</p>
              <div className="flex justify-between text-muted-foreground">
                <span>Reference:</span>
                <span className="font-mono">{cancelDialog?.bookingReference}</span>
              </div>
              <div className="flex justify-between text-muted-foreground">
                <span>Total Amount:</span>
                <span className="font-bold text-foreground">{formatCurrency(cancelDialog?.totalAmount || 0)}</span>
              </div>
            </div>

            <div className="mt-4">
              <RefundPolicyInfo />
            </div>
          </div>
          <DialogFooter className="flex gap-2">
            <Button variant="outline" className="flex-1" onClick={() => setCancelDialog(null)} disabled={cancelling}>
              Keep Booking
            </Button>
            <Button variant="destructive" className="flex-1" onClick={handleCancel} disabled={cancelling}>
              {cancelling ? 'Processing...' : 'Confirm Cancellation'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <Dialog open={!!reviewDialog} onOpenChange={() => setReviewDialog(null)}>
  <DialogContent className="sm:max-w-md">
    <DialogHeader>
      <DialogTitle>Review Organizer</DialogTitle>
    </DialogHeader>
    <div className="py-4 space-y-4">
      <p className="text-sm text-muted-foreground">
        How was your experience at <span className="font-medium text-foreground">{reviewDialog?.concertTitle}</span>?
      </p>
      <div className="space-y-1.5">
        <p className="text-xs font-medium">Rating</p>
        <div className="flex gap-2">
          {[1, 2, 3, 4, 5].map((star) => (
            <button key={star}
              onClick={() => setReviewForm(f => ({ ...f, rating: star }))}
              className={`text-2xl transition-transform hover:scale-110 ${star <= reviewForm.rating ? 'opacity-100' : 'opacity-30'}`}>
              ⭐
            </button>
          ))}
        </div>
      </div>
      <div className="space-y-1.5">
        <p className="text-xs font-medium">Your Review</p>
        <textarea
          className="w-full px-3 py-2 rounded-lg border bg-background text-sm resize-none focus:outline-none focus:ring-2 focus:ring-ring"
          rows={4}
          placeholder="Share your experience..."
          value={reviewForm.reviewText}
          onChange={(e) => setReviewForm(f => ({ ...f, reviewText: e.target.value }))}
        />
      </div>
      <div className="flex items-center gap-2">
        <input type="checkbox" id="attendAgain"
          checked={reviewForm.wouldAttendAgain}
          onChange={(e) => setReviewForm(f => ({ ...f, wouldAttendAgain: e.target.checked }))}
          className="rounded" />
        <label htmlFor="attendAgain" className="text-sm">I would attend this organizer's events again</label>
      </div>
    </div>
    <DialogFooter className="flex gap-2">
      <Button variant="outline" className="flex-1" onClick={() => setReviewDialog(null)} disabled={submittingReview}>
        Cancel
      </Button>
      <Button className="flex-1" onClick={handleSubmitReview}
        disabled={submittingReview || !reviewForm.reviewText.trim()}>
        {submittingReview ? 'Submitting...' : 'Submit Review'}
      </Button>
    </DialogFooter>
  </DialogContent>
</Dialog>
    </div>
  );
}