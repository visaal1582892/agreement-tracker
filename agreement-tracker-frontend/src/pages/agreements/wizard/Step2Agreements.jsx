import { Box, Typography } from '@mui/material';
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

      {state.agreementName && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2.5 }}>
          Agreement name: <strong>{state.agreementName}</strong> (auto-generated when income type and dates are saved)
        </Typography>
      )}

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
