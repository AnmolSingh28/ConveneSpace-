import { Link } from 'react-router-dom';
import { Music, Globe, Twitter, Instagram } from 'lucide-react';

export default function Footer() {
  return (
    <footer className="border-t bg-white mt-16">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
          <div>
            <div className="flex items-center gap-2 mb-4">
              <div className="bg-primary rounded-lg p-1.5">
                <Music className="h-4 w-4 text-white" />
              </div>
              <span className="font-bold text-lg">ConveneSpace</span>
            </div>
            <p className="text-sm text-muted-foreground leading-relaxed">
              Book tickets to the best concerts and live events happening across India.
            </p>
          </div>

          <div>
            <h3 className="font-semibold text-sm mb-3">Discover</h3>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li><Link to="/" className="hover:text-foreground transition-colors">Upcoming Concerts</Link></li>
              <li><Link to="/search?q=featured" className="hover:text-foreground transition-colors">Featured Events</Link></li>
              <li><Link to="/search?q=EDM" className="hover:text-foreground transition-colors">EDM & Electronic</Link></li>
              <li><Link to="/search?q=Bollywood" className="hover:text-foreground transition-colors">Bollywood Nights</Link></li>
            </ul>
          </div>

          <div>
            <h3 className="font-semibold text-sm mb-3">Account</h3>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li><Link to="/login" className="hover:text-foreground transition-colors">Login</Link></li>
              <li><Link to="/register" className="hover:text-foreground transition-colors">Create Account</Link></li>
              <li><Link to="/my-bookings" className="hover:text-foreground transition-colors">My Bookings</Link></li>
              <li><Link to="/profile" className="hover:text-foreground transition-colors">Profile</Link></li>
            </ul>
          </div>

          <div>
            <h3 className="font-semibold text-sm mb-3">Support</h3>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li><a href="#" className="hover:text-foreground transition-colors">Help Center</a></li>
              <li><a href="#" className="hover:text-foreground transition-colors">Refund Policy</a></li>
              <li><a href="#" className="hover:text-foreground transition-colors">Contact Us</a></li>
              <li><a href="#" className="hover:text-foreground transition-colors">Privacy Policy</a></li>
            </ul>
          </div>
        </div>

        <div className="border-t mt-8 pt-8 flex flex-col sm:flex-row justify-between items-center gap-4">
          <p className="text-xs text-muted-foreground">
            © 2026 ConveneSpace. All rights reserved.
          </p>
          <div className="flex items-center gap-4">
            <a href="#" className="text-muted-foreground hover:text-foreground transition-colors">
              <Globe className="h-4 w-4" />
            </a>
            <a href="#" className="text-muted-foreground hover:text-foreground transition-colors">
              <Twitter className="h-4 w-4" />
            </a>
            <a href="#" className="text-muted-foreground hover:text-foreground transition-colors">
              <Instagram className="h-4 w-4" />
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}