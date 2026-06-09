import { Box, Typography, TextField } from '@mui/material';
import AgreementDetailsCard from './AgreementDetailsCard';

export default function Step2Agreements({
  state,
  updateFields,
  updateAgreementDetails,
  updateAgreementCommercials,
  documentErrors,
  onClearDocumentError,
  serverAgreementId,
  sourceAgreement,
  onSaveContractDetails,
}) {
  const agreement = state.agreement;

  return (
    <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h6" fontWeight={600} mb={0.5}>Agreement Details</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Fill in details, commercial structure, and documents for this agreement.
      </Typography>

      <TextField
        label="Agreement Name"
        value={state.agreementName || ''}
        onChange={(e) => updateFields?.({ agreementName: e.target.value })}
        required
        fullWidth
        size="small"
        slotProps={{ htmlInput: { maxLength: 255 } }}
        sx={{ mb: 2.5 }}
      />

      <AgreementDetailsCard
        cardId={`agreement-card-${agreement.id}`}
        agreement={agreement}
        onUpdateDetails={updateAgreementDetails}
        onUpdateCommercials={updateAgreementCommercials}
        documentError={documentErrors?.[agreement.id]}
        onClearDocumentError={() => onClearDocumentError?.(agreement.id)}
        serverAgreementId={serverAgreementId}
        initialContractSnapshot={sourceAgreement}
        onSaveContractDetails={onSaveContractDetails}
      />
    </Box>
  );
}
