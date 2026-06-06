/**
 * Validates wizard state before submit-for-approval.
 * Save-as-draft bypasses this entirely.
 */
export function validateAgreementForSubmit(state, enqueueSnackbar) {
  if (!state.agreementName?.trim()) {
    enqueueSnackbar('Cannot submit: Agreement Name is missing.', { variant: 'warning' });
    return false;
  }
  if (!state.companyId) {
    enqueueSnackbar('Cannot submit: Company is missing.', { variant: 'warning' });
    return false;
  }
  if (!state.vendorIds?.length) {
    enqueueSnackbar('Cannot submit: At least one Vendor is required.', { variant: 'warning' });
    return false;
  }
  if (!state.productRules?.productRules?.length) {
    enqueueSnackbar('Cannot submit: At least one Product must be selected.', { variant: 'warning' });
    return false;
  }
  if (!state.agreements?.length) {
    enqueueSnackbar('Cannot submit: Agreement details are missing.', { variant: 'warning' });
    return false;
  }

  let valid = true;
  state.agreements.forEach((agreement, index) => {
    const { details, commercials } = agreement;
    const label = state.agreements.length > 1 ? `Agreement ${index + 1}: ` : '';

    if (!details.incomeTypeId) {
      enqueueSnackbar(`${label}Cannot submit: Income Type is missing.`, { variant: 'warning' });
      valid = false;
    } else if (!details.agreementTypeId) {
      enqueueSnackbar(`${label}Cannot submit: Agreement Type is missing.`, { variant: 'warning' });
      valid = false;
    } else if (!details.startDate) {
      enqueueSnackbar(`${label}Cannot submit: Start Date is missing.`, { variant: 'warning' });
      valid = false;
    } else if (!details.expiryDate) {
      enqueueSnackbar(`${label}Cannot submit: Expiry Date is missing.`, { variant: 'warning' });
      valid = false;
    } else if (!commercials.commercialStructure) {
      enqueueSnackbar(`${label}Cannot submit: Commercial Structure is missing.`, { variant: 'warning' });
      valid = false;
    } else if (commercials.commercialStructure === 'FLAT' && !commercials.commercialValue) {
      enqueueSnackbar(`${label}Cannot submit: Commercial Value is missing for FLAT structure.`, { variant: 'warning' });
      valid = false;
    } else if (!details.documents?.length) {
      enqueueSnackbar(`${label}Cannot submit: At least one document must be uploaded.`, { variant: 'warning' });
      valid = false;
    }
  });

  return valid;
}
