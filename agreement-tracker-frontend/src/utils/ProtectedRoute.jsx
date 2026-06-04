import { Navigate, Outlet } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { selectIsAuthenticated } from '../store/slices/authSlice';
import { ROUTES } from '../config/routes';

export function ProtectedRoute() {
  const isAuthenticated = useSelector(selectIsAuthenticated);
  return isAuthenticated ? <Outlet /> : <Navigate to={ROUTES.LOGIN} replace />;
}

export function AdminRoute() {
  const user = useSelector((s) => s.auth.user);
  const isAdmin = user?.roles?.includes('ADMIN');
  return isAdmin ? <Outlet /> : <Navigate to={ROUTES.DASHBOARD} replace />;
}
