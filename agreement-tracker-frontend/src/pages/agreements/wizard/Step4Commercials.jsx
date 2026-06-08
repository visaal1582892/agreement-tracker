import { useState } from 'react';
import {
  Box, Typography, Grid, RadioGroup, FormControlLabel, Radio,
  TextField, Button, FormControl, InputLabel, Select, MenuItem,
  Alert, Tabs, Tab,
} from '@mui/material';
import { Download, Upload } from '@mui/icons-material';
import { BRAND } from '../../../config/theme';
import SlabTiersTable from './SlabTiersTable';

const VALUE_TYPES = [
  { value: 'VALUE', label: 'Value' },
  { value: 'PERCENTAGE', label: 'Percentage' },
];

function CommercialForm({ commercials, onUpdate }) {

  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="subtitle2" gutterBottom>Structure Type *</Typography>
        <RadioGroup
          row
          value={commercials.commercialStructure}
          onChange={(e) => onUpdate({ commercialStructure: e.target.value })}
        >
          <FormControlLabel value="FLAT" control={<Radio />} label="Flat" />
          <FormControlLabel value="SLAB" control={<Radio />} label="Slab-based" />
        </RadioGroup>
      </Box>

      {commercials.commercialStructure === 'FLAT' && (
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <FormControl fullWidth size="small">
              <InputLabel>Value Type *</InputLabel>
              <Select
                value={commercials.valueType || 'VALUE'}
                label="Value Type *"
                onChange={(e) => onUpdate({
                  valueType: e.target.value,
                  commercialValue: '',
                  commercialPercentage: '',
                })}
              >
                {VALUE_TYPES.map((t) => <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>)}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            {(commercials.valueType || 'VALUE') === 'PERCENTAGE' ? (
              <TextField
                label="Commercial Percentage *"
                type="number"
                fullWidth
                value={commercials.commercialPercentage || ''}
                onChange={(e) => onUpdate({ commercialPercentage: e.target.value })}
                slotProps={{ input: { endAdornment: <Typography sx={{ ml: 1 }}>%</Typography> } }}
              />
            ) : (
              <TextField
                label="Commercial Value *"
                type="number"
                fullWidth
                value={commercials.commercialValue}
                onChange={(e) => onUpdate({ commercialValue: e.target.value })}
                slotProps={{ input: { startAdornment: <Typography sx={{ mr: 1 }}>₹</Typography> } }}
              />
            )}
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Calculation Formula (optional)"
              fullWidth
              value={commercials.calculationFormula || ''}
              onChange={(e) => onUpdate({ calculationFormula: e.target.value })}
              placeholder="e.g. (Sales × 2%)"
            />
          </Grid>
        </Grid>
      )}

      {commercials.commercialStructure === 'SLAB' && (
        <Box>
          <SlabTiersTable slabs={commercials.slabs || []} onUpdate={onUpdate} />

          {(commercials.slabs?.length ?? 0) > 0 && (
            <Alert severity="info" sx={{ mb: 2 }}>
              Once slabs and dates are defined, download the Excel template, fill in commercial values per period, then upload.
            </Alert>
          )}
          <Box sx={{ display: 'flex', gap: 2, mt: 2 }}>
            <Button variant="outlined" startIcon={<Download />} disabled={!(commercials.slabs?.length)}>
              Download Excel Template
            </Button>
            <Button variant="outlined" startIcon={<Upload />} component="label" disabled={!(commercials.slabs?.length)}>
              Upload Completed Matrix
              <input type="file" hidden accept=".xlsx,.xls" />
            </Button>
          </Box>
        </Box>
      )}
    </Box>
  );
}

export default function Step4Commercials({ state, updateAgreementCommercials }) {
  const [activeTab, setActiveTab] = useState(0);
  const agreements = state.agreements || [];

  return (
    <Box>
      <Typography variant="h6" fontWeight={600} mb={0.5}>Commercial Structure</Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        Define commercial arrangement for each agreement in the batch.
      </Typography>

      <Tabs
        value={activeTab}
        onChange={(_, v) => setActiveTab(v)}
        sx={{ mb: 3, borderBottom: `1px solid ${BRAND.borderLight}` }}
        variant="scrollable"
        scrollButtons="auto"
      >
        {agreements.map((_, index) => (
          <Tab key={agreements[index].id} label={`Agreement ${index + 1}`} />
        ))}
      </Tabs>

      {agreements.map((agreement, index) => (
        <Box key={agreement.id} role="tabpanel" hidden={activeTab !== index}>
          {activeTab === index && (
            <CommercialForm
              commercials={agreement.commercials}
              onUpdate={(patch) => updateAgreementCommercials(agreement.id, patch)}
            />
          )}
        </Box>
      ))}
    </Box>
  );
}
