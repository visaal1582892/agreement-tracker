import { Box, Alert } from '@mui/material';
import CollapsibleSection from '../../../components/wizard/CollapsibleSection';
import WizardSectionTitle from '../../../components/wizard/WizardSectionTitle';
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
  fieldErrors = {},
}) {
  const persisted = hasPersistedContractDetails(sourceAgreement);
  const snapshot = buildContractDetailsSnapshot(sourceAgreement);
  const startDate = snapshot?.startDate ?? agreement.details.startDate;
  const expiryDate = snapshot?.expiryDate ?? agreement.details.expiryDate;
  const lockOneTimeFrequency = agreement.details?.adhocSubType === ADHOC_SUB_TYPES.QPS;
  const incomeTypeId = agreement.details?.incomeTypeId ?? sourceAgreement?.incomeTypeId;
  const incomeTypeName = agreement.details?.incomeTypeName ?? sourceAgreement?.incomeTypeName;
  const isAssetRental = isAssetRentalIncomeType([], incomeTypeId, incomeTypeName);
  const assetCommercialHasError = Boolean(fieldErrors.flatPayout || fieldErrors.payoutPerStore);

  return (
    <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      <WizardSectionTitle
        title="Commercial Structure"
        info={
          isAssetRental
            ? 'Configure asset payout amounts.'
            : 'Configure flat baseline and performance slab targets.'
        }
        mb={2.5}
      />

      {!persisted && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          Contract dates must be saved in Step 1 before configuring commercials.
        </Alert>
      )}

      {persisted && isAssetRental && (
        <CollapsibleSection
          title="Base Commercials"
          description="Flat payout or per-store payout for this asset rental."
          forceExpand={assetCommercialHasError}
          hasError={assetCommercialHasError}
        >
          <AssetPayoutFields
            asset={agreement.asset}
            onUpdateAsset={onUpdateAsset}
            hideSectionTitle
            fieldErrors={fieldErrors}
          />
        </CollapsibleSection>
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
          fieldErrors={fieldErrors}
        />
      )}
    </Box>
  );
}
