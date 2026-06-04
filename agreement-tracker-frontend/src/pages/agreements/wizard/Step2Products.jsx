import { useEffect, useState } from 'react';
import {
  Box, Typography, Grid, FormControl, InputLabel, Select, MenuItem,
  FormGroup, FormControlLabel, Checkbox, Chip, Divider, Alert,
} from '@mui/material';
import axiosInstance from '../../../api/axiosInstance';
import { ENDPOINTS } from '../../../config/endpoints';

export default function Step2Products({ state, updateFields }) {
  const [manufacturers, setManufacturers] = useState([]);
  const [divisions, setDivisions] = useState([]);
  const [products, setProducts] = useState([]);
  const [selectedMfr, setSelectedMfr] = useState('');
  const [selectedDivisions, setSelectedDivisions] = useState([]);
  const [divisionOp, setDivisionOp] = useState('INCLUDE');
  const [productOp, setProductOp] = useState('INCLUDE');
  const [selectedProductIds, setSelectedProductIds] = useState(state.productIds || []);

  useEffect(() => {
    axiosInstance.get(ENDPOINTS.MANUFACTURERS).then(({ data }) => setManufacturers(data));
  }, []);

  useEffect(() => {
    if (selectedMfr) {
      axiosInstance.get(ENDPOINTS.DIVISIONS(selectedMfr)).then(({ data }) => {
        setDivisions(data);
        setSelectedDivisions([]);
      });
    }
  }, [selectedMfr]);

  useEffect(() => {
    if (!state.vendorIds?.length) return;
    const params = { vendorIds: state.vendorIds };
    if (selectedMfr) params.manufacturerId = selectedMfr;
    if (selectedDivisions.length) params.divisionIds = selectedDivisions;

    axiosInstance.get(ENDPOINTS.PRODUCTS, { params }).then(({ data }) => {
      let filtered = data;
      if (selectedDivisions.length) {
        if (divisionOp === 'EXCLUDE') {
          filtered = filtered.filter((p) => !selectedDivisions.includes(p.division?.id));
        } else {
          filtered = filtered.filter((p) => selectedDivisions.includes(p.division?.id));
        }
      }
      setProducts(filtered);
    });
  }, [state.vendorIds, selectedMfr, selectedDivisions, divisionOp]);

  const toggleProduct = (id) => {
    setSelectedProductIds((prev) => {
      const next = prev.includes(id) ? prev.filter((p) => p !== id) : [...prev, id];
      updateFields({ productIds: next });
      return next;
    });
  };

  const toggleDivision = (id) => {
    setSelectedDivisions((prev) =>
      prev.includes(id) ? prev.filter((d) => d !== id) : [...prev, id]
    );
  };

  const productsByDivision = products.reduce((acc, p) => {
    const key = p.divisionNameSnapshot || p.division?.divisionName || 'Other';
    if (!acc[key]) acc[key] = [];
    acc[key].push(p);
    return acc;
  }, {});

  return (
    <Box>
      <Typography variant="h6" fontWeight={600} mb={0.5}>Applicable Products</Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        Filter products mapped to the selected vendors. Products are fetched from the master catalog.
      </Typography>

      {!state.vendorIds?.length && (
        <Alert severity="warning" sx={{ mb: 2 }}>Select vendors in Step 1 first.</Alert>
      )}

      <Grid container spacing={3}>
        {/* Level 1: Manufacturer */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Box sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2, p: 2 }}>
            <Typography variant="subtitle2" fontWeight={600} mb={1.5}>Manufacturer</Typography>
            <FormControl fullWidth size="small">
              <InputLabel>Select Manufacturer</InputLabel>
              <Select value={selectedMfr} label="Select Manufacturer" onChange={(e) => setSelectedMfr(e.target.value)}>
                <MenuItem value="">All</MenuItem>
                {manufacturers.map((m) => (
                  <MenuItem key={m.id} value={m.id}>{m.manufacturerName}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
        </Grid>

        {/* Level 2: Divisions */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Box sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2, p: 2, minHeight: 200 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
              <Typography variant="subtitle2" fontWeight={600}>Divisions</Typography>
              <FormControl size="small" sx={{ minWidth: 120 }}>
                <Select value={divisionOp} onChange={(e) => setDivisionOp(e.target.value)}>
                  <MenuItem value="INCLUDE">Include Only</MenuItem>
                  <MenuItem value="EXCLUDE">Exclude</MenuItem>
                </Select>
              </FormControl>
            </Box>
            <FormGroup>
              {divisions.map((d) => (
                <FormControlLabel
                  key={d.id}
                  control={
                    <Checkbox
                      checked={selectedDivisions.includes(d.id)}
                      onChange={() => toggleDivision(d.id)}
                      size="small"
                    />
                  }
                  label={<Typography variant="body2">{d.divisionName}</Typography>}
                />
              ))}
            </FormGroup>
          </Box>
        </Grid>

        {/* Level 3: Products */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Box sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2, p: 2, minHeight: 200, maxHeight: 400, overflowY: 'auto' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
              <Typography variant="subtitle2" fontWeight={600}>Products</Typography>
              <FormControl size="small" sx={{ minWidth: 120 }}>
                <Select value={productOp} onChange={(e) => setProductOp(e.target.value)}>
                  <MenuItem value="INCLUDE">Include Only</MenuItem>
                  <MenuItem value="EXCLUDE">Exclude</MenuItem>
                </Select>
              </FormControl>
            </Box>
            {Object.entries(productsByDivision).map(([div, prods]) => (
              <Box key={div} mb={1.5}>
                <Typography variant="caption" color="text.secondary" fontWeight={600}>{div}</Typography>
                <FormGroup>
                  {prods.map((p) => (
                    <FormControlLabel
                      key={p.id}
                      control={
                        <Checkbox
                          checked={selectedProductIds.includes(p.id)}
                          onChange={() => toggleProduct(p.id)}
                          size="small"
                        />
                      }
                      label={<Typography variant="body2">{p.productName}</Typography>}
                    />
                  ))}
                </FormGroup>
                <Divider sx={{ mt: 0.5 }} />
              </Box>
            ))}
            {products.length === 0 && (
              <Typography variant="body2" color="text.secondary">Select manufacturer to view products</Typography>
            )}
          </Box>
        </Grid>
      </Grid>

      <Box sx={{ mt: 2 }}>
        <Typography variant="caption" color="text.secondary">
          {selectedProductIds.length} product(s) selected
        </Typography>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 1 }}>
          {products.filter((p) => selectedProductIds.includes(p.id)).map((p) => (
            <Chip key={p.id} label={p.productName} size="small" onDelete={() => toggleProduct(p.id)} />
          ))}
        </Box>
      </Box>
    </Box>
  );
}
