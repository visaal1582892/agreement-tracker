import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Grid, Box, Typography, Paper, List, ListItemButton, ListItemText,
  Divider, Button, Chip, alpha, Skeleton,
} from '@mui/material';
import {
  Description, Warning, HourglassEmpty, Cancel,
  ArrowForward, FiberManualRecord,
} from '@mui/icons-material';
import axiosInstance from '../../api/axiosInstance';
import { ENDPOINTS } from '../../config/endpoints';
import { ROUTES } from '../../config/routes';
import { BRAND } from '../../config/theme';
import KpiCard from '../../components/ui/KpiCard';
import StatusBadge from '../../components/ui/StatusBadge';
import PageHeader from '../../components/ui/PageHeader';
import { useAuth } from '../../hooks/useAuth';

const EXPIRY_BANDS = [
  { label: 'Expiring in 30 days', key: 'expiringIn30Days', color: '#DC2626', bg: '#FEE2E2' },
  { label: 'Expiring 31–60 days', key: 'expiringIn60Days', color: '#D97706', bg: '#FEF3C7' },
  { label: 'Expiring 61–90 days', key: 'expiringIn90Days', color: '#0369A1', bg: '#DBEAFE' },
  { label: 'In Progress',         key: 'inProgress',       color: '#7C3AED', bg: '#EDE9FE' },
];

export default function DashboardPage() {
  const navigate = useNavigate();
  const { user, isAdmin, isApprover } = useAuth();
  const [stats, setStats] = useState(null);
  const [recentGroups, setRecentGroups] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const [statsRes, groupsRes] = await Promise.all([
          axiosInstance.get(ENDPOINTS.DASHBOARD_STATS),
          axiosInstance.get(ENDPOINTS.AGREEMENT_GROUPS, { params: { page: 0, size: 6 } }),
        ]);
        setStats(statsRes.data);
        setRecentGroups(groupsRes.data.content || []);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const firstName = user?.fullName?.split(' ')[0] || 'User';

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h5" fontWeight={700} sx={{ color: BRAND.textPrimary }}>
          Good morning, {firstName} 👋
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.3 }}>
          Here's what's happening with your agreements today.
        </Typography>
      </Box>

      {/* KPI Cards */}
      <Grid container spacing={2.5} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          {loading ? <Skeleton variant="rounded" height={120} sx={{ borderRadius: 3 }} /> : (
            <KpiCard
              title="Active Agreements"
              value={stats?.totalActive ?? 0}
              icon={<Description />}
              color={BRAND.green}
              subtitle="Currently live"
            />
          )}
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          {loading ? <Skeleton variant="rounded" height={120} sx={{ borderRadius: 3 }} /> : (
            <KpiCard
              title="Expiring Soon"
              value={stats?.expiringIn30Days ?? 0}
              icon={<Warning />}
              color="#D97706"
              subtitle="Within 30 days"
            />
          )}
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          {loading ? <Skeleton variant="rounded" height={120} sx={{ borderRadius: 3 }} /> : (
            <KpiCard
              title="Pending Approval"
              value={stats?.pendingMyApproval ?? 0}
              icon={<HourglassEmpty />}
              color={BRAND.red}
              subtitle="Needs your action"
            />
          )}
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          {loading ? <Skeleton variant="rounded" height={120} sx={{ borderRadius: 3 }} /> : (
            <KpiCard
              title="Expired"
              value={stats?.expired ?? 0}
              icon={<Cancel />}
              color="#64748B"
              subtitle="Need renewal"
            />
          )}
        </Grid>
      </Grid>

      <Grid container spacing={2.5}>
        {/* Recent Agreements */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Paper sx={{ borderRadius: 3, overflow: 'hidden', border: `1px solid ${BRAND.borderLight}` }}>
            <Box sx={{ px: 3, py: 2.5, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <Box>
                <Typography variant="subtitle1">Recent Agreements</Typography>
                <Typography variant="caption" color="text.secondary">Latest activity across all companies</Typography>
              </Box>
              <Button
                size="small"
                endIcon={<ArrowForward fontSize="small" />}
                onClick={() => navigate(ROUTES.AGREEMENTS)}
                sx={{ color: BRAND.red, fontWeight: 600, '&:hover': { bgcolor: alpha(BRAND.red, 0.05) } }}
              >
                View all
              </Button>
            </Box>
            <Divider />

            {loading ? (
              <Box sx={{ p: 2 }}>
                {[...Array(5)].map((_, i) => (
                  <Skeleton key={i} variant="rounded" height={56} sx={{ mb: 1, borderRadius: 2 }} />
                ))}
              </Box>
            ) : recentGroups.length === 0 ? (
              <Box sx={{ py: 6, textAlign: 'center' }}>
                <Description sx={{ fontSize: 48, color: alpha(BRAND.red, 0.2), mb: 1 }} />
                <Typography variant="body2" color="text.secondary">No agreements yet</Typography>
                <Button
                  variant="contained" size="small" sx={{ mt: 2 }}
                  onClick={() => navigate(ROUTES.AGREEMENT_CREATE)}
                >
                  Create Agreement
                </Button>
              </Box>
            ) : (
              <List disablePadding>
                {recentGroups.map((g, i) => (
                  <Box key={g.id}>
                    <ListItemButton
                      onClick={() => navigate(`/agreements/groups/${g.id}`)}
                      sx={{ px: 3, py: 1.8, '&:hover': { bgcolor: alpha(BRAND.red, 0.02) } }}
                    >
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flex: 1, minWidth: 0 }}>
                        <Box sx={{
                          width: 38, height: 38, borderRadius: 2, flexShrink: 0,
                          bgcolor: alpha(BRAND.red, 0.08),
                          display: 'flex', alignItems: 'center', justifyContent: 'center',
                        }}>
                          <Description sx={{ fontSize: 18, color: BRAND.red }} />
                        </Box>
                        <Box sx={{ flex: 1, minWidth: 0 }}>
                          <Typography variant="body2" fontWeight={600} noWrap>{g.agreementNumber}</Typography>
                          <Typography variant="caption" color="text.secondary" noWrap>{g.companyName}</Typography>
                        </Box>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexShrink: 0 }}>
                          <StatusBadge status={g.currentStatus || 'DRAFT'} />
                          <Typography variant="caption" color="text.secondary" sx={{ minWidth: 24 }}>
                            V{g.currentVersionNumber || 1}
                          </Typography>
                        </Box>
                      </Box>
                    </ListItemButton>
                    {i < recentGroups.length - 1 && <Divider sx={{ ml: 3 }} />}
                  </Box>
                ))}
              </List>
            )}
          </Paper>
        </Grid>

        {/* Right column */}
        <Grid size={{ xs: 12, md: 4 }}>
          {/* Expiry Summary */}
          <Paper sx={{ borderRadius: 3, p: 3, border: `1px solid ${BRAND.borderLight}`, mb: 2.5 }}>
            <Typography variant="subtitle1" sx={{ mb: 2 }}>Expiry Summary</Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
              {EXPIRY_BANDS.map(({ label, key, color, bg }) => (
                <Box key={key} sx={{
                  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                  p: 1.5, borderRadius: 2, bgcolor: bg,
                  border: `1px solid ${alpha(color, 0.15)}`,
                }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <FiberManualRecord sx={{ fontSize: 10, color }} />
                    <Typography variant="caption" fontWeight={500} color={color}>{label}</Typography>
                  </Box>
                  <Typography variant="subtitle2" sx={{ color, fontWeight: 700 }}>
                    {loading ? '…' : (stats?.[key] ?? 0)}
                  </Typography>
                </Box>
              ))}
            </Box>
          </Paper>

          {/* Quick Actions */}
          {(isApprover || isAdmin) && (
            <Paper sx={{ borderRadius: 3, p: 3, border: `1px solid ${BRAND.borderLight}` }}>
              <Typography variant="subtitle1" sx={{ mb: 2 }}>Quick Actions</Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                <Button
                  fullWidth variant="contained"
                  onClick={() => navigate(ROUTES.APPROVALS)}
                  sx={{ justifyContent: 'space-between', py: 1.2 }}
                  endIcon={<ArrowForward fontSize="small" />}
                >
                  Approval Queue
                  {stats?.pendingMyApproval > 0 && (
                    <Chip
                      label={stats.pendingMyApproval}
                      size="small"
                      sx={{ bgcolor: alpha('#fff', 0.25), color: '#fff', fontSize: '0.7rem', height: 20, ml: 1 }}
                    />
                  )}
                </Button>
                {(isAdmin) && (
                  <Button
                    fullWidth variant="outlined"
                    onClick={() => navigate(ROUTES.AGREEMENT_CREATE)}
                    sx={{ py: 1.2 }}
                  >
                    + New Agreement
                  </Button>
                )}
              </Box>
            </Paper>
          )}
        </Grid>
      </Grid>
    </Box>
  );
}
