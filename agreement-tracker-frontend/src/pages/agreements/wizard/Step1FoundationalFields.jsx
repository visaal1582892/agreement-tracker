import { useEffect, useState } from 'react';
import {
  Box, Typography, Grid, FormControl, InputLabel, Select, MenuItem, TextField, Paper, Divider,
} from '@mui/material';
import axiosInstance from '../../../api/axiosInstance';
import { ENDPOINTS } from '../../../config/endpoints';
import { BRAND } from '../../../config/theme';
import DateRangeFields from '../../../components/forms/DateRangeFields';
import { BLANK_ASSET, buildIncomeTypeSwitchUpdates } from '../../../utils/incomeTypePayloadUtils';

const notesFieldSx = {
  '& .MuiOutlinedInput-root': {
    '& fieldset': { borderColor: BRAND.borderLight },
    '&:hover fieldset': { borderColor: '#94A3B8' },
    '&.Mui-focused fieldset': { borderColor: BRAND.red, borderWidth: 2 },
  },
};

export default function Step1FoundationalFields({
  agreement,
  onUpdateDetails,
  onUpdateAsset,
  onUpdateCommercials,
  updateProductRules,
}) {
  const [incomeTypes, setIncomeTypes] = useState([]);
  const [agreementTypes, setAgreementTypes] = useState([]);
  const details = agreement.details;

  useEffect(() => {
    axiosInstance.get(ENDPOINTS.INCOME_TYPES).then(({ data }) => setIncomeTypes(data));
    axiosInstance.get(ENDPOINTS.AGREEMENT_TYPES).then(({ data }) => setAgreementTypes(data));
  }, []);

  return (
    <Paper
      elevation={0}
      sx={{
        p: 2.5,
        borderRadius: 2.5,
        border: `1px solid ${BRAND.borderLight}`,
        bgcolor: BRAND.white,
        mt: 3,
      }}
    >
      <Typography sx={{ fontSize: '0.9375rem', fontWeight: 700, color: BRAND.textPrimary, mb: 0.5 }}>
        Agreement Metadata
      </Typography>
      <Typography sx={{ fontSize: '0.8125rem', color: '#64748B', mb: 2.5 }}>
        Select income type, agreement type, and contract duration. Step 2 configures commercial details from this selection.
      </Typography>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, sm: 6 }}>
          <FormControl fullWidth size="small">
            <InputLabel required>Income Type</InputLabel>
            <Select
              value={details.incomeTypeId || ''}
              label="Income Type *"
              onChange={(e) => {
                const selectedId = e.target.value;
                const selected = incomeTypes.find((type) => String(type.id) === String(selectedId));
                const nextDetails = {
                  incomeTypeId: selectedId,
                  incomeTypeName: selected?.name ?? null,
                };
                const switchUpdates = buildIncomeTypeSwitchUpdates(
                  incomeTypes,
                  details,
                  nextDetails,
                );
                onUpdateDetails({
                  ...nextDetails,
                  ...(switchUpdates?.details ?? {}),
                });
                if (switchUpdates?.resetAsset) {
                  onUpdateAsset(BLANK_ASSET);
                }
                if (switchUpdates?.clearProductRules) {
                  updateProductRules({
                    manufacturers: [],
                    divisionRules: [],
                    productRules: [],
                  });
                }
                if (switchUpdates?.resetCommercials) {
                  onUpdateCommercials({
                    commercialStructure: 'FLAT',
                    commercialValue: '',
                    calculationFormula: '',
                    selectedFrequencies: [],
                  });
                }
              }}
            >
              {incomeTypes.map((type) => (
                <MenuItem key={type.id} value={type.id}>{type.name}</MenuItem>
              ))}
            </Select>
          </FormControl>
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <FormControl fullWidth size="small">
            <InputLabel required>Agreement Type</InputLabel>
            <Select
              value={details.agreementTypeId || ''}
              label="Agreement Type *"
              onChange={(e) => onUpdateDetails({ agreementTypeId: e.target.value })}
            >
              {agreementTypes.map((type) => (
                <MenuItem key={type.id} value={type.id}>{type.name}</MenuItem>
              ))}
            </Select>
          </FormControl>
        </Grid>

        <Grid size={12}>
          <Divider sx={{ my: 0.5 }} />
        </Grid>

        <Grid size={12}>
          <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 0.5 }}>
            Contract Duration *
          </Typography>
          <DateRangeFields
            startDate={details.startDate}
            expiryDate={details.expiryDate}
            onChange={(fields) => onUpdateDetails(fields)}
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
            sx={notesFieldSx}
          />
        </Grid>
      </Grid>
    </Paper>
  );
}
