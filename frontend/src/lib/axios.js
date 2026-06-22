import axios from 'axios';

const api = axios.create({
 baseURL: 'https://convenespace.space',
  headers: {
    'Content-Type': 'application/json',
  },
});

//Attach JWT token to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

//URLs where refresh logic should NOT run
const excludedUrls = [
  '/api/v1/auth/login',
  '/api/v1/auth/register',
  '/api/v1/auth/verify-email',
  '/api/v1/auth/resend-otp',
  '/api/v1/auth/refresh-token',
];

//Auto refresh on 401
api.interceptors.response.use((response) => response,

  async (error) => {
    const original = error.config;

    //Skip refresh logic for auth endpoints
    if (
      excludedUrls.some((url) => original?.url?.includes(url)
      )
    ) {
      return Promise.reject(error);
    }

    //Retry only once
    if (
      error.response?.status === 401 &&
      !original._retry
    ) {

      original._retry = true;

      try {

        const refreshToken = localStorage.getItem('refreshToken');

        //No refresh token available
        if (!refreshToken) {
          throw new Error('No refresh token');
        }

        const res = await axios.post(
         'https://convenespace.space/api/v1/auth/refresh-token',
          { refreshToken }
        );

        const newToken = res.data.data.accessToken;

        localStorage.setItem('accessToken',newToken);

        original.headers.Authorization = `Bearer ${newToken}`;
      return api(original);

      } catch (err) {
        localStorage.clear();
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
