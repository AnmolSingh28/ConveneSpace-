import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Music, Loader2 } from 'lucide-react'; 
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Separator } from '../components/ui/separator';
import api from '../lib/axios';
import useAuthStore from '../store/authStore';
import toast from 'react-hot-toast';
import PageLoader from '../components/PageLoader'; 
import { FcGoogle } from "react-icons/fc";

export default function LoginPage() {
  const [form, setForm] = useState({ email: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const { setAuth } = useAuthStore();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true); // START FULL SCREEN SPINNER
    try {
      const res = await api.post('/api/v1/auth/login', form);
      setAuth(res.data.data);
      toast.success(`Welcome back, ${res.data.data.name}!`);
      navigate('/');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Login failed');
      setLoading(false); // STOP ONLY ON ERROR (on success, navigate will take over)
    }
  };

  const handleGoogleLogin = () => {
    window.location.href = 'https://convenespace.space/oauth2/authorization/google';
  };

  return (
    <div className="relative min-h-[calc(100vh-4rem)] flex items-center justify-center px-4">
      
      {/* FULL SCREEN OVERLAY SPINNER */}
      {loading && (
        <div className="fixed inset-0 bg-background/80 backdrop-blur-sm z-50 flex items-center justify-center">
          <PageLoader message="Getting things ready for you..." />
        </div>
      )}

      <div className={`w-full max-w-md transition-all duration-300 ${loading ? 'blur-sm scale-95 opacity-50' : ''}`}>
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-12 h-12 bg-primary rounded-xl mb-4 shadow-lg shadow-primary/20">
            <Music className="h-6 w-6 text-white" />
          </div>
          <h1 className="text-2xl font-bold">Welcome back</h1>
          <p className="text-muted-foreground mt-1">Sign in to your ConveneSpace account</p>
        </div>

        <div className="bg-card border rounded-2xl p-8 shadow-xl">
          <Button
            type="button"
            variant="outline"
            className="w-full gap-3 mb-6 h-12"
            onClick={handleGoogleLogin}
            disabled={loading}
          >
          
            <FcGoogle className="h-5 w-5" />
                               Continue with Google
           
          </Button>

          <div className="relative mb-6">
            <Separator />
            <span className="absolute left-1/2 -translate-x-1/2 -translate-y-1/2 bg-card px-2 text-xs text-muted-foreground">or</span>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="email">Email Address</Label>
              <Input id="email" type="email" placeholder="you@example.com" value={form.email} 
                onChange={(e) => setForm({ ...form, email: e.target.value })} required />
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <div className="relative">
                <Input id="password" type={showPassword ? 'text' : 'password'} value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })} required />
                <button type="button" className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                  onClick={() => setShowPassword(!showPassword)}>
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>

            <Button type="submit" className="w-full h-12 text-base font-semibold" disabled={loading}>
              Sign In
            </Button>
          </form>

          <div className="mt-6 text-center text-sm text-muted-foreground">
            Don't have an account?{' '}
            <Link to="/register" className="text-primary font-bold hover:underline">Sign up</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
