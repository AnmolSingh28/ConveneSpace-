import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft, Plus, Send, BarChart3,
  Ticket, Users, Loader2 ,Edit
} from 'lucide-react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Badge } from '../components/ui/badge';
import { Separator } from '../components/ui/separator';
import api from '../lib/axios';
import { formatCurrency, formatDate } from '../lib/utils';
import toast from 'react-hot-toast';
import PageLoader from '../components/PageLoader';
import EditConcertModal from '../components/EditConcertModal';
export default function ManageConcertPage() {
  const { concertId } = useParams();
  const navigate = useNavigate();
  const [concert, setConcert] = useState(null);
  const [loading, setLoading] = useState(true);
  const [publishing, setPublishing] = useState(false);
  const [addingTier, setAddingTier] = useState(false);
  const [showTierForm, setShowTierForm] = useState(false);
  const [sections, setSections] = useState([]);
  const [showEditModal, setShowEditModal] = useState(false);
  const [tierForm, setTierForm] = useState({
    tierName: '',
    sectionId: '',
    sectionType: 'GA',
    price: '',
    totalQuantity: '',
    maxPerUser: '4',
    lockTtlMinutes: '10',
    rowCount: '',    
    seatsPerRow: '',
  });

  useEffect(() => {
    fetchConcert();
  }, [concertId]);

  const fetchConcert = async () => {
    try {
      const res = await api.get(`/api/v1/concerts/${concertId}`);
      setConcert(res.data.data);
      if (res.data.data.venue?.id) {
        const sec = await api.get(
          `/api/v1/venues/${res.data.data.venue.id}/sections`
        );
        setSections(sec.data.data || []);
      }
    } catch {
      toast.error('Failed to load concert');
      navigate('/organizer');
    } finally {
      setLoading(false);
    }
  };

  const handlePublish = async () => {
    setPublishing(true);
    try {
      await api.patch(`/api/v1/concerts/${concertId}/publish`);
      toast.success('Concert published successfully!');
      fetchConcert();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to publish');
    } finally {
      setPublishing(false);
    }
  };

  const handleAddTier = async (e) => {
    e.preventDefault();
    setAddingTier(true);
    try {
      const isAssigned = tierForm.sectionType === 'ASSIGNED';
    
      const payload = {
        tierName: tierForm.tierName,
        sectionId: tierForm.sectionId,
        sectionType: tierForm.sectionType,
        price: parseFloat(tierForm.price),
        totalQuantity: isAssigned ? null : parseInt(tierForm.totalQuantity),
        rowCount: isAssigned ? parseInt(tierForm.rowCount) : null,
        seatsPerRow: isAssigned ? parseInt(tierForm.seatsPerRow) : null,
        maxPerUser: parseInt(tierForm.maxPerUser),
        lockTtlMinutes: parseInt(tierForm.lockTtlMinutes),
      };

      await api.post(`/api/v1/concerts/${concertId}/tiers`, payload);

      toast.success(`${tierForm.tierName} added successfully!`);
      setShowTierForm(false);
      
      setTierForm({
        tierName: '', sectionId: '', sectionType: 'GA', price: '',
        totalQuantity: '', maxPerUser: '4', lockTtlMinutes: '10', 
        rowCount: '', seatsPerRow: ''
      });
      
      fetchConcert();
    } catch (err) {
      toast.error(err.response?.data?.data?.sectionId || err.response?.data?.message || 'Failed to add tier');
    } finally {
      setAddingTier(false);
    }
  };

  if (loading || !concert) {
    return <PageLoader message="Fetching concert details from the King..." />;
  }

  const totalTickets = concert.ticketTiers?.reduce((sum, t) => sum + t.totalQuantity, 0) || 0;
  const soldTickets = concert.ticketTiers?.reduce((sum, t) => sum + (t.totalQuantity - t.availableQuantity), 0) || 0;

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-8">
      <button onClick={() => navigate('/organizer')} className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-6">
        <ArrowLeft className="h-4 w-4" /> Back to Dashboard
      </button>

      {/* Header */}
      <div className="flex items-start justify-between gap-4 mb-6">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <h1 className="text-xl font-bold">{concert.title}</h1>
            <Badge variant={concert.status === 'PUBLISHED' ? 'default' : 'secondary'}>{concert.status}</Badge>
          </div>
          <p className="text-sm text-muted-foreground">{formatDate(concert.concertDate)} · {concert.venue?.name}</p>
        </div>
        <div className="flex gap-2 shrink-0">
          {concert.status !== 'CANCELLED' && (
              <Button variant="outline" size="sm" className="gap-1.5" onClick={() => setShowEditModal(true)}>
                <Edit className="h-3.5 w-3.5" />Edit Event
              </Button>
          )}
          {concert.status === 'DRAFT' && (
              <Button onClick={handlePublish} disabled={publishing} className="gap-2">
                {publishing ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
                {publishing ? 'Publishing...' : 'Publish'}
              </Button>
          )}
        </div>
      </div>

      {/* Stats Section*/}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 mb-6">
        <div className="bg-card border rounded-xl p-4">
          <div className="flex items-center gap-2 mb-1">
            <Ticket className="h-4 w-4 text-muted-foreground" />
            <span className="text-xs text-muted-foreground">Total Tickets</span>
          </div>
          <p className="text-2xl font-bold">{totalTickets.toLocaleString()}</p>
        </div>
        <div className="bg-card border rounded-xl p-4">
          <div className="flex items-center gap-2 mb-1">
            <Users className="h-4 w-4 text-muted-foreground" />
            <span className="text-xs text-muted-foreground">Sold</span>
          </div>
          <p className="text-2xl font-bold text-green-600">{soldTickets.toLocaleString()}</p>
        </div>
        <div className="bg-card border rounded-xl p-4 col-span-2 sm:col-span-1">
          <div className="flex items-center gap-2 mb-1">
            <BarChart3 className="h-4 w-4 text-muted-foreground" />
            <span className="text-xs text-muted-foreground">Sell Through</span>
          </div>
          <p className="text-2xl font-bold">{totalTickets > 0 ? Math.round((soldTickets / totalTickets) * 100) : 0}%</p>
        </div>
      </div>

      {/* Ticket Tiers List Section. */}
      <div className="bg-card border rounded-xl p-5 mb-5">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-semibold">Ticket Tiers</h2>
          <Button size="sm" variant="outline" className="gap-1.5" onClick={() => setShowTierForm(!showTierForm)}>
            <Plus className="h-3.5 w-3.5" /> Add Tier
          </Button>
        </div>

        {concert.ticketTiers?.length === 0 ? (
          <p className="text-sm text-muted-foreground text-center py-4">No ticket tiers yet. Add one to get started.</p>
        ) : (
          <div className="space-y-3">
            {concert.ticketTiers?.map((tier) => (
              <div key={tier.id} className="flex items-center justify-between p-3 bg-muted/30 rounded-lg">
                <div>
                  <p className="font-medium text-sm">{tier.tierName}</p>
                  <p className="text-xs text-muted-foreground">{tier.sectionName} · Max {tier.maxPerUser}/person</p>
                </div>
                <div className="text-right">
                  <p className="font-semibold text-sm">{formatCurrency(tier.price)}</p>
                  <p className="text-xs text-muted-foreground">{tier.availableQuantity}/{tier.totalQuantity} left</p>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Add Tier Form */}
        {showTierForm && (
          <>
            <Separator className="my-4" />
            <form onSubmit={handleAddTier} className="space-y-4">
              <h3 className="text-sm font-bold text-primary">New Ticket Tier</h3>
              
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {/* 1. TIER NAME */}
                <div className="space-y-1.5">
                  <Label className="text-xs">Tier Name (Display Name)</Label>
                  <Input placeholder="e.g. VIP" value={tierForm.tierName} onChange={(e) => setTierForm({ ...tierForm, tierName: e.target.value })} required />
                </div>

                {/* 2. SECTION SELECTOR */}
                <div className="space-y-1.5">
  <Label className="text-xs">Venue Section</Label>

  <select
    className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary"
    value={tierForm.sectionId}
    onChange={(e) => {

      const selectedSec =
        sections.find(s => s.id === e.target.value);

      setTierForm({
        ...tierForm,
        sectionId: e.target.value,
        sectionType: selectedSec?.sectionType || 'GA'
      });
    }}
    required
  >
    <option value="">
      Select a physical area...
    </option>

    {sections.map((s) => (
      <option key={s.id} value={s.id}>
        {s.name} ({s.sectionType})
      </option>
    ))}
  </select>
</div>

                  <div className="space-y-1.5">
                  <Label className="text-xs">Arrangement Style</Label>
                  <select
                    className="w-full px-3 py-2 rounded-md border bg-background text-sm font-medium focus:ring-2 focus:ring-primary"
                    value={tierForm.sectionType}
                    onChange={(e) => setTierForm({ ...tierForm, sectionType: e.target.value })}
                    required
                  >
                    <option value="GA">Ground / Standing</option>
                    <option value="ASSIGNED">Numbered Seating</option>
                  </select>
                </div>
               

                {/* 4. DYNAMIC FIELDS */}
                {tierForm.sectionType === 'ASSIGNED' ? (
                  <div className="grid grid-cols-2 gap-3 col-span-1 sm:col-span-2 bg-blue-50/50 p-4 rounded-xl border border-blue-100">
                    <div className="space-y-1.5">
                      <Label className="text-xs font-bold text-blue-700">Rows (A-Z)</Label>
                      <Input type="number" value={tierForm.rowCount} onChange={(e) => setTierForm({ ...tierForm, rowCount: e.target.value })} required />
                    </div>
                    <div className="space-y-1.5">
                      <Label className="text-xs font-bold text-blue-700">Seats Per Row</Label>
                      <Input type="number" value={tierForm.seatsPerRow} onChange={(e) => setTierForm({ ...tierForm, seatsPerRow: e.target.value })} required />
                    </div>
                  </div>
                ) : (
                  <div className="space-y-1.5 col-span-1 sm:col-span-2">
                    <Label className="text-xs">Capacity (Total Tickets)</Label>
                    <Input type="number" value={tierForm.totalQuantity} onChange={(e) => setTierForm({ ...tierForm, totalQuantity: e.target.value })} required />
                  </div>
                )}

                {/* 5. PRICE & LIMITS */}
                <div className="space-y-1.5">
                  <Label className="text-xs">Price (₹)</Label>
                  <Input type="number" value={tierForm.price} onChange={(e) => setTierForm({ ...tierForm, price: e.target.value })} required />
                </div>
                <div className="space-y-1.5">
                  <Label className="text-xs">Max Per Person</Label>
                  <Input type="number" value={tierForm.maxPerUser} onChange={(e) => setTierForm({ ...tierForm, maxPerUser: e.target.value })} required />
                </div>
              </div>

              <div className="flex gap-2 pt-2">
     
                <Button type="submit" size="sm" disabled={addingTier} className="gap-2">
                  {addingTier && <Loader2 className="h-4 w-4 animate-spin" />}
                  {addingTier ? 'Creating...' : 'Create Tier'}
                </Button>
                <Button type="button" size="sm" variant="outline" onClick={() => setShowTierForm(false)}>Cancel</Button>
              </div>
            </form>
          </>
        )}
      </div>
      {showEditModal && (
          <EditConcertModal
              concert={concert}
              onClose={() => setShowEditModal(false)}
              onSaved={fetchConcert}
          />
      )}
    </div>
  );
}