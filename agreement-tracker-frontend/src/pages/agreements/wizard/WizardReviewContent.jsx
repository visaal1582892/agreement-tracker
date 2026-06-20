import { useEffect, useState } from 'react';
import { Box, Typography, Chip, Alert } from '@mui/material';
import dayjs from 'dayjs';
import { useSnackbar } from 'notistack';
import CollapsibleSection from '../../../components/wizard/CollapsibleSection';
import { formatTenureFromDates } from '../../../components/forms/DateRangeFields';
import { fetchSlabs } from '../../../api/commercialApi';
import { INCOME_TYPE_NAMES } from '../../../constants/incomeTypeNames';
import { ADHOC_SUB_TYPES } from '../../../constants/adhocSubTypes';
import { CALCULATION_BASIS_LABELS } from '../../../constants/calculationBasis';
import { PAYMENT_REALIZATION_OPTIONS, PAYOUT_FREQUENCY_OPTIONS, deriveHybridFlags } from '../../../constants/commercialStructure';
import CommercialsUploadModal from './CommercialsUploadModal';

function ReviewRow({ label, value }) {
  return (
    <Box sx={{ display: 'flex', py: 0.75 }}>
      <Typography variant="body2" color="text.secondary" sx={{ width: 180, flexShrink: 0 }}>{label}</Typography>
      <Typography variant="body2" fontWeight={500}>{value || '—'}</Typography>
    </Box>
  );
}

function resolveIncomeProfile(incomeTypeName) {
  if (incomeTypeName === INCOME_TYPE_NAMES.ASSET_RENTALS) return 'ASSET_RENTAL';
  if (incomeTypeName === INCOME_TYPE_NAMES.DATA_FEE) return 'DATA_FEE';
  if (incomeTypeName === INCOME_TYPE_NAMES.AD_HOC_ACTIVITIES) return 'AD_HOC';
  if (incomeTypeName === INCOME_TYPE_NAMES.COMMERCIAL_CONTRACTS) return 'COMMERCIAL_CONTRACTS';
  return 'STANDARD';
}

function formatCommercialValue(value, valueType) {
  if (!value) return '—';
  if (valueType === 'PERCENTAGE') return `${value}%`;
  return `₹${Number(value).toLocaleString('en-IN')}`;
}

function payoutFrequencyLabel(value) {
  return PAYOUT_FREQUENCY_OPTIONS.find((option) => option.value === value)?.label || value || '—';
}

function paymentRealizationLabel(value) {
  return PAYMENT_REALIZATION_OPTIONS.find((option) => option.value === value)?.label || value || '—';
}

export default function WizardReviewContent({
  wizardState,
  version,
  serverAgreementId,
  slabs: initialSlabs,
  storeOutletFileName,
}) {
  const { enqueueSnackbar } = useSnackbar();
  const [slabs, setSlabs] = useState(initialSlabs ?? []);
  const [loadingSlabs, setLoadingSlabs] = useState(false);

  const details = wizardState?.agreement?.details ?? {};
  const asset = wizardState?.agreement?.asset ?? {};
  const commercials = wizardState?.agreement?.commercials ?? {};
  const productRules = wizardState?.productRules ?? {};
  const incomeTypeName = version?.incomeTypeName ?? details.incomeTypeName;
  const profile = resolveIncomeProfile(incomeTypeName);
  const hybridFlags = deriveHybridFlags(commercials.commercialStructure);
  const enableFlat = commercials.enableFlatBaseline ?? hybridFlags.enableFlatBaseline;
  const enableSlab = commercials.enableSlabIncentives ?? hybridFlags.enableSlabIncentives;
  const isQps = profile === 'AD_HOC' && details.adhocSubType === ADHOC_SUB_TYPES.QPS;
  const stateNames = version?.states?.map((s) => s.stateName).join(', ')
    || (details.stateIds?.length ? `${details.stateIds.length} state(s) selected` : '—');

  useEffect(() => {
    if (!serverAgreementId || !enableSlab) {
      setSlabs(initialSlabs ?? []);
      return;
    }
    if (initialSlabs) {
      setSlabs(initialSlabs);
      return;
    }
    let cancelled = false;
    const load = async () => {
      setLoadingSlabs(true);
      try {
        const data = await fetchSlabs(serverAgreementId);
        if (!cancelled) setSlabs(data);
      } catch (err) {
        if (!cancelled) {
          enqueueSnackbar(err.response?.data?.message || 'Failed to load slabs', { variant: 'error' });
          setSlabs([]);
        }
      } finally {
        if (!cancelled) setLoadingSlabs(false);
      }
    };
    load();
    return () => { cancelled = true; };
  }, [serverAgreementId, enableSlab, initialSlabs, enqueueSnackbar]);

  return (
    <Box>
      <CollapsibleSection title="Partner Details">
        <ReviewRow label="Company" value={wizardState?.companyName} />
        <ReviewRow label="Agreement Group" value={wizardState?.companyAgreementGroupName} />
      </CollapsibleSection>

      <CollapsibleSection title="Agreement Classification">
        <ReviewRow label="Income Type" value={incomeTypeName} />
        <ReviewRow label="Agreement Type" value={version?.agreementTypeName} />
      </CollapsibleSection>

      <CollapsibleSection title="Duration & Notes">
        <ReviewRow
          label="Start Date"
          value={details.startDate ? dayjs(details.startDate).format('DD MMM YYYY') : version?.startDate ? dayjs(version.startDate).format('DD MMM YYYY') : ''}
        />
        <ReviewRow
          label="Expiry Date"
          value={details.expiryDate ? dayjs(details.expiryDate).format('DD MMM YYYY') : version?.expiryDate ? dayjs(version.expiryDate).format('DD MMM YYYY') : ''}
        />
        <ReviewRow
          label="Tenure"
          value={formatTenureFromDates(
            details.startDate ?? version?.startDate,
            details.expiryDate ?? version?.expiryDate,
          )}
        />
        {(details.notes || version?.notes) && (
          <ReviewRow label="Notes" value={details.notes || version?.notes} />
        )}
      </CollapsibleSection>

      {profile !== 'ASSET_RENTAL' && (
        <CollapsibleSection title="Scope & Operations">
          <ReviewRow label="Supply Vendors" value={`${wizardState?.vendorIds?.length || 0} selected`} />
          <ReviewRow label="Manufacturers" value={`${productRules.manufacturers?.length || 0} selected`} />
          <ReviewRow label="Division Rules" value={`${productRules.divisionRules?.length || 0} rule(s)`} />
          <ReviewRow label="Product Rules" value={`${productRules.productRules?.length || 0} rule(s)`} />
          {profile === 'AD_HOC' && details.adhocSubType && (
            <ReviewRow label="Activity Type" value={details.adhocSubType.replace('_', ' ')} />
          )}
        </CollapsibleSection>
      )}

      {profile === 'ASSET_RENTAL' && (
        <CollapsibleSection title="Scope & Operations">
          <ReviewRow label="Asset Category" value={asset.assetCategory || version?.asset?.assetCategory} />
          <ReviewRow label="Asset Type" value={asset.assetType || version?.asset?.assetType} />
          <ReviewRow label="Remarks" value={asset.remarks || version?.asset?.remarks} />
        </CollapsibleSection>
      )}

      {(profile === 'ASSET_RENTAL' || profile === 'COMMERCIAL_CONTRACTS' || profile === 'DATA_FEE'
        || (profile === 'AD_HOC' && details.adhocSubType === ADHOC_SUB_TYPES.CONSUMER_PRICE_OFF)) && (
        <CollapsibleSection title="Geography & Limits">
          {profile === 'ASSET_RENTAL' && (
            <>
              <ReviewRow label="States" value={stateNames} />
              <ReviewRow label="Participating Stores" value={asset.storeCount || version?.asset?.storeCount} />
              <ReviewRow
                label="Store Outlet List"
                value={storeOutletFileName || details.storeOutletList?.fileName || '—'}
              />
            </>
          )}
          {(profile === 'COMMERCIAL_CONTRACTS' || profile === 'DATA_FEE') && (
            <ReviewRow label="States" value={stateNames} />
          )}
          {profile === 'AD_HOC' && details.adhocSubType === ADHOC_SUB_TYPES.CONSUMER_PRICE_OFF && (
            <>
              <ReviewRow label="States" value={stateNames} />
              <ReviewRow label="Quantity / Value Cap" value={details.quantityCap} />
            </>
          )}
        </CollapsibleSection>
      )}

      <CollapsibleSection title="Settlement & Payment Routing">
        {profile !== 'ASSET_RENTAL' && (
          <ReviewRow
            label="Calculation Basis"
            value={CALCULATION_BASIS_LABELS[details.calculationBasis] || details.calculationBasis}
          />
        )}
        <ReviewRow
          label="Payment Realization"
          value={paymentRealizationLabel(details.paymentRealizationType ?? version?.paymentRealizationType)}
        />
        <ReviewRow label="Payout Lead Time" value={details.payoutBufferDays ? `${details.payoutBufferDays} days` : '—'} />
        <ReviewRow label="Invoice Vendor" value={details.invoiceVendorId ? `ID ${details.invoiceVendorId}` : version?.invoiceVendorId ? `ID ${version.invoiceVendorId}` : '—'} />
      </CollapsibleSection>

      <CollapsibleSection title="Supporting Documents">
        {details.documents?.length ? (
          details.documents.map((doc, i) => (
            <Box key={`${doc.fileName}-${i}`} sx={{ display: 'flex', gap: 1, mb: 0.5 }}>
              <Chip label={doc.documentType} size="small" variant="outlined" />
              <Typography variant="body2">{doc.fileName}</Typography>
            </Box>
          ))
        ) : (
          <Typography variant="body2" color="text.secondary">No documents uploaded in this session</Typography>
        )}
      </CollapsibleSection>

      {profile === 'ASSET_RENTAL' && (
        <CollapsibleSection title="Base Commercials">
          <ReviewRow
            label="Payout Mode"
            value={asset.payoutMode === 'PER_STORE' ? 'Payout per Store' : 'Flat Payout'}
          />
          <ReviewRow
            label="Amount"
            value={asset.payoutMode === 'PER_STORE'
              ? formatCommercialValue(asset.payoutPerStore || version?.asset?.payoutPerStore, 'FIXED')
              : formatCommercialValue(asset.flatPayout || version?.asset?.flatPayout, 'FIXED')}
          />
        </CollapsibleSection>
      )}

      {profile !== 'ASSET_RENTAL' && enableFlat && (
        <CollapsibleSection title="Base Commercials">
          {isQps && (
            <Alert severity="info" sx={{ mb: 2 }}>
              QPS payout frequency is locked to <strong>One-Time</strong>.
            </Alert>
          )}
          <ReviewRow label="Flat Baseline" value="Enabled" />
          <ReviewRow
            label="Value"
            value={formatCommercialValue(
              commercials.commercialValue ?? version?.commercialValue,
              commercials.valueType || commercials.flatValueType || version?.flatValueType,
            )}
          />
          <ReviewRow
            label="Payout Frequency"
            value={payoutFrequencyLabel(commercials.flatBaselineFrequency ?? version?.flatBaselineFrequency)}
          />
        </CollapsibleSection>
      )}

      {profile !== 'ASSET_RENTAL' && enableSlab && (
        <CollapsibleSection title="Performance Targets (Slabs)">
          <ReviewRow label="Slab Incentives" value="Enabled" />
          {loadingSlabs ? (
            <Typography variant="body2" color="text.secondary">Loading slabs…</Typography>
          ) : slabs.length > 0 ? (
            <Box sx={{ mt: 1 }}>
              <CommercialsUploadModal
                embedded
                readOnly
                agreementId={serverAgreementId ?? version?.id}
                slabs={slabs}
                startDate={details.startDate ?? version?.startDate}
                expiryDate={details.expiryDate ?? version?.expiryDate}
              />
            </Box>
          ) : (
            <Typography variant="body2" color="text.secondary">No slab rows configured</Typography>
          )}
        </CollapsibleSection>
      )}
    </Box>
  );
}
