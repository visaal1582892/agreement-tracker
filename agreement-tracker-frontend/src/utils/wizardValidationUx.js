export const WIZARD_FIELD_SCROLL_ORDER = [
  'supplyVendors',
  'assetCategory',
  'assetType',
  'adhocSubType',
  'products',
  'states',
  'storeCount',
  'storeOutletList',
  'quantityCap',
  'paymentRealization',
  'calculationBasis',
  'invoiceVendor',
  'documents',
  'commercialComponent',
  'commercialValue',
  'flatBaselineFrequency',
  'flatPayout',
  'payoutPerStore',
  'slabs',
];

export function getFirstWizardFieldErrorMessage(fieldErrors = {}) {
  for (const field of WIZARD_FIELD_SCROLL_ORDER) {
    if (fieldErrors[field]) return fieldErrors[field];
  }
  return 'Please complete required fields';
}

export function scrollToFirstWizardError(fieldErrors = {}, delayMs = 150) {
  setTimeout(() => {
    for (const field of WIZARD_FIELD_SCROLL_ORDER) {
      if (!fieldErrors[field]) continue;
      const el = document.querySelector(`[data-wizard-field="${field}"]`);
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        return;
      }
    }
    const fallback = document.querySelector('.has-error');
    if (fallback) {
      fallback.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }, delayMs);
}
