import { Button } from './ui/button';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export default function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null;

  const pages = [];
  const start = Math.max(0, page - 2);
  const end = Math.min(totalPages - 1, page + 2);

  for (let i = start; i <= end; i++) {
    pages.push(i);
  }

  return (
    <div className="flex items-center justify-center gap-1 mt-8">
      <Button
        variant="outline"
        size="sm"
        disabled={page === 0}
        onClick={() => onPageChange(page - 1)}
        className="h-8 w-8 p-0"
      >
        <ChevronLeft className="h-4 w-4" />
      </Button>

      {start > 0 && (
        <>
          <Button
            variant="outline"
            size="sm"
            onClick={() => onPageChange(0)}
            className="h-8 w-8 p-0 text-xs"
          >
            1
          </Button>
          {start > 1 && (
            <span className="text-muted-foreground text-sm px-1">...</span>
          )}
        </>
      )}

      {pages.map((p) => (
        <Button
          key={p}
          variant={p === page ? 'default' : 'outline'}
          size="sm"
          onClick={() => onPageChange(p)}
          className="h-8 w-8 p-0 text-xs"
        >
          {p + 1}
        </Button>
      ))}

      {end < totalPages - 1 && (
        <>
          {end < totalPages - 2 && (
            <span className="text-muted-foreground text-sm px-1">...</span>
          )}
          <Button
            variant="outline"
            size="sm"
            onClick={() => onPageChange(totalPages - 1)}
            className="h-8 w-8 p-0 text-xs"
          >
            {totalPages}
          </Button>
        </>
      )}

      <Button
        variant="outline"
        size="sm"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
        className="h-8 w-8 p-0"
      >
        <ChevronRight className="h-4 w-4" />
      </Button>
    </div>
  );
}