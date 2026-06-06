import { useState } from 'react';
import {
  Box, Typography, Grid, RadioGroup, FormControlLabel, Radio,
  TextField, Button, FormControl, InputLabel, Select, MenuItem,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Paper, IconButton, Alert, Tabs, Tab,
} from '@mui/material';
import { Add, Delete, Download, Upload } from '@mui/icons-material';
import { BRAND } from '../../../config/theme';

const DISTRIBUTIONS = ['MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY'];

function CommercialForm({ commercials, onUpdate }) {
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
            <TextField
              label="Commercial Value *"
              type="number"
              fullWidth
              value={commercials.commercialValue}
              onChange={(e) => onUpdate({ commercialValue: e.target.value })}
              slotProps={{ input: { startAdornment: <Typography sx={{ mr: 1 }}>₹</Typography> } }}
            />
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
          <Grid container spacing={2} sx={{ mb: 3 }}>
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

          <Button size="small" startIcon={<Add />} onClick={addSlab} sx={{ mb: 3 }}>
            Add Slab
          </Button>

          {slabs.length > 0 && (
            <Alert severity="info" sx={{ mb: 2 }}>
              Once slabs and dates are defined, download the Excel template, fill in commercial values per period, then upload.
            </Alert>
          )}
          <Box sx={{ display: 'flex', gap: 2 }}>
            <Button variant="outlined" startIcon={<Download />} disabled={slabs.length === 0}>
              Download Excel Template
            </Button>
            <Button variant="outlined" startIcon={<Upload />} component="label" disabled={slabs.length === 0}>
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
