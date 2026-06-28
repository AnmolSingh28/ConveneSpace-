import { useState, useEffect } from 'react';
import { X, Loader2, AlertTriangle, CalendarClock, Ban } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import api from '../lib/axios';
import toast from 'react-hot-toast';

function toLocal(raw) {
    if (!raw) return '';
    try {
        if (typeof raw === 'string' && raw.split('-')[0].length === 2) {
            const [d, t] = raw.split(' ');
            const [dd, mm, yyyy] = d.split('-');
            return `${yyyy}-${mm}-${dd}T${t?.slice(0, 5) || '00:00'}`;
        }
        const date = new Date(raw);
        return isNaN(date) ? '' : date.toISOString().slice(0, 16);
    } catch { return ''; }
}

const toISO = (s) => s ? new Date(s).toISOString() : null;
const F = ({ label, children }) => (
    <div className="space-y-1.5"><Label className="text-xs">{label}</Label>{children}</div>
);

export default function EditConcertModal({ concert, onClose, onSaved }) {
    const [venues, setVenues] = useState([]);
    const [categories, setCategories] = useState([]);
    const [saving, setSaving] = useState(false);
    const [activeStatus, setActiveStatus] = useState(null);
    const [form, setForm] = useState({
        title: concert.title || '',
        artistName: concert.artistName || '',
        description: concert.description || '',
        venueId: concert.venue?.id || '',
        categoryId: concert.categoryId || '',
        concertDate: toLocal(concert.concertDate),
        doorsOpenTime: toLocal(concert.doorsOpenTime),
        saleStartTime: toLocal(concert.saleStartTime),
        saleEndTime: toLocal(concert.saleEndTime),
        bannerImageUrl: concert.bannerImageUrl || '',
        requiresPreRegistration: concert.requiresPreRegistration || false,
    });
    const [postponeDate, setPostponeDate] = useState('');
    const [cancelReason, setCancelReason] = useState('');

    useEffect(() => {
        document.body.style.overflow = 'hidden';
        api.get('/api/v1/venues').then(r => setVenues(r.data.data || [])).catch(() => {});
        api.get('/api/v1/categories').then(r => setCategories(r.data.data || [])).catch(() => {});
        return () => { document.body.style.overflow = ''; };
    }, []);

    const set = (key) => (e) => setForm(f => ({ ...f, [key]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }));

    const handleSave = async () => {
        setSaving(true);
        try {
            await api.put(`/api/v1/concerts/${concert.id}`, {
                ...form,
                concertDate: toISO(form.concertDate),
                doorsOpenTime: toISO(form.doorsOpenTime),
                saleStartTime: toISO(form.saleStartTime),
                saleEndTime: toISO(form.saleEndTime),
            });
            toast.success('Event updated!');
            onSaved?.(); onClose();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to update');
        } finally { setSaving(false); }
    };

    const handlePostpone = async () => {
        if (!postponeDate) return toast.error('Select a new date');
        setSaving(true);
        try {
            await api.patch(`/api/v1/concerts/${concert.id}/postpone`, null, {
                params: { newDate: toISO(postponeDate) }
            });
            toast.success('Event postponed'); onSaved?.(); onClose();
        } catch (err) { toast.error(err.response?.data?.message || 'Failed'); }
        finally { setSaving(false); }
    };

    const handleCancel = async () => {
        if (!cancelReason.trim()) return toast.error('Provide a reason');
        setSaving(true);
        try {
            await api.patch(`/api/v1/concerts/${concert.id}/cancel`, null, {
                params: { reason: cancelReason }
            });
            toast.success('Event cancelled'); onSaved?.(); onClose();
        } catch (err) { toast.error(err.response?.data?.message || 'Failed'); }
        finally { setSaving(false); }
    };

    const isCancelled = concert.status === 'CANCELLED';

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} />
            <div className="relative bg-background border rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] flex flex-col">

                {/* Header */}
                <div className="flex items-center justify-between px-6 py-4 border-b shrink-0">
                    <div>
                        <h2 className="font-bold text-lg">Edit Event</h2>
                        <p className="text-xs text-muted-foreground truncate max-w-sm">{concert.title}</p>
                    </div>
                    <button onClick={onClose} className="p-2 hover:bg-muted rounded-lg"><X className="h-4 w-4" /></button>
                </div>

                {/* Body */}
                <div className="overflow-y-auto flex-1 px-6 py-5 space-y-6">
                    {isCancelled && (
                        <div className="flex items-center gap-3 bg-red-50 border border-red-200 rounded-xl p-4">
                            <Ban className="h-5 w-5 text-red-500 shrink-0" />
                            <p className="text-sm text-red-700 font-medium">This event is cancelled and cannot be edited.</p>
                        </div>
                    )}

                    {/* Basic Info */}
                    <section className="space-y-4">
                        <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Basic Info</p>
                        <F label="Event Title"><Input value={form.title} onChange={set('title')} disabled={isCancelled} /></F>
                        <F label="Artist / Performer"><Input value={form.artistName} onChange={set('artistName')} disabled={isCancelled} /></F>
                        <F label="Category">
                            <select className="w-full px-3 py-2 rounded-md border bg-background text-sm" value={form.categoryId} onChange={set('categoryId')} disabled={isCancelled}>
                                <option value="">Select category</option>
                                {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                            </select>
                        </F>
                        <F label="Description">
                            <textarea className="w-full min-h-20 px-3 py-2 rounded-md border bg-background text-sm resize-none" value={form.description} onChange={set('description')} disabled={isCancelled} />
                        </F>
                        <F label="Banner Image URL"><Input value={form.bannerImageUrl} onChange={set('bannerImageUrl')} disabled={isCancelled} placeholder="https://..." /></F>
                        {form.bannerImageUrl && <img src={form.bannerImageUrl} alt="" className="h-24 w-full object-cover rounded-lg border" onError={e => e.target.style.display='none'} />}
                    </section>

                    {/* Venue & Schedule */}
                    <section className="space-y-4">
                        <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Venue & Schedule</p>
                        <F label="Venue">
                            <select className="w-full px-3 py-2 rounded-md border bg-background text-sm" value={form.venueId} onChange={set('venueId')} disabled={isCancelled}>
                                <option value="">Select venue</option>
                                {venues.map(v => <option key={v.id} value={v.id}>{v.name}, {v.city}</option>)}
                            </select>
                        </F>
                        <div className="grid grid-cols-2 gap-4">
                            <F label="Event Date"><Input type="datetime-local" value={form.concertDate} onChange={set('concertDate')} disabled={isCancelled} /></F>
                            <F label="Doors Open"><Input type="datetime-local" value={form.doorsOpenTime} onChange={set('doorsOpenTime')} disabled={isCancelled} /></F>
                            <F label="Sale Start"><Input type="datetime-local" value={form.saleStartTime} onChange={set('saleStartTime')} disabled={isCancelled} /></F>
                            <F label="Sale End"><Input type="datetime-local" value={form.saleEndTime} onChange={set('saleEndTime')} disabled={isCancelled} /></F>
                        </div>
                        <label className="flex items-center gap-2 text-sm cursor-pointer">
                            <input type="checkbox" checked={form.requiresPreRegistration} onChange={set('requiresPreRegistration')} disabled={isCancelled} className="rounded" />
                            Requires pre-registration
                        </label>
                    </section>

                    {/* Status Actions */}
                    {!isCancelled && (
                        <section className="space-y-3">
                            <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Event Status</p>

                            {/* Postpone */}
                            <div className="border rounded-xl overflow-hidden">
                                <button type="button" onClick={() => setActiveStatus(s => s === 'postpone' ? null : 'postpone')}
                                        className="w-full flex items-center gap-3 p-4 text-left hover:bg-muted/40">
                                    <div className="w-8 h-8 rounded-lg bg-amber-100 text-amber-600 flex items-center justify-center shrink-0"><CalendarClock className="h-4 w-4" /></div>
                                    <div><p className="text-sm font-semibold">Postpone Event</p><p className="text-xs text-muted-foreground">Set a new date. Tickets stay valid.</p></div>
                                </button>
                                {activeStatus === 'postpone' && (
                                    <div className="px-4 pb-4 pt-3 border-t bg-muted/20 space-y-3">
                                        <F label="New Date"><Input type="datetime-local" value={postponeDate} onChange={e => setPostponeDate(e.target.value)} /></F>
                                        <Button size="sm" className="bg-amber-500 hover:bg-amber-600 text-white gap-2" disabled={saving} onClick={handlePostpone}>
                                            {saving ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <CalendarClock className="h-3.5 w-3.5" />} Confirm Postpone
                                        </Button>
                                    </div>
                                )}
                            </div>

                            {/* Cancel */}
                            <div className="border rounded-xl overflow-hidden">
                                <button type="button" onClick={() => setActiveStatus(s => s === 'cancel' ? null : 'cancel')}
                                        className="w-full flex items-center gap-3 p-4 text-left hover:bg-muted/40">
                                    <div className="w-8 h-8 rounded-lg bg-red-100 text-red-600 flex items-center justify-center shrink-0"><Ban className="h-4 w-4" /></div>
                                    <div><p className="text-sm font-semibold">Cancel Event</p><p className="text-xs text-muted-foreground">Permanently cancel. Cannot be undone.</p></div>
                                </button>
                                {activeStatus === 'cancel' && (
                                    <div className="px-4 pb-4 pt-3 border-t bg-muted/20 space-y-3">
                                        <div className="flex gap-2 bg-red-50 border border-red-100 rounded-lg p-3">
                                            <AlertTriangle className="h-4 w-4 text-red-500 shrink-0 mt-0.5" />
                                            <p className="text-xs text-red-700">Cancelling is permanent. Notify attendees about refunds separately.</p>
                                        </div>
                                        <F label="Reason"><Input placeholder="e.g. Artist unavailable" value={cancelReason} onChange={e => setCancelReason(e.target.value)} /></F>
                                        <Button size="sm" variant="destructive" className="gap-2" disabled={saving} onClick={handleCancel}>
                                            {saving ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Ban className="h-3.5 w-3.5" />} Confirm Cancel
                                        </Button>
                                    </div>
                                )}
                            </div>
                        </section>
                    )}
                </div>

                {/* Footer */}
                <div className="px-6 py-4 border-t shrink-0 flex justify-end gap-2">
                    <Button variant="outline" onClick={onClose} disabled={saving}>Discard</Button>
                    {!isCancelled && (
                        <Button onClick={handleSave} disabled={saving} className="min-w-28">
                            {saving ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                            {saving ? 'Saving...' : 'Save Changes'}
                        </Button>
                    )}
                </div>
            </div>
        </div>
    );
}