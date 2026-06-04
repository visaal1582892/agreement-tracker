import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Box, Grid, Paper, Typography, Divider, Button, List, ListItem, ListItemButton, ListItemText } from '@mui/material';
import { fetchPendingApprovals } from '../../store/slices/agreementSlice';
import PageHeader from '../../components/ui/PageHeader';
import StatusBadge from '../../components/ui/StatusBadge';
import AgreementDetailPage from '../agreements/AgreementDetailPage';
import { BRAND } from '../../config/theme';

export default function ApprovalsPage() {
  const dispatch = useDispatch();
  const { pendingApprovals, pendingTotal, loading } = useSelector((s) => s.agreements);
  const [selectedId, setSelectedId] = useState(null);

  useEffect(() => {
    dispatch(fetchPendingApprovals({ page: 0, size: 50 }));
  }, [dispatch]);

  useEffect(() => {
    if (pendingApprovals.length > 0 && !selectedId) {
      setSelectedId(pendingApprovals[0].agreementGroupId);
    }
  }, [pendingApprovals]);

  return (
    <Box>
      <PageHeader
        title="Approvals Workspace"
        subtitle={`${pendingTotal} agreement(s) pending your approval`}
      />

      <Grid container spacing={2} sx={{ height: 'calc(100vh - 200px)' }}>
        {/* Left Pane */}
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
                pendingApprovals.map((a) => (
                  <Box key={a.id}>
                    <ListItemButton
                      selected={selectedId === a.agreementGroupId}
                      onClick={() => setSelectedId(a.agreementGroupId)}
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
                ))
              )}
            </List>
          </Paper>
        </Grid>

        {/* Right Pane — embed detail */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Paper elevation={0} sx={{ borderRadius: 2, border: '1px solid', borderColor: 'divider', height: '100%', overflowY: 'auto', p: 3 }}>
            {selectedId ? (
              <AgreementDetailPage embeddedGroupId={selectedId} />
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
