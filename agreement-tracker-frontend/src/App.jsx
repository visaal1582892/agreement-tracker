import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Provider } from 'react-redux';
import { ThemeProvider } from '@mui/material/styles';
import { LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { SnackbarProvider } from 'notistack';
import store from './store';
import theme from './config/theme';
import { ROUTES } from './config/routes';
import { RIGHTS } from './config/rights';
import { ProtectedRoute, RightRoute } from './utils/ProtectedRoute';
import DashboardLayout from './layouts/DashboardLayout';
import LoginPage from './pages/auth/LoginPage';
import DashboardPage from './pages/dashboard/DashboardPage';
import AgreementListPage from './pages/agreements/AgreementListPage';
import AgreementCreatePage from './pages/agreements/AgreementCreatePage';
import AgreementDetailPage from './pages/agreements/AgreementDetailPage';
import ApprovalsPage from './pages/approvals/ApprovalsPage';
import MasterDataLayout from './pages/master/MasterDataLayout';
import CompanyMasterPage from './pages/master/CompanyMasterPage';
import VendorMasterPage from './pages/master/VendorMasterPage';
import ManufacturerMasterPage from './pages/master/ManufacturerMasterPage';
import DivisionMasterPage from './pages/master/DivisionMasterPage';
import ProductMasterPage from './pages/master/ProductMasterPage';
import IncomeTypePage from './pages/master/IncomeTypePage';
import AgreementTypePage from './pages/master/AgreementTypePage';
import RolePage from './pages/master/RolePage';
import RightPage from './pages/master/RightPage';

export default function App() {
  return (
    <Provider store={store}>
      <ThemeProvider theme={theme}>
        <LocalizationProvider dateAdapter={AdapterDayjs}>
          <SnackbarProvider
            maxSnack={4}
            anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
            autoHideDuration={4000}
          >
            <BrowserRouter>
              <Routes>
                <Route path={ROUTES.LOGIN} element={<LoginPage />} />

                <Route element={<ProtectedRoute />}>
                  <Route element={<DashboardLayout />}>
                    <Route element={<RightRoute rights={[RIGHTS.DASHBOARD_VIEW]} />}>
                      <Route index element={<DashboardPage />} />
                    </Route>

                    <Route element={<RightRoute rights={[RIGHTS.AGREEMENT_VIEW]} />}>
                      <Route path={ROUTES.AGREEMENTS} element={<AgreementListPage />} />
                      <Route path="/agreements/groups/:groupId" element={<AgreementDetailPage />} />
                    </Route>

                    <Route element={<RightRoute rights={[RIGHTS.AGREEMENT_CREATE]} />}>
                      <Route path={ROUTES.AGREEMENT_CREATE} element={<AgreementCreatePage />} />
                    </Route>

                    <Route element={<RightRoute rights={[RIGHTS.AGREEMENT_APPROVE]} />}>
                      <Route path={ROUTES.APPROVALS} element={<ApprovalsPage />} />
                    </Route>

                    <Route element={<RightRoute rights={[RIGHTS.MASTER_VIEW, RIGHTS.MASTER_MANAGE]} />}>
                      <Route path={ROUTES.MASTER} element={<MasterDataLayout />}>
                        <Route index element={<Navigate to={ROUTES.MASTER_COMPANIES} replace />} />
                        <Route path={ROUTES.MASTER_COMPANIES} element={<CompanyMasterPage />} />
                        <Route path={ROUTES.MASTER_VENDORS} element={<VendorMasterPage />} />
                        <Route path={ROUTES.MASTER_MANUFACTURERS} element={<ManufacturerMasterPage />} />
                        <Route path={ROUTES.MASTER_DIVISIONS} element={<DivisionMasterPage />} />
                        <Route path={ROUTES.MASTER_PRODUCTS} element={<ProductMasterPage />} />
                        <Route path={ROUTES.MASTER_INCOME_TYPES} element={<IncomeTypePage />} />
                        <Route path={ROUTES.MASTER_AGREEMENT_TYPES} element={<AgreementTypePage />} />
                        <Route path={ROUTES.MASTER_ROLES} element={<RolePage />} />
                        <Route path={ROUTES.MASTER_RIGHTS} element={<RightPage />} />
                      </Route>
                    </Route>
                  </Route>
                </Route>

                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </BrowserRouter>
          </SnackbarProvider>
        </LocalizationProvider>
      </ThemeProvider>
    </Provider>
  );
}
