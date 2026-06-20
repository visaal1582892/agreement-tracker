import { useRef, useState } from 'react';
import {
  Autocomplete, Box, Typography, Grid, FormControl, InputLabel, Select, MenuItem,
  TextField, Button, alpha,
} from '@mui/material';
import { UploadFile } from '@mui/icons-material';
import { BRAND } from '../../../config/theme';

const ASSET_CATEGORY_OPTIONS = [
  { value: 'PHYSICAL_ASSET', label: 'Physical Asset' },
  { value: 'ACTIVITY', label: 'Activity' },
];

const ASSET_TYPE_OPTIONS = ['Shelf', 'Window', 'Sampling'];

export default function AssetRentalFields({
  asset,
  stateOptions,
  selectedStateIds,
  storeOutletList,
  onUpdateAsset,
  onUpdateDetails,
}) {
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef(null);

  const selectedStates = stateOptions.filter((state) => selectedStateIds.includes(state.id));

  const handleStoreOutletAdd = (file) => {
    if (!file) return;
    onUpdateDetails({
      storeOutletList: {
        file,
        fileName: file.name,
        documentType: 'STORE_OUTLET_LIST',
        preview: URL.createObjectURL(file),
      },
    });
  };

  return (
    <Box sx={{ mb: 3 }}>
      <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 2 }}>
        Asset Rental Details
      </Typography>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, sm: 6 }}>
          <FormControl fullWidth size="small" required>
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
        </Grid>

        <Grid size={{ xs: 12, sm: 6 }}>
          <FormControl fullWidth size="small" required>
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
        </Grid>

        <Grid size={12}>
          <Autocomplete
            multiple
            options={stateOptions}
            value={selectedStates}
            getOptionLabel={(option) => option.stateName}
            isOptionEqualToValue={(option, value) => option.id === value.id}
            onChange={(_, newValue) => onUpdateDetails({ stateIds: newValue.map((state) => state.id) })}
            renderInput={(params) => (
              <TextField {...params} label="Location (States) *" size="small" placeholder="Select states" />
            )}
          />
        </Grid>

        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            label="Number of Participating Stores *"
            type="number"
            fullWidth
            size="small"
            value={asset?.storeCount ?? ''}
            onChange={(e) => onUpdateAsset({ storeCount: e.target.value })}
            slotProps={{ htmlInput: { min: 1 } }}
          />
        </Grid>

        <Grid size={12}>
          <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 1 }}>
            Upload List of Participating Outlets *
          </Typography>
          <Box
            onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
            onDragLeave={() => setDragOver(false)}
            onDrop={(e) => {
              e.preventDefault();
              setDragOver(false);
              handleStoreOutletAdd(e.dataTransfer.files[0]);
            }}
            onClick={() => fileInputRef.current?.click()}
            sx={{
              border: `2px dashed ${BRAND.borderLight}`,
              borderRadius: '10px',
              bgcolor: dragOver ? alpha(BRAND.red, 0.04) : BRAND.bgGray,
              p: 2.5,
              textAlign: 'center',
              cursor: 'pointer',
              transition: 'background-color 0.15s ease, border-color 0.15s ease',
              '&:hover': { bgcolor: alpha(BRAND.red, 0.03), borderColor: '#94A3B8' },
            }}
          >
            <UploadFile sx={{ fontSize: 36, color: BRAND.textSecondary, mb: 0.5 }} />
            <Typography variant="body2" color="text.secondary" mb={1}>
              Drag & drop or click to upload participating outlet list
            </Typography>
            <Button
              variant="outlined"
              size="small"
              onClick={(e) => { e.stopPropagation(); fileInputRef.current?.click(); }}
            >
              Browse File
            </Button>
            <input
              ref={fileInputRef}
              type="file"
              hidden
              onChange={(e) => {
                handleStoreOutletAdd(e.target.files[0]);
                e.target.value = '';
              }}
              accept=".pdf,.doc,.docx,.xls,.xlsx,.csv"
            />
          </Box>
          {storeOutletList?.fileName && (
            <Typography variant="body2" sx={{ mt: 1 }}>
              Selected: <strong>{storeOutletList.fileName}</strong>
            </Typography>
          )}
        </Grid>
      </Grid>
    </Box>
  );
}
