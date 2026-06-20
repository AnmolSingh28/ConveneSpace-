import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search } from 'lucide-react';
import { Input } from '../components/ui/input';
import { Button } from '../components/ui/button';
import ConcertCard from '../components/ConcertCard';
import ConcertCardSkeleton from '../components/ConcertCardSkeleton';
import api from '../lib/axios';
import { Loader2 } from 'lucide-react';
import PageLoader from '../components/PageLoader';
export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [query, setQuery] = useState(searchParams.get('q') || '');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const q = searchParams.get('q');
    if (q) {
      setQuery(q);
      doSearch(q);
    }
  }, [searchParams]);

  const doSearch = async (q) => {
    setLoading(true);
    try {
      const res = await api.get('/api/v1/concerts/search', {
        params: { query: q, size: 12 },
      });
      setResults(res.data.data.content || []);
    } catch {
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    if (!query.trim()) return;
    setSearchParams({ q: query });
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <form onSubmit={handleSearch} className="flex gap-2 max-w-lg mb-8">
  <div className="relative flex-1">
    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
    <Input
      className="pl-9"
      placeholder="Search artist or concert..."
      value={query}
      onChange={(e) => setQuery(e.target.value)}
    />
  </div>
  <Button type="submit" disabled={loading} className="gap-2">
    {loading && <Loader2 className="h-4 w-4 animate-spin" />}
    {loading ? 'Searching...' : 'Search'}
  </Button>
</form>
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {Array.from({ length: 6 }).map((_, i) => (
            <ConcertCardSkeleton key={i} />
          ))}
        </div>
      ) : results.length === 0 ? (
        <div className="text-center py-16 text-muted-foreground">
          <p>No results for "{searchParams.get('q')}"</p>
        </div>
      ) : (
        <>
          <p className="text-sm text-muted-foreground mb-4">
            {results.length} results for "{searchParams.get('q')}"
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            {results.map((c) => (
              <ConcertCard key={c.id} concert={c} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}