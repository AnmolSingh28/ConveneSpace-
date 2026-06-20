import { useState, useEffect } from 'react';
import { Badge } from '../components/ui/badge';
import { X } from 'lucide-react';
import ConcertGrid from '../components/ConcertGrid';
import Pagination from '../components/Pagination';
import SearchFilters from '../components/SearchFilters';
import api from '../lib/axios';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import PageLoader from '../components/PageLoader';

const RADIUS_OPTIONS = [
  { value: 25, label: '25 km' },
  { value: 50, label: '50 km' },
  { value: 100, label: '100 km' },
];

export default function HomePage() {
  const [featured, setFeatured]               = useState([]);
  const [concerts, setConcerts]               = useState([]);
  const [loading, setLoading]                 = useState(true);
  const [selectedCity, setSelectedCity]       = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [categories, setCategories]           = useState([]);
  const [page, setPage]                       = useState(0);
  const [totalPages, setTotalPages]           = useState(0);
  const [userLocation, setUserLocation]       = useState(null);
  const [locationLoading, setLocationLoading] = useState(false);
  const [selectedRadius, setSelectedRadius]   = useState(25);

  const navigate = useNavigate();

  useEffect(() => {
    fetchFeatured();
    api.get('/api/v1/categories').then((res) => setCategories(res.data.data || []));
  }, []);

  useEffect(() => {
    fetchConcerts();
  }, [selectedCity, selectedCategory, userLocation, selectedRadius, page]);

  const fetchFeatured = async () => {
    try {
      const res = await api.get('/api/v1/concerts/featured');
      setFeatured(res?.data?.data || []);
    } catch {
      setFeatured([]);
    }
  };

  const fetchConcerts = async () => {
    setLoading(true);
    try {
      let res;
      if (userLocation) {
        res = await api.get('/api/v1/concerts/nearby', {
          params: { lat: userLocation.lat, lng: userLocation.lng, radiusKm: selectedRadius, page, size: 9 },
        });
      } else if (selectedCategory) {
        res = await api.get(`/api/v1/concerts/category/${encodeURIComponent(selectedCategory)}`, {
          params: { page, size: 9 },
        });
      } else if (selectedCity) {
        res = await api.get(`/api/v1/concerts/city/${selectedCity}`, {
          params: { page, size: 9 },
        });
      } else {
        res = await api.get('/api/v1/concerts', { params: { page, size: 9 } });
      }
      const data = res?.data?.data || res?.data;
      setConcerts(data?.content || []);
      setTotalPages(data?.totalPages || 0);
    } catch (err) {
      console.error('CONCERT FETCH ERROR:', err);
      setConcerts([]);
      setTotalPages(0);
    } finally {
      setLoading(false);
    }
  };

  const handleUseLocation = () => {
    if (!navigator.geolocation) { toast.error('Geolocation not supported'); return; }
    setLocationLoading(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setUserLocation({ lat: pos.coords.latitude, lng: pos.coords.longitude });
        setSelectedRadius(25);
        setSelectedCity('');
        setSelectedCategory('');
        setPage(0);
        setLocationLoading(false);
        toast.success('Showing events near you');
      },
      () => { toast.error('Could not get location'); setLocationLoading(false); }
    );
  };

  const handleClearLocation = () => { setUserLocation(null); setPage(0); };

  const getSectionTitle = () => {
    if (userLocation) {
      const r = RADIUS_OPTIONS.find(r => r.value === selectedRadius);
      return `Events within ${r?.label || selectedRadius + ' km'}`;
    }
    if (selectedCategory) {
      const category = categories.find(c => c.id === selectedCategory);
      return `${category?.name || 'Category'} Events`;
    }
    if (selectedCity) return `Events in ${selectedCity}`;
    return 'Upcoming Events';
  };

  return (
    <div className="bg-background text-foreground">
      {/* HERO */}
      <div className="relative overflow-hidden border-b border-border/60">
        <div className="pointer-events-none absolute -right-24 -top-24 h-[420px] w-[420px] rounded-full bg-primary/10 blur-3xl" />
        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-16 pb-12 sm:pt-20 sm:pb-14">
          <div className="grid lg:grid-cols-[1.1fr_auto] lg:items-end gap-y-8 gap-x-12">
            <div>
              {/* LIVE/LOCATION PILL  */}
              {!userLocation ? (
                <button
                  type="button"
                  onClick={handleUseLocation}
                  disabled={locationLoading}
                  className="group inline-flex items-center gap-2 text-[11px] font-medium uppercase tracking-[0.2em] text-muted-foreground transition-colors hover:text-foreground disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <span className="relative flex h-2 w-2">
                    <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-primary opacity-75" />
                    <span className="relative inline-flex h-2 w-2 rounded-full bg-primary" />
                  </span>
                  {locationLoading ? 'Finding events near you' : 'Live events near you'}
                </button>
              ) : (
                <span className="inline-flex items-center gap-2 text-[11px] font-medium uppercase tracking-[0.2em] text-foreground">
                  <span className="relative flex h-2 w-2">
                    <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-primary opacity-75" />
                    <span className="relative inline-flex h-2 w-2 rounded-full bg-primary" />
                  </span>
                  Live · Nearby {selectedRadius}km
                  <button
                    type="button"
                    onClick={handleClearLocation}
                    aria-label="Clear location"
                    className="ml-1 rounded-full p-0.5 text-muted-foreground transition-colors hover:text-foreground"
                  >
                    <X className="h-3 w-3" />
                  </button>
                </span>
              )}

              <h1 className="mt-5 max-w-2xl text-5xl sm:text-7xl font-semibold tracking-[-0.03em] leading-[0.95]">
                Live events,
                <br />
                <span className="font-serif italic font-normal text-primary">unforgettable</span> moments
              </h1>
              <p className="mt-6 max-w-md text-base text-muted-foreground leading-relaxed text-pretty">
                Concerts, comedy, theatre and more — discover and book tickets to the best events happening near you.
              </p>
            </div>

            <div className="hidden lg:block text-right">
              <p className="text-6xl font-semibold tracking-tight tabular-nums">{featured.length + concerts.length}+</p>
              <p className="mt-1 text-xs uppercase tracking-[0.2em] text-muted-foreground">events on sale</p>
            </div>
          </div>

          <div className="mt-10 border-t border-border/60 pt-8">
            <SearchFilters
              onSearch={(q) => navigate(`/search?q=${encodeURIComponent(q)}`)}
              onCityChange={(city) => { setSelectedCity(city); setSelectedCategory(''); handleClearLocation(); setPage(0); }}
              onClear={() => { setSelectedCity(''); setPage(0); }}
              selectedCity={selectedCity}
              categories={categories}
              selectedCategory={selectedCategory}
              onCategoryChange={(cat) => { setSelectedCategory(cat); setSelectedCity(''); handleClearLocation(); setPage(0); }}
              userLocation={userLocation}
              selectedRadius={selectedRadius}
              onUseLocation={handleUseLocation}
              onRadiusChange={(r) => { setSelectedRadius(r); setPage(0); }}
              onClearLocation={handleClearLocation}
              locationLoading={locationLoading}
            />
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-14 sm:py-16">
        {/* FEATURED */}
        {featured.length > 0 && (
          <section className="mb-16">
            <div className="flex items-end justify-between mb-7 border-b border-border/60 pb-4">
              <div className="flex items-baseline gap-3">
                <span className="text-xs font-medium tabular-nums text-muted-foreground">01</span>
                <h2 className="text-2xl font-semibold tracking-tight">Featured events</h2>
              </div>
              <Badge variant="secondary" className="shrink-0 rounded-full text-xs font-medium">{featured.length} events</Badge>
            </div>
            <ConcertGrid concerts={featured} loading={false} />
          </section>
        )}

        <section>
          <div className="flex items-baseline gap-3 mb-7 border-b border-border/60 pb-4">
            <span className="text-xs font-medium tabular-nums text-muted-foreground">{featured.length > 0 ? '02' : '01'}</span>
            <h2 className="text-2xl font-semibold tracking-tight">{getSectionTitle()}</h2>
          </div>
          {loading ? (
            <PageLoader message="Finding events for you..." />
          ) : (
            <>
              <ConcertGrid
                concerts={concerts}
                loading={false}
                emptyMessage="No events found"
                emptyDescription={
                  userLocation ? 'No events in this radius. Try expanding the distance.'
                  : selectedCategory ? `No events in this category right now`
                  : selectedCity ? `No events in ${selectedCity} right now`
                  : 'Check back soon for new events'
                }
              />
              {concerts.length > 0 && (
                <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
              )}
            </>
          )}
        </section>
      </div>
    </div>
  );
}