import { useState } from 'react';
import {
  Box, Typography, Grid, RadioGroup, FormControlLabel, Radio,
  TextField, Button, FormControl, InputLabel, Select, MenuItem,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Paper, IconButton, Alert,
} from '@mui/material';
import { Add, Delete, Download, Upload } from '@mui/icons-material';
import { BRAND } from '../../../config/theme';

const DISTRIBUTIONS = ['MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY'];

export default function Step4Commercials({ state, updateFields }) {
  const [slabs, setSlabs] = useState(state.slabs || []);

  const addSlab = () => {
    const newSlabs = [...slabs, { slabName: '', fromValue: '', toValue: '', displayOrder: slabs.length + 1 }];
    setSlabs(newSlabs);
    updateFields({ slabs: newSlabs });
  };

  const updateSlab = (idx, field, val) => {
    const updated = slabs.map((s, i) => i === idx ? { ...s, [field]: val } : s);
    setSlabs(updated);
    updateFields({ slabs: updated });
  };

  const removeSlab = (idx) => {
    const updated = slabs.filter((_, i) => i !== idx);
    setSlabs(updated);
    updateFields({ slabs: updated });
  };

  return (
    <Box>
      <Typography variant="h6" fontWeight={600} mb={0.5}>Commercial Structure</Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        Define the commercial arrangement — flat value or slab-based tiers.
      </Typography>

      {/* Structure Toggle */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="subtitle2" gutterBottom>Structure Type *</Typography>
        <RadioGroup
          row
          value={state.commercialStructure}
          onChange={(e) => updateFields({ commercialStructure: e.target.value })}
        >
          <FormControlLabel value="FLAT" control={<Radio />} label="Flat" />
          <FormControlLabel value="SLAB" control={<Radio />} label="Slab-based" />
        </RadioGroup>
      </Box>

      {/* FLAT */}
      {state.commercialStructure === 'FLAT' && (
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Commercial Value *"
              type="number"
              fullWidth
              value={state.commercialValue}
              onChange={(e) => updateFields({ commercialValue: e.target.value })}
              slotProps={{ input: { startAdornment: <Typography sx={{ mr: 1 }}>₹</Typography> } }}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Calculation Formula (optional)"
              fullWidth
              value={state.calculationFormula || ''}
              onChange={(e) => updateFields({ calculationFormula: e.target.value })}
              placeholder="e.g. (Sales × 2%)"
            />
          </Grid>
        </Grid>
      )}

      {/* SLAB */}
      {state.commercialStructure === 'SLAB' && (
        <Box>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid size={{ xs: 12, sm: 4 }}>
              <FormControl fullWidth size="small">
                <InputLabel>Time Distribution *</InputLabel>
                <Select
                  value={state.timeDistribution || 'MONTHLY'}
                  label="Time Distribution *"
                  onChange={(e) => updateFields({ timeDistribution: e.target.value })}
                >
                  {DISTRIBUTIONS.map((d) => <MenuItem key={d} value={d}>{d.replace('_', '-')}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>
          </Grid>

          {/* Slab Definition */}
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

          <Button size="small" startIcon={<Add />} onClick={addSlab} sx={{ mb: 3 }}>
            Add Slab
          </Button>

          {/* Matrix Excel */}
          {slabs.length > 0 && (
            <Alert severity="info" sx={{ mb: 2 }}>
              Once slabs and dates are defined, download the Excel template, fill in commercial values per period, then upload.
            </Alert>
          )}
          <Box sx={{ display: 'flex', gap: 2 }}>
            <Button variant="outlined" startIcon={<Download />} disabled={slabs.length === 0}>
              📥 Download Excel Template
            </Button>
            <Button variant="outlined" startIcon={<Upload />} component="label" disabled={slabs.length === 0}>
              📤 Upload Completed Matrix
              <input type="file" hidden accept=".xlsx,.xls" />
            </Button>
          </Box>
        </Box>
      )}
    </Box>
  );
}
