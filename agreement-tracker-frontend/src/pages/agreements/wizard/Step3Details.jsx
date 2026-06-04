import { useEffect, useState } from 'react';
import { Box, Typography, Grid, FormControl, InputLabel, Select, MenuItem, TextField, Button, Chip } from '@mui/material';
import { UploadFile, Delete } from '@mui/icons-material';
import axiosInstance from '../../../api/axiosInstance';
import { ENDPOINTS } from '../../../config/endpoints';
import DateRangeFields from '../../../components/forms/DateRangeFields';

const DOCUMENT_TYPES = ['AGREEMENT', 'SUPPORTING_DOC', 'EMAIL', 'OTHER'];

export default function Step3Details({ state, updateFields }) {
  const [incomeTypes, setIncomeTypes] = useState([]);
  const [agreementTypes, setAgreementTypes] = useState([]);

  useEffect(() => {
    axiosInstance.get(ENDPOINTS.INCOME_TYPES).then(({ data }) => setIncomeTypes(data));
    axiosInstance.get(ENDPOINTS.AGREEMENT_TYPES).then(({ data }) => setAgreementTypes(data));
  }, []);

  const handleFileAdd = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const doc = { file, fileName: file.name, documentType: 'SUPPORTING_DOC', preview: URL.createObjectURL(file) };
    updateFields({ documents: [...(state.documents || []), doc] });
    e.target.value = '';
  };

  const removeDoc = (idx) => {
    updateFields({ documents: state.documents.filter((_, i) => i !== idx) });
  };

  return (
    <Box>
      <Typography variant="h6" fontWeight={600} mb={0.5}>Agreement Details</Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        Fill in the core details and upload supporting documents.
      </Typography>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, sm: 6 }}>
          <FormControl fullWidth size="small">
            <InputLabel required>Income Type</InputLabel>
            <Select
              value={state.incomeTypeId || ''}
              label="Income Type *"
              onChange={(e) => updateFields({ incomeTypeId: e.target.value })}
            >
              {incomeTypes.map((t) => <MenuItem key={t.id} value={t.id}>{t.name}</MenuItem>)}
            </Select>
          </FormControl>
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <FormControl fullWidth size="small">
            <InputLabel required>Agreement Type</InputLabel>
            <Select
              value={state.agreementTypeId || ''}
              label="Agreement Type *"
              onChange={(e) => updateFields({ agreementTypeId: e.target.value })}
            >
              {agreementTypes.map((t) => <MenuItem key={t.id} value={t.id}>{t.name}</MenuItem>)}
            </Select>
          </FormControl>
        </Grid>

        <Grid size={12}>
          <DateRangeFields
            startDate={state.startDate}
            expiryDate={state.expiryDate}
            tenureMonths={state.tenureMonths}
            onStartChange={(d) => updateFields({ startDate: d?.toISOString() })}
            onExpiryChange={(d) => updateFields({ expiryDate: d?.toISOString() })}
            onTenureChange={(v) => updateFields({ tenureMonths: v })}
          />
        </Grid>

        <Grid size={12}>
          <TextField
            label="Notes (optional)"
            multiline
            rows={2}
            fullWidth
            value={state.notes}
            onChange={(e) => updateFields({ notes: e.target.value })}
            inputProps={{ maxLength: 1000 }}
          />
        </Grid>

        {/* Document Upload */}
        <Grid size={12}>
          <Typography variant="subtitle2" gutterBottom>Documents</Typography>
          <Box sx={{
            border: '2px dashed', borderColor: 'divider', borderRadius: 2,
            p: 3, textAlign: 'center', mb: 2,
          }}>
            <UploadFile sx={{ fontSize: 40, color: 'text.secondary', mb: 1 }} />
            <Typography variant="body2" color="text.secondary" mb={1.5}>
              Drag & drop or click to upload
            </Typography>
            <Button variant="outlined" component="label" size="small">
              Browse Files
              <input type="file" hidden onChange={handleFileAdd} accept=".pdf,.doc,.docx,.jpg,.png,.eml" />
            </Button>
          </Box>

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {(state.documents || []).map((doc, i) => (
              <Box key={i} sx={{ display: 'flex', alignItems: 'center', gap: 1.5, border: '1px solid', borderColor: 'divider', borderRadius: 1.5, px: 2, py: 1 }}>
                <Typography variant="body2" sx={{ flex: 1 }}>{doc.fileName}</Typography>
                <FormControl size="small" sx={{ minWidth: 140 }}>
                  <Select
                    value={doc.documentType}
                    onChange={(e) => {
                      const docs = [...state.documents];
                      docs[i] = { ...docs[i], documentType: e.target.value };
                      updateFields({ documents: docs });
                    }}
                  >
                    {DOCUMENT_TYPES.map((t) => <MenuItem key={t} value={t}>{t.replace('_', ' ')}</MenuItem>)}
                  </Select>
                </FormControl>
                <Button size="small" color="error" onClick={() => removeDoc(i)} sx={{ minWidth: 0 }}>
                  <Delete fontSize="small" />
                </Button>
              </Box>
            ))}
          </Box>
        </Grid>
      </Grid>
    </Box>
  );
}
