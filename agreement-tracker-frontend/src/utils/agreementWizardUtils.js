import { fetchSlabs } from '../api/commercialApi';
import { formatLocalDateString } from './dateUtils';
import { isAdHocIncomeType, isAssetRentalIncomeType, isDataFeeIncomeType, resolveWizardIncomeContext } from './incomeTypeUtils';
import { sanitizeAgreementPayload } from './incomeTypePayloadUtils';
import { ADHOC_SUB_TYPES } from '../constants/adhocSubTypes';
import { CALCULATION_BASIS } from '../constants/calculationBasis';
import {
  deriveHybridFlags,
  PAYMENT_REALIZATION_TYPE,
  PAYOUT_FREQUENCY,
  resolveCommercialStructure,
} from '../constants/commercialStructure';
import { getFirstWizardFieldErrorMessage } from './wizardValidationUx';

export function mapCommercialsFromApi(agreement, slabCount = null) {
  const structure = agreement.commercialStructure ?? 'FLAT';
  let enableFlatBaseline = structure === 'FLAT' || structure === 'HYBRID';
  let enableSlabIncentives = structure === 'SLAB' || structure === 'HYBRID';

  if (slabCount === 0 && (structure === 'HYBRID' || structure === 'SLAB')) {
    enableSlabIncentives = false;
    enableFlatBaseline = true;
  }

  const commercialStructure = resolveCommercialStructure(enableFlatBaseline, enableSlabIncentives) ?? 'FLAT';

  return {
    commercialStructure,
    commercialValue: agreement.commercialValue ?? '',
    valueType: agreement.flatValueType ?? agreement.valueType ?? 'FIXED',
    flatValueType: agreement.flatValueType ?? agreement.valueType ?? 'FIXED',
    flatBaselineFrequency: agreement.flatBaselineFrequency ?? 'MONTHLY',
    enableFlatBaseline,
    enableSlabIncentives,
    calculationFormula: agreement.calculationFormula ?? '',
    selectedFrequencies: [],
    slabType: 'PURCHASE',
  };
}

export async function fetchSlabCountForVersion(versionId) {
  if (!versionId) return null;
  try {
    const slabs = await fetchSlabs(versionId);
    return Array.isArray(slabs) ? slabs.length : 0;
  } catch {
    return null;
  }
}

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
  const ctx = resolveWizardIncomeContext(state, options.sourceAgreement, options.incomeTypes ?? []);
  return sanitizeAgreementPayload(
    payload,
    ctx.incomeTypes,
    ctx.incomeTypeId,
    ctx.incomeTypeName,
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

export function validateCommercialConfigurationStep(state, enqueueSnackbar, incomeTypes = [], sourceAgreement = null) {
  if (!validateFoundationalMetadata(state, enqueueSnackbar)) return false;

  const fieldErrors = collectConfigurationStepErrors(state, incomeTypes, sourceAgreement);
  if (Object.keys(fieldErrors).length === 0) return true;

  enqueueSnackbar(getFirstWizardFieldErrorMessage(fieldErrors), { variant: 'warning' });
  return false;
}

function collectSettlementRoutingFieldErrors(details, isAssetRental) {
  const errors = {};
  if (!details?.paymentRealizationType) {
    errors.paymentRealization = 'Payment realization type is required';
  }
  const requiresInvoiceVendor = details?.paymentRealizationType === PAYMENT_REALIZATION_TYPE.DIRECT_PAYMENT_INVOICE;
  if (requiresInvoiceVendor && !details?.invoiceVendorId) {
    errors.invoiceVendor = isAssetRental
      ? 'Select the Finance / Non-Trade invoice vendor for Asset Rentals'
      : 'Invoice vendor is required for Direct Payment / Invoice';
  }
  if (!isAssetRental && !details?.calculationBasis) {
    errors.calculationBasis = 'Calculation basis is required';
  }
  return errors;
}

export function collectConfigurationStepErrors(state, incomeTypes = [], sourceAgreement = null) {
  const fieldErrors = {};
  const ctx = resolveWizardIncomeContext(state, sourceAgreement, incomeTypes);
  const { agreement, productRules } = state;
  const details = agreement?.details ?? {};
  const asset = agreement?.asset ?? {};
  const isAssetRental = isAssetRentalIncomeType(
    ctx.incomeTypes,
    ctx.incomeTypeId,
    ctx.incomeTypeName,
  );
  const isAdHoc = isAdHocIncomeType(
    ctx.incomeTypes,
    ctx.incomeTypeId,
    ctx.incomeTypeName,
  );
  const isDataFee = isDataFeeIncomeType(
    ctx.incomeTypes,
    ctx.incomeTypeId,
    ctx.incomeTypeName,
  );

  if (!isAssetRental && !state.vendorIds?.length) {
    fieldErrors.supplyVendors = 'Select at least one supply vendor';
  }

  if (!isAssetRental && !details.documents?.length) {
    fieldErrors.documents = 'At least one document is required';
  }

  if (isAssetRental) {
    if (!asset?.assetCategory) {
      fieldErrors.assetCategory = 'Asset category is required for Asset Rentals';
    }
    if (!asset?.assetType?.trim()) {
      fieldErrors.assetType = 'Asset type is required for Asset Rentals';
    }
    if (!details.stateIds?.length) {
      fieldErrors.states = 'Select at least one state for Asset Rentals';
    }
    if (!asset?.storeCount || Number(asset.storeCount) <= 0) {
      fieldErrors.storeCount = 'Enter the number of participating stores';
    }
    if (!hasStoreOutletListDocument(details)) {
      fieldErrors.storeOutletList = 'Upload the list of participating outlets';
    }
    Object.assign(fieldErrors, collectSettlementRoutingFieldErrors(details, true));
    return fieldErrors;
  }

  if (isDataFee && !details.stateIds?.length) {
    fieldErrors.states = 'Select at least one state for Data Fee';
  }

  if (isAdHoc) {
    if (!details.adhocSubType) {
      fieldErrors.adhocSubType = 'Select QPS or Consumer Price Off for Ad-Hoc Activities';
    } else if (details.adhocSubType === ADHOC_SUB_TYPES.CONSUMER_PRICE_OFF) {
      if (!details.stateIds?.length) {
        fieldErrors.states = 'Select at least one state for Consumer Price Off';
      }
      if (!details.quantityCap || Number(details.quantityCap) <= 0) {
        fieldErrors.quantityCap = 'Enter quantity / value cap for Consumer Price Off';
      }
    }
    if (!productRules?.productRules?.length) {
      fieldErrors.products = 'Select at least one product';
    }
    Object.assign(fieldErrors, collectSettlementRoutingFieldErrors(details, false));
    return fieldErrors;
  }

  if (!productRules?.productRules?.length) {
    fieldErrors.products = 'Select at least one product';
  }
  Object.assign(fieldErrors, collectSettlementRoutingFieldErrors(details, false));
  return fieldErrors;
}

function resolveCommercialEnableFlags(commercials = {}) {
  const enableFlat = commercials.enableFlatBaseline === true
    || (commercials.enableFlatBaseline == null
      && ['FLAT', 'HYBRID'].includes(commercials.commercialStructure));
  const enableSlab = commercials.enableSlabIncentives === true
    || (commercials.enableSlabIncentives == null
      && ['SLAB', 'HYBRID'].includes(commercials.commercialStructure));
  return { enableFlat, enableSlab };
}

export function collectCommercialStructureStepErrors(state, incomeTypes = [], sourceAgreement = null) {
  const fieldErrors = {};
  const ctx = resolveWizardIncomeContext(state, sourceAgreement, incomeTypes);
  const isAssetRental = isAssetRentalIncomeType(
    ctx.incomeTypes,
    ctx.incomeTypeId,
    ctx.incomeTypeName,
  );
  const agreement = state.agreement ?? {};
  const asset = agreement.asset ?? {};
  const commercials = agreement.commercials ?? {};

  if (isAssetRental) {
    if (asset?.payoutMode === 'PER_STORE') {
      if (!asset.payoutPerStore || Number(asset.payoutPerStore) <= 0) {
        fieldErrors.payoutPerStore = 'Enter payout per store';
      }
    } else if (!asset?.flatPayout || Number(asset.flatPayout) <= 0) {
      fieldErrors.flatPayout = 'Enter flat payout amount';
    }
    return fieldErrors;
  }

  const { enableFlat, enableSlab } = resolveCommercialEnableFlags(commercials);

  if (!enableFlat && !enableSlab) {
    fieldErrors.commercialComponent = 'Enable at least one commercial component';
  }
  if (enableFlat) {
    if (!commercials.commercialValue) {
      fieldErrors.commercialValue = 'Flat baseline value is required';
    }
    if (!commercials.flatBaselineFrequency) {
      fieldErrors.flatBaselineFrequency = 'Flat baseline frequency is required';
    }
  }
  return fieldErrors;
}

export async function collectCommercialStructureStepErrorsAsync(
  state,
  incomeTypes = [],
  sourceAgreement = null,
  serverAgreementId = null,
) {
  const fieldErrors = collectCommercialStructureStepErrors(state, incomeTypes, sourceAgreement);
  const ctx = resolveWizardIncomeContext(state, sourceAgreement, incomeTypes);
  const isAssetRental = isAssetRentalIncomeType(
    ctx.incomeTypes,
    ctx.incomeTypeId,
    ctx.incomeTypeName,
  );
  if (isAssetRental) return fieldErrors;

  const { enableSlab } = resolveCommercialEnableFlags(state.agreement?.commercials ?? {});
  if (!enableSlab || fieldErrors.commercialComponent) return fieldErrors;

  const versionId = serverAgreementId ?? sourceAgreement?.id;
  if (!versionId) {
    fieldErrors.slabs = 'Please add at least one slab row, or disable Slab-Based Incentives.';
    return fieldErrors;
  }

  try {
    const slabs = await fetchSlabs(versionId);
    if (!Array.isArray(slabs) || slabs.length === 0) {
      fieldErrors.slabs = 'Please add at least one slab row, or disable Slab-Based Incentives.';
    }
  } catch {
    fieldErrors.slabs = 'Unable to validate slab incentives';
  }
  return fieldErrors;
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
  if (!hasStoreOutletListDocument(details)) {
    enqueueSnackbar('Upload the list of participating outlets', { variant: 'warning' });
    return false;
  }
  return true;
}

function hasStoreOutletListDocument(details) {
  const outlet = details?.storeOutletList;
  if (
    outlet?.documentType === 'STORE_OUTLET_LIST'
    && (outlet.file || outlet.fileName)
  ) {
    return true;
  }
  const documents = details?.documents ?? [];
  return documents.some(
    (doc) => doc.documentType === 'STORE_OUTLET_LIST' && (doc.file || doc.fileName || doc.id),
  );
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

export function validateAgreementDetailsStep(state, enqueueSnackbar, incomeTypes = [], sourceAgreement = null) {
  return validateCommercialConfigurationStep(state, enqueueSnackbar, incomeTypes, sourceAgreement);
}

export function validateCommercialStructureStepSync(
  state,
  enqueueSnackbar,
  incomeTypes = [],
  sourceAgreement = null,
) {
  const base = validateCommercialStructureBase(state, enqueueSnackbar, incomeTypes, sourceAgreement);
  if (!base.ok) return false;
  if (base.enableSlab) return false;
  return true;
}

function validateCommercialStructureBase(
  state,
  enqueueSnackbar,
  incomeTypes = [],
  sourceAgreement = null,
) {
  const fieldErrors = collectCommercialStructureStepErrors(state, incomeTypes, sourceAgreement);
  const { enableSlab } = resolveCommercialEnableFlags(state.agreement?.commercials ?? {});

  if (Object.keys(fieldErrors).length > 0) {
    enqueueSnackbar(getFirstWizardFieldErrorMessage(fieldErrors), { variant: 'warning' });
    return { ok: false, enableSlab };
  }
  return { ok: true, enableSlab };
}

export function resolveHighestAccessibleStep(state, sourceAgreement = null, incomeTypes = []) {
  const noop = () => {};
  if (!validateStep1Fields(state, noop)) return 0;
  if (!validateCommercialConfigurationStep(state, noop, incomeTypes, sourceAgreement)) return 1;
  if (!validateCommercialStructureStepSync(state, noop, incomeTypes, sourceAgreement)) return 2;
  return 3;
}

export async function validateCommercialStructureStep(
  state,
  enqueueSnackbar,
  incomeTypes = [],
  sourceAgreement = null,
  serverAgreementId = null,
) {
  const fieldErrors = await collectCommercialStructureStepErrorsAsync(
    state,
    incomeTypes,
    sourceAgreement,
    serverAgreementId,
  );
  if (Object.keys(fieldErrors).length === 0) return true;
  enqueueSnackbar(getFirstWizardFieldErrorMessage(fieldErrors), { variant: 'warning' });
  return false;
}

export async function validateCurrentAgreementDetails(
  state,
  enqueueSnackbar,
  incomeTypes = [],
  sourceAgreement = null,
  serverAgreementId = null,
) {
  if (!validateAgreementDetailsStep(state, enqueueSnackbar, incomeTypes, sourceAgreement)) return false;
  return validateCommercialStructureStep(
    state,
    enqueueSnackbar,
    incomeTypes,
    sourceAgreement,
    serverAgreementId,
  );
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
