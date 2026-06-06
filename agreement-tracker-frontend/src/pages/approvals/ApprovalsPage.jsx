import { useCallback, useEffect, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  Box, Grid, Paper, Typography, Divider, List, ListItemButton, ListItemText,
  TextField, InputAdornment, TablePagination, CircularProgress, alpha,
} from '@mui/material';
import { Search, TaskAlt, InboxOutlined } from '@mui/icons-material';
import { fetchPendingApprovals } from '../../store/slices/agreementSlice';
import PageHeader from '../../components/ui/PageHeader';
import StatusBadge from '../../components/ui/StatusBadge';
import AgreementDetailPage from '../agreements/AgreementDetailPage';
import { BRAND } from '../../config/theme';

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50];

function PendingEmptyState({ hasSearch }) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        px: 3,
        py: 6,
        minHeight: 280,
      }}
    >
      <Box
        sx={{
          width: 72,
          height: 72,
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          bgcolor: alpha(BRAND.green, 0.1),
          mb: 2,
        }}
      >
        {hasSearch ? (
          <InboxOutlined sx={{ fontSize: 36, color: BRAND.textSecondary }} />
        ) : (
          <TaskAlt sx={{ fontSize: 36, color: BRAND.green }} />
        )}
      </Box>
      <Typography variant="subtitle1" fontWeight={700} gutterBottom>
        {hasSearch ? 'No matching approvals' : 'All caught up!'}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 260, lineHeight: 1.6 }}>
        {hasSearch
          ? 'Try a different agreement number or company name.'
          : 'No agreements are waiting for your review right now.'}
      </Typography>
    </Box>
  );
}

function DetailEmptyState() {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        minHeight: 360,
        textAlign: 'center',
        px: 4,
      }}
    >
      <Box
        sx={{
          width: 64,
          height: 64,
          borderRadius: '50%',
          bgcolor: alpha(BRAND.red, 0.08),
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          mb: 2,
        }}
      >
        <Search sx={{ fontSize: 30, color: BRAND.red }} />
      </Box>
      <Typography variant="subtitle1" fontWeight={600} gutterBottom>
        Select an agreement to review
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 320 }}>
        Choose a pending agreement from the list to view details, version history, and approval actions.
      </Typography>
    </Box>
  );
}

export default function ApprovalsPage() {
  const dispatch = useDispatch();
  const {
    pendingApprovals,
    pendingTotal,
    loading,
  } = useSelector((s) => s.agreements);

  const [selectedGroupId, setSelectedGroupId] = useState(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [searchInput, setSearchInput] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const prevSearchRef = useRef('');

  const totalCount = pendingTotal ?? 0;

  const loadPending = useCallback(() => {
    dispatch(fetchPendingApprovals({ page, size: rowsPerPage, search: searchQuery }));
  }, [dispatch, page, rowsPerPage, searchQuery]);

  useEffect(() => {
    loadPending();
  }, [loadPending]);

  useEffect(() => {
    const timer = setTimeout(() => {
      const next = searchInput.trim();
      if (next !== prevSearchRef.current) {
        prevSearchRef.current = next;
        setPage(0);
      }
      setSearchQuery(next);
    }, 350);
    return () => clearTimeout(timer);
  }, [searchInput]);

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

  const handlePageChange = (_, newPage) => {
    setPage(newPage);
  };

  const handleRowsPerPageChange = (e) => {
    setRowsPerPage(parseInt(e.target.value, 10));
    setPage(0);
  };

  const hasSearch = Boolean(searchQuery);

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100%' }}>
      <PageHeader
        title="Approvals Workspace"
        subtitle={`${totalCount} agreement${totalCount === 1 ? '' : 's'} pending your approval`}
      />

      <Grid container spacing={2} sx={{ flex: 1, minHeight: 480 }}>
        <Grid size={{ xs: 12, md: 4 }}>
          <Paper
            elevation={0}
            sx={{
              borderRadius: 2,
              border: '1px solid',
              borderColor: 'divider',
              height: '100%',
              display: 'flex',
              flexDirection: 'column',
              overflow: 'hidden',
            }}
          >
            <Box
              sx={{
                px: 2,
                py: 1.5,
                bgcolor: BRAND.bgGray,
                borderBottom: '1px solid',
                borderColor: 'divider',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: 1.5,
                flexWrap: 'wrap',
              }}
            >
              <Typography variant="subtitle2" fontWeight={600} sx={{ whiteSpace: 'nowrap' }}>
                Pending Approvals ({totalCount})
              </Typography>
              <TextField
                size="small"
                placeholder="Search…"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                sx={{ minWidth: 140, flex: 1, maxWidth: 200 }}
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <Search fontSize="small" />
                      </InputAdornment>
                    ),
                  },
                }}
              />
            </Box>

            <Box sx={{ flex: 1, overflowY: 'auto' }}>
              {loading ? (
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', py: 6 }}>
                  <CircularProgress size={28} />
                </Box>
              ) : pendingApprovals.length === 0 ? (
                <PendingEmptyState hasSearch={hasSearch} />
              ) : (
                <List disablePadding>
                  {pendingApprovals.map((a) => {
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
                            primary={(
                              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 1 }}>
                                <Typography variant="body2" fontWeight={600} noWrap>{a.agreementNumber}</Typography>
                                <StatusBadge status="PENDING_APPROVAL" />
                              </Box>
                            )}
                            secondary={a.companyName}
                          />
                        </ListItemButton>
                        <Divider />
                      </Box>
                    );
                  })}
                </List>
              )}
            </Box>

            <TablePagination
              component="div"
              count={totalCount}
              page={page}
              onPageChange={handlePageChange}
              rowsPerPage={rowsPerPage}
              onRowsPerPageChange={handleRowsPerPageChange}
              rowsPerPageOptions={PAGE_SIZE_OPTIONS}
              labelRowsPerPage="Rows:"
              sx={{
                borderTop: '1px solid',
                borderColor: 'divider',
                '.MuiTablePagination-toolbar': { minHeight: 48, px: 1 },
                '.MuiTablePagination-selectLabel, .MuiTablePagination-displayedRows': {
                  fontSize: '0.8rem',
                },
              }}
            />
          </Paper>
        </Grid>

        <Grid size={{ xs: 12, md: 8 }}>
          <Paper
            elevation={0}
            sx={{
              borderRadius: 2,
              border: '1px solid',
              borderColor: 'divider',
              height: '100%',
              overflowY: 'auto',
              p: selectedGroupId ? 3 : 0,
            }}
          >
            {selectedGroupId ? (
              <AgreementDetailPage
                key={selectedGroupId}
                embeddedGroupId={selectedGroupId}
                onActionComplete={handleActionComplete}
              />
            ) : (
              <DetailEmptyState />
            )}
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
}
