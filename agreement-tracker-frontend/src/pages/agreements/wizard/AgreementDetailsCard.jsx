import { useEffect, useRef, useState } from 'react';
import {
  Autocomplete, Box, Typography, Grid, FormControl, InputLabel, Select, MenuItem,
  TextField, Button, Chip, FormHelperText, IconButton, alpha, Paper, Alert,
  CircularProgress,
} from '@mui/material';
import { UploadFile, Delete, Edit, Lock } from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import axiosInstance from '../../../api/axiosInstance';
import { ENDPOINTS } from '../../../config/endpoints';
import { BRAND } from '../../../config/theme';
import DateRangeFields from '../../../components/forms/DateRangeFields';
import {
  buildContractDetailsSnapshot,
  hasPersistedContractDetails,
  validateContractDetailsFields,
} from '../../../utils/agreementWizardUtils';
import CommercialFields from './CommercialFields';

const DOCUMENT_TYPES = ['AGREEMENT', 'SUPPORTING_DOC', 'EMAIL', 'OTHER'];

const notesFieldSx = {
  '& .MuiOutlinedInput-root': {
    '& fieldset': { borderColor: BRAND.borderLight },
    '&:hover fieldset': { borderColor: '#94A3B8' },
    '&.Mui-focused fieldset': { borderColor: BRAND.red, borderWidth: 2 },
  },
};

function formatDateLabel(value) {
  if (!value) return '—';
  const raw = value.split('T')[0];
  const [year, month, day] = raw.split('-');
  if (!year || !month || !day) return raw;
  return `${day}/${month}/${year}`;
}

export default function AgreementDetailsCard({
  cardId,
  agreement,
  onUpdateDetails,
  onUpdateCommercials,
  documentError,
  onClearDocumentError,
  serverAgreementId,
  initialContractSnapshot,
  onSaveContractDetails,
}) {
  const { enqueueSnackbar } = useSnackbar();
  const [incomeTypes, setIncomeTypes] = useState([]);
  const [agreementTypes, setAgreementTypes] = useState([]);
  const [stateOptions, setStateOptions] = useState([]);
  const [dragOver, setDragOver] = useState(false);
  const [savingContract, setSavingContract] = useState(false);
  const [savedSnapshot, setSavedSnapshot] = useState(null);
  const [isLocked, setIsLocked] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const fileInputRef = useRef(null);
  const details = agreement.details;

  const fieldsDisabled = isLocked && !isEditing;
  const commercialsEnabled = isLocked && !isEditing && savedSnapshot;

  useEffect(() => {
    axiosInstance.get(ENDPOINTS.INCOME_TYPES).then(({ data }) => setIncomeTypes(data));
    axiosInstance.get(ENDPOINTS.AGREEMENT_TYPES).then(({ data }) => setAgreementTypes(data));
    axiosInstance.get(ENDPOINTS.STATES).then(({ data }) => setStateOptions(Array.isArray(data) ? data : []));
  }, []);

  useEffect(() => {
    if (hasPersistedContractDetails(initialContractSnapshot)) {
      setSavedSnapshot(buildContractDetailsSnapshot(initialContractSnapshot));
      setIsLocked(true);
      setIsEditing(false);
      return;
    }
    setSavedSnapshot(null);
    setIsLocked(false);
    setIsEditing(false);
  }, [serverAgreementId, initialContractSnapshot]);

  const addDocument = (file) => {
    if (!file) return;
    const doc = {
      file,
      fileName: file.name,
      documentType: 'SUPPORTING_DOC',
      preview: URL.createObjectURL(file),
    };
    onUpdateDetails({ documents: [...(details.documents || []), doc] });
    onClearDocumentError?.();
  };

  const handleFileAdd = (e) => {
    addDocument(e.target.files[0]);
    e.target.value = '';
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    addDocument(e.dataTransfer.files[0]);
  };

  const removeDoc = (idx) => {
    onUpdateDetails({ documents: details.documents.filter((_, i) => i !== idx) });
  };

  const handleSaveContractDetails = async () => {
    if (!validateContractDetailsFields(details, enqueueSnackbar)) return;
    if (!serverAgreementId) {
      enqueueSnackbar('Save the agreement draft first', { variant: 'warning' });
      return;
    }
    setSavingContract(true);
    try {
      const saved = await onSaveContractDetails?.();
      const snapshot = buildContractDetailsSnapshot(saved) ?? {
        incomeTypeId: details.incomeTypeId,
        agreementTypeId: details.agreementTypeId,
        startDate: details.startDate,
        expiryDate: details.expiryDate,
        notes: details.notes ?? '',
        stateIds: details.stateIds ?? [],
      };
      setSavedSnapshot(snapshot);
      setIsLocked(true);
      setIsEditing(false);
      enqueueSnackbar('Contract details saved — commercials are now available', { variant: 'success' });
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Failed to save contract details', { variant: 'error' });
    } finally {
      setSavingContract(false);
    }
  };

  const handleStartEdit = () => {
    setIsEditing(true);
  };

  const handleCancelEdit = () => {
    if (savedSnapshot) {
      onUpdateDetails({
        incomeTypeId: savedSnapshot.incomeTypeId,
        agreementTypeId: savedSnapshot.agreementTypeId,
        startDate: savedSnapshot.startDate,
        expiryDate: savedSnapshot.expiryDate,
        notes: savedSnapshot.notes ?? '',
        stateIds: savedSnapshot.stateIds ?? [],
      });
    }
    setIsEditing(false);
  };

  const selectedStateIds = details.stateIds ?? [];
  const selectedStates = stateOptions.filter((s) => selectedStateIds.includes(s.id));
  const statesReadOnlyLabel = selectedStates.length
    ? selectedStates.map((s) => `${s.stateName} (${s.stateCode})`).join(', ')
    : '—';

  const documents = details.documents || [];

  return (
    <Paper
      id={cardId}
      elevation={0}
      sx={{
        p: 2.5,
        mb: 2,
        borderRadius: 2.5,
        border: `1px solid ${BRAND.borderLight}`,
        bgcolor: BRAND.white,
        scrollMarginTop: 96,
      }}
    >
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Typography variant="subtitle1" fontWeight={700}>
            Contract Details
          </Typography>
          {isLocked && !isEditing && (
            <Chip
              icon={<Lock sx={{ fontSize: '14px !important' }} />}
              label="Saved"
              size="small"
              color="success"
              variant="outlined"
            />
          )}
        </Box>
        {isLocked && !isEditing && (
          <Button size="small" startIcon={<Edit />} onClick={handleStartEdit}>
            Edit Contract Details
          </Button>
        )}
      </Box>

      {isEditing && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          Commercials are paused while you edit. Saved dates (
          {formatDateLabel(savedSnapshot?.startDate)} – {formatDateLabel(savedSnapshot?.expiryDate)}
          ) still drive template generation until you save contract details again.
          Slabs and uploaded targets are not removed.
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, sm: 6 }}>
          <FormControl fullWidth size="small" disabled={fieldsDisabled}>
            <InputLabel required>Income Type</InputLabel>
            <Select
              value={details.incomeTypeId || ''}
              label="Income Type *"
              onChange={(e) => onUpdateDetails({ incomeTypeId: e.target.value })}
            >
              {incomeTypes.map((t) => <MenuItem key={t.id} value={t.id}>{t.name}</MenuItem>)}
            </Select>
          </FormControl>
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <FormControl fullWidth size="small" disabled={fieldsDisabled}>
            <InputLabel required>Agreement Type</InputLabel>
            <Select
              value={details.agreementTypeId || ''}
              label="Agreement Type *"
              onChange={(e) => onUpdateDetails({ agreementTypeId: e.target.value })}
            >
              {agreementTypes.map((t) => <MenuItem key={t.id} value={t.id}>{t.name}</MenuItem>)}
            </Select>
          </FormControl>
        </Grid>

        <Grid size={12}>
          {fieldsDisabled ? (
            <TextField
              label="States"
              size="small"
              fullWidth
              value={statesReadOnlyLabel}
              disabled
              sx={notesFieldSx}
            />
          ) : (
            <Autocomplete
              multiple
              options={stateOptions}
              value={selectedStates}
              getOptionLabel={(option) => option.stateName}
              isOptionEqualToValue={(option, value) => option.id === value.id}
              onChange={(_, newValue) => onUpdateDetails({ stateIds: newValue.map((s) => s.id) })}
              renderInput={(params) => (
                <TextField {...params} label="States" size="small" placeholder="Select states" />
              )}
            />
          )}
        </Grid>

        <Grid size={12}>
          <DateRangeFields
            startDate={details.startDate}
            expiryDate={details.expiryDate}
            onChange={(fields) => onUpdateDetails(fields)}
            disabled={fieldsDisabled}
          />
        </Grid>

        <Grid size={12}>
          <TextField
            label="Notes (optional)"
            multiline
            rows={2}
            fullWidth
            size="small"
            value={details.notes || ''}
            onChange={(e) => onUpdateDetails({ notes: e.target.value })}
            slotProps={{ htmlInput: { maxLength: 1000 } }}
            disabled={fieldsDisabled}
            sx={notesFieldSx}
          />
        </Grid>

        <Grid size={12}>
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            {(!isLocked || isEditing) && (
              <>
                <Button
                  variant="contained"
                  onClick={handleSaveContractDetails}
                  disabled={savingContract || fieldsDisabled}
                  sx={{ bgcolor: BRAND.red }}
                >
                  {savingContract ? <CircularProgress size={20} color="inherit" /> : 'Save Contract Details'}
                </Button>
                {isEditing && (
                  <Button variant="outlined" onClick={handleCancelEdit} disabled={savingContract}>
                    Cancel Edit
                  </Button>
                )}
              </>
            )}
          </Box>
          {!commercialsEnabled && !isEditing && (
            <Alert severity="info" sx={{ mt: 2 }}>
              Save contract details before configuring commercials. Start and expiry dates must be stored on the server first.
            </Alert>
          )}
        </Grid>

        {commercialsEnabled ? (
          <Grid size={12}>
            <CommercialFields
              key={`commercials-${serverAgreementId}-${savedSnapshot.startDate}-${savedSnapshot.expiryDate}`}
              commercials={agreement.commercials}
              onUpdate={onUpdateCommercials}
              serverAgreementId={serverAgreementId}
              startDate={savedSnapshot.startDate}
              expiryDate={savedSnapshot.expiryDate}
            />
          </Grid>
        ) : isEditing ? (
          <Grid size={12}>
            <Alert severity="info">
              Commercial structure and slabs unlock again after you save contract details.
            </Alert>
          </Grid>
        ) : null}

        <Grid size={12}>
          <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 1.5 }}>
            Documents *
          </Typography>
          <Box
            onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
            onDragLeave={() => setDragOver(false)}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
            sx={{
              border: `2px dashed ${documentError ? BRAND.red : BRAND.borderLight}`,
              borderRadius: '10px',
              bgcolor: dragOver ? alpha(BRAND.red, 0.04) : BRAND.bgGray,
              p: 2.5,
              textAlign: 'center',
              mb: 1.5,
              cursor: 'pointer',
              transition: 'background-color 0.15s ease, border-color 0.15s ease',
              '&:hover': { bgcolor: alpha(BRAND.red, 0.03), borderColor: '#94A3B8' },
            }}
          >
            <UploadFile sx={{ fontSize: 36, color: BRAND.textSecondary, mb: 0.5 }} />
            <Typography variant="body2" color="text.secondary" mb={1}>
              Drag & drop or click to upload
            </Typography>
            <Button
              variant="outlined"
              size="small"
              onClick={(e) => { e.stopPropagation(); fileInputRef.current?.click(); }}
            >
              Browse Files
            </Button>
            <input
              ref={fileInputRef}
              type="file"
              hidden
              onChange={handleFileAdd}
              accept=".pdf,.doc,.docx,.jpg,.jpeg,.png,.eml"
            />
          </Box>

          {documentError && (
            <FormHelperText error sx={{ mb: 1 }}>{documentError}</FormHelperText>
          )}

          {documents.length > 0 && (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              {documents.map((doc, i) => (
                <Box
                  key={`${doc.fileName}-${i}`}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1.5,
                    border: `1px solid ${BRAND.borderLight}`,
                    borderRadius: '8px',
                    px: 1.5,
                    py: 1,
                    bgcolor: BRAND.white,
                  }}
                >
                  <Chip label={doc.fileName} size="small" sx={{ flex: 1, justifyContent: 'flex-start', maxWidth: '100%' }} />
                  <FormControl size="small" sx={{ minWidth: 150 }}>
                    <Select
                      value={doc.documentType}
                      onChange={(e) => {
                        const docs = [...documents];
                        docs[i] = { ...docs[i], documentType: e.target.value };
                        onUpdateDetails({ documents: docs });
                      }}
                    >
                      {DOCUMENT_TYPES.map((t) => (
                        <MenuItem key={t} value={t}>{t.replace('_', ' ')}</MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                  <IconButton size="small" color="error" onClick={() => removeDoc(i)} aria-label="Remove document">
                    <Delete fontSize="small" />
                  </IconButton>
                </Box>
              ))}
            </Box>
          )}
        </Grid>
      </Grid>
    </Paper>
  );
}
