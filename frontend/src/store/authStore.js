import { create } from 'zustand';
import { persist } from 'zustand/middleware';

const useAuthStore = create(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,

      setAuth: (data) => {
        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);
        set({
          user: {
            id: data.userId,
            name: data.name,
            email: data.email,
            role: data.role,
          },
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
        });
      },

      logout: () => {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        set({ user: null, accessToken: null, refreshToken: null });
      },

      isAuthenticated: () => {
        const state = useAuthStore.getState();
        return !!state.accessToken;
      },
    }),
    {
      name: 'auth-storage',
    }
  )
);

export default useAuthStore;