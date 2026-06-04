import { useSelector } from 'react-redux';
import { selectCurrentUser, selectIsAuthenticated, selectHasRole } from '../store/slices/authSlice';

export function useAuth() {
  const user = useSelector(selectCurrentUser);
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const isAdmin = useSelector(selectHasRole('ADMIN'));
  const isApprover = useSelector(selectHasRole('APPROVER'));
  const isAccountManager = useSelector(selectHasRole('ACCOUNT_MANAGER'));
  const isLeadership = useSelector(selectHasRole('LEADERSHIP'));

  const hasRole = (role) => user?.roles?.includes(role) ?? false;

  return { user, isAuthenticated, isAdmin, isApprover, isAccountManager, isLeadership, hasRole };
}
