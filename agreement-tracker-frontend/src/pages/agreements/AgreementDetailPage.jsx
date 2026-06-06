import { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import {
  Box, Grid, Typography, Paper, Divider, Chip, Button, Select, MenuItem,
  FormControl, Stepper, Step, StepLabel, StepContent, TextField,
  Dialog, DialogTitle, DialogContent, DialogActions, Alert, Tabs, Tab,
  List, ListItemButton, ListItemText, Accordion, AccordionSummary, AccordionDetails,
} from '@mui/material';
import { Edit, ExpandMore, PowerSettingsNew, ContentCopy, SwapHoriz, History } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import axiosInstance from '../../api/axiosInstance';
import { ENDPOINTS } from '../../config/endpoints';
import { BRAND } from '../../config/theme';
import StatusBadge from '../../components/ui/StatusBadge';
import LoadingOverlay from '../../components/ui/LoadingOverlay';
import { useAuth } from '../../hooks/useAuth';
import { RIGHTS } from '../../config/rights';
import { useModal } from '../../hooks/useModal';
import { ROUTES } from '../../config/routes';
import { fetchAgreementForClone } from '../../utils/agreementClone';
import { submitAgreementForApproval, approveAgreement, rejectAgreement } from '../../store/slices/agreementSlice';
import TransferOwnershipModal from '../../components/agreements/TransferOwnershipModal';
import dayjs from 'dayjs';

export default function AgreementDetailPage({ embeddedGroupId, onActionComplete } = {}) {
  const { groupId: routeGroupId } = useParams();
  const groupId = embeddedGroupId ?? routeGroupId;
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { enqueueSnackbar } = useSnackbar();
  const { user, hasRight } = useAuth();

  const [group, setGroup] = useState(null);
  const [versions, setVersions] = useState([]);
  const [selectedVersionId, setSelectedVersionId] = useState(null);
  const [agreement, setAgreement] = useState(null);
  const [timeline, setTimeline] = useState([]);
  const [loading, setLoading] = useState(true);
  const [rejectRemarks, setRejectRemarks] = useState('');
  const [activeTab, setActiveTab] = useState('details');
  const [transferOpen, setTransferOpen] = useState(false);

  const rejectModal = useModal();
  const terminateModal = useModal();
  const [terminateData, setTerminateData] = useState({ terminationDate: '', terminationReason: '' });

  const loadVersionDetail = useCallback(async (versionId) => {
    if (!versionId) return null;
    try {
      const [agrRes, tlRes] = await Promise.all([
        axiosInstance.get(ENDPOINTS.AGREEMENT_BY_ID(versionId)),
        axiosInstance.get(ENDPOINTS.AGREEMENT_TIMELINE(versionId)),
      ]);
      setAgreement(agrRes.data);
      setTimeline(tlRes.data);
      return agrRes.data;
    } catch {
      enqueueSnackbar('Failed to load version details', { variant: 'error' });
      return null;
    }
  }, [enqueueSnackbar]);

  const applyVersionPatch = useCallback((updated) => {
    if (!updated) return;
    setAgreement(updated);
    setVersions((prev) => prev.map((v) => (
      v.id === updated.id
        ? {
          ...v,
          approvalStatus: updated.approvalStatus,
          derivedStatus: updated.derivedStatus,
        }
        : v
    )));
    if (updated.approvalStatus === 'APPROVED') {
      setGroup((prev) => (prev ? { ...prev, currentVersionId: updated.id } : prev));
    }
  }, []);

  const load = async (preferredVersionId, { silent = false } = {}) => {
    if (!groupId || groupId === 'undefined') return;
    if (!silent) setLoading(true);
    try {
      const [groupRes, versionsRes] = await Promise.all([
        axiosInstance.get(ENDPOINTS.AGREEMENT_GROUP_BY_ID(groupId)),
        axiosInstance.get(ENDPOINTS.AGREEMENT_VERSIONS(groupId)),
      ]);
      setGroup(groupRes.data);
      setVersions(versionsRes.data);

      const pendingVersion = versionsRes.data.find((v) => v.approvalStatus === 'PENDING_APPROVAL');
      const nextVersionId = preferredVersionId
        || pendingVersion?.id
        || groupRes.data.currentVersionId
        || versionsRes.data[versionsRes.data.length - 1]?.id;

      setSelectedVersionId(nextVersionId);
      if (nextVersionId) {
        await loadVersionDetail(nextVersionId);
      } else {
        setAgreement(null);
        setTimeline([]);
      }
    } catch {
      enqueueSnackbar('Failed to load agreement', { variant: 'error' });
    } finally {
      if (!silent) setLoading(false);
    }
  };

  useEffect(() => {
    if (!groupId || groupId === 'undefined') return;
    setGroup(null);
    setVersions([]);
    setAgreement(null);
    setTimeline([]);
    setSelectedVersionId(null);
    load();
  }, [groupId]);

  useEffect(() => {
    if (!selectedVersionId) return;
    loadVersionDetail(selectedVersionId);
  }, [selectedVersionId, loadVersionDetail]);

  const refreshAfterMutation = async (updated, versionId = selectedVersionId) => {
    applyVersionPatch(updated);
    await load(versionId ?? updated?.id, { silent: true });
  };

  const handleSubmit = async () => {
    try {
      const updated = await dispatch(submitAgreementForApproval(selectedVersionId)).unwrap();
      enqueueSnackbar('Submitted for approval', { variant: 'success' });
      await refreshAfterMutation(updated, selectedVersionId);
    } catch (err) {
      enqueueSnackbar(typeof err === 'string' ? err : 'Submit failed', { variant: 'error' });
    }
  };

  const handleApprove = async () => {
    const versionId = selectedVersionId;
    try {
      const updated = await dispatch(approveAgreement({ agreementId: versionId })).unwrap();
      enqueueSnackbar('Agreement approved', { variant: 'success' });
      await refreshAfterMutation(updated, versionId);
      onActionComplete?.();
    } catch (err) {
      enqueueSnackbar(typeof err === 'string' ? err : 'Approval failed', { variant: 'error' });
    }
  };

  const handleReject = async () => {
    const versionId = selectedVersionId;
    try {
      const updated = await dispatch(rejectAgreement({ agreementId: versionId, remarks: rejectRemarks })).unwrap();
      enqueueSnackbar('Agreement rejected', { variant: 'success' });
      rejectModal.close();
      setRejectRemarks('');
      await refreshAfterMutation(updated, versionId);
      onActionComplete?.();
    } catch (err) {
      enqueueSnackbar(typeof err === 'string' ? err : 'Rejection failed', { variant: 'error' });
    }
  };

  const handleNewVersion = async () => {
    try {
      const { data } = await axiosInstance.post(ENDPOINTS.AGREEMENT_NEW_VERSION(groupId));
      await axiosInstance.put(ENDPOINTS.AGREEMENT_SUBMIT(data.id));
      const msg = agreement?.approvalStatus === 'REJECTED'
        ? `Revision V${data.versionNumber} submitted for approval`
        : `New version V${data.versionNumber} submitted for approval`;
      enqueueSnackbar(msg, { variant: 'success' });
      await load();
      setSelectedVersionId(data.id);
      onActionComplete?.();
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
  const canSubmit = isOwner && agreement?.approvalStatus === 'DRAFT' && hasRight(RIGHTS.AGREEMENT_EDIT);
  const canApprove = hasRight(RIGHTS.AGREEMENT_APPROVE) && agreement?.approvalStatus === 'PENDING_APPROVAL' && agreement?.ownerId !== user?.id;
  const canNewVersion = (isOwner || hasRight(RIGHTS.AGREEMENT_EDIT)) && agreement?.approvalStatus === 'APPROVED';
  const canRevise = isOwner && agreement?.approvalStatus === 'REJECTED' && hasRight(RIGHTS.AGREEMENT_EDIT);
  const canTerminate = hasRight(RIGHTS.AGREEMENT_EDIT) && agreement?.approvalStatus === 'APPROVED' && !agreement?.terminationDate;
  const canClone = hasRight(RIGHTS.AGREEMENT_CREATE) && agreement;
  const canTransfer = (isOwner || hasRight(RIGHTS.ADMIN_USERS)) && selectedVersionId;

  const latestVersion = versions.reduce(
    (max, v) => (!max || v.versionNumber > max.versionNumber ? v : max),
    null,
  );
  const isReadOnlyView = agreement && latestVersion && selectedVersionId !== latestVersion.id;

  const handleClone = async () => {
    if (!selectedVersionId) return;
    try {
      const clonedData = await fetchAgreementForClone(axiosInstance, ENDPOINTS, selectedVersionId);
      navigate(ROUTES.AGREEMENT_CREATE, { state: { clonedData } });
    } catch {
      enqueueSnackbar('Failed to prepare clone data', { variant: 'error' });
    }
  };

  const handleSelectVersion = (versionId) => {
    setSelectedVersionId(versionId);
    setActiveTab('details');
  };

  const ACTION_COLOR = { SUBMITTED: '#2196F3', APPROVED: BRAND.green, REJECTED: BRAND.red };

  if (!groupId || groupId === 'undefined') {
    return (
      <Typography color="text.secondary">Select an agreement to review</Typography>
    );
  }

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

      <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)} sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Tab label="Details" value="details" />
        <Tab label="Version History" value="history" icon={<History sx={{ fontSize: 18 }} />} iconPosition="start" />
      </Tabs>

      {isReadOnlyView && (
        <Alert severity="info" sx={{ mb: 2 }}>Viewing historical version — read-only.</Alert>
      )}

      {activeTab === 'history' ? (
        <Paper elevation={0} sx={{ p: 2, borderRadius: 2, border: '1px solid', borderColor: 'divider' }}>
          <List disablePadding>
            {versions.map((v) => (
              <Box key={v.id}>
                <ListItemButton
                  selected={selectedVersionId === v.id}
                  onClick={() => handleSelectVersion(v.id)}
                  sx={{ borderRadius: 1 }}
                >
                  <ListItemText
                    primary={
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Typography fontWeight={600}>V{v.versionNumber}</Typography>
                        <StatusBadge status={v.derivedStatus || v.approvalStatus} />
                        {group?.currentVersionId === v.id && (
                          <Chip label="Current" size="small" color="success" variant="outlined" />
                        )}
                      </Box>
                    }
                    secondary={`${v.startDate ? dayjs(v.startDate).format('DD MMM YYYY') : '—'} – ${v.expiryDate ? dayjs(v.expiryDate).format('DD MMM YYYY') : '—'} · ${v.ownerName || '—'}`}
                  />
                </ListItemButton>
                <Divider />
              </Box>
            ))}
          </List>
        </Paper>
      ) : (
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
                {!isReadOnlyView && canSubmit && (
                  <Button variant="contained" fullWidth onClick={handleSubmit} sx={{ bgcolor: BRAND.red }}>
                    Submit for Approval
                  </Button>
                )}
                {!isReadOnlyView && canApprove && (
                  <>
                    <Button variant="contained" fullWidth onClick={handleApprove} sx={{ bgcolor: BRAND.green }}>
                      ✓ Approve
                    </Button>
                    <Button variant="outlined" color="error" fullWidth onClick={rejectModal.open}>
                      ✗ Reject
                    </Button>
                  </>
                )}
                {!isReadOnlyView && canNewVersion && (
                  <Button variant="outlined" fullWidth startIcon={<Edit />}
                    onClick={() => navigate(`/agreements/${selectedVersionId}/edit`)}>
                    Edit (New Version)
                  </Button>
                )}
                {!isReadOnlyView && canRevise && (
                  <Button variant="contained" fullWidth startIcon={<Edit />}
                    onClick={() => navigate(`/agreements/${selectedVersionId}/edit`)}
                    sx={{ bgcolor: BRAND.red }}>
                    Revise & Resubmit
                  </Button>
                )}
                {!isReadOnlyView && canTerminate && (
                  <Button variant="outlined" color="error" fullWidth startIcon={<PowerSettingsNew />} onClick={terminateModal.open}>
                    Terminate
                  </Button>
                )}
                {canClone && (
                  <Button variant="outlined" fullWidth startIcon={<ContentCopy />} onClick={handleClone}>
                    Clone (Copy Products)
                  </Button>
                )}
                {canTransfer && !isReadOnlyView && (
                  <Button variant="outlined" fullWidth startIcon={<SwapHoriz />} onClick={() => setTransferOpen(true)}>
                    Transfer Ownership
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
      )}

      <TransferOwnershipModal
        open={transferOpen}
        onClose={() => setTransferOpen(false)}
        agreementId={selectedVersionId}
        agreementLabel={group?.agreementNumber}
        onSuccess={() => {
          enqueueSnackbar('Ownership transferred', { variant: 'success' });
          load();
          onActionComplete?.();
        }}
      />

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
