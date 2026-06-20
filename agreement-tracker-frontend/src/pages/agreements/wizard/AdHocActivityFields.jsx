import { useEffect } from 'react';
import {
  Box, Typography, RadioGroup, FormControlLabel, Radio, Alert,
} from '@mui/material';
import Step2Products from './Step2Products';
import WizardFieldAnchor from '../../../components/wizard/WizardFieldAnchor';
import { ADHOC_SUB_TYPES, ADHOC_SUB_TYPE_LABELS } from '../../../constants/adhocSubTypes';

export default function AdHocActivityFields({
  state,
  details,
  commercials,
  updateProductRules,
  onUpdateDetails,
  onUpdateCommercials,
  section = 'scope',
  fieldErrors = {},
}) {
  const subType = details.adhocSubType || '';

  useEffect(() => {
    if (subType === ADHOC_SUB_TYPES.QPS) {
      onUpdateCommercials({ selectedFrequencies: ['ONE_TIME'] });
    }
  }, [subType, onUpdateCommercials]);

  if (section === 'geography') {
    return null;
  }

  return (
    <Box>
      <WizardFieldAnchor field="adhocSubType" error={fieldErrors.adhocSubType}>
        <Typography variant="body2" fontWeight={600} sx={{ mb: 1 }}>
          Ad-Hoc Activity Type *
        </Typography>
        <RadioGroup
          row
          value={subType}
          onChange={(e) => onUpdateDetails({ adhocSubType: e.target.value })}
          sx={{ mb: 2 }}
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
      </WizardFieldAnchor>

      {!subType && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Select QPS or Consumer Price Off to configure scope fields.
        </Alert>
      )}

      {subType === ADHOC_SUB_TYPES.QPS && (
        <Step2Products
          state={state}
          updateProductRules={updateProductRules}
          error={fieldErrors.products}
          info="Select vendors to load products. Add manufacturers to reveal divisions and narrow the product list. Configure flat baseline and slab incentives in Step 3 — QPS locks payout frequency to One-Time."
        />
      )}

      {subType === ADHOC_SUB_TYPES.CONSUMER_PRICE_OFF && (
        <Step2Products
          state={state}
          updateProductRules={updateProductRules}
          error={fieldErrors.products}
        />
      )}
    </Box>
  );
}
