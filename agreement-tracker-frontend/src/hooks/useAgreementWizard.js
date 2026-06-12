import { useState, useCallback } from 'react';

export function createBlankAgreement() {
  return {
    id: `agr-${Date.now()}`,
    details: {
      incomeTypeId: null,
      agreementTypeId: null,
      startDate: null,
      expiryDate: null,
      notes: '',
      stateIds: [],
      documents: [],
    },
    commercials: {
      commercialStructure: 'FLAT',
      commercialValue: '',
      valueType: 'FIXED',
      calculationFormula: '',
      selectedFrequencies: [],
      slabType: 'PURCHASE',
    },
  };
}

const INITIAL_STATE = {
  step: 0,
  agreementName: '',
  companyId: null,
  companyName: '',
  companyAgreementGroupId: null,
  companyAgreementGroupName: '',
  newCompanyAgreementGroupName: '',
  vendorIds: [],
  productRules: {
    manufacturers: [],
    divisionRules: [],
    productRules: [],
  },
  agreement: createBlankAgreement(),
};

function mapProductRulesFromApi(agreement) {
  return {
    manufacturers: agreement.manufacturerIds ?? [],
    divisionRules: agreement.divisionRules?.map((r) => ({ id: r.id, ruleType: r.ruleType })) ?? [],
    productRules: agreement.productRules?.map((r) => ({ id: r.id, ruleType: r.ruleType })) ?? [],
  };
}

export function useAgreementWizard() {
  const [state, setState] = useState(INITIAL_STATE);

  const updateStep = useCallback((step) => {
    setState((prev) => ({ ...prev, step }));
  }, []);

  const updateFields = useCallback((fields) => {
    setState((prev) => ({ ...prev, ...fields }));
  }, []);

  const updateProductRules = useCallback((patch) => {
    setState((prev) => ({
      ...prev,
      productRules: { ...prev.productRules, ...patch },
    }));
  }, []);

  const updateAgreementDetails = useCallback((patch) => {
    setState((prev) => ({
      ...prev,
      agreement: {
        ...prev.agreement,
        details: { ...prev.agreement.details, ...patch },
      },
    }));
  }, []);

  const updateAgreementCommercials = useCallback((patch) => {
    setState((prev) => ({
      ...prev,
      agreement: {
        ...prev.agreement,
        commercials: { ...prev.agreement.commercials, ...patch },
      },
    }));
  }, []);

  const nextStep = useCallback(() => {
    setState((prev) => ({ ...prev, step: Math.min(prev.step + 1, 2) }));
  }, []);

  const prevStep = useCallback(() => {
    setState((prev) => ({ ...prev, step: Math.max(prev.step - 1, 0) }));
  }, []);

  const reset = useCallback(() => setState({
    ...INITIAL_STATE,
    agreement: createBlankAgreement(),
  }), []);

  const clearStep2Fields = useCallback(() => {
    setState((prev) => ({
      ...prev,
      agreementName: '',
      agreement: createBlankAgreement(),
    }));
  }, []);

  const hydrateFromEdit = useCallback((agreement) => {
    if (!agreement) return;
    setState({
      step: 0,
      agreementName: agreement.agreementName ?? '',
      companyId: agreement.companyId ?? null,
      companyName: agreement.companyName ?? '',
      companyAgreementGroupId: agreement.companyAgreementGroupId ?? null,
      companyAgreementGroupName: agreement.companyAgreementGroupName ?? '',
      newCompanyAgreementGroupName: '',
      vendorIds: agreement.vendors?.map((v) => v.vendorId) ?? [],
      productRules: mapProductRulesFromApi(agreement),
      agreement: {
        id: `agr-edit-${agreement.id}`,
        details: {
          incomeTypeId: agreement.incomeTypeId ?? null,
          agreementTypeId: agreement.agreementTypeId ?? null,
          startDate: agreement.startDate ?? null,
          expiryDate: agreement.expiryDate ?? null,
          notes: agreement.notes ?? '',
          stateIds: agreement.stateIds ?? agreement.states?.map((s) => s.id) ?? [],
          documents: [],
        },
        commercials: {
          commercialStructure: agreement.commercialStructure ?? 'FLAT',
          commercialValue: agreement.commercialValue ?? '',
          valueType: agreement.valueType ?? 'FIXED',
          calculationFormula: agreement.calculationFormula ?? '',
          selectedFrequencies: [],
          slabType: 'PURCHASE',
        },
      },
    });
  }, []);

  const applyCloneResponse = useCallback((cloned) => {
    if (!cloned) return;
    setState((prev) => ({
      ...prev,
      step: 1,
      agreementName: '',
      companyId: cloned.companyId ?? null,
      companyName: cloned.companyName ?? '',
      companyAgreementGroupId: cloned.companyAgreementGroupId ?? prev.companyAgreementGroupId,
      companyAgreementGroupName: cloned.companyAgreementGroupName ?? prev.companyAgreementGroupName,
      vendorIds: cloned.vendors?.map((v) => v.vendorId) ?? [],
      productRules: mapProductRulesFromApi(cloned),
      agreement: createBlankAgreement(),
    }));
  }, []);

  return {
    state,
    updateStep,
    updateFields,
    updateProductRules,
    updateAgreementDetails,
    updateAgreementCommercials,
    nextStep,
    prevStep,
    reset,
    clearStep2Fields,
    hydrateFromEdit,
    applyCloneResponse,
  };
}
