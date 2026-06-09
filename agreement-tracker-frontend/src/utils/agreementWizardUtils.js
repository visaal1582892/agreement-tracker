export function buildAgreementDetailsPayload(agreement) {
  if (!agreement) {
    return { details: {}, commercials: {} };
  }
  const { details, commercials } = agreement;
  return {
    details: {
      incomeTypeId: details.incomeTypeId || null,
      agreementTypeId: details.agreementTypeId || null,
      startDate: details.startDate?.split('T')[0] || null,
      expiryDate: details.expiryDate?.split('T')[0] || null,
      notes: details.notes || null,
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
    agreementName: state.agreementName?.trim() || null,
    companyId: state.companyId,
    vendorIds: state.vendorIds ?? [],
    productRules: state.productRules ?? {},
    agreements: [],
  };
}

export function buildStep1UpdatePayload(state) {
  const { details, commercials } = buildAgreementDetailsPayload(state.agreement);
  return {
    agreementName: state.agreementName?.trim() || null,
    companyId: state.companyId,
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
  };
}

export function validateStep2LoopFields(state, enqueueSnackbar) {
  if (!state.agreementName?.trim()) {
    enqueueSnackbar('Agreement name is required', { variant: 'warning' });
    return false;
  }
  const { details } = state.agreement ?? {};
  if (!details?.startDate || !details?.expiryDate) {
    enqueueSnackbar('Start and expiry dates are required', { variant: 'warning' });
    return false;
  }
  return true;
}

export function internalStepFromUrl(urlStep) {
  const parsed = Number.parseInt(urlStep, 10);
  if (Number.isNaN(parsed) || parsed < 1 || parsed > 3) return null;
  return parsed - 1;
}

export function urlStepFromInternal(internalStep) {
  return internalStep + 1;
}
