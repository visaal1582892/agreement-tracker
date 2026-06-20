import { useRef, useState } from 'react';
import {
  Autocomplete, Box, Grid, TextField, Button, alpha,
} from '@mui/material';
import { UploadFile } from '@mui/icons-material';
import { BRAND } from '../../../config/theme';
import WizardSectionTitle from '../../../components/wizard/WizardSectionTitle';
import WizardFieldAnchor from '../../../components/wizard/WizardFieldAnchor';

export default function AssetRentalGeographyFields({
  asset,
  stateOptions,
  selectedStateIds,
  storeOutletList,
  onUpdateAsset,
  onUpdateDetails,
  fieldErrors = {},
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
    <Box>
      <Grid container spacing={3}>
        <Grid size={12}>
          <WizardFieldAnchor field="states" error={fieldErrors.states}>
            <Autocomplete
              multiple
              options={stateOptions}
              value={selectedStates}
              getOptionLabel={(option) => option.stateName}
              isOptionEqualToValue={(option, value) => option.id === value.id}
              onChange={(_, newValue) => onUpdateDetails({ stateIds: newValue.map((state) => state.id) })}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Region / States *"
                  size="small"
                  placeholder="Select states"
                  error={Boolean(fieldErrors.states)}
                />
              )}
            />
          </WizardFieldAnchor>
        </Grid>

        <Grid size={{ xs: 12, sm: 6 }}>
          <WizardFieldAnchor field="storeCount" error={fieldErrors.storeCount}>
            <TextField
              label="Number of Participating Stores *"
              type="number"
              fullWidth
              size="small"
              value={asset?.storeCount ?? ''}
              onChange={(e) => onUpdateAsset({ storeCount: e.target.value })}
              error={Boolean(fieldErrors.storeCount)}
              slotProps={{ htmlInput: { min: 1 } }}
            />
          </WizardFieldAnchor>
        </Grid>

        <Grid size={12}>
          <WizardSectionTitle
            title="Store Outlet List *"
            info="Upload list of participating outlets (PDF, Excel, or CSV)."
            variant="subtitle2"
            fontWeight={700}
            mb={1}
          />
          <WizardFieldAnchor field="storeOutletList" error={fieldErrors.storeOutletList}>
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
                border: `2px dashed ${fieldErrors.storeOutletList ? BRAND.red : BRAND.borderLight}`,
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
              <Box component="span" sx={{ display: 'block', mt: 1, fontSize: '0.875rem' }}>
                Selected: <strong>{storeOutletList.fileName}</strong>
              </Box>
            )}
          </WizardFieldAnchor>
        </Grid>
      </Grid>
    </Box>
  );
}
