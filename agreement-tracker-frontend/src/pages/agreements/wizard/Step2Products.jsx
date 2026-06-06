import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Box, Typography, Grid, FormControl, Select, MenuItem,
  FormGroup, FormControlLabel, Checkbox, Chip, Button, Alert, CircularProgress,
} from '@mui/material';
import axiosInstance from '../../../api/axiosInstance';
import { ENDPOINTS } from '../../../config/endpoints';
import { BRAND } from '../../../config/theme';
import SearchableSelect from '../../../components/forms/SearchableSelect';

const panelSx = {
  border: `1px solid ${BRAND.borderLight}`,
  borderRadius: '10px',
  bgcolor: BRAND.white,
  p: 2,
  height: '100%',
  display: 'flex',
  flexDirection: 'column',
};

const listSx = {
  flex: 1,
  mt: 1,
  overflowY: 'auto',
  maxHeight: 320,
};

function uniqueManufacturers(products) {
  const map = new Map();
  products.forEach((p) => {
    if (p.manufacturerId && !map.has(p.manufacturerId)) {
      map.set(p.manufacturerId, {
        id: p.manufacturerId,
        manufacturerName: p.manufacturerName,
      });
    }
  });
  return Array.from(map.values());
}

function divisionsFromPool(products) {
  const map = new Map();
  products.forEach((p) => {
    if (p.divisionId && !map.has(p.divisionId)) {
      map.set(p.divisionId, { id: p.divisionId, divisionName: p.divisionName });
    }
  });
  return Array.from(map.values());
}

function manufacturerPool(products, selectedManufacturers) {
  if (!selectedManufacturers.length) return products;
  const mfrIds = new Set(selectedManufacturers.map((m) => m.id));
  return products.filter((p) => mfrIds.has(p.manufacturerId));
}

function visibleProducts(pool, selectedDivisionIds, divisionOp, applyDivisionFilter) {
  if (!applyDivisionFilter) return pool;
  if (!selectedDivisionIds.length) return [];
  if (divisionOp === 'INCLUDE') {
    return pool.filter((p) => selectedDivisionIds.includes(p.divisionId));
  }
  return pool.filter((p) => !selectedDivisionIds.includes(p.divisionId));
}

export default function Step2Products({ state, updateProductRules }) {
  const sharedRules = state.productRules || { manufacturers: [], divisionRules: [], productRules: [] };
  const [vendorProducts, setVendorProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [fetchError, setFetchError] = useState(null);
  const [selectedManufacturers, setSelectedManufacturers] = useState([]);
  const [selectedDivisionIds, setSelectedDivisionIds] = useState([]);
  const [checkedProductIds, setCheckedProductIds] = useState([]);
  const [divisionOp, setDivisionOp] = useState('INCLUDE');
  const [productOp, setProductOp] = useState('INCLUDE');
  const hasRehydrated = useRef(false);
  const prevAvailableDivisionIdsRef = useRef([]);
  const prevVisibleProductIdsRef = useRef([]);

  useEffect(() => {
    if (!state.vendorIds?.length) {
      setVendorProducts([]);
      hasRehydrated.current = false;
      return;
    }
    hasRehydrated.current = false;
    setLoading(true);
    setFetchError(null);
    axiosInstance
      .get(ENDPOINTS.PRODUCTS, { params: { 'vendorIds[]': state.vendorIds } })
      .then(({ data }) => setVendorProducts(Array.isArray(data) ? data : []))
      .catch((err) => {
        console.error('Failed to load vendor products:', err);
        setFetchError(err.response?.data?.message || 'Failed to load products for selected vendors.');
        setVendorProducts([]);
      })
      .finally(() => setLoading(false));
  }, [state.vendorIds]);

  const manufacturerOptions = useMemo(
    () => uniqueManufacturers(vendorProducts),
    [vendorProducts],
  );

  const hasManufacturerFilter = selectedManufacturers.length > 0;

  const filteredByManufacturer = useMemo(
    () => (hasManufacturerFilter
      ? manufacturerPool(vendorProducts, selectedManufacturers)
      : vendorProducts),
    [vendorProducts, selectedManufacturers, hasManufacturerFilter],
  );

  const availableDivisions = useMemo(() => {
    if (!hasManufacturerFilter) return [];
    return divisionsFromPool(filteredByManufacturer);
  }, [hasManufacturerFilter, filteredByManufacturer]);

  const divisionFilteredProducts = useMemo(
    () => visibleProducts(
      filteredByManufacturer,
      selectedDivisionIds,
      divisionOp,
      hasManufacturerFilter,
    ),
    [filteredByManufacturer, selectedDivisionIds, divisionOp, hasManufacturerFilter],
  );

  // Rehydrate local state from wizard state when editing an existing draft
  useEffect(() => {
    if (!vendorProducts.length || !sharedRules.manufacturers?.length || hasRehydrated.current) return;
    hasRehydrated.current = true;
    const seedMfrs = manufacturerOptions.filter((m) => sharedRules.manufacturers.includes(m.id));
    setSelectedManufacturers(seedMfrs);
    if (sharedRules.divisionRules?.length) {
      setDivisionOp(sharedRules.divisionRules[0].ruleType);
      const divisionIds = sharedRules.divisionRules.map((r) => r.id);
      setSelectedDivisionIds(divisionIds);
      prevAvailableDivisionIdsRef.current = divisionsFromPool(
        manufacturerPool(vendorProducts, seedMfrs),
      ).map((d) => d.id);
    }
    if (sharedRules.productRules?.length) {
      setProductOp(sharedRules.productRules[0].ruleType);
      const productIds = sharedRules.productRules.map((r) => r.id);
      setCheckedProductIds(productIds);
      prevVisibleProductIdsRef.current = productIds;
    }
  }, [vendorProducts, sharedRules, manufacturerOptions]);

  useEffect(() => {
    if (!vendorProducts.length) {
      setSelectedManufacturers([]);
      setSelectedDivisionIds([]);
      setCheckedProductIds([]);
      prevAvailableDivisionIdsRef.current = [];
      prevVisibleProductIdsRef.current = [];
      return;
    }
    if (!hasRehydrated.current) setSelectedManufacturers([]);
  }, [vendorProducts]);

  // Auto-select all divisions on first load; auto-select newly added divisions thereafter
  useEffect(() => {
    if (!hasManufacturerFilter) {
      if (!sharedRules.manufacturers?.length) {
        setSelectedDivisionIds([]);
        prevAvailableDivisionIdsRef.current = [];
      }
      return;
    }

    const availableIds = availableDivisions.map((d) => d.id);
    const previouslyAvailable = prevAvailableDivisionIdsRef.current;
    const newlyAdded = availableIds.filter((id) => !previouslyAvailable.includes(id));
    prevAvailableDivisionIdsRef.current = availableIds;

    setSelectedDivisionIds((prev) => {
      const pruned = prev.filter((id) => availableIds.includes(id));
      if (previouslyAvailable.length === 0) {
        return availableIds;
      }
      if (newlyAdded.length === 0) {
        return pruned;
      }
      return [...new Set([...pruned, ...newlyAdded])];
    });
  }, [availableDivisions, hasManufacturerFilter, sharedRules.manufacturers?.length]);

  // Auto-select all products on first load; auto-select newly visible products thereafter
  useEffect(() => {
    const visibleIds = divisionFilteredProducts.map((p) => p.id);
    if (!visibleIds.length) {
      if (!sharedRules.manufacturers?.length) {
        setCheckedProductIds([]);
        prevVisibleProductIdsRef.current = [];
      }
      return;
    }

    const previouslyVisible = prevVisibleProductIdsRef.current;
    const newlyAdded = visibleIds.filter((id) => !previouslyVisible.includes(id));
    prevVisibleProductIdsRef.current = visibleIds;

    setCheckedProductIds((prev) => {
      const pruned = prev.filter((id) => visibleIds.includes(id));
      if (previouslyVisible.length === 0) {
        return visibleIds;
      }
      if (newlyAdded.length === 0) {
        return pruned;
      }
      return [...new Set([...pruned, ...newlyAdded])];
    });
  }, [divisionFilteredProducts, sharedRules.manufacturers?.length]);

  // Emit rule-based payload to wizard state
  useEffect(() => {
    updateProductRules({
      manufacturers: selectedManufacturers.map((m) => m.id),
      divisionRules: selectedDivisionIds.map((id) => ({ id, ruleType: divisionOp })),
      productRules: checkedProductIds.map((id) => ({ id, ruleType: productOp })),
    });
  }, [selectedManufacturers, selectedDivisionIds, divisionOp, checkedProductIds, productOp, updateProductRules]);

  const handleManufacturersChange = useCallback((selected) => {
    setSelectedManufacturers(Array.isArray(selected) ? selected : []);
  }, []);

  const toggleDivision = (id) => {
    setSelectedDivisionIds((prev) =>
      prev.includes(id) ? prev.filter((d) => d !== id) : [...prev, id],
    );
  };

  const toggleProduct = (id) => {
    setCheckedProductIds((prev) =>
      prev.includes(id) ? prev.filter((p) => p !== id) : [...prev, id],
    );
  };

  const selectAllDivisions = () => setSelectedDivisionIds(availableDivisions.map((d) => d.id));
  const deselectAllDivisions = () => setSelectedDivisionIds([]);

  const selectAllProducts = () => setCheckedProductIds(divisionFilteredProducts.map((p) => p.id));
  const deselectAllProducts = () => setCheckedProductIds([]);

  const productsByDivision = divisionFilteredProducts.reduce((acc, p) => {
    const key = p.divisionName || 'Other';
    if (!acc[key]) acc[key] = [];
    acc[key].push(p);
    return acc;
  }, {});

  const resolvedProductIds = productOp === 'INCLUDE'
    ? checkedProductIds.filter((id) => divisionFilteredProducts.some((p) => p.id === id))
    : divisionFilteredProducts.filter((p) => !checkedProductIds.includes(p.id)).map((p) => p.id);

  const selectedProductChips = divisionFilteredProducts.filter((p) => resolvedProductIds.includes(p.id));

  return (
    <Box>
      <Typography variant="h6" fontWeight={600} mb={0.5}>Applicable Products</Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        All vendor-mapped products load selected. Use manufacturers and divisions to narrow the list.
      </Typography>

      {!state.vendorIds?.length && (
        <Alert severity="warning" sx={{ mb: 2 }}>Select vendors above first.</Alert>
      )}
      {fetchError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setFetchError(null)}>{fetchError}</Alert>
      )}
      {loading && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
          <CircularProgress size={18} />
          <Typography variant="body2" color="text.secondary">Loading vendor products…</Typography>
        </Box>
      )}

      <Grid container spacing={2.5} alignItems="stretch">
        {/* Manufacturer */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Box sx={panelSx}>
            <Typography variant="subtitle2" fontWeight={700} mb={1}>Manufacturer</Typography>
            <SearchableSelect
              label="Manufacturers (optional)"
              placeholder="Filter by manufacturer…"
              isMulti
              options={manufacturerOptions}
              value={selectedManufacturers}
              onChange={handleManufacturersChange}
              getOptionLabel={(o) => o.manufacturerName || ''}
              isOptionEqualToValue={(o, v) => o.id === v.id}
              disabled={!vendorProducts.length || loading}
              maxVisibleChips={2}
            />
            <Typography variant="caption" color="text.secondary" sx={{ mt: 1.5, display: 'block' }}>
              {selectedManufacturers.length
                ? `${selectedManufacturers.length} manufacturer(s) filtering list`
                : `Showing all ${vendorProducts.length} vendor product(s)`}
            </Typography>
          </Box>
        </Grid>

        {/* Divisions */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Box sx={panelSx}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                <Typography variant="subtitle2" fontWeight={700}>Divisions</Typography>
                <Typography variant="caption" color="text.disabled">|</Typography>
                <Button size="small" variant="text" sx={{ minWidth: 0, px: 0.5, fontSize: '0.72rem' }} onClick={selectAllDivisions} disabled={!hasManufacturerFilter || !availableDivisions.length}>Select All</Button>
                <Typography variant="caption" color="text.disabled">|</Typography>
                <Button size="small" variant="text" sx={{ minWidth: 0, px: 0.5, fontSize: '0.72rem' }} onClick={deselectAllDivisions} disabled={!hasManufacturerFilter || !availableDivisions.length}>Clear All</Button>
              </Box>
              <FormControl size="small" sx={{ minWidth: 120 }} disabled={!hasManufacturerFilter}>
                <Select value={divisionOp} onChange={(e) => setDivisionOp(e.target.value)}>
                  <MenuItem value="INCLUDE">Include Only</MenuItem>
                  <MenuItem value="EXCLUDE">Exclude</MenuItem>
                </Select>
              </FormControl>
            </Box>
            <Box sx={listSx}>
              {!hasManufacturerFilter ? (
                <Typography variant="body2" color="text.secondary" sx={{ py: 1 }}>
                  Select manufacturer(s) to view divisions
                </Typography>
              ) : availableDivisions.length === 0 ? (
                <Typography variant="body2" color="text.secondary" sx={{ py: 1 }}>
                  No divisions for selected manufacturers
                </Typography>
              ) : (
                <FormGroup>
                  {availableDivisions.map((d) => (
                    <FormControlLabel
                      key={d.id}
                      control={
                        <Checkbox
                          checked={selectedDivisionIds.includes(d.id)}
                          onChange={() => toggleDivision(d.id)}
                          size="small"
                        />
                      }
                      label={<Typography variant="body2">{d.divisionName}</Typography>}
                    />
                  ))}
                </FormGroup>
              )}
            </Box>
          </Box>
        </Grid>

        {/* Products */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Box sx={panelSx}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                <Typography variant="subtitle2" fontWeight={700}>Products</Typography>
                <Typography variant="caption" color="text.disabled">|</Typography>
                <Button size="small" variant="text" sx={{ minWidth: 0, px: 0.5, fontSize: '0.72rem' }} onClick={selectAllProducts} disabled={!divisionFilteredProducts.length}>Select All</Button>
                <Typography variant="caption" color="text.disabled">|</Typography>
                <Button size="small" variant="text" sx={{ minWidth: 0, px: 0.5, fontSize: '0.72rem' }} onClick={deselectAllProducts} disabled={!divisionFilteredProducts.length}>Clear All</Button>
              </Box>
              <FormControl size="small" sx={{ minWidth: 120 }}>
                <Select value={productOp} onChange={(e) => setProductOp(e.target.value)}>
                  <MenuItem value="INCLUDE">Include Only</MenuItem>
                  <MenuItem value="EXCLUDE">Exclude</MenuItem>
                </Select>
              </FormControl>
            </Box>
            <Box sx={listSx}>
              {divisionFilteredProducts.length === 0 ? (
                <Typography variant="body2" color="text.secondary" sx={{ py: 1 }}>
                  {!vendorProducts.length
                    ? 'No vendor products loaded'
                    : hasManufacturerFilter && !selectedDivisionIds.length
                      ? 'Select at least one division'
                      : 'No products match current filters'}
                </Typography>
              ) : (
                Object.entries(productsByDivision).map(([divName, prods]) => (
                  <Box key={divName} mb={1.5}>
                    <Typography variant="caption" color="text.secondary" fontWeight={700} sx={{ display: 'block', mb: 0.5 }}>
                      {divName}
                    </Typography>
                    <FormGroup>
                      {prods.map((p) => (
                        <FormControlLabel
                          key={p.id}
                          control={
                            <Checkbox
                              checked={checkedProductIds.includes(p.id)}
                              onChange={() => toggleProduct(p.id)}
                              size="small"
                            />
                          }
                          label={<Typography variant="body2">{p.productName}</Typography>}
                        />
                      ))}
                    </FormGroup>
                  </Box>
                ))
              )}
            </Box>
          </Box>
        </Grid>
      </Grid>

      <Box sx={{ mt: 2.5, p: 2, borderRadius: '10px', border: `1px solid ${BRAND.borderLight}`, bgcolor: BRAND.bgGray }}>
        <Typography variant="caption" color="text.secondary" fontWeight={600}>
          {resolvedProductIds.length} product(s) selected
          {productOp === 'EXCLUDE' ? ' (exclude mode — unchecked items kept)' : ''}
        </Typography>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.75, mt: 1 }}>
          {selectedProductChips.map((p) => (
            <Chip
              key={p.id}
              label={p.productName}
              size="small"
              onDelete={() => {
                if (productOp === 'INCLUDE') {
                  toggleProduct(p.id);
                } else {
                  setCheckedProductIds((prev) => [...prev, p.id]);
                }
              }}
            />
          ))}
        </Box>
      </Box>
    </Box>
  );
}
