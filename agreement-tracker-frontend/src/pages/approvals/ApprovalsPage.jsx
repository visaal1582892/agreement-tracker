import { useCallback, useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Box, Grid, Paper, Typography, Divider, List, ListItem, ListItemButton, ListItemText } from '@mui/material';
import { fetchPendingApprovals } from '../../store/slices/agreementSlice';
import PageHeader from '../../components/ui/PageHeader';
import StatusBadge from '../../components/ui/StatusBadge';
import AgreementDetailPage from '../agreements/AgreementDetailPage';
import { BRAND } from '../../config/theme';

export default function ApprovalsPage() {
  const dispatch = useDispatch();
  const { pendingApprovals, pendingTotal, loading } = useSelector((s) => s.agreements);
  const [selectedGroupId, setSelectedGroupId] = useState(null);

  const loadPending = useCallback(() => {
    dispatch(fetchPendingApprovals({ page: 0, size: 50 }));
  }, [dispatch]);

  useEffect(() => {
    loadPending();
  }, [loadPending]);

  useEffect(() => {
    if (pendingApprovals.length === 0) {
      setSelectedGroupId(null);
      return;
    }
    const stillVisible = pendingApprovals.some((a) => a.agreementGroupId === selectedGroupId);
    if (!selectedGroupId || !stillVisible) {
      setSelectedGroupId(pendingApprovals[0].agreementGroupId ?? null);
    }
  }, [pendingApprovals, selectedGroupId]);

  const handleActionComplete = () => {
    loadPending();
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100%' }}>
      <PageHeader
        title="Approvals Workspace"
        subtitle={`${pendingTotal} agreement(s) pending your approval`}
      />

      <Grid container spacing={2} sx={{ flex: 1, minHeight: 480 }}>
        <Grid size={{ xs: 12, md: 4 }}>
          <Paper elevation={0} sx={{ borderRadius: 2, border: '1px solid', borderColor: 'divider', height: '100%', overflowY: 'auto' }}>
            <Box sx={{ px: 2, py: 1.5, bgcolor: BRAND.bgGray, borderRadius: '8px 8px 0 0' }}>
              <Typography variant="subtitle2" fontWeight={600}>Pending Approvals ({pendingTotal})</Typography>
            </Box>
            <Divider />
            <List disablePadding>
              {loading ? (
                <ListItem><ListItemText primary="Loading…" /></ListItem>
              ) : pendingApprovals.length === 0 ? (
                <ListItem><ListItemText primary="No pending approvals" secondary="All caught up! 🎉" /></ListItem>
              ) : (
                pendingApprovals.map((a) => {
                  const groupId = a.agreementGroupId;
                  if (!groupId) return null;
                  return (
                    <Box key={a.id}>
                      <ListItemButton
                        selected={selectedGroupId === groupId}
                        onClick={() => setSelectedGroupId(groupId)}
                        sx={{ '&.Mui-selected': { bgcolor: '#FFF0F0', borderLeft: `3px solid ${BRAND.red}` } }}
                      >
                        <ListItemText
                          primary={
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                              <Typography variant="body2" fontWeight={600}>{a.agreementNumber}</Typography>
                              <StatusBadge status="PENDING_APPROVAL" />
                            </Box>
                          }
                          secondary={a.companyName}
                        />
                      </ListItemButton>
                      <Divider />
                    </Box>
                  );
                })
              )}
            </List>
          </Paper>
        </Grid>

        <Grid size={{ xs: 12, md: 8 }}>
          <Paper elevation={0} sx={{ borderRadius: 2, border: '1px solid', borderColor: 'divider', height: '100%', overflowY: 'auto', p: 3 }}>
            {selectedGroupId ? (
              <AgreementDetailPage
                key={selectedGroupId}
                embeddedGroupId={selectedGroupId}
                onActionComplete={handleActionComplete}
              />
            ) : (
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
                <Typography color="text.secondary">Select an agreement to review</Typography>
              </Box>
            )}
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
}
