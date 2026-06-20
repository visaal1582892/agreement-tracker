import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Box, Typography, Grid, TextField, Button, Checkbox, FormControlLabel,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Paper, IconButton, Alert, CircularProgress, FormControl, InputLabel, Select, MenuItem,
} from '@mui/material';
import { Add, Delete, Edit } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import {
  createSlab,
  deleteSlab,
  fetchSlabs,
  updateSlab,
  validateSlabAgainstExisting,
} from '../../../api/commercialApi';
import { ensureDraftVersionForCommercial } from '../../../utils/commercialDraftOrchestration';
import CollapsibleSection from '../../../components/wizard/CollapsibleSection';
import WizardFieldAnchor from '../../../components/wizard/WizardFieldAnchor';
import CommercialValueInput from '../../../components/forms/CommercialValueInput';
import {
  PAYOUT_FREQUENCY,
  PAYOUT_FREQUENCY_OPTIONS,
  deriveHybridFlags,
  resolveCommercialStructure,
} from '../../../constants/commercialStructure';

const EMPTY_LINEAR_SLAB = {
  threshold: '',
  valueType: 'PERCENTAGE',
  commercialValue: '',
  payoutFrequency: PAYOUT_FREQUENCY.MONTHLY,
};

function toLinearSlabPayload(row, fromValue) {
  return {
    fromValue,
    toValue: Number(row.threshold),
    valueType: row.valueType,
    commercialValue: Number(row.commercialValue),
    payoutFrequency: row.payoutFrequency,
    slabType: 'PURCHASE',
  };
}

function buildLinearFromValues(rows) {
  const sorted = [...rows].sort((a, b) => Number(a.threshold) - Number(b.threshold));
  let previousTo = 0;
  return sorted.map((row) => {
    const payload = toLinearSlabPayload(row, previousTo);
    previousTo = payload.toValue;
    return payload;
  });
}

export default function HybridCommercialFields({
  commercials,
  onUpdate,
  serverAgreementId,
  sourceAgreement,
  versionSourceId,
  buildVersionedEditPayload,
  onDraftVersionCreated,
  lockOneTimeFrequency = false,
  fieldErrors = {},
}) {
  const { enqueueSnackbar } = useSnackbar();
  const hybridFlags = deriveHybridFlags(commercials.commercialStructure);
  const enableFlatBaseline = commercials.enableFlatBaseline ?? hybridFlags.enableFlatBaseline;
  const enableSlabIncentives = commercials.enableSlabIncentives ?? hybridFlags.enableSlabIncentives;

  const [slabs, setSlabs] = useState([]);
  const [loadingSlabs, setLoadingSlabs] = useState(false);
  const [savingSlab, setSavingSlab] = useState(false);
  const [draftSlab, setDraftSlab] = useState(EMPTY_LINEAR_SLAB);
  const [editingSlabId, setEditingSlabId] = useState(null);
  const [commercialVersionId, setCommercialVersionId] = useState(serverAgreementId);
  const correctedGhostSlabRef = useRef(false);

  const frequencyOptions = useMemo(
    () => (lockOneTimeFrequency
      ? PAYOUT_FREQUENCY_OPTIONS.filter((option) => option.value === PAYOUT_FREQUENCY.ONE_TIME)
      : PAYOUT_FREQUENCY_OPTIONS),
    [lockOneTimeFrequency],
  );

  useEffect(() => {
    setCommercialVersionId(serverAgreementId);
  }, [serverAgreementId]);

  useEffect(() => {
    if (lockOneTimeFrequency) {
      if (commercials.flatBaselineFrequency !== PAYOUT_FREQUENCY.ONE_TIME) {
        onUpdate({ flatBaselineFrequency: PAYOUT_FREQUENCY.ONE_TIME });
      }
    }
  }, [lockOneTimeFrequency, commercials.flatBaselineFrequency, onUpdate]);

  const syncStructureFlags = useCallback((nextFlat, nextSlab, patch = {}) => {
    onUpdate({
      enableFlatBaseline: nextFlat,
      enableSlabIncentives: nextSlab,
      commercialStructure: resolveCommercialStructure(nextFlat, nextSlab),
      ...patch,
    });
  }, [onUpdate]);

  const resolveMutationVersionId = useCallback(async () => {
    if (!buildVersionedEditPayload || !sourceAgreement) {
      return serverAgreementId ?? commercialVersionId;
    }
    const { versionId, draftCreated, draftResponse } = await ensureDraftVersionForCommercial({
      serverAgreementId: serverAgreementId ?? commercialVersionId,
      sourceAgreement,
      versionSourceId,
      buildVersionedEditPayload,
    });
    if (draftCreated && draftResponse) {
      onDraftVersionCreated?.(draftResponse);
    }
    if (versionId) {
      setCommercialVersionId(versionId);
    }
    return versionId ?? serverAgreementId ?? commercialVersionId;
  }, [
    buildVersionedEditPayload,
    commercialVersionId,
    onDraftVersionCreated,
    serverAgreementId,
    sourceAgreement,
    versionSourceId,
  ]);

  const loadSlabs = useCallback(async () => {
    if (!serverAgreementId || !enableSlabIncentives) {
      setSlabs([]);
      return;
    }
    setLoadingSlabs(true);
    try {
      const data = await fetchSlabs(serverAgreementId);
      setSlabs(data);
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Failed to load slabs', { variant: 'error' });
    } finally {
      setLoadingSlabs(false);
    }
  }, [serverAgreementId, enableSlabIncentives, enqueueSnackbar]);

  useEffect(() => {
    loadSlabs();
  }, [loadSlabs]);

  useEffect(() => {
    if (loadingSlabs || correctedGhostSlabRef.current) return;
    if (slabs.length > 0) return;
    const structure = commercials.commercialStructure;
    const ghostSlabState = structure === 'HYBRID' || structure === 'SLAB' || enableSlabIncentives;
    if (!ghostSlabState) return;
    correctedGhostSlabRef.current = true;
    syncStructureFlags(true, false);
  }, [
    loadingSlabs,
    slabs.length,
    commercials.commercialStructure,
    enableSlabIncentives,
    syncStructureFlags,
  ]);

  const updateDraft = (field, value) => {
    setDraftSlab((prev) => ({ ...prev, [field]: value }));
  };

  const resetDraft = () => {
    setDraftSlab({
      ...EMPTY_LINEAR_SLAB,
      payoutFrequency: lockOneTimeFrequency ? PAYOUT_FREQUENCY.ONE_TIME : PAYOUT_FREQUENCY.MONTHLY,
    });
    setEditingSlabId(null);
  };

  const validateDraftSlab = () => {
    if (!draftSlab.threshold || draftSlab.commercialValue === '') {
      enqueueSnackbar('Complete cap threshold and payout value before saving', { variant: 'warning' });
      return false;
    }
    if (Number(draftSlab.threshold) <= 0) {
      enqueueSnackbar('Cap / target threshold must be greater than 0', { variant: 'warning' });
      return false;
    }
    const peers = slabs.filter((slab) => slab.id !== editingSlabId);
    const sorted = [...peers.map((slab) => ({
      threshold: String(slab.toValue),
      valueType: slab.valueType,
      commercialValue: String(slab.commercialValue),
      payoutFrequency: slab.payoutFrequency,
    })), draftSlab].sort((a, b) => Number(a.threshold) - Number(b.threshold));

    const payloads = buildLinearFromValues(sorted);
    const currentPayload = payloads.find((payload) => payload.toValue === Number(draftSlab.threshold));
    const conflict = validateSlabAgainstExisting(
      peers,
      currentPayload,
      editingSlabId,
    );
    if (conflict) {
      enqueueSnackbar(conflict, { variant: 'warning' });
      return false;
    }
    return true;
  };

  const handleSaveSlab = async () => {
    if (!validateDraftSlab()) return;
    setSavingSlab(true);
    try {
      const versionId = await resolveMutationVersionId();
      if (!versionId) {
        enqueueSnackbar('Save the agreement draft first before adding slabs', { variant: 'warning' });
        return;
      }
      const payload = toLinearSlabPayload(
        draftSlab,
        editingSlabId
          ? Number(slabs.find((slab) => slab.id === editingSlabId)?.fromValue ?? 0)
          : Number(slabs.length ? Math.max(...slabs.map((slab) => Number(slab.toValue))) : 0),
      );
      if (payload.toValue <= payload.fromValue) {
        enqueueSnackbar('Cap threshold must exceed the previous slab boundary', { variant: 'warning' });
        return;
      }
      if (editingSlabId) {
        await updateSlab(versionId, editingSlabId, payload);
        enqueueSnackbar('Slab updated', { variant: 'success' });
      } else {
        await createSlab(versionId, payload);
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
      threshold: String(slab.toValue),
      valueType: slab.valueType,
      commercialValue: String(slab.commercialValue),
      payoutFrequency: lockOneTimeFrequency
        ? PAYOUT_FREQUENCY.ONE_TIME
        : (slab.payoutFrequency || PAYOUT_FREQUENCY.MONTHLY),
    });
  };

  const handleDeleteSlab = async (slabId) => {
    try {
      const versionId = await resolveMutationVersionId();
      if (!versionId) return;
      await deleteSlab(versionId, slabId);
      if (editingSlabId === slabId) resetDraft();
      enqueueSnackbar('Slab deleted', { variant: 'success' });
      await loadSlabs();
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Failed to delete slab', { variant: 'error' });
    }
  };

  const baseHasError = Boolean(
    fieldErrors.commercialComponent
    || fieldErrors.commercialValue
    || fieldErrors.flatBaselineFrequency,
  );
  const slabsHasError = Boolean(fieldErrors.slabs || fieldErrors.commercialComponent);

  return (
    <Box>
      <CollapsibleSection
        title="Base Commercials"
        description={
          lockOneTimeFrequency
            ? 'Flat baseline payout value, type, and frequency. QPS agreements lock payout frequency to One-Time.'
            : 'Flat baseline payout value, type, and frequency.'
        }
        forceExpand={baseHasError}
        hasError={baseHasError}
      >
        {fieldErrors.commercialComponent && (
          <Alert severity="error" sx={{ mb: 2 }} data-wizard-field="commercialComponent" className="has-error">
            {fieldErrors.commercialComponent}
          </Alert>
        )}
        <FormControlLabel
          control={(
            <Checkbox
              checked={enableFlatBaseline}
              onChange={(e) => syncStructureFlags(e.target.checked, enableSlabIncentives)}
            />
          )}
          label="Enable Flat Baseline Payout"
          sx={{ mb: 2 }}
        />

        {!enableFlatBaseline && !enableSlabIncentives && (
          <Alert severity="info" sx={{ mb: 2 }}>
            Enable flat baseline or slab incentives to continue.
          </Alert>
        )}

        {enableFlatBaseline && (
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6 }}>
              <WizardFieldAnchor field="commercialValue" error={fieldErrors.commercialValue}>
                <CommercialValueInput
                  label="Baseline Value"
                  required
                  value={commercials.commercialValue}
                  onChangeValue={(value) => onUpdate({ commercialValue: value })}
                  type={commercials.valueType || commercials.flatValueType || 'FIXED'}
                  onChangeType={(type) => onUpdate({ valueType: type, flatValueType: type })}
                  error={Boolean(fieldErrors.commercialValue)}
                  helperText={fieldErrors.commercialValue ? '' : undefined}
                />
              </WizardFieldAnchor>
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <WizardFieldAnchor field="flatBaselineFrequency" error={fieldErrors.flatBaselineFrequency}>
                <FormControl fullWidth size="small" required error={Boolean(fieldErrors.flatBaselineFrequency)}>
                  <InputLabel>Payout Frequency</InputLabel>
                  <Select
                  value={commercials.flatBaselineFrequency || (lockOneTimeFrequency ? PAYOUT_FREQUENCY.ONE_TIME : PAYOUT_FREQUENCY.MONTHLY)}
                  label="Payout Frequency *"
                  onChange={(e) => onUpdate({ flatBaselineFrequency: e.target.value })}
                  disabled={lockOneTimeFrequency}
                >
                  {frequencyOptions.map((option) => (
                    <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
                  ))}
                </Select>
              </FormControl>
              </WizardFieldAnchor>
            </Grid>
          </Grid>
        )}
      </CollapsibleSection>

      <CollapsibleSection
        title="Performance Targets (Slabs)"
        description="1D slab table for tiered incentives above baseline performance."
        forceExpand={slabsHasError}
        hasError={slabsHasError}
      >
        <WizardFieldAnchor field="slabs" error={fieldErrors.slabs}>
        <FormControlLabel
          control={(
            <Checkbox
              checked={enableSlabIncentives}
              onChange={(e) => syncStructureFlags(enableFlatBaseline, e.target.checked)}
            />
          )}
          label="Enable Slab-Based Incentives"
          sx={{ mb: 2 }}
        />

        {enableSlabIncentives && (
          <>
            {loadingSlabs ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
                <CircularProgress size={24} />
              </Box>
            ) : (
              <TableContainer component={Paper} elevation={0} sx={{ mb: 2, border: '1px solid', borderColor: 'divider' }}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 600 }}>Cap / Target</TableCell>
                      <TableCell sx={{ fontWeight: 600 }}>Commercial Type</TableCell>
                      <TableCell sx={{ fontWeight: 600 }}>Payout Value</TableCell>
                      <TableCell sx={{ fontWeight: 600 }}>Frequency</TableCell>
                      <TableCell width={100} />
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {slabs.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={5}>
                          <Typography variant="body2" color="text.secondary">No slabs added yet.</Typography>
                        </TableCell>
                      </TableRow>
                    ) : (
                      slabs.map((slab) => (
                        <TableRow key={slab.id} selected={editingSlabId === slab.id}>
                          <TableCell>{slab.toValue}</TableCell>
                          <TableCell>{slab.valueType === 'PERCENTAGE' ? 'Percentage %' : 'Fixed ₹'}</TableCell>
                          <TableCell>{slab.commercialValue}</TableCell>
                          <TableCell>
                            {PAYOUT_FREQUENCY_OPTIONS.find((option) => option.value === slab.payoutFrequency)?.label
                              || slab.payoutFrequency
                              || '—'}
                          </TableCell>
                          <TableCell>
                            <IconButton size="small" onClick={() => handleEditSlab(slab)} aria-label="Edit slab">
                              <Edit fontSize="small" />
                            </IconButton>
                            <IconButton size="small" color="error" onClick={() => handleDeleteSlab(slab.id)} aria-label="Delete slab">
                              <Delete fontSize="small" />
                            </IconButton>
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            )}

            <Grid container spacing={2} sx={{ mb: 2 }}>
              <Grid size={{ xs: 12, sm: 3 }}>
                <TextField
                  label="Cap / Target Threshold *"
                  type="number"
                  fullWidth
                  size="small"
                  value={draftSlab.threshold}
                  onChange={(e) => updateDraft('threshold', e.target.value)}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 3 }}>
                <CommercialValueInput
                  label="Payout Value *"
                  value={draftSlab.commercialValue}
                  onChangeValue={(value) => updateDraft('commercialValue', value)}
                  type={draftSlab.valueType}
                  onChangeType={(type) => updateDraft('valueType', type)}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 3 }}>
                <FormControl fullWidth size="small">
                  <InputLabel>Slab Payout Frequency</InputLabel>
                  <Select
                    value={draftSlab.payoutFrequency}
                    label="Slab Payout Frequency"
                    onChange={(e) => updateDraft('payoutFrequency', e.target.value)}
                    disabled={lockOneTimeFrequency}
                  >
                    {frequencyOptions.map((option) => (
                      <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
            </Grid>

            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button
                size="small"
                startIcon={savingSlab ? <CircularProgress size={16} /> : <Add />}
                onClick={handleSaveSlab}
                disabled={savingSlab}
              >
                {editingSlabId ? 'Update Slab Row' : 'Add Slab Row'}
              </Button>
              {editingSlabId && (
                <Button size="small" variant="outlined" onClick={resetDraft} disabled={savingSlab}>
                  Cancel Edit
                </Button>
              )}
            </Box>

            {!serverAgreementId && (
              <Alert severity="warning" sx={{ mt: 2 }}>
                Save the agreement draft before adding slab rows.
              </Alert>
            )}
          </>
        )}
        </WizardFieldAnchor>
      </CollapsibleSection>
    </Box>
  );
}
