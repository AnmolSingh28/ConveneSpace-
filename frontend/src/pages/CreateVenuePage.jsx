import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import api from "../lib/axios";
import toast from "react-hot-toast";
export default function CreateVenuePage() {
    const navigate = useNavigate();
    const [saving, setSaving] = useState(false);
    const [venueForm, setVenueForm] = useState({
        name: "",
        city: "",
        address: "",
        venueType: "ESTABLISHED",
        totalCapacity: "",
        googleMapsURL: "",
        locationDescription: "",
    });
    const handleCreateVenue = async (e) => {
        e.preventDefault();
        setSaving(true);
        try {
            await api.post("/api/v1/venues", {
                ...venueForm,
                totalCapacity: parseInt(venueForm.totalCapacity),
            });
            toast.success("Venue created successfully");
            navigate("/organizer/create");
        } catch (err) {
            toast.error(
                err.response?.data?.message || "Failed to create venue"
            );
        } finally {
            setSaving(false);
        }
    };
    return (
        <div className="max-w-2xl mx-auto px-4 sm:px-6 py-8">
            <button
                onClick={() => navigate("/organizer/create")}
                className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-6"
            >
                <ArrowLeft className="h-4 w-4" />
                Back to Create Event
            </button>
            <h1 className="text-2xl font-bold mb-6">
                Create Venue
            </h1>

            <form
                onSubmit={handleCreateVenue}
                className="bg-card border rounded-xl p-6 space-y-4"
            > <div className="space-y-2">
                    <Label>Venue Name</Label>
                    <Input
                        placeholder="e.g. Jio World Centre"
                        value={venueForm.name}
                        onChange={(e) =>
                            setVenueForm({
                                ...venueForm,
                                name: e.target.value,
                            })
                        }
                        required
                    />
                </div>
                <div className="space-y-2">
                    <Label>City</Label>
                    <Input
                        placeholder="e.g. Mumbai"
                        value={venueForm.city}
                        onChange={(e) =>
                            setVenueForm({
                                ...venueForm,
                                city: e.target.value,
                            })
                        }
                        required
                    />
                </div>
                <div className="space-y-2">
                    <Label>Address</Label>
                    <Input
                        placeholder="Full Address"
                        value={venueForm.address}
                        onChange={(e) =>
                            setVenueForm({
                                ...venueForm,
                                address: e.target.value,
                            })
                        }
                        required
                    />
                </div>
                <div className="space-y-2">
                    <Label>Venue Type</Label>
                    <select
                        className="w-full px-3 py-2 rounded-md border bg-background text-sm"
                        value={venueForm.venueType}
                        onChange={(e) =>
                            setVenueForm({
                                ...venueForm,
                                venueType: e.target.value,
                            })
                        }
                    >
                        <option value="ESTABLISHED">Established</option>
                        <option value="TEMPORARY">Temporary</option>
                    </select>
                </div>
                <div className="space-y-2">
                    <Label>Total Capacity</Label>
                    <Input
                        type="number"
                        placeholder="e.g. 10000"
                        value={venueForm.totalCapacity}
                        onChange={(e) =>
                            setVenueForm({
                                ...venueForm,
                                totalCapacity: e.target.value,
                            })
                        }
                        required
                    />
                </div>
                <div className="space-y-2">
                    <Label>Google Maps URL (Optional)</Label>
                    <Input
                        placeholder="https://maps.google.com/..."
                        value={venueForm.googleMapsURL}
                        onChange={(e) =>
                            setVenueForm({
                                ...venueForm,
                                googleMapsURL: e.target.value,
                            })
                        }
                    />
                </div>
                <div className="space-y-2">
                    <Label>Location Description (Optional)</Label>
                    <textarea
                        className="w-full min-h-24 px-3 py-2 rounded-md border bg-background text-sm resize-none"
                        placeholder="Nearby landmarks or directions..."
                        value={venueForm.locationDescription}
                        onChange={(e) =>
                            setVenueForm({
                                ...venueForm,
                                locationDescription: e.target.value,
                            })
                        }
                    />
                </div>
                <Button
                    type="submit"
                    className="w-full"
                    disabled={saving}
                >
                    {saving ? "Creating..." : "Create Venue"}
                </Button>
            </form>
        </div>
    );
}