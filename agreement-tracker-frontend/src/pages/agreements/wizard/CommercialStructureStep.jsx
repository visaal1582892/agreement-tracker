import { Box, Typography, Alert } from '@mui/material';
import HybridCommercialFields from './HybridCommercialFields';
import AssetPayoutFields from './AssetPayoutFields';
import { buildContractDetailsSnapshot, hasPersistedContractDetails } from '../../../utils/agreementWizardUtils';
import { isAssetRentalIncomeType } from '../../../utils/incomeTypeUtils';
import { ADHOC_SUB_TYPES } from '../../../constants/adhocSubTypes';

export default function CommercialStructureStep({
  agreement,
  onUpdateCommercials,
  onUpdateAsset,
  serverAgreementId,
  sourceAgreement,
  versionSourceId,
  buildVersionedEditPayload,
  onDraftVersionCreated,
}) {
  const persisted = hasPersistedContractDetails(sourceAgreement);
  const snapshot = buildContractDetailsSnapshot(sourceAgreement);
  const startDate = snapshot?.startDate ?? agreement.details.startDate;
  const expiryDate = snapshot?.expiryDate ?? agreement.details.expiryDate;
  const lockOneTimeFrequency = agreement.details?.adhocSubType === ADHOC_SUB_TYPES.QPS;
  const isAssetRental = isAssetRentalIncomeType(
    [],
    agreement.details?.incomeTypeId,
    agreement.details?.incomeTypeName,
  );

  return (
    <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h6" fontWeight={600} mb={0.5}>Commercial Structure</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2.5 }}>
        {isAssetRental
          ? 'Configure asset payout amounts for this rental agreement.'
          : 'Configure flat baseline, slab incentives, or a hybrid commercial model.'}
      </Typography>

      {!persisted && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          Contract dates must be saved in Step 1 before configuring commercials. Go back to Foundational Setup and click Next.
        </Alert>
      )}

      {persisted && isAssetRental && (
        <AssetPayoutFields
          asset={agreement.asset}
          onUpdateAsset={onUpdateAsset}
        />
      )}

      {persisted && !isAssetRental && (
        <HybridCommercialFields
          key={`commercials-${serverAgreementId}-${startDate}-${expiryDate}`}
          commercials={agreement.commercials}
          onUpdate={onUpdateCommercials}
          serverAgreementId={serverAgreementId}
          sourceAgreement={sourceAgreement}
          versionSourceId={versionSourceId}
          buildVersionedEditPayload={buildVersionedEditPayload}
          onDraftVersionCreated={onDraftVersionCreated}
          startDate={startDate}
          expiryDate={expiryDate}
          lockOneTimeFrequency={lockOneTimeFrequency}
        />
      )}
    </Box>
  );
}
