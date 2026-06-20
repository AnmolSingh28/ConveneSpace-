import { Link, useNavigate } from 'react-router-dom';
import { Music, User, LogOut, Menu, X, Sun, Moon } from 'lucide-react';
import { useState, useEffect } from 'react';
import useAuthStore from '../store/authStore';
import { Button } from './ui/button';
import toast from 'react-hot-toast';
import api from '../lib/axios';
import PageLoader from './PageLoader';

import useBookingStore from '../store/bookingStore';

export default function Navbar() {
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const { user, accessToken, logout } = useAuthStore();
  const { activeLock, clearActiveLock, clearBooking } = useBookingStore();
  const [timeLeft, setTimeLeft] = useState(0);

 
  const [theme, setTheme] = useState(() =>
    typeof window !== 'undefined' && localStorage.getItem('theme') === 'dark' ? 'dark' : 'light'
  );
  useEffect(() => {
    const root = document.documentElement;
    if (theme === 'dark') root.classList.add('dark');
    else root.classList.remove('dark');
    localStorage.setItem('theme', theme);
  }, [theme]);
  const toggleTheme = () => setTheme((t) => (t === 'dark' ? 'light' : 'dark'));

  useEffect(() => {
    if (!activeLock) return;
    const interval = setInterval(() => {
      const remaining = Math.max(0, Math.floor((activeLock.expiresAt - Date.now()) / 1000));
      setTimeLeft(remaining);
      if (remaining <= 0) {
        clearActiveLock();
        clearInterval(interval);
      }
    }, 1000);
    return () => clearInterval(interval);
  }, [activeLock]);

  const handleLogout = () => {
    setIsLoggingOut(true);
    api.post('/api/v1/auth/logout').catch(() => {
      console.log("Backend failed to logout, but Slave is leaving anyway.");
    });
    setTimeout(() => {
      clearBooking();
      logout();
      setIsLoggingOut(false);
      toast.success('Logged out successfully');
      navigate('/login');
    }, 800);
  };

  return (
    <>
      {/* SCREEN TYPE SPINNER OVERLAY */}
      {isLoggingOut && (
        <div className="fixed inset-0 bg-background/80 backdrop-blur-sm z-[100] flex items-center justify-center">
          <PageLoader message="Ending your session securely..." />
        </div>
      )}

      <nav className="sticky top-0 z-50 border-b bg-background/80 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        {activeLock && timeLeft > 0 && (
          <div
            onClick={() => navigate(`/book/${activeLock.tierId}`)}
            className="bg-amber-500 text-white text-xs font-semibold py-2 px-4 text-center cursor-pointer hover:bg-amber-600 transition-colors"
          >
            ⏱ Seats reserved — {Math.floor(timeLeft / 60)}:{String(timeLeft % 60).padStart(2, '0')} remaining. Tap to continue booking →
          </div>
        )}
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">

            {/* Logo */}
            <Link to="/" className="flex items-center gap-2.5">
              <div className="bg-primary rounded-xl p-1.5 shadow-sm">
                <Music className="h-5 w-5 text-primary-foreground" />
              </div>
              <span className="font-semibold text-xl tracking-tight text-foreground">
                ConveneSpace
              </span>
            </Link>

            {/* Desktop Nav */}
            <div className="hidden md:flex items-center gap-7">
              <Link to="/" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
                Events
              </Link>

              {accessToken && (
                <Link to="/my-bookings" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
                  My Bookings
                </Link>
              )}

              {accessToken && (user?.role === 'ORGANIZER' || user?.role === 'ADMIN') && (
                <Link to="/organizer" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
                  Dashboard
                </Link>
              )}

              {accessToken && user?.role === 'ADMIN' && (
                <Link to="/admin" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
                  Admin
                </Link>
              )}
            </div>

            {/* Desktop Actions */}
            <div className="hidden md:flex items-center gap-2">
              <Button variant="ghost" size="icon" onClick={toggleTheme} aria-label="Toggle theme" className="rounded-full">
                {theme === 'dark' ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
              </Button>
              {accessToken ? (
                <>
                  <Link to="/profile">
                    <Button variant="ghost" size="sm" className="gap-2">
                      <User className="h-4 w-4" />
                      {user?.name?.split(' ')[0] || 'User'}
                    </Button>
                  </Link>

                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={handleLogout}
                    className="gap-2 text-destructive hover:text-destructive hover:bg-destructive/10"
                  >
                    <LogOut className="h-4 w-4" />
                    Logout
                  </Button>
                </>
              ) : (
                <div className="flex items-center gap-2">
                  <Link to="/login">
                    <Button variant="ghost" size="sm">Login</Button>
                  </Link>
                  <Link to="/register">
                    <Button size="sm">Get Started</Button>
                  </Link>
                </div>
              )}
            </div>

            {/* Mobile Menu Button */}
            <div className="md:hidden flex items-center gap-1">
              <Button variant="ghost" size="icon" onClick={toggleTheme} aria-label="Toggle theme" className="rounded-full">
                {theme === 'dark' ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
              </Button>
              <button className="p-2 text-foreground" onClick={() => setMenuOpen(!menuOpen)}>
                {menuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
              </button>
            </div>
          </div>

          {/* Mobile Menu Content */}
          {menuOpen && (
            <div className="md:hidden border-t py-4 space-y-3 bg-background">
              <Link to="/" className="block text-sm font-medium px-2 py-1" onClick={() => setMenuOpen(false)}>
                Events
              </Link>
              {accessToken && (
                <Link to="/my-bookings" className="block text-sm font-medium px-2 py-1" onClick={() => setMenuOpen(false)}>
                  My Bookings
                </Link>
              )}
              {accessToken ? (
                <>
                  <Link to="/profile" className="block text-sm font-medium px-2 py-1" onClick={() => setMenuOpen(false)}>
                    Profile
                  </Link>
                  <button
                    onClick={() => { handleLogout(); setMenuOpen(false); }}
                    className="block w-full text-left text-sm font-medium px-2 py-1 text-destructive"
                  >
                    Logout
                  </button>
                </>
              ) : (
                <div className="flex flex-col gap-2 px-2 pt-2">
                  <Link to="/login" onClick={() => setMenuOpen(false)} className="w-full">
                    <Button variant="outline" className="w-full">Login</Button>
                  </Link>
                  <Link to="/register" onClick={() => setMenuOpen(false)} className="w-full">
                    <Button className="w-full">Register</Button>
                  </Link>
                </div>
              )}
            </div>
          )}
        </div>
      </nav>
    </>
  );
}