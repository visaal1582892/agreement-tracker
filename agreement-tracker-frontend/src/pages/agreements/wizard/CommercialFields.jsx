import { useCallback, useEffect, useState } from 'react';
import {
  Box, Typography, Grid, RadioGroup, FormControlLabel, Radio,
  TextField, Button,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Paper, IconButton, Alert, CircularProgress,
} from '@mui/material';
import { Add, Delete, Edit, TableChart } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import { useModal } from '../../../hooks/useModal';
import {
  createPurchaseSlab,
  deletePurchaseSlab,
  fetchPurchaseSlabs,
  formatSlabLabel,
  toSlabPayload,
  updatePurchaseSlab,
  validateSlabAgainstExisting,
} from '../../../api/commercialApi';
import CommercialValueInput from '../../../components/forms/CommercialValueInput';
import CommercialsUploadModal from './CommercialsUploadModal';

const EMPTY_DRAFT = {
  fromValue: '',
  toValue: '',
  valueType: 'PERCENTAGE',
  commercialValue: '',
};

export default function CommercialFields({
  commercials,
  onUpdate,
  serverAgreementId,
  startDate,
  expiryDate,
}) {
  const { enqueueSnackbar } = useSnackbar();
  const uploadModal = useModal();
  const [draftSlab, setDraftSlab] = useState(EMPTY_DRAFT);
  const [editingSlabId, setEditingSlabId] = useState(null);
  const [savedSlabs, setSavedSlabs] = useState([]);
  const [loadingSlabs, setLoadingSlabs] = useState(false);
  const [savingSlab, setSavingSlab] = useState(false);

  const selectedFrequencies = commercials.selectedFrequencies || [];

  const loadSlabs = useCallback(async () => {
    if (!serverAgreementId || commercials.commercialStructure !== 'SLAB') {
      setSavedSlabs([]);
      return;
    }
    setLoadingSlabs(true);
    try {
      const data = await fetchPurchaseSlabs(serverAgreementId);
      setSavedSlabs(data);
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Failed to load slabs', { variant: 'error' });
    } finally {
      setLoadingSlabs(false);
    }
  }, [serverAgreementId, commercials.commercialStructure, enqueueSnackbar]);

  useEffect(() => {
    loadSlabs();
  }, [loadSlabs]);

  const updateDraft = (field, value) => {
    setDraftSlab((prev) => ({ ...prev, [field]: value }));
  };

  const resetDraft = () => {
    setDraftSlab(EMPTY_DRAFT);
    setEditingSlabId(null);
  };

  const validateDraftSlab = () => {
    if (draftSlab.fromValue === '' || draftSlab.toValue === '' || draftSlab.commercialValue === '') {
      enqueueSnackbar('Complete all slab fields before saving', { variant: 'warning' });
      return false;
    }
    if (Number(draftSlab.fromValue) < 0) {
      enqueueSnackbar('From value must be greater than or equal to 0', { variant: 'warning' });
      return false;
    }
    if (Number(draftSlab.toValue) <= Number(draftSlab.fromValue)) {
      enqueueSnackbar('To value must be greater than from value', { variant: 'warning' });
      return false;
    }
    const payload = toSlabPayload(draftSlab);
    const conflict = validateSlabAgainstExisting(savedSlabs, payload, editingSlabId);
    if (conflict) {
      enqueueSnackbar(conflict, { variant: 'warning' });
      return false;
    }
    return true;
  };

  const handleSaveSlab = async () => {
    if (!serverAgreementId) {
      enqueueSnackbar('Save the agreement draft first before adding slabs', { variant: 'warning' });
      return;
    }
    if (!validateDraftSlab()) return;

    setSavingSlab(true);
    try {
      const payload = toSlabPayload(draftSlab);
      if (editingSlabId) {
        await updatePurchaseSlab(serverAgreementId, editingSlabId, payload);
        enqueueSnackbar('Slab updated', { variant: 'success' });
      } else {
        await createPurchaseSlab(serverAgreementId, payload);
        enqueueSnackbar('Slab added', { variant: 'success' });
      }
      resetDraft();
      await loadSlabs();
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Failed to save slab', { variant: 'error' });
    } finally {
      setSavingSlab(false);
    }
  };

  const handleEditSlab = (slab) => {
    setEditingSlabId(slab.id);
    setDraftSlab({
      fromValue: String(slab.fromValue),
      toValue: String(slab.toValue),
      valueType: slab.valueType,
      commercialValue: String(slab.commercialValue),
    });
  };

  const handleDeleteSlab = async (slabId) => {
    if (!serverAgreementId) return;
    try {
      await deletePurchaseSlab(serverAgreementId, slabId);
      if (editingSlabId === slabId) {
        resetDraft();
      }
      enqueueSnackbar('Slab deleted', { variant: 'success' });
      await loadSlabs();
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Failed to delete slab', { variant: 'error' });
    }
  };

  const openUploadModal = () => {
    if (!serverAgreementId) {
      enqueueSnackbar('Save the agreement draft first', { variant: 'warning' });
      return;
    }
    if (!savedSlabs.length) {
      enqueueSnackbar('Add at least one purchase slab first', { variant: 'warning' });
      return;
    }
    uploadModal.open();
  };

  return (
    <Box>
      <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 1.5 }}>
        Commercial Structure *
      </Typography>

      <Box sx={{ mb: 2 }}>
        <RadioGroup
          row
          value={commercials.commercialStructure}
          onChange={(e) => onUpdate({ commercialStructure: e.target.value })}
        >
          <FormControlLabel value="FLAT" control={<Radio size="small" />} label="Flat" />
          <FormControlLabel value="SLAB" control={<Radio size="small" />} label="Slab-based" />
        </RadioGroup>
      </Box>

      {commercials.commercialStructure === 'FLAT' && (
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <CommercialValueInput
              label="Commercial Value"
              required
              value={commercials.commercialValue}
              onChangeValue={(v) => onUpdate({ commercialValue: v })}
              type={commercials.valueType || 'FIXED'}
              onChangeType={(t) => onUpdate({ valueType: t })}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Calculation Formula (optional)"
              fullWidth
              size="small"
              value={commercials.calculationFormula || ''}
              onChange={(e) => onUpdate({ calculationFormula: e.target.value })}
              placeholder="e.g. (Sales × 2%)"
            />
          </Grid>
        </Grid>
      )}

      {commercials.commercialStructure === 'SLAB' && (
        <Box>
          <Typography variant="subtitle2" gutterBottom>Saved Slabs</Typography>
          {loadingSlabs ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
              <CircularProgress size={24} />
            </Box>
          ) : savedSlabs.length > 0 ? (
            <TableContainer component={Paper} elevation={0} sx={{ mb: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 600 }}>Slab Rule</TableCell>
                    <TableCell width={100} />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {savedSlabs.map((slab) => (
                    <TableRow key={slab.id} selected={editingSlabId === slab.id}>
                      <TableCell>{formatSlabLabel(slab)}</TableCell>
                      <TableCell>
                        <IconButton size="small" onClick={() => handleEditSlab(slab)} aria-label="Edit slab">
                          <Edit fontSize="small" />
                        </IconButton>
                        <IconButton size="small" color="error" onClick={() => handleDeleteSlab(slab.id)} aria-label="Delete slab">
                          <Delete fontSize="small" />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            <Alert severity="info" sx={{ mb: 2 }}>Add and save at least one slab rule.</Alert>
          )}

          <Typography variant="subtitle2" gutterBottom>
            {editingSlabId ? 'Edit Slab' : 'Add Slab'}
          </Typography>
          <Grid container spacing={2} sx={{ mb: 2 }}>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                label="From Value"
                type="number"
                fullWidth
                size="small"
                value={draftSlab.fromValue}
                onChange={(e) => updateDraft('fromValue', e.target.value)}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                label="To Value"
                type="number"
                fullWidth
                size="small"
                value={draftSlab.toValue}
                onChange={(e) => updateDraft('toValue', e.target.value)}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <CommercialValueInput
                label="Commercial Value"
                value={draftSlab.commercialValue}
                onChangeValue={(v) => updateDraft('commercialValue', v)}
                type={draftSlab.valueType}
                onChangeType={(t) => updateDraft('valueType', t)}
              />
            </Grid>
          </Grid>

          <Box sx={{ display: 'flex', gap: 1, mb: 3 }}>
            <Button
              size="small"
              startIcon={savingSlab ? <CircularProgress size={16} /> : <Add />}
              onClick={handleSaveSlab}
              disabled={savingSlab}
            >
              {editingSlabId ? 'Update Slab' : 'Add Slab'}
            </Button>
            {editingSlabId && (
              <Button size="small" variant="outlined" onClick={resetDraft} disabled={savingSlab}>
                Cancel Edit
              </Button>
            )}
          </Box>

          <Button
            variant="contained"
            startIcon={<TableChart />}
            onClick={openUploadModal}
            disabled={!savedSlabs.length}
          >
            Upload / View Commercials
          </Button>

          {!serverAgreementId && (
            <Alert severity="warning" sx={{ mt: 2 }}>
              Save the agreement draft before adding slabs or uploading commercial targets.
            </Alert>
          )}

          <CommercialsUploadModal
            open={uploadModal.isOpen}
            onClose={uploadModal.close}
            agreementId={serverAgreementId}
            slabs={savedSlabs}
            startDate={startDate}
            expiryDate={expiryDate}
            selectedFrequencies={selectedFrequencies}
            onFrequenciesChange={(value) => onUpdate({ selectedFrequencies: value })}
          />
        </Box>
      )}
    </Box>
  );
}
