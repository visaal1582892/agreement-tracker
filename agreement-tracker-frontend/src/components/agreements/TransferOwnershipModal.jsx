import { useCallback, useEffect, useState } from 'react';
import {
  Alert, Dialog, DialogTitle, DialogContent, DialogActions, Button, Typography,
} from '@mui/material';
import { useSnackbar } from 'notistack';
import axiosInstance from '../../api/axiosInstance';
import { ENDPOINTS } from '../../config/endpoints';
import SearchableSelect from '../forms/SearchableSelect';

export default function TransferOwnershipModal({
  open,
  onClose,
  agreementId,
  agreementLabel,
  onSuccess,
}) {
  const { enqueueSnackbar } = useSnackbar();
  const [users, setUsers] = useState([]);
  const [selectedUser, setSelectedUser] = useState(null);
  const [loading, setLoading] = useState(false);
  const [searching, setSearching] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const searchUsers = useCallback(async (query) => {
    setSearching(true);
    try {
      const { data } = await axiosInstance.get(ENDPOINTS.USER_LOOKUP, {
        params: { q: query?.trim() || '' },
      });
      setUsers(Array.isArray(data) ? data : []);
    } catch {
      setUsers([]);
    } finally {
      setSearching(false);
    }
  }, []);

  useEffect(() => {
    if (open) {
      setSelectedUser(null);
      searchUsers('');
    }
  }, [open, searchUsers]);

  const handleTransfer = async () => {
    if (!agreementId || !selectedUser?.id) return;
    setSubmitting(true);
    try {
      await axiosInstance.put(ENDPOINTS.AGREEMENT_TRANSFER(agreementId), {
        newOwnerUserId: selectedUser.id,
      });
      onSuccess?.();
      onClose();
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Transfer failed', { variant: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle fontWeight={700}>Transfer Ownership</DialogTitle>
      <DialogContent>
        <Alert severity="warning" sx={{ mb: 2 }}>
          You will lose access to edit this agreement once transferred.
        </Alert>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Reassign {agreementLabel || 'this agreement'} to a new Account Manager.
        </Typography>
        <SearchableSelect
          label="New Owner"
          placeholder="Search users…"
          options={users}
          value={selectedUser}
          onChange={setSelectedUser}
          onSearch={searchUsers}
          loading={searching}
          getOptionLabel={(u) => u?.fullName || u?.username || ''}
          isOptionEqualToValue={(a, b) => a?.id === b?.id}
          required
        />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} variant="outlined">Cancel</Button>
        <Button
          onClick={handleTransfer}
          variant="contained"
          disabled={!selectedUser?.id || submitting}
        >
          {submitting ? 'Transferring…' : 'Transfer'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
