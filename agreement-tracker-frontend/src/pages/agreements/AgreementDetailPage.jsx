import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Grid, Typography, Paper, Divider, Chip, Button, Select, MenuItem,
  FormControl, Stepper, Step, StepLabel, StepContent, TextField,
  Dialog, DialogTitle, DialogContent, DialogActions, Tooltip, IconButton,
  Accordion, AccordionSummary, AccordionDetails, Alert,
} from '@mui/material';
import { Edit, ExpandMore, SwapHoriz, PowerSettingsNew } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import axiosInstance from '../../api/axiosInstance';
import { ENDPOINTS } from '../../config/endpoints';
import { BRAND } from '../../config/theme';
import StatusBadge from '../../components/ui/StatusBadge';
import LoadingOverlay from '../../components/ui/LoadingOverlay';
import { useAuth } from '../../hooks/useAuth';
import { useModal } from '../../hooks/useModal';
import dayjs from 'dayjs';

export default function AgreementDetailPage() {
  const { groupId } = useParams();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const { user, isAdmin, isApprover, isAccountManager } = useAuth();

  const [group, setGroup] = useState(null);
  const [versions, setVersions] = useState([]);
  const [selectedVersionId, setSelectedVersionId] = useState(null);
  const [agreement, setAgreement] = useState(null);
  const [timeline, setTimeline] = useState([]);
  const [loading, setLoading] = useState(true);
  const [rejectRemarks, setRejectRemarks] = useState('');

  const rejectModal = useModal();
  const terminateModal = useModal();
  const [terminateData, setTerminateData] = useState({ terminationDate: '', terminationReason: '' });

  const load = async () => {
    try {
      const [groupRes, versionsRes] = await Promise.all([
        axiosInstance.get(ENDPOINTS.AGREEMENT_GROUP_BY_ID(groupId)),
        axiosInstance.get(ENDPOINTS.AGREEMENT_VERSIONS(groupId)),
      ]);
      setGroup(groupRes.data);
      setVersions(versionsRes.data);
      const currentId = groupRes.data.currentVersionId || versionsRes.data[0]?.id;
      setSelectedVersionId(currentId);
    } catch {
      enqueueSnackbar('Failed to load agreement', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [groupId]);

  useEffect(() => {
    if (!selectedVersionId) return;
    const loadVersion = async () => {
      try {
        const [agrRes, tlRes] = await Promise.all([
          axiosInstance.get(ENDPOINTS.AGREEMENT_BY_ID(selectedVersionId)),
          axiosInstance.get(ENDPOINTS.AGREEMENT_TIMELINE(selectedVersionId)),
        ]);
        setAgreement(agrRes.data);
        setTimeline(tlRes.data);
      } catch {
        enqueueSnackbar('Failed to load version details', { variant: 'error' });
      }
    };
    loadVersion();
  }, [selectedVersionId]);

  const handleSubmit = async () => {
    try {
      await axiosInstance.post(ENDPOINTS.AGREEMENT_SUBMIT(selectedVersionId));
      enqueueSnackbar('Submitted for approval', { variant: 'success' });
      load();
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Submit failed', { variant: 'error' });
    }
  };

  const handleApprove = async () => {
    try {
      await axiosInstance.post(ENDPOINTS.AGREEMENT_APPROVE(selectedVersionId), { remarks: 'Approved' });
      enqueueSnackbar('Agreement approved', { variant: 'success' });
      load();
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Approval failed', { variant: 'error' });
    }
  };

  const handleReject = async () => {
    try {
      await axiosInstance.post(ENDPOINTS.AGREEMENT_REJECT(selectedVersionId), { remarks: rejectRemarks });
      enqueueSnackbar('Agreement rejected', { variant: 'success' });
      rejectModal.close();
      setRejectRemarks('');
      load();
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Rejection failed', { variant: 'error' });
    }
  };

  const handleNewVersion = async () => {
    try {
      const { data } = await axiosInstance.post(ENDPOINTS.AGREEMENT_NEW_VERSION(groupId));
      enqueueSnackbar(`New draft V${data.versionNumber} created`, { variant: 'success' });
      setVersions((prev) => [...prev, data]);
      setSelectedVersionId(data.id);
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Failed', { variant: 'error' });
    }
  };

  const handleTerminate = async () => {
    try {
      await axiosInstance.post(ENDPOINTS.AGREEMENT_TERMINATE(selectedVersionId), terminateData);
      enqueueSnackbar('Agreement terminated', { variant: 'success' });
      terminateModal.close();
      load();
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Termination failed', { variant: 'error' });
    }
  };

  const isOwner = agreement?.ownerId === user?.id;
  const canSubmit = isOwner && agreement?.approvalStatus === 'DRAFT';
  const canApprove = (isApprover || isAdmin) && agreement?.approvalStatus === 'PENDING_APPROVAL' && agreement?.ownerId !== user?.id;
  const canNewVersion = (isOwner || isAdmin) && agreement?.approvalStatus === 'APPROVED';
  const canTerminate = isAdmin && agreement?.approvalStatus === 'APPROVED' && !agreement?.terminationDate;

  const ACTION_COLOR = { SUBMITTED: '#2196F3', APPROVED: BRAND.green, REJECTED: BRAND.red };

  if (loading) return <LoadingOverlay open />;

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700}>{group?.agreementNumber}</Typography>
          <Typography variant="body2" color="text.secondary">{group?.companyName}</Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
          {/* Version Switcher */}
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <Select value={selectedVersionId || ''} onChange={(e) => setSelectedVersionId(e.target.value)}>
              {versions.map((v) => (
                <MenuItem key={v.id} value={v.id}>
                  V{v.versionNumber} {group?.currentVersionId === v.id ? '(Current)' : ''}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          {agreement && <StatusBadge status={agreement.derivedStatus || agreement.approvalStatus} />}
        </Box>
      </Box>

      <Grid container spacing={3}>
        {/* Left: Data */}
        <Grid size={{ xs: 12, md: 8 }}>
          {agreement ? (
            <>
              <Accordion defaultExpanded elevation={0} sx={{ borderRadius: 2, border: '1px solid', borderColor: 'divider', mb: 2 }}>
                <AccordionSummary expandIcon={<ExpandMore />}>
                  <Typography fontWeight={600}>Company & Vendors</Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <Grid container spacing={1}>
                    <Grid size={4}><Typography variant="caption" color="text.secondary">Company</Typography><Typography variant="body2">{agreement.companyName}</Typography></Grid>
                    <Grid size={4}><Typography variant="caption" color="text.secondary">Owner</Typography><Typography variant="body2">{agreement.ownerName}</Typography></Grid>
                    <Grid size={12}><Divider sx={{ my: 1 }} />
                      {agreement.vendors?.map((v) => <Chip key={v.vendorId} label={v.vendorName} size="small" sx={{ mr: 0.5, mb: 0.5 }} />)}
                    </Grid>
                  </Grid>
                </AccordionDetails>
              </Accordion>

              <Accordion defaultExpanded elevation={0} sx={{ borderRadius: 2, border: '1px solid', borderColor: 'divider', mb: 2 }}>
                <AccordionSummary expandIcon={<ExpandMore />}>
                  <Typography fontWeight={600}>Agreement Details</Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <Grid container spacing={2}>
                    <Grid size={{ xs: 6, sm: 3 }}><Typography variant="caption" color="text.secondary">Income Type</Typography><Typography variant="body2">{agreement.incomeTypeName || '—'}</Typography></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><Typography variant="caption" color="text.secondary">Agreement Type</Typography><Typography variant="body2">{agreement.agreementTypeName || '—'}</Typography></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><Typography variant="caption" color="text.secondary">Start Date</Typography><Typography variant="body2">{agreement.startDate ? dayjs(agreement.startDate).format('DD MMM YYYY') : '—'}</Typography></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><Typography variant="caption" color="text.secondary">Expiry Date</Typography><Typography variant="body2">{agreement.expiryDate ? dayjs(agreement.expiryDate).format('DD MMM YYYY') : '—'}</Typography></Grid>
                  </Grid>
                </AccordionDetails>
              </Accordion>

              <Accordion defaultExpanded elevation={0} sx={{ borderRadius: 2, border: '1px solid', borderColor: 'divider', mb: 2 }}>
                <AccordionSummary expandIcon={<ExpandMore />}>
                  <Typography fontWeight={600}>Commercial Structure</Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <Grid container spacing={2}>
                    <Grid size={{ xs: 6, sm: 3 }}><Typography variant="caption" color="text.secondary">Structure</Typography><Typography variant="body2">{agreement.commercialStructure}</Typography></Grid>
                    {agreement.commercialStructure === 'FLAT' && (
                      <Grid size={{ xs: 6, sm: 3 }}><Typography variant="caption" color="text.secondary">Value</Typography><Typography variant="body2">₹{Number(agreement.commercialValue || 0).toLocaleString('en-IN')}</Typography></Grid>
                    )}
                  </Grid>
                </AccordionDetails>
              </Accordion>

              <Accordion elevation={0} sx={{ borderRadius: 2, border: '1px solid', borderColor: 'divider' }}>
                <AccordionSummary expandIcon={<ExpandMore />}>
                  <Typography fontWeight={600}>Products ({agreement.products?.length || 0})</Typography>
                </AccordionSummary>
                <AccordionDetails>
                  {agreement.products?.map((p) => (
                    <Chip key={p.productId} label={`${p.productName} (${p.divisionName || ''})`} size="small" sx={{ mr: 0.5, mb: 0.5 }} />
                  ))}
                </AccordionDetails>
              </Accordion>
            </>
          ) : (
            <Typography color="text.secondary">Select a version to view details.</Typography>
          )}
        </Grid>

        {/* Right: Timeline + Actions */}
        <Grid size={{ xs: 12, md: 4 }}>
          {/* Actions */}
          {agreement && (
            <Paper elevation={0} sx={{ p: 2, borderRadius: 2, border: '1px solid', borderColor: 'divider', mb: 2 }}>
              <Typography variant="subtitle2" fontWeight={600} mb={1.5}>Actions</Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                {canSubmit && (
                  <Button variant="contained" fullWidth onClick={handleSubmit} sx={{ bgcolor: BRAND.red }}>
                    Submit for Approval
                  </Button>
                )}
                {canApprove && (
                  <>
                    <Button variant="contained" fullWidth onClick={handleApprove} sx={{ bgcolor: BRAND.green }}>
                      ✓ Approve
                    </Button>
                    <Button variant="outlined" color="error" fullWidth onClick={rejectModal.open}>
                      ✗ Reject
                    </Button>
                  </>
                )}
                {canNewVersion && (
                  <Button variant="outlined" fullWidth startIcon={<Edit />} onClick={handleNewVersion}>
                    Create New Version
                  </Button>
                )}
                {canTerminate && (
                  <Button variant="outlined" color="error" fullWidth startIcon={<PowerSettingsNew />} onClick={terminateModal.open}>
                    Terminate
                  </Button>
                )}
              </Box>
            </Paper>
          )}

          {/* Approval Timeline */}
          <Paper elevation={0} sx={{ p: 2, borderRadius: 2, border: '1px solid', borderColor: 'divider' }}>
            <Typography variant="subtitle2" fontWeight={600} mb={1.5}>Approval Timeline</Typography>
            {timeline.length === 0 ? (
              <Typography variant="body2" color="text.secondary">No activity yet</Typography>
            ) : (
              <Stepper orientation="vertical" nonLinear>
                {timeline.map((t, i) => (
                  <Step key={t.id} active completed>
                    <StepLabel
                      StepIconComponent={() => (
                        <Box sx={{ width: 12, height: 12, borderRadius: '50%', bgcolor: ACTION_COLOR[t.action] || '#999', mt: 0.5 }} />
                      )}
                    >
                      <Typography variant="body2" fontWeight={600}>
                        {t.action} — {t.actorName || `User ${t.actorUserId}`}
                      </Typography>
                    </StepLabel>
                    <StepContent>
                      {t.remarks && <Typography variant="caption" color="text.secondary">"{t.remarks}"</Typography>}
                      <Typography variant="caption" display="block" color="text.secondary">
                        {t.timestamp ? dayjs(t.timestamp).format('DD MMM YYYY, hh:mm A') : ''}
                      </Typography>
                    </StepContent>
                  </Step>
                ))}
              </Stepper>
            )}
          </Paper>
        </Grid>
      </Grid>

      {/* Reject Modal */}
      <Dialog open={rejectModal.isOpen} onClose={rejectModal.close} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight={700}>Reject Agreement</DialogTitle>
        <DialogContent>
          <Alert severity="warning" sx={{ mb: 2 }}>Once rejected, this version cannot be edited. Owner must create a new version.</Alert>
          <TextField
            label="Rejection Remarks *"
            multiline rows={4}
            fullWidth
            value={rejectRemarks}
            onChange={(e) => setRejectRemarks(e.target.value)}
            placeholder="Clearly state the reason for rejection…"
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={rejectModal.close} variant="outlined">Cancel</Button>
          <Button onClick={handleReject} variant="contained" color="error" disabled={!rejectRemarks.trim()}>
            Reject Agreement
          </Button>
        </DialogActions>
      </Dialog>

      {/* Terminate Modal */}
      <Dialog open={terminateModal.isOpen} onClose={terminateModal.close} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight={700} color="error">Terminate Agreement</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid size={12}>
              <TextField
                label="Termination Date *" type="date"
                fullWidth
                slotProps={{ inputLabel: { shrink: true } }}
                value={terminateData.terminationDate}
                onChange={(e) => setTerminateData((p) => ({ ...p, terminationDate: e.target.value }))}
              />
            </Grid>
            <Grid size={12}>
              <TextField
                label="Termination Reason *" multiline rows={3}
                fullWidth
                value={terminateData.terminationReason}
                onChange={(e) => setTerminateData((p) => ({ ...p, terminationReason: e.target.value }))}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={terminateModal.close} variant="outlined">Cancel</Button>
          <Button onClick={handleTerminate} variant="contained" color="error"
            disabled={!terminateData.terminationDate || !terminateData.terminationReason.trim()}>
            Terminate
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
