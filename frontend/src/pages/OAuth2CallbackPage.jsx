import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import useAuthStore from '../store/authStore';
import toast from 'react-hot-toast';
import PageLoader from '../components/PageLoader';
import api from '../lib/axios';
export default function OAuth2CallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();

useEffect(() => {
  const code = searchParams.get('code');

  if (!code) {
    toast.error('Google login failed. Please try again.');
    navigate('/login');
    return;
  }

  api.post('/api/v1/auth/oauth2/exchange', { code })
    .then((res) => {
      setAuth(res.data.data);
      toast.success(`Welcome, ${res.data.data.name}!`);
      navigate('/');
    })
    .catch(() => {
      toast.error('Google login failed. Please try again.');
      navigate('/login');
    });
}, []);

  return <PageLoader />;
}