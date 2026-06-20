import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Music, Building, Shield, ChevronDown, ChevronUp, Plus, X, Users, Ban, ShieldCheck } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import api from '../lib/axios';
import useAuthStore from '../store/authStore';
import toast from 'react-hot-toast';

const SECTION_TYPES = ['GA', 'ASSIGNED'];
const ROLES = ['USER', 'ORGANIZER'];

export default function AdminDashboard() {
  const { user } = useAuthStore();
  const navigate = useNavigate();
  const [tab, setTab] = useState('venues');
  const [venues, setVenues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showVenueForm, setShowVenueForm] = useState(false);
  const [saving, setSaving] = useState(false);
const [concerts, setConcerts] = useState([]);
const [concertsLoading, setConcertsLoading] = useState(true);
const [concertPage, setConcertPage] = useState(0);
const [concertTotalPages, setConcertTotalPages] = useState(0);
const [postponeModal, setPostponeModal] = useState(null); // concertId
const [newDate, setNewDate] = useState('');
const [users, setUsers] = useState([]);
const [usersLoading, setUsersLoading] = useState(true);
const [userPage, setUserPage] = useState(0);
const [userTotalPages, setUserTotalPages] = useState(0);
const [categories, setCategories] = useState([]);
const [categoryName, setCategoryName] = useState('');
const [categoriesLoading, setCategoriesLoading] = useState(false);
  
  const [expandedVenue, setExpandedVenue] = useState(null);
  const [venueSections, setVenueSections] = useState({});
  const [showSectionForm, setShowSectionForm] = useState(null); // venueId
  const [sectionForm, setSectionForm] = useState({
    name: '',
    sectionType: 'GA',
    totalCapacity: '',
  });
  const [savingSection, setSavingSection] = useState(false);

  const [venueForm, setVenueForm] = useState({
    name: '', city: '', address: '',
    venueType: 'ESTABLISHED', totalCapacity: '',
    googleMapsURL: '', locationDescription: '',
  });

  useEffect(() => {
    if (user?.role !== 'ADMIN') { navigate('/'); return; }
    fetchVenues();
  }, []);

  useEffect(() => {
    if (tab === 'users' && users.length === 0) fetchUsers(0);
  }, [tab]);

  useEffect(() => {
    if (tab === 'concerts' && concerts.length === 0) fetchConcerts(0);
}, [tab]);

useEffect(() => {
  if (tab === 'categories' && categories.length === 0) {
    fetchCategories();
  }
}, [tab]);
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

  const fetchUsers = async (page = 0) => {
    setUsersLoading(true);
    try {
      const res = await api.get('/api/v1/users/admin/all', { params: { page, size: 20 } });
      setUsers(res.data.data?.content || []);
      setUserTotalPages(res.data.data?.totalPages || 0);
      setUserPage(page);
    } catch { toast.error('Failed to load users'); }
    finally { setUsersLoading(false); }
  };
  const fetchConcerts = async (page = 0) => {
    setConcertsLoading(true);
    try {
        const endpoint = user?.role === 'ADMIN' 
            ? '/api/v1/concerts/admin/all' 
            : '/api/v1/concerts/my-concerts';
        const res = await api.get(endpoint, { params: { page, size: 20 } });
        setConcerts(res.data.data?.content || []);
        setConcertTotalPages(res.data.data?.totalPages || 0);
        setConcertPage(page);
    } catch { toast.error('Failed to load concerts'); }
    finally { setConcertsLoading(false); }
};
const fetchCategories = async () => {
  setCategoriesLoading(true);

  try {
    const res = await api.get('/api/v1/categories');
    setCategories(res.data.data || []);
  } catch {
    toast.error('Failed to load categories');
  } finally {
    setCategoriesLoading(false);
  }
};
const handleCreateCategory = async () => {
  if (!categoryName.trim()) return;

  try {
    await api.post(
      `/api/v1/categories?name=${encodeURIComponent(categoryName)}`
    );

    toast.success('Category created');

    setCategoryName('');
    fetchCategories();

  } catch (err) {
    toast.error(err.response?.data?.message || 'Failed to create category');
  }
};
const handleDeactivateCategory = async (id) => {
  try {
    await api.delete(`/api/v1/categories/${id}`);

    toast.success('Category deactivated');

    fetchCategories();
  } catch {
    toast.error('Failed to deactivate category');
  }
};
  const handleExpandVenue = (venueId) => {
    if (expandedVenue === venueId) {
      setExpandedVenue(null);
    } else {
      setExpandedVenue(venueId);
      if (!venueSections[venueId]) fetchSections(venueId);
    }
  };

  const handleCreateVenue = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const res = await api.post('/api/v1/venues', {
        ...venueForm,
        totalCapacity: parseInt(venueForm.totalCapacity),
      });
      toast.success('Venue created! Now add sections.');
      setShowVenueForm(false);
      setVenueForm({ name: '', city: '', address: '', venueType: 'ESTABLISHED', totalCapacity: '', googleMapsURL: '', locationDescription: '' });
      await fetchVenues();
      const newVenueId = res.data.data?.id;
      if (newVenueId) {
        setExpandedVenue(newVenueId);
        setShowSectionForm(newVenueId);
        setVenueSections(prev => ({ ...prev, [newVenueId]: [] }));
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create venue');
    } finally { setSaving(false); }
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

  const handleDeactivateVenue = async (venueId) => {
    try {
      await api.delete(`/api/v1/venues/${venueId}`);
      toast.success('Venue deactivated');
      fetchVenues();
    } catch { toast.error('Failed to deactivate venue'); }
  };

  const handleDeactivateSection = async (sectionId, venueId) => {
    try {
      await api.delete(`/api/v1/venues/sections/${sectionId}`);
      toast.success('Section removed');
      fetchSections(venueId);
    } catch { toast.error('Failed to remove section'); }
  };

  const handleBanToggle = async (userId, isActive) => {
    try {
      await api.patch(`/api/v1/users/${userId}/${isActive ? 'ban' : 'unban'}`);
      toast.success(isActive ? 'User banned' : 'User unbanned');
      fetchUsers(userPage);
    } catch { toast.error('Action failed'); }
  };

  const handleRoleChange = async (userId, newRole) => {
    try {
      await api.patch(`/api/v1/users/${userId}/role`, null, { params: { role: newRole } });
      toast.success('Role updated');
      fetchUsers(userPage);
    } catch (err) { toast.error(err.response?.data?.message || 'Failed to update role'); }
  };
  const handleCancelConcert = async (concertId) => {
    const reason = window.prompt('Reason for cancellation (optional):');
    if (reason === null) return; // user clicked cancel
    try {
        await api.patch(`/api/v1/concerts/${concertId}/cancel`, null, { params: { reason } });
        toast.success('Concert cancelled');
        fetchConcerts(concertPage);
    } catch (err) { toast.error(err.response?.data?.message || 'Failed to cancel'); }
};

const handlePostponeConcert = async (concertId) => {
    if (!newDate) { toast.error('Select a new date'); return; }
    try {
        await api.patch(`/api/v1/concerts/${concertId}/postpone`, null, {
            params: { newDate: newDate }
        });
        toast.success('Concert postponed');
        setPostponeModal(null);
        setNewDate('');
        fetchConcerts(concertPage);
    } catch (err) { toast.error(err.response?.data?.message || 'Failed to postpone'); }
};
const handleFeatureToggle = async (concertId, currentlyFeatured) => {
    try {
        await api.patch(`/api/v1/concerts/${concertId}/feature`, null, {
            params: { featured: !currentlyFeatured }
        });
        toast.success(currentlyFeatured ? 'Unfeatured' : 'Featured');
        fetchConcerts(concertPage);
    } catch { toast.error('Failed to update feature status'); }
};

  const TABS = [
    { id: 'venues', label: 'Venues', icon: Building },
    { id: 'concerts', label: 'Concerts', icon: Music },
    { id: 'users', label: 'Users', icon: Users },
    { id: 'categories', label: 'Categories', icon: Music },
  ];

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-3 mb-8">
        <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center">
          <Shield className="h-5 w-5 text-primary" />
        </div>
        <div>
          <h1 className="text-2xl font-bold">Admin Dashboard</h1>
          <p className="text-sm text-muted-foreground">Manage venues, concerts, and users</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 mb-6 border-b">
        {TABS.map((t) => (
          <button key={t.id} onClick={() => setTab(t.id)}
            className={`flex items-center gap-2 px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
              tab === t.id ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground'
            }`}>
            <t.icon className="h-4 w-4" />{t.label}
          </button>
        ))}
      </div>

      {/* Venues tab */}
      {tab === 'venues' && (
        <div>
          <div className="flex justify-between items-center mb-4">
            <h2 className="font-semibold">Venues ({venues.length})</h2>
            <Button size="sm" onClick={() => setShowVenueForm(!showVenueForm)}>Add Venue</Button>
          </div>

          {showVenueForm && (
            <div className="border rounded-xl p-5 mb-5 bg-card">
              <h3 className="font-medium mb-4">New Venue</h3>
              <form onSubmit={handleCreateVenue} className="space-y-3">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div className="space-y-1.5">
                    <Label className="text-xs">Venue Name</Label>
                    <Input placeholder="e.g. Jio World Centre" value={venueForm.name}
                      onChange={(e) => setVenueForm({ ...venueForm, name: e.target.value })} required />
                  </div>
                  <div className="space-y-1.5">
                    <Label className="text-xs">City</Label>
                    <Input placeholder="e.g. Mumbai" value={venueForm.city}
                      onChange={(e) => setVenueForm({ ...venueForm, city: e.target.value })} required />
                  </div>
                  <div className="space-y-1.5 sm:col-span-2">
                    <Label className="text-xs">Address</Label>
                    <Input placeholder="Full address" value={venueForm.address}
                      onChange={(e) => setVenueForm({ ...venueForm, address: e.target.value })} required />
                  </div>
                  <div className="space-y-1.5">
                    <Label className="text-xs">Type</Label>
                    <select className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                      value={venueForm.venueType} onChange={(e) => setVenueForm({ ...venueForm, venueType: e.target.value })}>
                      <option value="ESTABLISHED">Established</option>
                      <option value="TEMPORARY">Temporary</option>
                    </select>
                  </div>
                  <div className="space-y-1.5">
                    <Label className="text-xs">Total Capacity</Label>
                    <Input type="number" placeholder="e.g. 10000" value={venueForm.totalCapacity}
                      onChange={(e) => setVenueForm({ ...venueForm, totalCapacity: e.target.value })} required />
                  </div>
                  <div className="space-y-1.5 sm:col-span-2">
                    <Label className="text-xs">Google Maps URL (optional)</Label>
                    <Input placeholder="https://maps.google.com/..." value={venueForm.googleMapsURL}
                      onChange={(e) => setVenueForm({ ...venueForm, googleMapsURL: e.target.value })} />
                  </div>
                </div>
                <div className="flex gap-2">
                  <Button type="submit" size="sm" disabled={saving}>{saving ? 'Creating...' : 'Create Venue'}</Button>
                  <Button type="button" size="sm" variant="outline" onClick={() => setShowVenueForm(false)}>Cancel</Button>
                </div>
              </form>
            </div>
          )}

          {loading ? (
            <p className="text-sm text-muted-foreground">Loading...</p>
          ) : (
            <div className="space-y-3">
              {venues.map((venue) => (
                <div key={venue.id} className="border rounded-xl bg-card overflow-hidden">
                  <div className="p-4 flex items-start justify-between gap-4">
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <h3 className="font-medium text-sm">{venue.name}</h3>
                        <Badge variant={venue.active ? 'default' : 'secondary'} className="text-xs">
                          {venue.active ? 'Active' : 'Inactive'}
                        </Badge>
                      </div>
                      <p className="text-xs text-muted-foreground">{venue.city} · {venue.totalCapacity?.toLocaleString()} capacity</p>
                      <p className="text-xs text-muted-foreground mt-0.5">{venue.address}</p>
                    </div>
                    <div className="flex gap-2 shrink-0">
                      <Button variant="outline" size="sm"
                        onClick={() => handleExpandVenue(venue.id)}
                        className="flex items-center gap-1 text-xs">
                        Sections
                        {expandedVenue === venue.id ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
                      </Button>
                      {venue.active && (
                        <Button variant="outline" size="sm"
                          className="text-destructive border-destructive/30 hover:bg-destructive/10 text-xs"
                          onClick={() => handleDeactivateVenue(venue.id)}>
                          Deactivate
                        </Button>
                      )}
                    </div>
                  </div>

                  {expandedVenue === venue.id && (
                    <div className="border-t bg-muted/30 p-4">
                      <div className="flex justify-between items-center mb-3">
                        <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Sections</span>
                        <Button size="sm" variant="outline" className="text-xs h-7 gap-1"
                          onClick={() => { setShowSectionForm(venue.id); setSectionForm({ name: '', sectionType: 'GA', totalCapacity: '' }); }}>
                          <Plus className="h-3 w-3" /> Add Section
                        </Button>
                      </div>

                      {(venueSections[venue.id] || []).length === 0 ? (
                        <p className="text-xs text-muted-foreground italic">No sections yet — add at least one GA or Seating section.</p>
                      ) : (
                        <div className="space-y-2 mb-3">
                          {(venueSections[venue.id] || []).map((section) => (
                            <div key={section.id} className="flex items-center justify-between bg-card rounded-lg px-3 py-2 border">
                              <div>
                                <span className="text-sm font-medium">{section.name}</span>
                                <span className="ml-2 text-xs text-muted-foreground">
                                  {section.sectionType} · {section.totalCapacity?.toLocaleString()} capacity
                                </span>
                              </div>
                              <button onClick={() => handleDeactivateSection(section.id, venue.id)}
                                className="text-muted-foreground hover:text-destructive transition-colors">
                                <X className="h-3.5 w-3.5" />
                              </button>
                            </div>
                          ))}
                        </div>
                      )}

                      {showSectionForm === venue.id && (
                        <div className="border rounded-lg p-3 bg-card mt-2">
                          <p className="text-xs font-medium mb-2">New Section</p>
                          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                            <div className="space-y-1">
                              <Label className="text-xs">Section Name</Label>
                              <Input placeholder="e.g. Floor GA" value={sectionForm.name}
                                onChange={(e) => setSectionForm({ ...sectionForm, name: e.target.value })} />
                            </div>
                            <div className="space-y-1">
                              <Label className="text-xs">Type</Label>
                              <select className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring"
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
                            <Button size="sm" variant="outline" className="text-xs h-7"
                              onClick={() => setShowSectionForm(null)}>Cancel</Button>
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Concerts tab */}
      {tab === 'concerts' && (
    <div>
        <h2 className="font-semibold mb-4">
            {user?.role === 'ADMIN' ? 'All Concerts' : 'My Concerts'}
        </h2>
        {concertsLoading ? (
            <p className="text-sm text-muted-foreground">Loading...</p>
        ) : concerts.length === 0 ? (
            <div className="text-center py-12 text-muted-foreground">
                <Music className="h-10 w-10 mx-auto mb-3 opacity-50" />
                <p>No concerts found</p>
            </div>
        ) : (
            <div className="space-y-2">
                {concerts.map((c) => (
                    <div key={c.id} className="border rounded-xl bg-card p-4">
                        <div className="flex items-start justify-between gap-4">
                            <div>
                                <div className="flex items-center gap-2 flex-wrap">
                                    <h3 className="font-medium text-sm">{c.title}</h3>
                                    <Badge variant="outline" className="text-xs">{c.status}</Badge>
                                    {c.featured && <Badge className="text-xs">Featured</Badge>}
                                </div>
                                <p className="text-xs text-muted-foreground mt-0.5">
                                    {c.artistName} · {c.venueName}, {c.venueCity}
                                </p>
                                <p className="text-xs text-muted-foreground">
                                    {new Date(c.concertDate).toLocaleString()}
                                </p>
                                {user?.role === 'ADMIN' && (
                                    <p className="text-xs text-muted-foreground mt-0.5">
                                        Organizer: {c.organizerName}
                                    </p>
                                )}
                            </div>
                            <div className="flex flex-col gap-2 shrink-0 items-end">
                                {['PUBLISHED', 'POSTPONED'].includes(c.status) && (
                                    <>
                                        <Button variant="outline" size="sm" className="text-xs"
                                            onClick={() => { setPostponeModal(c.id); setNewDate(''); }}>
                                            Postpone
                                        </Button>
                                        <Button variant="outline" size="sm"
                                            className="text-destructive border-destructive/30 hover:bg-destructive/10 text-xs"
                                            onClick={() => handleCancelConcert(c.id)}>
                                            Cancel
                                        </Button>
                                    </>
                                )}
                                {user?.role === 'ADMIN' && (
                                    <Button variant="outline" size="sm" className="text-xs"
                                        onClick={() => handleFeatureToggle(c.id, c.featured)}>
                                        {c.featured ? 'Unfeature' : 'Feature'}
                                    </Button>
                                )}
                            </div>
                        </div>

                        {/* Postpone inline form */}
                        {postponeModal === c.id && (
                            <div className="mt-3 pt-3 border-t flex items-center gap-2">
                                <Input type="datetime-local" value={newDate}
                                    onChange={(e) => setNewDate(e.target.value)}
                                    className="w-auto text-sm" />
                                <Button size="sm" className="text-xs"
                                    onClick={() => handlePostponeConcert(c.id)}>Confirm</Button>
                                <Button size="sm" variant="outline" className="text-xs"
                                    onClick={() => setPostponeModal(null)}>Cancel</Button>
                            </div>
                        )}
                    </div>
                ))}
            </div>
        )}

        {concertTotalPages > 1 && (
            <div className="flex justify-center gap-2 mt-4">
                <Button size="sm" variant="outline" disabled={concertPage === 0}
                    onClick={() => fetchConcerts(concertPage - 1)}>Previous</Button>
                <span className="text-sm self-center">Page {concertPage + 1} of {concertTotalPages}</span>
                <Button size="sm" variant="outline" disabled={concertPage + 1 >= concertTotalPages}
                    onClick={() => fetchConcerts(concertPage + 1)}>Next</Button>
            </div>
        )}
    </div>
)}

      {/* Users tab */}
      {tab === 'users' && (
        <div>
          <h2 className="font-semibold mb-4">Users</h2>
          {usersLoading ? (
            <p className="text-sm text-muted-foreground">Loading...</p>
          ) : (
            <div className="space-y-2">
              {users.map((u) => (
                <div key={u.id} className="flex items-center justify-between border rounded-xl bg-card p-4">
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-medium text-sm">{u.name}</h3>
                      <Badge variant={u.active ? 'default' : 'destructive'} className="text-xs">
                        {u.active ? 'Active' : 'Banned'}
                      </Badge>
                      <Badge variant="outline" className="text-xs">{u.role}</Badge>
                    </div>
                    <p className="text-xs text-muted-foreground mt-0.5">{u.email}</p>
                  </div>
                  <div className="flex gap-2 items-center">
                    {u.role !== 'ADMIN' && (
                      <select
                        className="text-xs border rounded-md px-2 py-1 bg-background"
                        value={u.role}
                        onChange={(e) => handleRoleChange(u.id, e.target.value)}
                      >
                        {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                      </select>
                    )}
                    {u.role !== 'ADMIN' && (
                      <Button
                        variant="outline" size="sm"
                        className={`text-xs ${u.active ? 'text-destructive border-destructive/30 hover:bg-destructive/10' : ''}`}
                        onClick={() => handleBanToggle(u.id, u.active)}
                      >
                        {u.active ? <><Ban className="h-3 w-3 mr-1" />Ban</> : <><ShieldCheck className="h-3 w-3 mr-1" />Unban</>}
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}

          {userTotalPages > 1 && (
            <div className="flex justify-center gap-2 mt-4">
              <Button size="sm" variant="outline" disabled={userPage === 0}
                onClick={() => fetchUsers(userPage - 1)}>Previous</Button>
              <span className="text-sm self-center">Page {userPage + 1} of {userTotalPages}</span>
              <Button size="sm" variant="outline" disabled={userPage + 1 >= userTotalPages}
                onClick={() => fetchUsers(userPage + 1)}>Next</Button>
            </div>
          )}
        </div>
      )}
      {tab === 'categories' && (
  <div>
    <h2 className="font-semibold mb-4">Categories</h2>

    <div className="flex gap-2 mb-4">
      <Input
        placeholder="New category"
        value={categoryName}
        onChange={(e) => setCategoryName(e.target.value)}
      />

      <Button onClick={handleCreateCategory}>
        Add Category
      </Button>
    </div>

    {categoriesLoading ? (
      <p>Loading...</p>
    ) : (
      <div className="space-y-2">
        {categories.map((cat) => (
          <div
            key={cat.id}
            className="flex items-center justify-between border rounded-xl bg-card p-4"
          >
            <div>
              <h3 className="font-medium text-sm">
                {cat.name}
              </h3>
            </div>

            <Button
              variant="outline"
              size="sm"
              className="text-destructive"
              onClick={() => handleDeactivateCategory(cat.id)}
            >
              Deactivate
            </Button>
          </div>
        ))}
      </div>
    )}
  </div>
)}
    </div>
  );
}