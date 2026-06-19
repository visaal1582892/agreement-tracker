import { formatLocalDateString } from './dateUtils';

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
    },
    commercials: {
      commercialStructure: commercials.commercialStructure || null,
      commercialValue: commercials.commercialValue || null,
      calculationFormula: commercials.calculationFormula || null,
    },
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

export function buildStep1UpdatePayload(state) {
  const { details, commercials } = buildAgreementDetailsPayload(state.agreement);
  return {
    companyId: state.companyId,
    companyAgreementGroupId: state.companyAgreementGroupId || null,
    newCompanyAgreementGroupName: state.newCompanyAgreementGroupName?.trim() || null,
    vendorIds: state.vendorIds ?? [],
    productRules: state.productRules ?? {},
    details,
    commercials,
  };
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
  if (!state.vendorIds?.length) {
    enqueueSnackbar('Select at least one vendor', { variant: 'warning' });
    return false;
  }
  if (!state.productRules?.productRules?.length) {
    enqueueSnackbar('Select at least one product', { variant: 'warning' });
    return false;
  }
  return true;
}

export function validateContractDetailsFields(details, enqueueSnackbar) {
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
  if (!details?.documents?.length) {
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

export function validateAgreementDetailsStep(state, enqueueSnackbar) {
  if (!validateStep2LoopFields(state, enqueueSnackbar)) return false;
  const { details } = state.agreement ?? {};
  return validateContractDetailsFields(details, enqueueSnackbar);
}

export function validateCommercialStructureStep(state, enqueueSnackbar) {
  const { commercials } = state.agreement ?? {};
  if (!commercials?.commercialStructure) {
    enqueueSnackbar('Commercial structure is required', { variant: 'warning' });
    return false;
  }
  if (commercials.commercialStructure === 'FLAT' && !commercials.commercialValue) {
    enqueueSnackbar('Commercial value is required for FLAT structure', { variant: 'warning' });
    return false;
  }
  return true;
}

export function validateCurrentAgreementDetails(state, enqueueSnackbar) {
  if (!validateAgreementDetailsStep(state, enqueueSnackbar)) return false;
  return validateCommercialStructureStep(state, enqueueSnackbar);
}

export function validateStep2LoopFields(state, enqueueSnackbar) {
  const { details } = state.agreement ?? {};
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

export function internalStepFromUrl(urlStep) {
  const parsed = Number.parseInt(urlStep, 10);
  if (Number.isNaN(parsed) || parsed < 1 || parsed > 4) return null;
  return parsed - 1;
}

export function urlStepFromInternal(internalStep) {
  return internalStep + 1;
}
