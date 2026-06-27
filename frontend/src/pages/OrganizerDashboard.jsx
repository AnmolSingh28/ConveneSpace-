import { useState, useEffect } from 'react';
import { Plus, Eye, Edit, Ticket, TrendingUp, Star, Users,Building, ChevronDown, ChevronUp, X, BarChart3, IndianRupee, CalendarCheck } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '../components/ui/button';
import { Skeleton } from '../components/ui/skeleton';
import api from '../lib/axios';
import useAuthStore from '../store/authStore';
import { formatDate, formatCurrency } from '../lib/utils';
import toast from 'react-hot-toast';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Trash2 } from "lucide-react";

function MetricCard({ icon: Icon, label, value, color }) {
  return (
    <div className="bg-card border rounded-xl p-4 flex items-center gap-4">
      <div className={`w-10 h-10 rounded-lg flex items-center justify-center shrink-0 ${color}`}>
        <Icon className="h-5 w-5" />
      </div>
      <div>
        <p className="text-xs text-muted-foreground">{label}</p>
        <p className="text-lg font-bold">{value}</p>
      </div>
    </div>
  );
}

export default function OrganizerDashboard() {
  const { user } = useAuthStore();
  const navigate = useNavigate();
  const [concerts, setConcerts] = useState([]);
  const [metrics, setMetrics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadingMetrics, setLoadingMetrics] = useState(true);
  const [venues, setVenues] = useState([]);
  const [showVenueForm, setShowVenueForm] = useState(false);
  const [expandedVenue, setExpandedVenue] = useState(null);
  const [venueSections, setVenueSections] = useState({});
  const [showSectionForm, setShowSectionForm] = useState(null);
  const [saving, setSaving] = useState(false);
  const [savingSection, setSavingSection] = useState(false);
  const SECTION_TYPES = ['GA', 'ASSIGNED'];
  const [sectionForm, setSectionForm] = useState({ name: '', sectionType: 'GA', totalCapacity: '' });
  const [venueForm, setVenueForm] = useState({ name: '', city: '', address: '', venueType: 'ESTABLISHED', totalCapacity: '', googleMapsURL: '', locationDescription: '' });

  useEffect(() => {
    if (!user) return;
    if (user.role !== 'ORGANIZER' && user.role !== 'ADMIN') {
      navigate('/');
      return;
    }
    fetchMyConcerts();
    fetchMetrics();
    fetchVenues();
  }, [user]);

  const fetchMyConcerts = async () => {
    try {
      const res = await api.get('/api/v1/concerts/my-concerts', { params: { size: 20 } });
      setConcerts(res.data.data.content || []);
    } catch {
      toast.error('Failed to load concerts');
    } finally {
      setLoading(false);
    }
  };

  const fetchMetrics = async () => {
    try {
      const res = await api.get('/api/v1/analytics/dashboard');
      setMetrics(res.data.data);
    } catch {
      
    } finally {
      setLoadingMetrics(false);
    }
  };
  const fetchVenues = async () => {
    try {
      const res = await api.get('/api/v1/venues');
      setVenues(res.data.data || []);
    } catch { toast.error('Failed to load venues'); }
    finally { setLoading(false); }
  };

  const fetchSections = async (venueId) => {
    try {
      const res = await api.get(`/api/v1/venues/${venueId}/sections`);
      setVenueSections(prev => ({ ...prev, [venueId]: res.data.data || [] }));
    } catch { toast.error('Failed to load sections'); }
  };
  const handleExpandVenue = (venueId) => {
    if (expandedVenue === venueId) {
      setExpandedVenue(null);
    } else {
      setExpandedVenue(venueId);
      if (!venueSections[venueId]) fetchSections(venueId);
    }
  };



  const handleAddSection = async (venueId) => {
    setSavingSection(true);
    try {
      await api.post(`/api/v1/venues/${venueId}/sections`, {
        name: sectionForm.name,
        sectionType: sectionForm.sectionType,
        totalCapacity: parseInt(sectionForm.totalCapacity),
      });
      toast.success('Section added');
      setSectionForm({ name: '', sectionType: 'GA', totalCapacity: '' });
      setShowSectionForm(null);
      fetchSections(venueId);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to add section');
    } finally { setSavingSection(false); }
  };
  const handleDeleteSection = async (venueId, sectionId) => {
    if (!window.confirm("Delete this section?")) return;

    try {
      await api.delete(`/api/v1/venues/${venueId}/sections/${sectionId}`);
      toast.success("Section deleted");
      fetchSections(venueId);
    } catch (err) {
      toast.error(err.response?.data?.message || "Failed to delete section");
    }
  };

  const STATUS_COLORS = {
    DRAFT: 'bg-gray-100 text-gray-700',
    PUBLISHED: 'bg-green-100 text-green-700',
    SOLD_OUT: 'bg-red-100 text-red-700',
    CANCELLED: 'bg-red-100 text-red-700',
    COMPLETED: 'bg-blue-100 text-blue-700',
  };

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold">Organizer Dashboard</h1>
          <p className="text-muted-foreground text-sm mt-1">
            Manage your concerts and events
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline"
                  onClick={() => setShowVenueForm(!showVenueForm)}
          >
            <Building className="h-4 w-4 mr-2" />
            Manage Venues
          </Button>

          <Link to="/organizer/create">
            <Button className="gap-2">
              <Plus className="h-4 w-4" />
              Create Concert
            </Button>
          </Link>
        </div>
      </div>

      {/* ANALYTICS METRICS */}
      <div className="mb-8">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground mb-3">Overview</h2>
        {loadingMetrics ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
            {[1,2,3,4,5,6].map(i => <Skeleton key={i} className="h-20 rounded-xl" />)}
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
            <MetricCard
              icon={IndianRupee}
              label="Total Revenue"
              value={metrics ? formatCurrency(metrics.totalRevenue) : '₹0'}
              color="bg-emerald-100 text-emerald-600"
            />
            <MetricCard
              icon={Ticket}
              label="Total Bookings"
              value={metrics?.totalBookings ?? 0}
              color="bg-blue-100 text-blue-600"
            />
            <MetricCard
              icon={CalendarCheck}
              label="Events Hosted"
              value={metrics?.totalEventsHosted ?? 0}
              color="bg-purple-100 text-purple-600"
            />
            <MetricCard
              icon={Star}
              label="Avg Rating"
              value={metrics?.averageRating ? `${metrics.averageRating.toFixed(1)} ⭐` : 'N/A'}
              color="bg-amber-100 text-amber-600"
            />
            <MetricCard
              icon={BarChart3}
              label="Total Reviews"
              value={metrics?.totalReviews ?? 0}
              color="bg-pink-100 text-pink-600"
            />
            <MetricCard
              icon={Users}
              label="Attendance Rate"
              value={metrics?.attendanceRate ? `${metrics.attendanceRate.toFixed(1)}%` : '0%'}
              color="bg-orange-100 text-orange-600"
            />
          </div>
        )}
      </div>

      {/* CONCERTS LIST */}
      <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground mb-3">Your Concerts</h2>
      {loading ? (
        <div className="space-y-4">
          {[1, 2, 3].map((i) => <Skeleton key={i} className="h-24 w-full rounded-xl" />)}
        </div>
      ) : concerts.length === 0 ? (
        <div className="text-center py-16 border rounded-xl bg-muted/30">
          <Ticket className="h-12 w-12 text-muted-foreground mx-auto mb-3" />
          <p className="font-medium">No concerts yet</p>
          <p className="text-sm text-muted-foreground mt-1 mb-4">Create your first concert to get started</p>
          <Link to="/organizer/create"><Button>Create Concert</Button></Link>
        </div>
      ) : (
        <div className="space-y-4">
          {concerts.map((concert) => (
            <div key={concert.id} className="border rounded-xl p-5 bg-card hover:shadow-sm transition-shadow">
              <div className="flex items-start justify-between gap-4">
                <div className="flex gap-4 flex-1 min-w-0">
                  <div className="w-16 h-16 rounded-lg overflow-hidden bg-muted shrink-0">
                    {concert.bannerImageUrl ? (
                      <img src={concert.bannerImageUrl} alt={concert.title} className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-2xl">🎵</div>
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap mb-1">
                      <h3 className="font-semibold text-sm truncate">{concert.title}</h3>
                      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_COLORS[concert.status] || 'bg-gray-100 text-gray-700'}`}>
                        {concert.status}
                      </span>
                    </div>
                    <p className="text-sm text-muted-foreground">{concert.artistName}</p>
                    <p className="text-xs text-muted-foreground mt-1">
                      {formatDate(concert.concertDate)} · {concert.venueName}, {concert.venueCity}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <Link to={`/concerts/${concert.id}`}>
                    <Button variant="outline" size="sm" className="gap-1.5">
                      <Eye className="h-3.5 w-3.5" />View
                    </Button>
                  </Link>
                  <Link to={`/organizer/concert/${concert.id}`}>
                    <Button size="sm" className="gap-1.5">
                      <Edit className="h-3.5 w-3.5" />Manage
                    </Button>
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
      {/* VENUE MANAGEMENT */}
      {showVenueForm && (
          <div className="border rounded-xl p-5 mb-6 bg-card">
            <div className="flex justify-between items-center mb-4">
              <h2 className="font-semibold">Manage Venues</h2>
              <Button variant="outline" size="sm" onClick={() => setShowVenueForm(false)}>Close</Button>
            </div>

            {/* Existing venues */}
            <div className="space-y-3 mb-4">
              {venues.map((venue) => (
                  <div key={venue.id} className="border rounded-xl bg-background overflow-hidden">
                    <div className="p-3 flex items-start justify-between gap-4">
                      <div>
                        <h3 className="font-medium text-sm">{venue.name}</h3>
                        <p className="text-xs text-muted-foreground">{venue.city} · {venue.totalCapacity?.toLocaleString()} capacity</p>
                      </div>
                      <Button variant="outline" size="sm" onClick={() => handleExpandVenue(venue.id)} className="text-xs flex items-center gap-1">
                        Sections {expandedVenue === venue.id ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
                      </Button>
                    </div>

                    {expandedVenue === venue.id && (
                        <div className="border-t bg-muted/30 p-3">
                          <div className="flex justify-between items-center mb-2">
                            <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Sections ({venueSections[venue.id]?.length || 0})</span>
                            <Button size="sm" variant="outline" className="text-xs h-7 gap-1"
                                    onClick={() => { setShowSectionForm(venue.id); }}>
                              <Plus className="h-3 w-3" /> Add Section
                            </Button>
                          </div>
                          {(venueSections[venue.id] || []).length === 0 ? (
                              <p className="text-xs text-muted-foreground italic">No sections yet.</p>
                          ) : (
                              <div className="space-y-1 mb-2">
                                {(venueSections[venue.id] || []).map((section) => (
                                    <div key={section.id} className="flex items-center justify-between bg-card rounded-lg px-3 py-2 border">
                                      <div>
                                        <p className="text-sm font-medium">{section.name}</p>
                                        <p className="text-xs text-muted-foreground">
                                          {section.sectionType} • {section.totalCapacity?.toLocaleString()} Capacity
                                        </p>
                                      </div>
                                      <Button size="icon" variant="ghost" className="h-8 w-8 text-red-500 hover:bg-red-100"
                                          onClick={() => handleDeleteSection(venue.id, section.id)}
                                      ><Trash2 className="h-4 w-4" />
                                      </Button>
                                    </div>
                                ))}
                              </div>
                          )}
                          {showSectionForm === venue.id && (
                              <div className="border rounded-lg p-3 bg-card mt-2">
                                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                                  <div className="space-y-1">
                                    <Label className="text-xs">Name</Label>
                                    <Input placeholder="e.g. Floor GA" value={sectionForm.name}
                                           onChange={(e) => setSectionForm({ ...sectionForm, name: e.target.value })} />
                                  </div>
                                  <div className="space-y-1">
                                    <Label className="text-xs">Type</Label>
                                    <select className="w-full px-3 py-2 rounded-md border bg-background text-sm"
                                            value={sectionForm.sectionType}
                                            onChange={(e) => setSectionForm({ ...sectionForm, sectionType: e.target.value })}>
                                      {SECTION_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                                    </select>
                                  </div>
                                  <div className="space-y-1">
                                    <Label className="text-xs">Capacity</Label>
                                    <Input type="number" placeholder="e.g. 500" value={sectionForm.totalCapacity}
                                           onChange={(e) => setSectionForm({ ...sectionForm, totalCapacity: e.target.value })} />
                                  </div>
                                </div>
                                <div className="flex gap-2 mt-2">
                                  <Button size="sm" className="text-xs h-7" disabled={savingSection || !sectionForm.name || !sectionForm.totalCapacity}
                                          onClick={() => handleAddSection(venue.id)}>
                                    {savingSection ? 'Adding...' : 'Add Section'}
                                  </Button>
                                  <Button size="sm" variant="outline" className="text-xs h-7" onClick={() => setShowSectionForm(null)}>Cancel</Button>
                                </div>
                              </div>
                          )}
                        </div>
                    )}
                  </div>
              ))}
            </div>

            {/* Add New Venue Button */}
            <h3 className="font-medium text-sm mb-3">Add New Venue</h3>
            <Link to="/organizer/venue/create">
              <Button className="w-full">
                <Plus className="h-4 w-4 mr-2" />
                Create Venue
              </Button>
            </Link>
          </div>
      )}

    </div>
  );
}