import ConcertCard from './ConcertCard';
import ConcertCardSkeleton from './ConcertCardSkeleton';
import EmptyState from './EmptyState';
import { Music } from 'lucide-react';
import { Button } from './ui/button';

export default function ConcertGrid({
  concerts,
  loading,
  emptyMessage = 'No concerts found',
  emptyDescription = 'Try a different search or check back later',
}) {
  if (loading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        {Array.from({ length: 6 }).map((_, i) => (
          <ConcertCardSkeleton key={i} />
        ))}
      </div>
    );
  }
  if (!concerts?.length) {
    return (
      <EmptyState
        icon={Music}
        title={emptyMessage}
        description={emptyDescription}
      />
    );
  }
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
      {concerts.map((c) => (
        <ConcertCard key={c.id} concert={c} />
      ))}
    </div>
  );
}