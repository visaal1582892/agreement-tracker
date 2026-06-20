import {
  Box, Typography, Grid, TextField, RadioGroup, FormControlLabel, Radio, InputAdornment,
} from '@mui/material';
import WizardFieldAnchor from '../../../components/wizard/WizardFieldAnchor';

export default function AssetPayoutFields({
  asset,
  onUpdateAsset,
  hideSectionTitle = false,
  fieldErrors = {},
}) {
  const payoutMode = asset?.payoutMode || 'FLAT';

  return (
    <Box>
      {!hideSectionTitle && (
        <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 1 }}>
          Asset Payout Structure *
        </Typography>
      )}
      <RadioGroup
        row
        value={payoutMode}
        onChange={(e) => onUpdateAsset({ payoutMode: e.target.value })}
        sx={{ mb: 2 }}
      >
        <FormControlLabel value="FLAT" control={<Radio size="small" />} label="Flat Payout" />
        <FormControlLabel value="PER_STORE" control={<Radio size="small" />} label="Payout per Store" />
      </RadioGroup>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, sm: 6 }}>
          {payoutMode === 'FLAT' ? (
            <WizardFieldAnchor field="flatPayout" error={fieldErrors.flatPayout}>
              <TextField
                label="Flat Payout *"
                type="number"
                fullWidth
                size="small"
                value={asset?.flatPayout ?? ''}
                onChange={(e) => onUpdateAsset({ flatPayout: e.target.value })}
                error={Boolean(fieldErrors.flatPayout)}
                slotProps={{
                  input: {
                    startAdornment: <InputAdornment position="start">₹</InputAdornment>,
                  },
                  htmlInput: { min: 0, step: '0.01' },
                }}
              />
            </WizardFieldAnchor>
          ) : (
            <WizardFieldAnchor field="payoutPerStore" error={fieldErrors.payoutPerStore}>
              <TextField
                label="Payout per Store *"
                type="number"
                fullWidth
                size="small"
                value={asset?.payoutPerStore ?? ''}
                onChange={(e) => onUpdateAsset({ payoutPerStore: e.target.value })}
                error={Boolean(fieldErrors.payoutPerStore)}
                slotProps={{
                  input: {
                    startAdornment: <InputAdornment position="start">₹</InputAdornment>,
                  },
                  htmlInput: { min: 0, step: '0.01' },
                }}
              />
            </WizardFieldAnchor>
          )}
        </Grid>
      </Grid>
    </Box>
  );
}
