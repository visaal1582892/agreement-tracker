import {
  Box, Grid, FormControl, InputLabel, Select, MenuItem, TextField,
} from '@mui/material';
import WizardFieldAnchor from '../../../components/wizard/WizardFieldAnchor';

const ASSET_CATEGORY_OPTIONS = [
  { value: 'PHYSICAL_ASSET', label: 'Physical Asset' },
  { value: 'ACTIVITY', label: 'Activity' },
];

const ASSET_TYPE_OPTIONS = ['Shelf', 'Window', 'Sampling'];

export default function AssetRentalScopeFields({ asset, onUpdateAsset, fieldErrors = {} }) {
  return (
    <Box>
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, sm: 6 }}>
          <WizardFieldAnchor field="assetCategory" error={fieldErrors.assetCategory}>
            <FormControl fullWidth size="small" required error={Boolean(fieldErrors.assetCategory)}>
              <InputLabel>Asset Category</InputLabel>
              <Select
                value={asset?.assetCategory || 'PHYSICAL_ASSET'}
                label="Asset Category *"
                onChange={(e) => onUpdateAsset({ assetCategory: e.target.value })}
              >
                {ASSET_CATEGORY_OPTIONS.map((option) => (
                  <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </WizardFieldAnchor>
        </Grid>

        <Grid size={{ xs: 12, sm: 6 }}>
          <WizardFieldAnchor field="assetType" error={fieldErrors.assetType}>
            <FormControl fullWidth size="small" required error={Boolean(fieldErrors.assetType)}>
              <InputLabel>Asset Type</InputLabel>
              <Select
                value={asset?.assetType || ''}
                label="Asset Type *"
                onChange={(e) => onUpdateAsset({ assetType: e.target.value })}
              >
                {ASSET_TYPE_OPTIONS.map((type) => (
                  <MenuItem key={type} value={type}>{type}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </WizardFieldAnchor>
        </Grid>

        <Grid size={12}>
          <TextField
            label="Description / Remarks"
            multiline
            rows={2}
            fullWidth
            size="small"
            value={asset?.remarks ?? ''}
            onChange={(e) => onUpdateAsset({ remarks: e.target.value })}
            slotProps={{ htmlInput: { maxLength: 500 } }}
          />
        </Grid>
      </Grid>
    </Box>
  );
}
