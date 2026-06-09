/**
 * Build wizard pre-fill payload from agreement API response.
 * Keeps company, vendors, productRules only — no ids/dates/commercials/status/notes/docs.
 */
export function buildClonedWizardData(agreement) {
  if (!agreement) return null;

  return {
    agreementName: agreement.agreementName
      ? `${agreement.agreementName} (Copy)`
      : '',
    companyId: agreement.companyId,
    companyName: agreement.companyName,
    vendorIds: (agreement.vendors || []).map((v) => v.vendorId),
    productRules: {
      manufacturers: agreement.manufacturerIds || [],
      divisionRules: (agreement.divisionRules || []).map((r) => ({
        id: r.id,
        ruleType: r.ruleType,
      })),
      productRules: (agreement.productRules || []).map((r) => ({
        id: r.id,
        ruleType: r.ruleType,
      })),
    },
  };
}

export async function fetchAgreementForClone(axiosInstance, endpoints, agreementId) {
  const { data } = await axiosInstance.get(endpoints.AGREEMENT_BY_ID(agreementId));
  return buildClonedWizardData(data);
}

export async function cloneAgreementOnServer(axiosInstance, endpoints, agreementId) {
  const { data } = await axiosInstance.post(endpoints.AGREEMENT_CLONE(agreementId));
  return data;
}
