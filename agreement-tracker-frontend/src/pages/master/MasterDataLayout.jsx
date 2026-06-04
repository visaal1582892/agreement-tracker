import { NavLink, Outlet, useLocation } from 'react-router-dom';
import {
  Box, Typography, Tabs, Tab, Paper, alpha,
} from '@mui/material';
import {
  Business, LocalShipping, Factory, AccountTree,
  Inventory2, AttachMoney, Description, ManageAccounts, Security,
} from '@mui/icons-material';
import { BRAND } from '../../config/theme';
import { ROUTES } from '../../config/routes';

const MASTER_TABS = [
  { label: 'Companies',        icon: <Business />,        path: ROUTES.MASTER_COMPANIES },
  { label: 'Vendors',          icon: <LocalShipping />,   path: ROUTES.MASTER_VENDORS },
  { label: 'Manufacturers',    icon: <Factory />,         path: ROUTES.MASTER_MANUFACTURERS },
  { label: 'Divisions',        icon: <AccountTree />,     path: ROUTES.MASTER_DIVISIONS },
  { label: 'Products',         icon: <Inventory2 />,      path: ROUTES.MASTER_PRODUCTS },
  { label: 'Income Types',     icon: <AttachMoney />,     path: ROUTES.MASTER_INCOME_TYPES },
  { label: 'Agreement Types',  icon: <Description />,     path: ROUTES.MASTER_AGREEMENT_TYPES },
  { label: 'Roles',            icon: <ManageAccounts />,  path: ROUTES.MASTER_ROLES },
  { label: 'Rights',           icon: <Security />,        path: ROUTES.MASTER_RIGHTS },
];

export default function MasterDataLayout() {
  const location = useLocation();

  const activeIndex = MASTER_TABS.findIndex((t) =>
    location.pathname === t.path || location.pathname.startsWith(t.path + '/'),
  );

  return (
    <Box>
      {/* Page header */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h5" sx={{ fontWeight: 800 }} color="text.primary">
          Master Data Configuration
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          Manage lookup tables, reference data, and system configuration
        </Typography>
      </Box>

      {/* Tab navigation */}
      <Paper
        elevation={0}
        sx={{
          mb: 3,
          borderRadius: 2,
          border: `1px solid ${BRAND.borderLight}`,
          overflow: 'hidden',
        }}
      >
        <Tabs
          value={activeIndex === -1 ? 0 : activeIndex}
          variant="scrollable"
          scrollButtons="auto"
          slotProps={{
            indicator: {
              style: { backgroundColor: BRAND.red, height: 3 },
            },
          }}
          sx={{
            px: 1,
            '& .MuiTab-root': {
              minHeight: 56,
              fontSize: '0.82rem',
              fontWeight: 600,
              textTransform: 'none',
              color: BRAND.textSecondary,
              gap: 0.75,
              '&.Mui-selected': { color: BRAND.red },
              '&:hover': { bgcolor: alpha(BRAND.red, 0.04) },
            },
          }}
        >
          {MASTER_TABS.map((tab) => (
            <Tab
              key={tab.path}
              label={tab.label}
              icon={tab.icon}
              iconPosition="start"
              component={NavLink}
              to={tab.path}
            />
          ))}
        </Tabs>
      </Paper>

      {/* Active master page */}
      <Outlet />
    </Box>
  );
}
