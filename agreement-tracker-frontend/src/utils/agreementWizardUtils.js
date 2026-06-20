import { formatLocalDateString } from './dateUtils';
import { isAdHocIncomeType, isAssetRentalIncomeType } from './incomeTypeUtils';
import { sanitizeAgreementPayload } from './incomeTypePayloadUtils';
import { ADHOC_SUB_TYPES } from '../constants/adhocSubTypes';
import { CALCULATION_BASIS } from '../constants/calculationBasis';
import {
  deriveHybridFlags,
  PAYMENT_REALIZATION_TYPE,
  PAYOUT_FREQUENCY,
  resolveCommercialStructure,
} from '../constants/commercialStructure';

function buildAssetPayload(asset) {
  if (!asset) return null;
  const assetType = asset.assetType?.trim();
  if (!assetType) return null;
  const payoutMode = asset.payoutMode || 'FLAT';
  return {
    assetCategory: asset.assetCategory || null,
    assetType,
    storeCount: asset.storeCount !== '' && asset.storeCount != null
      ? Number(asset.storeCount)
      : null,
    flatPayout: payoutMode === 'FLAT' && asset.flatPayout !== '' && asset.flatPayout != null
      ? asset.flatPayout
      : null,
    payoutPerStore: payoutMode === 'PER_STORE' && asset.payoutPerStore !== '' && asset.payoutPerStore != null
      ? asset.payoutPerStore
      : null,
    remarks: asset.remarks?.trim() || null,
  };
}

export function buildAgreementDetailsPayload(agreement) {
  if (!agreement) {
    return { details: {}, commercials: {} };
  }
  const { details, commercials } = agreement;
  return {
    details: {
      incomeTypeId: details.incomeTypeId || null,
      agreementTypeId: details.agreementTypeId || null,
      startDate: formatLocalDateString(details.startDate),
      expiryDate: formatLocalDateString(details.expiryDate),
      notes: details.notes || null,
      stateIds: details.stateIds?.length ? details.stateIds : [],
      adhocSubType: details.adhocSubType || null,
      quantityCap: details.quantityCap !== '' && details.quantityCap != null
        ? details.quantityCap
        : null,
      invoiceVendorId: details.invoiceVendorId || null,
      payoutBufferDays: details.payoutBufferDays !== '' && details.payoutBufferDays != null
        ? Number(details.payoutBufferDays)
        : null,
      calculationBasis: details.calculationBasis || CALCULATION_BASIS.VENDOR_INVOICE,
      paymentRealizationType: details.paymentRealizationType || PAYMENT_REALIZATION_TYPE.DIRECT_PAYMENT_INVOICE,
    },
    commercials: (() => {
      const hybridFlags = deriveHybridFlags(commercials.commercialStructure);
      const enableFlatBaseline = commercials.enableFlatBaseline ?? hybridFlags.enableFlatBaseline;
      const enableSlabIncentives = commercials.enableSlabIncentives ?? hybridFlags.enableSlabIncentives;
      return {
        commercialStructure: commercials.commercialStructure
          || resolveCommercialStructure(enableFlatBaseline, enableSlabIncentives),
        commercialValue: commercials.commercialValue || null,
        flatValueType: commercials.flatValueType || commercials.valueType || null,
        flatBaselineFrequency: commercials.flatBaselineFrequency || null,
        enableFlatBaseline,
        enableSlabIncentives,
        calculationFormula: commercials.calculationFormula || null,
      };
    })(),
    asset: buildAssetPayload(agreement.asset),
  };
}

export function buildStep1CreatePayload(state) {
  return {
    companyId: state.companyId,
    companyAgreementGroupId: state.companyAgreementGroupId || null,
    newCompanyAgreementGroupName: state.newCompanyAgreementGroupName?.trim() || null,
    vendorIds: state.vendorIds ?? [],
    productRules: state.productRules ?? {},
    agreements: [],
  };
}

export function buildStep1UpdatePayload(state, { requiresReapproval = false } = {}) {
  const { details, commercials, asset } = buildAgreementDetailsPayload(state.agreement);
  const payload = {
    companyId: state.companyId,
    companyAgreementGroupId: state.companyAgreementGroupId || null,
    newCompanyAgreementGroupName: state.newCompanyAgreementGroupName?.trim() || null,
    vendorIds: state.vendorIds ?? [],
    productRules: state.productRules ?? {},
    details,
    commercials,
    asset,
  };
  if (requiresReapproval) {
    payload.requiresReapproval = true;
  }
  return payload;
}

export function buildSanitizedStep1UpdatePayload(state, options = {}) {
  const payload = buildStep1UpdatePayload(state, options);
  const { details } = state.agreement ?? {};
  return sanitizeAgreementPayload(
    payload,
    options.incomeTypes ?? [],
    details?.incomeTypeId,
    details?.incomeTypeName,
  );
}

export function validateStep1Fields(state, enqueueSnackbar) {
  if (!state.companyId) {
    enqueueSnackbar('Company is required', { variant: 'warning' });
    return false;
  }
  if (!state.companyAgreementGroupId && !state.newCompanyAgreementGroupName?.trim()) {
    enqueueSnackbar('Select or enter a company agreement group', { variant: 'warning' });
    return false;
  }
  return validateFoundationalMetadata(state, enqueueSnackbar);
}

export function validateFoundationalMetadata(state, enqueueSnackbar) {
  const { details } = state.agreement ?? {};
  if (!details?.incomeTypeId) {
    enqueueSnackbar('Income type is required', { variant: 'warning' });
    return false;
  }
  if (!details?.agreementTypeId) {
    enqueueSnackbar('Agreement type is required', { variant: 'warning' });
    return false;
  }
  if (!details?.startDate) {
    enqueueSnackbar('Start date is required', { variant: 'warning' });
    return false;
  }
  if (!details?.expiryDate) {
    enqueueSnackbar('Expiry date is required', { variant: 'warning' });
    return false;
  }
  return true;
}

export function validateCommercialConfigurationStep(state, enqueueSnackbar, incomeTypes = []) {
  if (!validateFoundationalMetadata(state, enqueueSnackbar)) return false;

  const { agreement, productRules } = state;
  const isAssetRental = isAssetRentalIncomeType(
    incomeTypes,
    agreement?.details?.incomeTypeId,
    agreement?.details?.incomeTypeName,
  );
  const isAdHoc = isAdHocIncomeType(
    incomeTypes,
    agreement?.details?.incomeTypeId,
    agreement?.details?.incomeTypeName,
  );

  if (!isAssetRental && !state.vendorIds?.length) {
    enqueueSnackbar('Select at least one supply vendor', { variant: 'warning' });
    return false;
  }

  if (!validateContractDetailsFields(agreement?.details, enqueueSnackbar, {
    skipDocuments: isAssetRental,
    skipFoundational: true,
  })) {
    return false;
  }

  if (isAssetRental) {
    if (!validateAssetRentalConfigurationFields(agreement, enqueueSnackbar)) return false;
    return validateSettlementRoutingFields(agreement?.details, true, enqueueSnackbar);
  }
  if (isAdHoc) {
    if (!validateAdHocFields(agreement, enqueueSnackbar)) return false;
    if (!productRules?.productRules?.length) {
      enqueueSnackbar('Select at least one product', { variant: 'warning' });
      return false;
    }
    return validateSettlementRoutingFields(agreement?.details, false, enqueueSnackbar);
  }

  if (!productRules?.productRules?.length) {
    enqueueSnackbar('Select at least one product', { variant: 'warning' });
    return false;
  }
  return validateSettlementRoutingFields(agreement?.details, isAssetRental, enqueueSnackbar);
}

function validateSettlementRoutingFields(details, isAssetRental, enqueueSnackbar) {
  if (!details?.paymentRealizationType) {
    enqueueSnackbar('Payment realization type is required', { variant: 'warning' });
    return false;
  }
  const requiresInvoiceVendor = details.paymentRealizationType === PAYMENT_REALIZATION_TYPE.DIRECT_PAYMENT_INVOICE;
  if (requiresInvoiceVendor && !details?.invoiceVendorId) {
    enqueueSnackbar(
      isAssetRental
        ? 'Select the Finance / Non-Trade invoice vendor for Asset Rentals'
        : 'Invoice vendor is required for Direct Payment / Invoice',
      { variant: 'warning' },
    );
    return false;
  }
  if (!isAssetRental && !details?.calculationBasis) {
    enqueueSnackbar('Calculation basis is required', { variant: 'warning' });
    return false;
  }
  return true;
}

function validateAssetRentalConfigurationFields(agreement, enqueueSnackbar) {
  const { asset, details } = agreement ?? {};
  if (!asset?.assetCategory) {
    enqueueSnackbar('Asset category is required for Asset Rentals', { variant: 'warning' });
    return false;
  }
  if (!asset?.assetType?.trim()) {
    enqueueSnackbar('Asset type is required for Asset Rentals', { variant: 'warning' });
    return false;
  }
  if (!details?.stateIds?.length) {
    enqueueSnackbar('Select at least one state for Asset Rentals', { variant: 'warning' });
    return false;
  }
  if (!asset?.storeCount || Number(asset.storeCount) <= 0) {
    enqueueSnackbar('Enter the number of participating stores', { variant: 'warning' });
    return false;
  }
  if (!details?.storeOutletList) {
    enqueueSnackbar('Upload the list of participating outlets', { variant: 'warning' });
    return false;
  }
  return true;
}

function validateAssetRentalPayoutFields(agreement, enqueueSnackbar) {
  const { asset } = agreement ?? {};
  if (asset?.payoutMode === 'PER_STORE') {
    if (!asset.payoutPerStore || Number(asset.payoutPerStore) <= 0) {
      enqueueSnackbar('Enter payout per store', { variant: 'warning' });
      return false;
    }
  } else if (!asset?.flatPayout || Number(asset.flatPayout) <= 0) {
    enqueueSnackbar('Enter flat payout amount', { variant: 'warning' });
    return false;
  }
  return true;
}

function validateAdHocFields(agreement, enqueueSnackbar) {
  const { details, commercials } = agreement ?? {};
  if (!details?.adhocSubType) {
    enqueueSnackbar('Select QPS or Consumer Price Off for Ad-Hoc Activities', { variant: 'warning' });
    return false;
  }
  if (details.adhocSubType === ADHOC_SUB_TYPES.QPS) {
    return true;
  }
  if (details.adhocSubType === ADHOC_SUB_TYPES.CONSUMER_PRICE_OFF) {
    if (!details.stateIds?.length) {
      enqueueSnackbar('Select at least one state for Consumer Price Off', { variant: 'warning' });
      return false;
    }
    if (!details.quantityCap || Number(details.quantityCap) <= 0) {
      enqueueSnackbar('Enter quantity / value cap for Consumer Price Off', { variant: 'warning' });
      return false;
    }
  }
  return true;
}

export function validateConfigurationStep(state, incomeTypes, enqueueSnackbar) {
  return validateCommercialConfigurationStep(state, enqueueSnackbar, incomeTypes);
}

export function validateContractDetailsFields(details, enqueueSnackbar, { skipDocuments = false, skipFoundational = false } = {}) {
  if (!skipFoundational) {
    if (!details?.incomeTypeId) {
      enqueueSnackbar('Select income type before saving contract details', { variant: 'warning' });
      return false;
    }
    if (!details?.agreementTypeId) {
      enqueueSnackbar('Select agreement type before saving contract details', { variant: 'warning' });
      return false;
    }
    if (!details?.startDate || !details?.expiryDate) {
      enqueueSnackbar('Start and expiry dates are required before saving contract details', { variant: 'warning' });
      return false;
    }
  }
  if (!skipDocuments && !details?.documents?.length) {
    enqueueSnackbar('At least one document is required', { variant: 'warning' });
    return false;
  }
  return true;
}

export function hasPersistedContractDetails(agreement) {
  if (!agreement) return false;
  const incomeTypeId = agreement.incomeTypeId ?? agreement.details?.incomeTypeId;
  const agreementTypeId = agreement.agreementTypeId ?? agreement.details?.agreementTypeId;
  const startDate = agreement.startDate ?? agreement.details?.startDate;
  const expiryDate = agreement.expiryDate ?? agreement.details?.expiryDate;
  return !!(incomeTypeId && agreementTypeId && startDate && expiryDate);
}

export function buildContractDetailsSnapshot(agreement) {
  if (!agreement) return null;
  return {
    incomeTypeId: agreement.incomeTypeId ?? agreement.details?.incomeTypeId ?? null,
    agreementTypeId: agreement.agreementTypeId ?? agreement.details?.agreementTypeId ?? null,
    startDate: agreement.startDate ?? agreement.details?.startDate ?? null,
    expiryDate: agreement.expiryDate ?? agreement.details?.expiryDate ?? null,
    notes: agreement.notes ?? agreement.details?.notes ?? '',
    stateIds: agreement.stateIds ?? agreement.details?.stateIds ?? [],
  };
}

export function validateAgreementDetailsStep(state, enqueueSnackbar, incomeTypes = []) {
  return validateCommercialConfigurationStep(state, enqueueSnackbar, incomeTypes);
}

export function validateCommercialStructureStep(state, enqueueSnackbar, incomeTypes = []) {
  const { agreement } = state;
  const isAssetRental = isAssetRentalIncomeType(
    incomeTypes,
    agreement?.details?.incomeTypeId,
    agreement?.details?.incomeTypeName,
  );
  if (isAssetRental) {
    return validateAssetRentalPayoutFields(agreement, enqueueSnackbar);
  }

  const { commercials } = agreement ?? {};
  const enableFlat = commercials?.enableFlatBaseline
    ?? ['FLAT', 'HYBRID'].includes(commercials?.commercialStructure);
  const enableSlab = commercials?.enableSlabIncentives
    ?? ['SLAB', 'HYBRID'].includes(commercials?.commercialStructure);

  if (!enableFlat && !enableSlab) {
    enqueueSnackbar('Enable at least one commercial component', { variant: 'warning' });
    return false;
  }
  if (enableFlat) {
    if (!commercials?.commercialValue) {
      enqueueSnackbar('Flat baseline value is required', { variant: 'warning' });
      return false;
    }
    if (!commercials?.flatBaselineFrequency) {
      enqueueSnackbar('Flat baseline frequency is required', { variant: 'warning' });
      return false;
    }
  }
  return true;
}

export function validateCurrentAgreementDetails(state, enqueueSnackbar) {
  if (!validateAgreementDetailsStep(state, enqueueSnackbar)) return false;
  return validateCommercialStructureStep(state, enqueueSnackbar);
}

export function validateStep2LoopFields(state, enqueueSnackbar) {
  return validateFoundationalMetadata(state, enqueueSnackbar);
}

export function internalStepFromUrl(urlStep) {
  const parsed = Number.parseInt(urlStep, 10);
  if (Number.isNaN(parsed) || parsed < 1 || parsed > 4) return null;
  return parsed - 1;
}

export function urlStepFromInternal(internalStep) {
  return internalStep + 1;
}
