import { useState, useEffect } from 'react';
import { Plus, Eye, Edit, Ticket, TrendingUp, Star, Users, BarChart3, IndianRupee, CalendarCheck } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '../components/ui/button';
import { Skeleton } from '../components/ui/skeleton';
import api from '../lib/axios';
import useAuthStore from '../store/authStore';
import { formatDate, formatCurrency } from '../lib/utils';
import toast from 'react-hot-toast';

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

  useEffect(() => {
    if (!user) return;
    if (user.role !== 'ORGANIZER' && user.role !== 'ADMIN') {
      navigate('/');
      return;
    }
    fetchMyConcerts();
    fetchMetrics();
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
          <p className="text-muted-foreground text-sm mt-1">Manage your concerts and events</p>
        </div>
        <Link to="/organizer/create">
          <Button className="gap-2">
            <Plus className="h-4 w-4" />
            Create Concert
          </Button>
        </Link>
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
    </div>
  );
}