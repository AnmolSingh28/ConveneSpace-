import { create } from 'zustand';
import { persist } from 'zustand/middleware';

const useBookingStore = create(
  persist(
    (set) => ({
      selectedConcert: null,
      selectedTier: null,
      lockedQuantity: 0,
      lockExpiresAt: null,
      selectedSeatIds: [],
      lockedStep: 'select',
      lockTimerEnd: null,
      activeLock: null,

     setSelectedSeatIds: (ids) => set({ selectedSeatIds: Array.isArray(ids) ? ids : [] }),
      setLockedStep: (step) => set({ lockedStep: step }),
      setLockTimerEnd: (ts) => set({ lockTimerEnd: ts }),
      setSelectedConcert: (concert) => set({ selectedConcert: concert }),
      setSelectedTier: (tier) => set({ selectedTier: tier }),
      setLock: (quantity, expiresAt) => set({ lockedQuantity: quantity, lockExpiresAt: expiresAt }),
      setActiveLock: (tierId, expiresAt) => set({ activeLock: { tierId, expiresAt } }),
      clearActiveLock: () => set({ activeLock: null }),
      clearBooking: () => set({
        selectedTier: null,
        lockedQuantity: 0,
        lockExpiresAt: null,
        selectedSeatIds: [],
        lockedStep: 'select',
        lockTimerEnd: null,
        activeLock: null,
      }),
    }),
    {
      name: 'booking-store', // localStorage key
      partialize: (state) => ({
        selectedConcert: state.selectedConcert,
        selectedSeatIds: state.selectedSeatIds,
        lockedStep: state.lockedStep,
        lockTimerEnd: state.lockTimerEnd,
        activeLock: state.activeLock,
      }),
    }
  )
);

export default useBookingStore;