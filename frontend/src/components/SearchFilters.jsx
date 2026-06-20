import { useState } from 'react';
import { Search, MapPin, Filter, X, Navigation, Loader2 } from 'lucide-react';
import { Input } from './ui/input';
import { Button } from './ui/button';
import { Badge } from './ui/badge';

const CITIES = ['Mumbai', 'Delhi', 'Bangalore', 'Hyderabad', 'Chennai', 'Pune', 'Kolkata', 'Jaipur'];
const RADIUS_OPTIONS = [
  { value: 25, label: '25 km' },
  { value: 50, label: '50 km' },
  { value: 100, label: '100 km' },
];

export default function SearchFilters({
  onSearch,
  onCityChange,
  onClear,
  selectedCity,
  categories = [],
  selectedCategory,
  onCategoryChange,
  userLocation,
  selectedRadius,
  onUseLocation,
  onRadiusChange,
  onClearLocation,
  locationLoading,
}) {
  const [query, setQuery] = useState('');
  const [showFilters, setShowFilters] = useState(false);

  const handleSearch = (e) => {
    e.preventDefault();
    onSearch(query);
  };

  const activeFilterCount = [selectedCity, selectedCategory, userLocation].filter(Boolean).length;

  return (
    <div className="w-full max-w-2xl mx-auto">
      <form onSubmit={handleSearch} className="flex gap-2">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            className="pl-9 bg-background"
            placeholder="Search artist, concert, venue..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>
        <Button type="submit">Search</Button>
        <Button
          type="button"
          variant="outline"
          onClick={() => setShowFilters(!showFilters)}
          className="gap-2 relative"
        >
          <Filter className="h-4 w-4" />
          <span className="hidden sm:inline">Filters</span>
          {activeFilterCount > 0 && (
            <span className="absolute -top-1.5 -right-1.5 bg-primary text-white text-[10px] w-4 h-4 rounded-full flex items-center justify-center">
              {activeFilterCount}
            </span>
          )}
        </Button>
      </form>

      {/* Filter Panel */}
      {showFilters && (
        <div className="mt-3 bg-background border rounded-xl shadow-lg p-4 space-y-4 text-left">

          {/* City */}
          <div>
            <p className="text-xs font-semibold text-muted-foreground mb-2 uppercase tracking-wide">City</p>
            <div className="flex flex-wrap gap-2">
              <button
                onClick={() => onCityChange('')}
                className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${
                  !selectedCity ? 'bg-primary text-white' : 'bg-secondary text-secondary-foreground hover:bg-secondary/80'
                }`}
              >
                All Cities
              </button>
              {CITIES.map((city) => (
                <button
                  key={city}
                  onClick={() => { onCityChange(city); setShowFilters(false); }}
                  className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${
                    selectedCity === city ? 'bg-primary text-white' : 'bg-secondary text-secondary-foreground hover:bg-secondary/80'
                  }`}
                >
                  {city}
                </button>
              ))}
            </div>
          </div>

          {/* Category */}
          <div>
            <p className="text-xs font-semibold text-muted-foreground mb-2 uppercase tracking-wide">Category</p>
            <div className="flex flex-wrap gap-2">
              <button
                onClick={() => onCategoryChange('')}
                className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${
                  !selectedCategory ? 'bg-primary text-white' : 'bg-secondary text-secondary-foreground hover:bg-secondary/80'
                }`}
              >
                All
              </button>
              {categories.map((cat) => (
                <button
                  key={cat.id}
                 onClick={() => { onCategoryChange(cat.id); setShowFilters(false);  }}
                  
              className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${
                    selectedCategory === cat.name ? 'bg-primary text-white' : 'bg-secondary text-secondary-foreground hover:bg-secondary/80'
                  }`}
                >
                  {cat.name}
                </button>
              ))}
            </div>
          </div>

          {/* Location */}
          <div>
            <p className="text-xs font-semibold text-muted-foreground mb-2 uppercase tracking-wide">Nearby</p>
            {!userLocation ? (
              <button
                onClick={() => { onUseLocation(); setShowFilters(false); }}
                disabled={locationLoading}
                className="flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium bg-secondary text-secondary-foreground hover:bg-secondary/80 transition-colors disabled:opacity-50"
              >
                {locationLoading ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Navigation className="h-3.5 w-3.5" />}
                {locationLoading ? 'Getting location...' : 'Use my location'}
              </button>
            ) : (
              <div className="flex flex-wrap gap-2 items-center">
                {RADIUS_OPTIONS.map((r) => (
                  <button
                    key={r.value}
                    onClick={() => { onRadiusChange(r.value); setShowFilters(false); }}
                    className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${
                      selectedRadius === r.value ? 'bg-primary text-white' : 'bg-secondary text-secondary-foreground hover:bg-secondary/80'
                    }`}
                  >
                    {r.label}
                  </button>
                ))}
                <button onClick={onClearLocation} className="text-xs text-muted-foreground hover:text-foreground">
                  ✕ Clear location
                </button>
              </div>
            )}
          </div>

          {/* Active filters + Clear all */}
          {activeFilterCount > 0 && (
            <div className="flex items-center justify-between pt-2 border-t">
              <div className="flex flex-wrap gap-1.5">
                {selectedCity && (
                  <Badge variant="secondary" className="gap-1 text-xs">
                    {selectedCity}
                    <button onClick={() => onCityChange('')}><X className="h-3 w-3" /></button>
                  </Badge>
                )}
                {selectedCategory && (
                  <Badge variant="secondary" className="gap-1 text-xs">
                    {selectedCategory}
                    <button onClick={() => onCategoryChange('')}><X className="h-3 w-3" /></button>
                  </Badge>
                )}
                {userLocation && (
                  <Badge variant="secondary" className="gap-1 text-xs">
                    Nearby {selectedRadius}km
                    <button onClick={onClearLocation}><X className="h-3 w-3" /></button>
                  </Badge>
                )}
              </div>
              <button
                onClick={() => { onClear(); onCategoryChange(''); onClearLocation(); setShowFilters(false); }}
                className="text-xs text-destructive hover:underline"
              >
                Clear all
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}