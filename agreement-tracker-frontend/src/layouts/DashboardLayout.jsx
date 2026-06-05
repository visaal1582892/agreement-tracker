import { useState } from 'react';
import { Outlet, useNavigate, useLocation, Link } from 'react-router-dom';
import {
  AppBar, Box, CssBaseline, Drawer, List, ListItem,
  ListItemButton, Toolbar, Typography,
  Badge, Avatar, Menu, MenuItem, Divider, Tooltip, alpha,
  IconButton,
} from '@mui/material';
import {
  Dashboard, Description, CheckCircle,
  NotificationsNone,
  Logout, Person, ExpandMore, Storage,
} from '@mui/icons-material';
import { useDispatch } from 'react-redux';
import { BRAND } from '../config/theme';
import { ROUTES } from '../config/routes';
import { RIGHTS } from '../config/rights';
import { logout } from '../store/slices/authSlice';
import { useAuth } from '../hooks/useAuth';

const DRAWER_WIDTH = 260;
const DRAWER_COLLAPSED_WIDTH = 68;

const NAV_ITEMS = [
  { label: 'Dashboard',   icon: <Dashboard sx={{ fontSize: 20 }} />,  path: ROUTES.DASHBOARD,   rights: [RIGHTS.DASHBOARD_VIEW] },
  { label: 'Agreements',  icon: <Description sx={{ fontSize: 20 }} />, path: ROUTES.AGREEMENTS,  rights: [RIGHTS.AGREEMENT_VIEW] },
  { label: 'Approvals',   icon: <CheckCircle sx={{ fontSize: 20 }} />, path: ROUTES.APPROVALS,   rights: [RIGHTS.AGREEMENT_APPROVE], badge: true },
  { label: 'Master Data', icon: <Storage sx={{ fontSize: 20 }} />,     path: ROUTES.MASTER,      rights: [RIGHTS.MASTER_VIEW, RIGHTS.MASTER_MANAGE] },
];

export default function DashboardLayout() {
  const [hovered, setHovered] = useState(false);
  const [anchorEl, setAnchorEl] = useState(null);
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();
  const { user, hasAnyRight } = useAuth();

  const handleLogout = () => {
    dispatch(logout());
    navigate(ROUTES.LOGIN);
  };

  const filteredNav = NAV_ITEMS.filter((item) => hasAnyRight(item.rights));

  const isActive = (path) =>
    location.pathname === path || (path !== '/' && location.pathname.startsWith(path));

  const userInitial = user?.fullName?.[0]?.toUpperCase() || 'U';

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      <CssBaseline />

      {/* ── Top App Bar ──────────────────────────────────────── */}
      <AppBar
        position="fixed"
        sx={{
          zIndex: (t) => t.zIndex.drawer - 1,
          bgcolor: '#fff',
          color: BRAND.textPrimary,
          borderBottom: `1px solid ${BRAND.borderLight}`,
          boxShadow: 'none',
          width: `calc(100% - ${DRAWER_COLLAPSED_WIDTH}px)`,
          ml: `${DRAWER_COLLAPSED_WIDTH}px`,
        }}
      >
        <Toolbar sx={{ gap: 2, minHeight: '64px !important' }}>
          <Box sx={{ minWidth: 0 }}>
            <Typography sx={{ fontSize: 16, fontWeight: 800, color: BRAND.textPrimary, lineHeight: 1.1 }}>
              MedPlus
            </Typography>
            <Typography
              sx={{
                fontSize: 10,
                fontWeight: 600,
                letterSpacing: '0.05em',
                textTransform: 'uppercase',
                color: BRAND.textSecondary,
                lineHeight: 1.2,
              }}
            >
              Agreement Tracker
            </Typography>
          </Box>
          <Box sx={{ flexGrow: 1 }} />

          {/* Notifications */}
          <Tooltip title="Notifications">
            <IconButton sx={{ color: BRAND.textSecondary, '&:hover': { bgcolor: BRAND.bgGray } }}>
              <Badge badgeContent={3} color="error" sx={{ '& .MuiBadge-badge': { fontSize: '0.65rem' } }}>
                <NotificationsNone />
              </Badge>
            </IconButton>
          </Tooltip>

          {/* AppBar user button */}
          <Box
            onClick={(e) => setAnchorEl(e.currentTarget)}
            sx={{
              display: 'flex', alignItems: 'center', gap: 1,
              cursor: 'pointer', borderRadius: 2, px: 1.5, py: 0.6,
              '&:hover': { bgcolor: BRAND.bgGray },
              transition: 'background-color 0.15s ease',
            }}
          >
            <Avatar sx={{ width: 32, height: 32, bgcolor: BRAND.red, fontSize: 14, fontWeight: 700, color: '#fff' }}>
              {userInitial}
            </Avatar>
            <Box sx={{ display: { xs: 'none', sm: 'block' }, ml: 0.5 }}>
              <Typography variant="body2" sx={{ fontWeight: 600, lineHeight: 1.2 }}>
                {user?.fullName || 'User'}
              </Typography>
              <Typography variant="caption" color="text.secondary" display="block">
                {user?.roles?.[0] || 'Admin'}
              </Typography>
            </Box>
            <ExpandMore sx={{ fontSize: 18, color: 'text.secondary' }} />
          </Box>
        </Toolbar>
      </AppBar>

      {/* Shared user dropdown */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
        PaperProps={{
          sx: { mt: 1, minWidth: 210, borderRadius: 2, border: `1px solid ${BRAND.borderLight}`, boxShadow: BRAND.shadowMd },
        }}
      >
        <Box sx={{ px: 2, py: 1.5 }}>
          <Typography variant="subtitle2">{user?.fullName}</Typography>
          <Typography variant="caption" color="text.secondary">{user?.email}</Typography>
        </Box>
        <Divider />
        <MenuItem onClick={() => { setAnchorEl(null); navigate('/profile'); }} sx={{ gap: 1.5, py: 1.2 }}>
          <Person fontSize="small" sx={{ color: 'text.secondary' }} />
          <Typography variant="body2">Profile</Typography>
        </MenuItem>
        <MenuItem onClick={handleLogout} sx={{ gap: 1.5, py: 1.2, color: 'error.main' }}>
          <Logout fontSize="small" />
          <Typography variant="body2">Sign out</Typography>
        </MenuItem>
      </Menu>

      {/* ── Sidebar ─────────────────────────────────────────── */}
      <Drawer
        variant="permanent"
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
        sx={{
          width: DRAWER_COLLAPSED_WIDTH,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: hovered ? DRAWER_WIDTH : DRAWER_COLLAPSED_WIDTH,
            boxSizing: 'border-box',
            bgcolor: '#fff',
            borderRight: `1px solid ${BRAND.borderLight}`,
            boxShadow: hovered ? '4px 0 24px rgba(0,0,0,0.08)' : 'none',
            display: 'flex',
            flexDirection: 'column',
            overflowX: 'hidden',
            transition: 'width 0.22s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.22s ease',
            zIndex: (t) => t.zIndex.drawer + 2,
          },
        }}
      >
        {/* Logo */}
        <Box sx={{
          height: 64,
          px: 1.5,
          flexShrink: 0,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'flex-start',
          gap: 1.5,
          overflow: 'hidden',
        }}>
          {/* Raw logo — no wrapper box */}
          <Box
            component="img"
            src="/images/medplus_logo.png"
            alt="MedPlus"
            sx={{ width: 32, height: 32, objectFit: 'contain', flexShrink: 0 }}
          />
          {/* Two-name text — slides in/out, always two distinct lines */}
          <Box sx={{
            overflow: 'hidden',
            whiteSpace: 'nowrap',
            opacity: hovered ? 1 : 0,
            maxWidth: hovered ? 180 : 0,
            transition: 'opacity 0.2s ease, max-width 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
          }}>
            <Typography sx={{ fontWeight: 800, fontSize: 15, color: BRAND.textPrimary, lineHeight: 1.15 }}>
              MedPlus
            </Typography>
            <Typography sx={{
              fontWeight: 700,
              fontSize: 9.5,
              color: BRAND.red,
              letterSpacing: '0.07em',
              textTransform: 'uppercase',
              lineHeight: 1.3,
            }}>
              Agreement Tracker
            </Typography>
          </Box>
        </Box>

        {/* Nav items */}
        <List sx={{ px: 0, pt: 1, pb: 1, flex: 1 }}>
          {filteredNav.map((item) => {
            const active = isActive(item.path);
            return (
              <ListItem key={item.path} disablePadding sx={{ mb: 0.5, px: hovered ? 1 : 0 }}>
                <Tooltip title={!hovered ? item.label : ''} placement="right" arrow>
                  <ListItemButton
                    component={Link}
                    to={item.path}
                    sx={{
                      borderRadius: hovered ? 2 : 0,
                      py: 1.2,
                      px: hovered ? 1.5 : 0,
                      minHeight: 44,
                      width: '100%',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: hovered ? 'flex-start' : 'center',
                      gap: 1.5,
                      bgcolor: active ? alpha(BRAND.red, 0.08) : 'transparent',
                      color: active ? BRAND.red : BRAND.textSecondary,
                      position: 'relative',
                      overflow: 'hidden',
                      '&:hover': { bgcolor: active ? alpha(BRAND.red, 0.12) : BRAND.bgGray },
                      transition: 'background-color 0.15s ease, color 0.15s ease, padding 0.22s ease',
                    }}
                  >
                    <Box sx={{
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      color: 'inherit',
                      flexShrink: 0,
                    }}>
                      {item.badge ? (
                        <Badge
                          badgeContent={2}
                          color="error"
                          sx={{ '& .MuiBadge-badge': { fontSize: '0.58rem', minWidth: 15, height: 15 } }}
                        >
                          {item.icon}
                        </Badge>
                      ) : item.icon}
                    </Box>
                    <Box sx={{
                      overflow: 'hidden',
                      opacity: hovered ? 1 : 0,
                      maxWidth: hovered ? 160 : 0,
                      transition: 'opacity 0.15s ease, max-width 0.22s cubic-bezier(0.4, 0, 0.2, 1)',
                      display: 'flex', alignItems: 'center', flex: 1,
                    }}>
                      <Typography sx={{
                        color: 'inherit',
                        fontSize: '0.875rem',
                        fontWeight: active ? 700 : 500,
                        whiteSpace: 'nowrap',
                      }}>
                        {item.label}
                      </Typography>
                    </Box>
                  </ListItemButton>
                </Tooltip>
              </ListItem>
            );
          })}
        </List>

        {/* Bottom sign out */}
        <Box sx={{ py: 1.5, px: hovered ? 1.5 : 0, borderTop: `1px solid ${BRAND.borderLight}` }}>
          <ListItem disablePadding>
            <Tooltip title={!hovered ? 'Sign out' : ''} placement="right" arrow>
              <ListItemButton
                onClick={handleLogout}
                sx={{
                  borderRadius: hovered ? 2 : 0,
                  py: 1.2,
                  px: hovered ? 1.5 : 0,
                  minHeight: 44,
                  width: '100%',
                  display: 'flex', alignItems: 'center',
                  justifyContent: hovered ? 'flex-start' : 'center',
                  gap: 1.5,
                  color: BRAND.textSecondary,
                  '&:hover': { bgcolor: BRAND.bgGray, color: 'error.main' },
                  transition: 'background-color 0.15s ease, padding 0.22s ease',
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'inherit', flexShrink: 0 }}>
                  <Logout sx={{ fontSize: 20 }} />
                </Box>
                <Box sx={{
                  overflow: 'hidden',
                  opacity: hovered ? 1 : 0,
                  maxWidth: hovered ? 160 : 0,
                  transition: 'opacity 0.15s ease, max-width 0.22s cubic-bezier(0.4, 0, 0.2, 1)',
                  display: 'flex', alignItems: 'center', flex: 1,
                }}>
                  <Typography sx={{ color: 'inherit', fontSize: '0.85rem', fontWeight: 500, whiteSpace: 'nowrap' }}>
                    Sign out
                  </Typography>
                </Box>
              </ListItemButton>
            </Tooltip>
          </ListItem>
        </Box>
      </Drawer>

      {/* ── Main Content ─────────────────────────────────────── */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          pt: '80px',
          minWidth: 0,
        }}
      >
        <Outlet />
      </Box>
    </Box>
  );
}

