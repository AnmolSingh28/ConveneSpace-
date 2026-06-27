import { Routes, Route, Navigate } from 'react-router-dom';
import useAuthStore from './store/authStore';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import ErrorBoundary from './components/ErrorBoundary';
import OrganizerProfilePage from './pages/OrganizerProfilePage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import OtpPage from './pages/OtpPage';
import ConcertDetailPage from './pages/ConcertDetailPage';
import BookingPage from './pages/BookingPage';
import MyBookingsPage from './pages/MyBookingsPage';
import ProfilePage from './pages/ProfilePage';
import SearchPage from './pages/SearchPage';
import NotFoundPage from './pages/NotFoundPage';
import OAuth2CallbackPage from './pages/OAuth2CallbackPage';
import OrganizerDashboard from './pages/OrganizerDashboard';
import CreateConcertPage from './pages/CreateConcertPage';
import ManageConcertPage from './pages/ManageConcertPage';
import AdminDashboard from './pages/AdminDashboard';
import VirtualQueuePage from './pages/VirtualQueuePage';
import CreateVenuePage from './pages/CreateVenuePage';
function ProtectedRoute({ children }) {
  const { accessToken } = useAuthStore();
  return accessToken ? children : <Navigate to="/login" replace />;
}

function OrganizerRoute({ children }) {
  const { user, accessToken } = useAuthStore();
  if (!accessToken) return <Navigate to="/login" replace />;
  if (user?.role !== 'ORGANIZER' && user?.role !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }
  return children;
}

function AdminRoute({ children }) {
  const { user, accessToken } = useAuthStore();
  if (!accessToken) return <Navigate to="/login" replace />;
  if (user?.role !== 'ADMIN') return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  return (
    <ErrorBoundary>
      <div className="min-h-screen bg-background flex flex-col">
        <Navbar />
        <main className="flex-1">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/verify-otp" element={<OtpPage />} />
            <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />
            <Route path="/concerts/:id" element={<ConcertDetailPage />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="/queue/:tierId" element={<VirtualQueuePage />} />
            <Route
              path="/book/:tierId"
              element={
                <ProtectedRoute>
                  <BookingPage />
                </ProtectedRoute>
              }
            />
            <Route path="/organizer/:organizerId" element={<OrganizerProfilePage />} />
            <Route
              path="/my-bookings"
              element={
                <ProtectedRoute>
                  <MyBookingsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/profile"
              element={
                <ProtectedRoute>
                  <ProfilePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/organizer"
              element={
                <OrganizerRoute>
                  <OrganizerDashboard />
                </OrganizerRoute>
              }
            />
            <Route
              path="/organizer/create"
              element={
                <OrganizerRoute>
                  <CreateConcertPage />
                </OrganizerRoute>
              }
            />
            <Route
              path="/organizer/concert/:concertId"
              element={
                <OrganizerRoute>
                  <ManageConcertPage />
                </OrganizerRoute>
              }
            />
            <Route
              path="/admin"
              element={
                <AdminRoute>
                  <AdminDashboard />
                </AdminRoute>
              }
            />
              <Route
                  path="/organizer/venue/create"
                  element={
                      <OrganizerRoute>
                          <CreateVenuePage />
                      </OrganizerRoute>
                  }
              />
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </main>
        <Footer />
      </div>
    </ErrorBoundary>
  );
}