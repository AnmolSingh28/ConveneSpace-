import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import api from '../lib/axios';
import toast from 'react-hot-toast';
import { ArrowLeft } from 'lucide-react';

export default function CreateConcertPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [venues, setVenues] = useState([]);
  const [categories, setCategories] = useState([]);
  const [showNewCategory, setShowNewCategory] = useState(false);
const [newCategory, setNewCategory] = useState('');
const [creatingCategory, setCreatingCategory] = useState(false);
  const DRAFT_KEY = "concert-form-draft";
  const DRAFT_EXPIRY = 30 * 60 * 1000;
  const [form, setForm] = useState({
    title: '',
    artistName: '',
    description: '',
    venueId: '',
    categoryId: '',
    concertDate: '',
    doorsOpenTime: '',
    saleStartTime: '',
    saleEndTime: '',
    bannerImageUrl: '',
    requiresPreRegistration: false,
  });

  useEffect(() => {
    const saved = localStorage.getItem(DRAFT_KEY);
    if (saved) {
      try{
      const { form: savedForm, expiresAt } = JSON.parse(saved);
      if (Date.now() < expiresAt) {
        setForm(savedForm);
      } else {
        localStorage.removeItem(DRAFT_KEY);
      }
    }catch{localStorage.removeItem(DRAFT_KEY); }
    }
    api.get('/api/v1/venues').then((res) => {
      setVenues(res.data.data || []);
    });
    api.get('/api/v1/categories').then((res) => {
      const cats = res.data.data || [];
      setCategories(cats);
      if (cats.length > 0) {
        setForm((f) => ({ ...f, categoryId: f.categoryId || cats[0].id }));
      }
    });
  }, []);
  useEffect(() => {
    localStorage.setItem(DRAFT_KEY,
        JSON.stringify({
          form,
          expiresAt: Date.now() + DRAFT_EXPIRY,
        })
    );
  }, [form]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const payload = {
        ...form,
        concertDate: new Date(form.concertDate).toISOString(),
        doorsOpenTime: form.doorsOpenTime
          ? new Date(form.doorsOpenTime).toISOString()
          : null,
        saleStartTime: new Date(form.saleStartTime).toISOString(),
        saleEndTime: form.saleEndTime
          ? new Date(form.saleEndTime).toISOString()
          : null,
      };
      const res = await api.post('/api/v1/concerts', payload);
      toast.success('Event created successfully');
      localStorage.removeItem(DRAFT_KEY);
      navigate(`/organizer/concert/${res.data.data.id}`);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create event');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto px-4 sm:px-6 py-8">
      <button
        onClick={() => navigate('/organizer')}
        className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-6"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to Dashboard
      </button>

      <h1 className="text-2xl font-bold mb-6">Create Event</h1>

      <form onSubmit={handleSubmit} className="space-y-5">
        <div className="bg-card border rounded-xl p-5 space-y-4">
          <h2 className="font-semibold text-sm">Basic Info</h2>

          <div className="space-y-2">
            <Label>Event Title</Label>
            <Input
              placeholder="e.g. Coldplay — Music of the Spheres"
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              required
            />
          </div>

          <div className="space-y-2">
            <Label>Artist / Performer Name</Label>
            <Input
              placeholder="e.g. Coldplay"
              value={form.artistName}
              onChange={(e) => setForm({ ...form, artistName: e.target.value })}
              required
            />
          </div>

          <div className="space-y-2">
            <Label>Event Category</Label>
          <select
  className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring"
  value={form.categoryId}
  onChange={(e) => {
    if (e.target.value === "__new__") {
      setShowNewCategory(true);
      return;
    }

    setShowNewCategory(false);
    setForm({ ...form, categoryId: e.target.value });
  }}
  required
>
    {categories.map((cat) => (
    <option key={cat.id} value={cat.id}>
      {cat.name}
    </option>
  ))}

  <option value="__new__">
    + Add New Category
  </option>
    </select>
    {showNewCategory && (
  <div className="mt-3 space-y-2">
    <Input
      placeholder="Enter category name"
      value={newCategory}
      onChange={(e) => setNewCategory(e.target.value)}
    />

    <Button
      type="button"
      onClick={async () => {
        try {
          const res = await api.post(
            `/api/v1/categories?name=${encodeURIComponent(newCategory)}`
          );

          const created = res.data.data;

          setCategories(prev => [...prev, created]);

          setForm(f => ({
            ...f,
            categoryId: created.id
          }));

          setShowNewCategory(false);
          setNewCategory('');

          toast.success('Category created');
        } catch (err) {
          toast.error('Failed to create category');
        }
      }}
    >
      Create Category
    </Button>
  </div>
)}
          </div>

          <div className="space-y-2">
            <Label>Description</Label>
            <textarea
              className="w-full min-h-24 px-3 py-2 rounded-md border bg-background text-sm resize-none focus:outline-none focus:ring-2 focus:ring-ring"
              placeholder="Describe your event..."
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              required
            />
          </div>

          <div className="space-y-2">
            <Label>Banner Image URL</Label>
            <Input
              placeholder="https://..."
              value={form.bannerImageUrl}
              onChange={(e) =>
                setForm({ ...form, bannerImageUrl: e.target.value })
              }
            />
          </div>
        </div>

        <div className="bg-card border rounded-xl p-5 space-y-4">
          <h2 className="font-semibold text-sm">Venue & Schedule</h2>

          <div className="space-y-2">
            <Label>Venue</Label>
            <select
              className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              value={form.venueId}
              onChange={(e) => setForm({ ...form, venueId: e.target.value })}
              required
            >
              <option value="">Select a venue</option>
              {venues.map((v) => (
                <option key={v.id} value={v.id}>
                  {v.name}, {v.city}
                </option>
              ))}

            </select>
            <Button
                type="button"
                variant="link"
                className="px-0 h-auto"
                onClick={() => navigate("/organizer/venue/create")}
            >
              + Add New Venue
            </Button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Event Date & Time</Label>
              <Input
                type="datetime-local"
                value={form.concertDate}
                onChange={(e) =>
                  setForm({ ...form, concertDate: e.target.value })
                }
                required
              />
            </div>
            <div className="space-y-2">
              <Label>Doors Open</Label>
              <Input
                type="datetime-local"
                value={form.doorsOpenTime}
                onChange={(e) =>
                  setForm({ ...form, doorsOpenTime: e.target.value })
                }
              />
            </div>
            <div className="space-y-2">
              <Label>Sale Start</Label>
              <Input
                type="datetime-local"
                value={form.saleStartTime}
                onChange={(e) =>
                  setForm({ ...form, saleStartTime: e.target.value })
                }
                required
              />
            </div>
            <div className="space-y-2">
              <Label>Sale End</Label>
              <Input
                type="datetime-local"
                value={form.saleEndTime}
                onChange={(e) =>
                  setForm({ ...form, saleEndTime: e.target.value })
                }
              />
            </div>
          </div>

          <div className="flex items-center gap-3">
            <input
              type="checkbox"
              id="preReg"
              checked={form.requiresPreRegistration}
              onChange={(e) =>
                setForm({ ...form, requiresPreRegistration: e.target.checked })
              }
              className="rounded"
            />
            <Label htmlFor="preReg" className="cursor-pointer">
              Requires pre-registration (high demand events)
            </Label>
          </div>
        </div>

        <Button type="submit" className="w-full" size="lg" disabled={loading}>
          {loading ? 'Creating...' : 'Create Event'}
        </Button>
      </form>
    </div>
  );
}