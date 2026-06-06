import { useState } from 'react';
import {
  Box, Typography, Grid, RadioGroup, FormControlLabel, Radio,
  TextField, Button, FormControl, InputLabel, Select, MenuItem,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Paper, IconButton, Alert,
} from '@mui/material';
import { Add, Delete, Download, Upload } from '@mui/icons-material';

const DISTRIBUTIONS = ['MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY'];

export default function CommercialFields({ commercials, onUpdate }) {
  const [slabs, setSlabs] = useState(commercials.slabs || []);

  const syncSlabs = (updated) => {
    setSlabs(updated);
    onUpdate({ slabs: updated });
  };

  const addSlab = () => {
    const newSlabs = [...slabs, { slabName: '', fromValue: '', toValue: '', displayOrder: slabs.length + 1 }];
    syncSlabs(newSlabs);
  };

  const updateSlab = (idx, field, val) => {
    syncSlabs(slabs.map((s, i) => (i === idx ? { ...s, [field]: val } : s)));
  };

  const removeSlab = (idx) => {
    syncSlabs(slabs.filter((_, i) => i !== idx));
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
            <TextField
              label="Commercial Value *"
              type="number"
              fullWidth
              size="small"
              value={commercials.commercialValue}
              onChange={(e) => onUpdate({ commercialValue: e.target.value })}
              slotProps={{ input: { startAdornment: <Typography sx={{ mr: 1 }}>₹</Typography> } }}
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
          <Grid container spacing={2} sx={{ mb: 2 }}>
            <Grid size={{ xs: 12, sm: 4 }}>
              <FormControl fullWidth size="small">
                <InputLabel>Time Distribution *</InputLabel>
                <Select
                  value={commercials.timeDistribution || 'MONTHLY'}
                  label="Time Distribution *"
                  onChange={(e) => onUpdate({ timeDistribution: e.target.value })}
                >
                  {DISTRIBUTIONS.map((d) => <MenuItem key={d} value={d}>{d.replace('_', '-')}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>
          </Grid>

          <Typography variant="subtitle2" gutterBottom>Slab Tiers</Typography>
          <TableContainer component={Paper} elevation={0} sx={{ mb: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 600 }}>Slab Name</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>From Value (₹)</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>To Value (₹)</TableCell>
                  <TableCell width={50} />
                </TableRow>
              </TableHead>
              <TableBody>
                {slabs.map((slab, idx) => (
                  <TableRow key={idx}>
                    <TableCell>
                      <TextField
                        size="small" fullWidth
                        value={slab.slabName}
                        onChange={(e) => updateSlab(idx, 'slabName', e.target.value)}
                        placeholder="e.g. 0–1L"
                      />
                    </TableCell>
                    <TableCell>
                      <TextField
                        size="small" type="number" fullWidth
                        value={slab.fromValue}
                        onChange={(e) => updateSlab(idx, 'fromValue', e.target.value)}
                      />
                    </TableCell>
                    <TableCell>
                      <TextField
                        size="small" type="number" fullWidth
                        value={slab.toValue}
                        onChange={(e) => updateSlab(idx, 'toValue', e.target.value)}
                      />
                    </TableCell>
                    <TableCell>
                      <IconButton size="small" color="error" onClick={() => removeSlab(idx)}>
                        <Delete fontSize="small" />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>

          <Button size="small" startIcon={<Add />} onClick={addSlab} sx={{ mb: 2 }}>
            Add Slab
          </Button>

          {slabs.length > 0 && (
            <Alert severity="info" sx={{ mb: 2 }}>
              Once slabs and dates are defined, download the Excel template, fill in commercial values per period, then upload.
            </Alert>
          )}
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
            <Button variant="outlined" size="small" startIcon={<Download />} disabled={slabs.length === 0}>
              Download Excel Template
            </Button>
            <Button variant="outlined" size="small" startIcon={<Upload />} component="label" disabled={slabs.length === 0}>
              Upload Completed Matrix
              <input type="file" hidden accept=".xlsx,.xls" />
            </Button>
          </Box>
        </Box>
      )}
    </Box>
  );
}
