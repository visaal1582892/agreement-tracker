import { useEffect } from 'react';
import {
  Autocomplete, Box, Typography, Grid, TextField, RadioGroup, FormControlLabel, Radio, Alert,
} from '@mui/material';
import Step2Products from './Step2Products';
import { ADHOC_SUB_TYPES, ADHOC_SUB_TYPE_LABELS } from '../../../constants/adhocSubTypes';

export default function AdHocActivityFields({
  state,
  details,
  commercials,
  stateOptions,
  updateProductRules,
  onUpdateDetails,
  onUpdateCommercials,
}) {
  const subType = details.adhocSubType || '';
  const selectedStateIds = details.stateIds ?? [];
  const selectedStates = stateOptions.filter((stateOption) => selectedStateIds.includes(stateOption.id));

  useEffect(() => {
    if (subType === ADHOC_SUB_TYPES.QPS) {
      onUpdateCommercials({ selectedFrequencies: ['ONE_TIME'] });
    }
  }, [subType, onUpdateCommercials]);

  return (
    <Box sx={{ mb: 3 }}>
      <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 1.5 }}>
        Ad-Hoc Activity Type *
      </Typography>
      <RadioGroup
        row
        value={subType}
        onChange={(e) => onUpdateDetails({ adhocSubType: e.target.value })}
        sx={{ mb: 2.5 }}
      >
        <FormControlLabel
          value={ADHOC_SUB_TYPES.QPS}
          control={<Radio size="small" />}
          label={ADHOC_SUB_TYPE_LABELS.QPS}
        />
        <FormControlLabel
          value={ADHOC_SUB_TYPES.CONSUMER_PRICE_OFF}
          control={<Radio size="small" />}
          label={ADHOC_SUB_TYPE_LABELS.CONSUMER_PRICE_OFF}
        />
      </RadioGroup>

      {!subType && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Select QPS or Consumer Price Off to configure activity fields.
        </Alert>
      )}

      {subType === ADHOC_SUB_TYPES.QPS && (
        <>
          {!state.vendorIds?.length && (
            <Typography variant="body2" color="warning.main" sx={{ mb: 2 }}>
              Select supply vendors above before configuring products.
            </Typography>
          )}
          <Step2Products state={state} updateProductRules={updateProductRules} />
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
            Configure flat baseline and slab incentives in Step 3. QPS frequency locked to One-Time.
          </Typography>
        </>
      )}

      {subType === ADHOC_SUB_TYPES.CONSUMER_PRICE_OFF && (
        <Grid container spacing={3}>
          <Grid size={12}>
            <Autocomplete
              multiple
              options={stateOptions}
              value={selectedStates}
              getOptionLabel={(option) => option.stateName}
              isOptionEqualToValue={(option, value) => option.id === value.id}
              onChange={(_, newValue) => onUpdateDetails({ stateIds: newValue.map((stateOption) => stateOption.id) })}
              renderInput={(params) => (
                <TextField {...params} label="Location (States) *" size="small" placeholder="Select states" />
              )}
            />
          </Grid>
          <Grid size={12}>
            {!state.vendorIds?.length && (
              <Typography variant="body2" color="warning.main" sx={{ mb: 2 }}>
              Select supply vendors above before configuring products.
              </Typography>
            )}
            <Step2Products state={state} updateProductRules={updateProductRules} />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <TextField
              label="Quantity / Value Cap *"
              type="number"
              fullWidth
              size="small"
              value={details.quantityCap ?? ''}
              onChange={(e) => onUpdateDetails({ quantityCap: e.target.value })}
              helperText="Maximum units or monetary value before the campaign stops (e.g., 50,000 units)."
              slotProps={{ htmlInput: { min: 0, step: '0.01' } }}
            />
          </Grid>
        </Grid>
      )}
    </Box>
  );
}
