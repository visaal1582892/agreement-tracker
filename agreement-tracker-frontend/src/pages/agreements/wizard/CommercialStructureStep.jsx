import { Box, Typography, Alert } from '@mui/material';
import CommercialFields from './CommercialFields';
import { buildContractDetailsSnapshot, hasPersistedContractDetails } from '../../../utils/agreementWizardUtils';

export default function CommercialStructureStep({
  agreement,
  onUpdateCommercials,
  serverAgreementId,
  sourceAgreement,
}) {
  const persisted = hasPersistedContractDetails(sourceAgreement);
  const snapshot = buildContractDetailsSnapshot(sourceAgreement);
  const startDate = snapshot?.startDate ?? agreement.details.startDate;
  const expiryDate = snapshot?.expiryDate ?? agreement.details.expiryDate;

  return (
    <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h6" fontWeight={600} mb={0.5}>Commercial Structure</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2.5 }}>
        Configure flat or slab-based commercial rules for this agreement.
      </Typography>

      {!persisted && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          Contract dates must be saved before configuring commercials. Go back to Agreement Details and click Next.
        </Alert>
      )}

      {persisted && (
        <CommercialFields
          key={`commercials-${serverAgreementId}-${startDate}-${expiryDate}`}
          commercials={agreement.commercials}
          onUpdate={onUpdateCommercials}
          serverAgreementId={serverAgreementId}
          startDate={startDate}
          expiryDate={expiryDate}
        />
      )}
    </Box>
  );
}
