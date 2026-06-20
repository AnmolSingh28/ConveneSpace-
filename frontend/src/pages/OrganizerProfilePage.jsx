import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Star, Calendar, MapPin, Users, ChevronRight, ThumbsUp } from 'lucide-react';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Skeleton } from '../components/ui/skeleton';
import api from '../lib/axios';
import { formatDate, formatCurrency } from '../lib/utils';
import toast from 'react-hot-toast';

function StarRating({ rating, size = 'sm' }) {
  const stars = [1, 2, 3, 4, 5];
  const sz = size === 'sm' ? 'h-3.5 w-3.5' : 'h-5 w-5';
  return (
    <div className="flex items-center gap-0.5">
      {stars.map((s) => (
        <Star
          key={s}
          className={`${sz} ${s <= Math.round(rating) ? 'text-amber-400 fill-amber-400' : 'text-muted-foreground/30'}`}
        />
      ))}
    </div>
  );
}

function RatingBar({ rating, count, total }) {
  const pct = total > 0 ? Math.round((count / total) * 100) : 0;
  return (
    <div className="flex items-center gap-2">
      <span className="text-xs text-muted-foreground w-4">{rating}</span>
      <Star className="h-3 w-3 text-amber-400 fill-amber-400 shrink-0" />
      <div className="flex-1 h-1.5 bg-muted rounded-full overflow-hidden">
        <div className="h-full bg-amber-400 rounded-full" style={{ width: `${pct}%` }} />
      </div>
      <span className="text-xs text-muted-foreground w-6 text-right">{count}</span>
    </div>
  );
}

export default function OrganizerProfilePage() {
  const { organizerId } = useParams();
  const navigate = useNavigate();

  const [summary, setSummary] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [concerts, setConcerts] = useState([]);
  const [loadingSummary, setLoadingSummary] = useState(true);
  const [loadingReviews, setLoadingReviews] = useState(true);
  const [loadingConcerts, setLoadingConcerts] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  useEffect(() => {
    fetchSummary();
    fetchConcerts();
  }, [organizerId]);

  useEffect(() => {
    fetchReviews();
  }, [organizerId, page]);

  const fetchSummary = async () => {
    try {
      const res = await api.get(`/api/v1/reviews/organizer/${organizerId}/summary`);
      setSummary(res.data.data);
    } catch {
      toast.error('Failed to load organizer info');
    } finally {
      setLoadingSummary(false);
    }
  };

  const fetchReviews = async () => {
    setLoadingReviews(true);
    try {
      const res = await api.get(`/api/v1/reviews/organizer/${organizerId}`, {
        params: { page, size: 10 },
      });
      const data = res.data.data;
      setReviews(data.content || []);
      setTotalPages(data.totalPages || 1);
    } catch {
      toast.error('Failed to load reviews');
    } finally {
      setLoadingReviews(false);
    }
  };

  const fetchConcerts = async () => {
    try {
      const res = await api.get(`/api/v1/concerts/organizer/${organizerId}`, {
        params: { page: 0, size: 6 },
      });
     setConcerts((res.data.data?.content || []).filter(c => c.status !== 'DRAFT'));
    } catch {} 
    finally {
      setLoadingConcerts(false);
    }
  };

  const getRatingLabel = (rating) => {
    if (!rating) return 'No ratings yet';
    if (rating >= 4.5) return 'Excellent';
    if (rating >= 4.0) return 'Very Good';
    if (rating >= 3.0) return 'Good';
    if (rating >= 2.0) return 'Fair';
    return 'Poor';
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">

      {/* Header */}
      <div className="bg-card border rounded-2xl p-6">
        {loadingSummary ? (
          <div className="space-y-3">
            <Skeleton className="h-8 w-48" />
            <Skeleton className="h-4 w-32" />
          </div>
        ) : (
          <div className="flex flex-col sm:flex-row gap-6 items-start">
            {/* Avatar */}
            <div className="w-16 h-16 rounded-2xl bg-primary/10 flex items-center justify-center text-2xl font-bold text-primary shrink-0">
              {summary?.organizerName?.[0] || 'O'}
            </div>

            <div className="flex-1">
              <h1 className="text-xl font-bold">{summary?.organizerName || 'Organizer'}</h1>
              <p className="text-sm text-muted-foreground mt-0.5">Event Organizer</p>

              {summary?.totalReviews > 0 ? (
                <div className="flex items-center gap-3 mt-3">
                  <StarRating rating={summary.averageRating} size="md" />
                  <span className="text-lg font-bold">{summary.averageRating?.toFixed(1)}</span>
                  <span className="text-sm text-muted-foreground">
                    {getRatingLabel(summary.averageRating)}
                  </span>
                  <span className="text-sm text-muted-foreground">
                    · {summary.totalReviews} {summary.totalReviews === 1 ? 'review' : 'reviews'}
                  </span>
                </div>
              ) : (
                <p className="text-sm text-muted-foreground mt-2">No reviews yet</p>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Rating breakdown */}
      {!loadingSummary && summary?.totalReviews > 0 && (
        <div className="bg-card border rounded-2xl p-6">
          <h2 className="font-semibold mb-4">Rating Breakdown</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            <div className="space-y-2">
              {[5, 4, 3, 2, 1].map((r) => {
                const found = summary.distribution?.find((d) => d.rating === r);
                return (
                  <RatingBar
                    key={r}
                    rating={r}
                    count={found?.count || 0}
                    total={summary.totalReviews}
                  />
                );
              })}
            </div>
            <div className="flex flex-col items-center justify-center">
              <span className="text-5xl font-black">{summary.averageRating?.toFixed(1)}</span>
              <StarRating rating={summary.averageRating} size="md" />
              <span className="text-sm text-muted-foreground mt-1">
                Based on {summary.totalReviews} reviews
              </span>
            </div>
          </div>
        </div>
      )}

      {/* Past concerts */}
      <div className="bg-card border rounded-2xl p-6">
        <h2 className="font-semibold mb-4">Events by this Organizer</h2>
        {loadingConcerts ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {[1, 2, 3, 4].map((i) => <Skeleton key={i} className="h-20 rounded-xl" />)}
          </div>
        ) : concerts.length === 0 ? (
          <p className="text-sm text-muted-foreground">No events found</p>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {concerts.map((c) => (
              <button
                key={c.id}
                onClick={() => navigate(`/concerts/${c.id}`)}
                className="text-left p-3 rounded-xl border hover:border-primary hover:bg-primary/5 transition-all"
              >
                <p className="font-medium text-sm line-clamp-1">{c.title}</p>
                <div className="flex items-center gap-3 mt-1">
                  <span className="flex items-center gap-1 text-xs text-muted-foreground">
                    <Calendar className="h-3 w-3" />{formatDate(c.concertDate)}
                  </span>
                  <span className="flex items-center gap-1 text-xs text-muted-foreground">
                    <MapPin className="h-3 w-3" />{c.venueCity}
                  </span>
                </div>
                <div className="flex items-center justify-between mt-1">
                  <span className="text-xs font-medium">{formatCurrency(c.startingPrice)}</span>
                  <Badge variant={c.status === 'PUBLISHED' ? 'default' : 'secondary'} className="text-[10px]">
                    {c.status}
                  </Badge>
                </div>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Reviews */}
      <div className="bg-card border rounded-2xl p-6">
        <h2 className="font-semibold mb-4">
          Reviews {summary?.totalReviews > 0 && `(${summary.totalReviews})`}
        </h2>

        {loadingReviews ? (
          <div className="space-y-4">
            {[1, 2, 3].map((i) => <Skeleton key={i} className="h-24 rounded-xl" />)}
          </div>
        ) : reviews.length === 0 ? (
          <p className="text-sm text-muted-foreground">No reviews yet.</p>
        ) : (
          <div className="space-y-4">
            {reviews.map((review) => (
              <div key={review.id} className="border rounded-xl p-4 space-y-2">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="text-sm font-medium">{review.reviewerName}</p>
                    <p className="text-xs text-muted-foreground">{review.concertTitle}</p>
                  </div>
                  <div className="flex flex-col items-end gap-1 shrink-0">
                    <StarRating rating={review.rating} />
                    <span className="text-[10px] text-muted-foreground">
                      {formatDate(review.createdAt)}
                    </span>
                  </div>
                </div>
                <p className="text-sm text-muted-foreground leading-relaxed">{review.reviewText}</p>
                {review.wouldAttendAgain && (
                  <div className="flex items-center gap-1 text-xs text-emerald-600">
                    <ThumbsUp className="h-3 w-3" /> Would attend again
                  </div>
                )}
              </div>
            ))}

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex justify-center gap-2 pt-2">
                <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
                  Previous
                </Button>
                <span className="text-sm text-muted-foreground flex items-center px-2">
                  {page + 1} / {totalPages}
                </span>
                <Button variant="outline" size="sm" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>
                  Next
                </Button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}