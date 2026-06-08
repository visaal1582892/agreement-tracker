import {
  Box, Typography, Grid, RadioGroup, FormControlLabel, Radio,
  TextField, Button, FormControl, InputLabel, Select, MenuItem, Alert,
} from '@mui/material';
import { Download, Upload } from '@mui/icons-material';
import SlabTiersTable from './SlabTiersTable';

const VALUE_TYPES = [
  { value: 'VALUE', label: 'Value' },
  { value: 'PERCENTAGE', label: 'Percentage' },
];

export default function CommercialFields({ commercials, onUpdate }) {

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
                size="small"
                value={commercials.commercialPercentage || ''}
                onChange={(e) => onUpdate({ commercialPercentage: e.target.value })}
                slotProps={{ input: { endAdornment: <Typography sx={{ ml: 1 }}>%</Typography> } }}
              />
            ) : (
              <TextField
                label="Commercial Value *"
                type="number"
                fullWidth
                size="small"
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
          <SlabTiersTable slabs={commercials.slabs || []} onUpdate={onUpdate} />

          {(commercials.slabs?.length ?? 0) > 0 && (
            <Alert severity="info" sx={{ mb: 2 }}>
              Once slabs and dates are defined, download the Excel template, fill in commercial values per period, then upload.
            </Alert>
          )}
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mt: 2 }}>
            <Button variant="outlined" size="small" startIcon={<Download />} disabled={!(commercials.slabs?.length)}>
              Download Excel Template
            </Button>
            <Button variant="outlined" size="small" startIcon={<Upload />} component="label" disabled={!(commercials.slabs?.length)}>
              Upload Completed Matrix
              <input type="file" hidden accept=".xlsx,.xls" />
            </Button>
          </Box>
        </Box>
      )}
    </Box>
  );
}
