import { useCallback, useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField, Typography, Alert,
} from '@mui/material';
import axiosInstance from '../api/axiosInstance';
import { ENDPOINTS } from '../config/endpoints';
import { BRAND } from '../config/theme';
import { RIGHTS } from '../config/rights';

export function GroupDeleteDialogs({
  modal,
  group,
  reason,
  onReasonChange,
  onClose,
  onConfirm,
  submitting,
}) {
  if (!modal || !group) return null;

  const isDrafts = modal === 'drafts';
  const isApproval = modal === 'approval';

  return (
    <Dialog open onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle fontWeight={700}>
        {isApproval ? 'Submit Group Deletion for Approval' : 'Delete Group'}
      </DialogTitle>
      <DialogContent>
        {isDrafts && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            This group contains drafts. Deleting this group will permanently delete all associated
            drafts. This action cannot be undone.
          </Alert>
        )}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Group: <strong>{group.name}</strong>
        </Typography>
        <TextField
          label="Reason for Deletion"
          required
          multiline
          rows={3}
          fullWidth
          value={reason}
          onChange={(e) => onReasonChange(e.target.value)}
          placeholder="Explain why this group should be deleted…"
        />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} variant="outlined" disabled={submitting}>Cancel</Button>
        <Button
          onClick={onConfirm}
          variant="contained"
          color="error"
          disabled={submitting || !reason.trim()}
          sx={!isApproval ? { bgcolor: BRAND.red, '&:hover': { bgcolor: BRAND.redDark } } : undefined}
        >
          {isApproval ? 'Submit for Approval' : 'Delete Group'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export function useGroupDeletion({ onSuccess }) {
  const [modal, setModal] = useState(null);
  const [group, setGroup] = useState(null);
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [statusError, setStatusError] = useState(null);

  const closeModal = useCallback(() => {
    setModal(null);
    setGroup(null);
    setReason('');
  }, []);

  const startDelete = useCallback(async (targetGroup) => {
    setStatusError(null);
    try {
      const { data } = await axiosInstance.get(
        ENDPOINTS.COMPANY_AGREEMENT_GROUP_DELETION_STATUS(targetGroup.id),
      );
      switch (data.status) {
        case 'HAS_ACTIVE':
          setStatusError('Cannot delete group: Active agreements exist.');
          return { blocked: true };
        case 'ONLY_DRAFTS':
          setGroup(targetGroup);
          setReason('');
          setModal('drafts');
          return { blocked: false };
        case 'REQUIRES_APPROVAL':
          setGroup(targetGroup);
          setReason('');
          setModal('approval');
          return { blocked: false };
        case 'READY':
          setGroup(targetGroup);
          setReason('');
          setModal('ready');
          return { blocked: false };
        default:
          setStatusError('Unable to determine deletion status for this group.');
          return { blocked: true };
      }
    } catch (err) {
      setStatusError(err.response?.data?.message || 'Failed to check deletion status');
      return { blocked: true };
    }
  }, []);

  const confirmDelete = useCallback(async () => {
    if (!group?.id || !reason.trim()) return;
    setSubmitting(true);
    try {
      if (modal === 'approval') {
        await axiosInstance.post(
          ENDPOINTS.COMPANY_AGREEMENT_GROUP_DELETION_REQUEST(group.id),
          { reason: reason.trim() },
        );
      } else {
        await axiosInstance.delete(ENDPOINTS.COMPANY_AGREEMENT_GROUP_BY_ID(group.id), {
          params: { reason: reason.trim() },
        });
      }
      closeModal();
      onSuccess?.(modal === 'approval'
        ? 'Group deletion submitted for approval'
        : 'Group deleted successfully');
    } catch (err) {
      setStatusError(err.response?.data?.message || 'Deletion failed');
    } finally {
      setSubmitting(false);
    }
  }, [group, reason, modal, closeModal, onSuccess]);

  return {
    modal,
    group,
    reason,
    setReason,
    submitting,
    statusError,
    setStatusError,
    startDelete,
    closeModal,
    confirmDelete,
  };
}

export function canDeleteGroup(row, user, hasRight, isApprover) {
  if (hasRight(RIGHTS.ADMIN_USERS)) return true;
  if (isApprover) return true;
  if (row.createdByUserId === user?.id) return true;
  return false;
}
