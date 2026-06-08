import {
  Box, Typography, Button, Select, MenuItem, TextField,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Paper, IconButton, InputAdornment,
} from '@mui/material';
import { Add, Delete } from '@mui/icons-material';

const DISTRIBUTIONS = ['MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY'];
const VALUE_TYPES = [
  { value: 'VALUE', label: 'Value' },
  { value: 'PERCENTAGE', label: 'Percentage' },
];

const TIME_PERIOD_OPTIONS = {
  MONTHLY: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
  QUARTERLY: ['Q1', 'Q2', 'Q3', 'Q4'],
  HALF_YEARLY: ['H1', 'H2'],
  YEARLY: ['Year'],
};

const DISTRIBUTION_LABELS = {
  MONTHLY: 'Monthly',
  QUARTERLY: 'Quarterly',
  HALF_YEARLY: 'Half-Yearly',
  YEARLY: 'Yearly',
};

function createEmptySlab(displayOrder) {
  return {
    slabName: '',
    commercialType: 'VALUE',
    commercialValue: '',
    fromValue: '',
    toValue: '',
    timeDistribution: 'MONTHLY',
    timePeriod: '',
    displayOrder,
  };
}

function CommercialInput({ slab, onChange }) {
  const isPercentage = (slab.commercialType || 'VALUE') === 'PERCENTAGE';

  return (
    <TextField
      label="Commercial"
      size="small"
      type="number"
      fullWidth
      value={slab.commercialValue || ''}
      onChange={(e) => onChange('commercialValue', e.target.value)}
      slotProps={{
        input: {
          startAdornment: (
            <InputAdornment position="start" sx={{ mr: 0 }}>
              <Select
                size="small"
                value={slab.commercialType || 'VALUE'}
                onChange={(e) => onChange('commercialType', e.target.value)}
                variant="standard"
                disableUnderline
                sx={{ fontSize: '0.8rem', minWidth: 88 }}
              >
                {VALUE_TYPES.map((t) => (
                  <MenuItem key={t.value} value={t.value} sx={{ fontSize: '0.85rem' }}>
                    {t.label}
                  </MenuItem>
                ))}
              </Select>
            </InputAdornment>
          ),
          endAdornment: isPercentage ? (
            <InputAdornment position="end"><Typography sx={{ fontSize: '0.85rem' }}>%</Typography></InputAdornment>
          ) : (
            <InputAdornment position="end"><Typography sx={{ fontSize: '0.85rem' }}>₹</Typography></InputAdornment>
          ),
        },
      }}
    />
  );
}

export default function SlabTiersTable({ slabs = [], onUpdate }) {
  const updateSlab = (idx, field, val) => {
    onUpdate({
      slabs: slabs.map((s, i) => {
        if (i !== idx) return s;
        const next = { ...s, [field]: val };
        if (field === 'timeDistribution') next.timePeriod = '';
        return next;
      }),
    });
  };

  const addSlab = () => onUpdate({ slabs: [...slabs, createEmptySlab(slabs.length + 1)] });

  const removeSlab = (idx) => onUpdate({ slabs: slabs.filter((_, i) => i !== idx) });

  return (
    <Box>
      <Typography variant="subtitle2" gutterBottom>Slab Tiers</Typography>
      <TableContainer
        component={Paper}
        elevation={0}
        sx={{ mb: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2, overflowX: 'auto' }}
      >
        <Table size="small" sx={{ minWidth: 900 }}>
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 600, minWidth: 130 }}>Slab Name</TableCell>
              <TableCell sx={{ fontWeight: 600, minWidth: 200 }}>Commercial</TableCell>
              <TableCell sx={{ fontWeight: 600, minWidth: 100 }}>From</TableCell>
              <TableCell sx={{ fontWeight: 600, minWidth: 100 }}>To</TableCell>
              <TableCell sx={{ fontWeight: 600, minWidth: 130 }}>Time Distribution</TableCell>
              <TableCell sx={{ fontWeight: 600, minWidth: 120 }}>Time Period</TableCell>
              <TableCell width={50} />
            </TableRow>
          </TableHead>
          <TableBody>
            {slabs.map((slab, idx) => {
              const distribution = slab.timeDistribution || 'MONTHLY';
              const periodOptions = TIME_PERIOD_OPTIONS[distribution] || [];

              return (
                <TableRow key={idx}>
                  <TableCell>
                    <TextField
                      size="small"
                      fullWidth
                      value={slab.slabName}
                      onChange={(e) => updateSlab(idx, 'slabName', e.target.value)}
                      placeholder="e.g. 0–1L"
                    />
                  </TableCell>
                  <TableCell>
                    <CommercialInput
                      slab={slab}
                      onChange={(field, val) => updateSlab(idx, field, val)}
                    />
                  </TableCell>
                  <TableCell>
                    <TextField
                      size="small"
                      type="number"
                      fullWidth
                      value={slab.fromValue}
                      onChange={(e) => updateSlab(idx, 'fromValue', e.target.value)}
                    />
                  </TableCell>
                  <TableCell>
                    <TextField
                      size="small"
                      type="number"
                      fullWidth
                      value={slab.toValue}
                      onChange={(e) => updateSlab(idx, 'toValue', e.target.value)}
                    />
                  </TableCell>
                  <TableCell>
                    <Select
                      size="small"
                      fullWidth
                      value={distribution}
                      onChange={(e) => updateSlab(idx, 'timeDistribution', e.target.value)}
                    >
                      {DISTRIBUTIONS.map((d) => (
                        <MenuItem key={d} value={d}>{DISTRIBUTION_LABELS[d]}</MenuItem>
                      ))}
                    </Select>
                  </TableCell>
                  <TableCell>
                    <Select
                      size="small"
                      fullWidth
                      value={slab.timePeriod || ''}
                      displayEmpty
                      onChange={(e) => updateSlab(idx, 'timePeriod', e.target.value)}
                    >
                      <MenuItem value="" disabled>
                        Select {DISTRIBUTION_LABELS[distribution].toLowerCase()} period
                      </MenuItem>
                      {periodOptions.map((p) => (
                        <MenuItem key={p} value={p}>{p}</MenuItem>
                      ))}
                    </Select>
                  </TableCell>
                  <TableCell>
                    <IconButton size="small" color="error" onClick={() => removeSlab(idx)}>
                      <Delete fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>

      <Button size="small" startIcon={<Add />} onClick={addSlab}>
        Add Slab
      </Button>
    </Box>
  );
}
