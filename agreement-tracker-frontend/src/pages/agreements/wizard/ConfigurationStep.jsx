import { useEffect, useRef, useState } from 'react';
import {
  Autocomplete, Box, Typography, Grid, TextField, Button, Chip, FormControl, Select, MenuItem,
  FormHelperText, IconButton, alpha, Paper, Divider, Alert,
} from '@mui/material';
import { UploadFile, Delete } from '@mui/icons-material';
import axiosInstance from '../../../api/axiosInstance';
import { ENDPOINTS } from '../../../config/endpoints';
import { BRAND } from '../../../config/theme';
import Step2Products from './Step2Products';
import Step2SupplyVendors from './Step2SupplyVendors';
import AssetRentalFields from './AssetRentalFields';
import AdHocActivityFields from './AdHocActivityFields';
import SettlementRoutingFields from './SettlementRoutingFields';
import { isAdHocIncomeType, isAssetRentalIncomeType } from '../../../utils/incomeTypeUtils';

const DOCUMENT_TYPES = ['AGREEMENT', 'SUPPORTING_DOC', 'EMAIL', 'OTHER'];

export default function ConfigurationStep({
  state,
  agreement,
  onUpdateDetails,
  onUpdateAsset,
  onUpdateCommercials,
  updateProductRules,
  updateFields,
  documentError,
  onClearDocumentError,
}) {
  const [incomeTypes, setIncomeTypes] = useState([]);
  const [stateOptions, setStateOptions] = useState([]);
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef(null);
  const details = agreement.details;
  const asset = agreement.asset ?? {};

  useEffect(() => {
    axiosInstance.get(ENDPOINTS.INCOME_TYPES).then(({ data }) => setIncomeTypes(data));
    axiosInstance.get(ENDPOINTS.STATES).then(({ data }) => setStateOptions(Array.isArray(data) ? data : []));
  }, []);

  const isAssetRental = isAssetRentalIncomeType(
    incomeTypes,
    details.incomeTypeId,
    details.incomeTypeName,
  );
  const isAdHoc = isAdHocIncomeType(
    incomeTypes,
    details.incomeTypeId,
    details.incomeTypeName,
  );
  const selectedStateIds = details.stateIds ?? [];
  const selectedStates = stateOptions.filter((stateOption) => selectedStateIds.includes(stateOption.id));
  const documents = details.documents || [];
  const hasIncomeType = Boolean(details.incomeTypeId);

  const addDocument = (file) => {
    if (!file) return;
    const doc = {
      file,
      fileName: file.name,
      documentType: 'SUPPORTING_DOC',
      preview: URL.createObjectURL(file),
    };
    onUpdateDetails({ documents: [...documents, doc] });
    onClearDocumentError?.();
  };

  const removeDoc = (idx) => {
    onUpdateDetails({ documents: documents.filter((_, i) => i !== idx) });
  };

  return (
    <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h6" fontWeight={600} mb={0.5}>Commercial Configuration</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Configure vendors, scope, and commercial terms based on the income type selected in Step 1.
      </Typography>

      {state.agreementName && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2.5 }}>
          Agreement name: <strong>{state.agreementName}</strong>
        </Typography>
      )}

      {!hasIncomeType && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          Select income type, agreement type, and dates in Step 1 before configuring commercial details.
        </Alert>
      )}

      <Paper
        elevation={0}
        sx={{
          p: 2.5,
          borderRadius: 2.5,
          border: `1px solid ${BRAND.borderLight}`,
          bgcolor: BRAND.white,
        }}
      >
        {!isAssetRental && (
          <Step2SupplyVendors
            vendorIds={state.vendorIds}
            onVendorChange={(vendorIds) => updateFields({ vendorIds })}
          />
        )}

        {hasIncomeType && !isAssetRental && <Divider sx={{ my: 3 }} />}

        {!hasIncomeType ? null : isAssetRental ? (
          <AssetRentalFields
            asset={asset}
            stateOptions={stateOptions}
            selectedStateIds={selectedStateIds}
            storeOutletList={details.storeOutletList}
            onUpdateAsset={onUpdateAsset}
            onUpdateDetails={onUpdateDetails}
          />
        ) : isAdHoc ? (
          <AdHocActivityFields
            state={state}
            details={details}
            commercials={agreement.commercials}
            stateOptions={stateOptions}
            updateProductRules={updateProductRules}
            onUpdateDetails={onUpdateDetails}
            onUpdateCommercials={onUpdateCommercials}
          />
        ) : (
          <>
            <Step2Products state={state} updateProductRules={updateProductRules} />

            <Grid container spacing={3} sx={{ mt: 1 }}>
              <Grid size={12}>
                <Autocomplete
                  multiple
                  options={stateOptions}
                  value={selectedStates}
                  getOptionLabel={(option) => option.stateName}
                  isOptionEqualToValue={(option, value) => option.id === value.id}
                  onChange={(_, newValue) => onUpdateDetails({ stateIds: newValue.map((stateOption) => stateOption.id) })}
                  renderInput={(params) => (
                    <TextField {...params} label="States" size="small" placeholder="Select states" />
                  )}
                />
              </Grid>
            </Grid>
          </>
        )}

        {hasIncomeType && (
          <>
            <Divider sx={{ my: 3 }} />

            <SettlementRoutingFields
              vendorIds={state.vendorIds}
              invoiceVendorId={details.invoiceVendorId}
              payoutBufferDays={details.payoutBufferDays}
              calculationBasis={details.calculationBasis}
              paymentRealizationType={details.paymentRealizationType}
              incomeTypeId={details.incomeTypeId}
              incomeTypeName={details.incomeTypeName}
              onUpdateDetails={onUpdateDetails}
            />

            <Divider sx={{ my: 3 }} />

            <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 1.5 }}>
              Documents *
            </Typography>
            <Box
              onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
              onDragLeave={() => setDragOver(false)}
              onDrop={(e) => {
                e.preventDefault();
                setDragOver(false);
                addDocument(e.dataTransfer.files[0]);
              }}
              onClick={() => fileInputRef.current?.click()}
              sx={{
                border: `2px dashed ${documentError ? BRAND.red : BRAND.borderLight}`,
                borderRadius: '10px',
                bgcolor: dragOver ? alpha(BRAND.red, 0.04) : BRAND.bgGray,
                p: 2.5,
                textAlign: 'center',
                mb: 1.5,
                cursor: 'pointer',
                transition: 'background-color 0.15s ease, border-color 0.15s ease',
                '&:hover': { bgcolor: alpha(BRAND.red, 0.03), borderColor: '#94A3B8' },
              }}
            >
              <UploadFile sx={{ fontSize: 36, color: BRAND.textSecondary, mb: 0.5 }} />
              <Typography variant="body2" color="text.secondary" mb={1}>
                Drag & drop or click to upload
              </Typography>
              <Button
                variant="outlined"
                size="small"
                onClick={(e) => { e.stopPropagation(); fileInputRef.current?.click(); }}
              >
                Browse Files
              </Button>
              <input
                ref={fileInputRef}
                type="file"
                hidden
                onChange={(e) => {
                  addDocument(e.target.files[0]);
                  e.target.value = '';
                }}
                accept=".pdf,.doc,.docx,.jpg,.jpeg,.png,.eml"
              />
            </Box>

            {documentError && (
              <FormHelperText error sx={{ mb: 1 }}>{documentError}</FormHelperText>
            )}

            {documents.length > 0 && (
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                {documents.map((doc, i) => (
                  <Box
                    key={`${doc.fileName}-${i}`}
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 1.5,
                      border: `1px solid ${BRAND.borderLight}`,
                      borderRadius: '8px',
                      px: 1.5,
                      py: 1,
                      bgcolor: BRAND.white,
                    }}
                  >
                    <Chip label={doc.fileName} size="small" sx={{ flex: 1, justifyContent: 'flex-start', maxWidth: '100%' }} />
                    <FormControl size="small" sx={{ minWidth: 150 }}>
                      <Select
                        value={doc.documentType}
                        onChange={(e) => {
                          const nextDocs = [...documents];
                          nextDocs[i] = { ...nextDocs[i], documentType: e.target.value };
                          onUpdateDetails({ documents: nextDocs });
                        }}
                      >
                        {DOCUMENT_TYPES.map((type) => (
                          <MenuItem key={type} value={type}>{type.replace('_', ' ')}</MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                    <IconButton size="small" color="error" onClick={() => removeDoc(i)} aria-label="Remove document">
                      <Delete fontSize="small" />
                    </IconButton>
                  </Box>
                ))}
              </Box>
            )}
          </>
        )}
      </Paper>
    </Box>
  );
}
