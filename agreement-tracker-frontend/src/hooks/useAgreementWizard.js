import { useState, useCallback } from 'react';

let agreementIdCounter = 0;

export function createBlankAgreement() {
  agreementIdCounter += 1;
  return {
    id: `agr-${agreementIdCounter}-${Date.now()}`,
    details: {
      incomeTypeId: null,
      agreementTypeId: null,
      startDate: null,
      expiryDate: null,
      tenureValue: '',
      tenureUnit: 'MONTHS',
      notes: '',
      documents: [],
    },
    commercials: {
      commercialStructure: 'FLAT',
      commercialValue: '',
      calculationFormula: '',
      slabs: [],
      timeDistribution: 'MONTHLY',
    },
  };
}

const INITIAL_STATE = {
  step: 0,
  companyId: null,
  companyName: '',
  vendorIds: [],
  productRules: {
    manufacturers: [],
    divisionRules: [],
    productRules: [],
  },
  agreements: [createBlankAgreement()],
};

export function useAgreementWizard() {
  const [state, setState] = useState(INITIAL_STATE);

  const updateStep = useCallback((step) => {
    setState((prev) => ({ ...prev, step }));
  }, []);

  const updateField = useCallback((field, value) => {
    setState((prev) => ({ ...prev, [field]: value }));
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

  const addAgreement = useCallback(() => {
    setState((prev) => ({
      ...prev,
      agreements: [...prev.agreements, createBlankAgreement()],
    }));
  }, []);

  const removeAgreement = useCallback((agreementId) => {
    setState((prev) => {
      if (prev.agreements.length <= 1) return prev;
      return {
        ...prev,
        agreements: prev.agreements.filter((a) => a.id !== agreementId),
      };
    });
  }, []);

  const updateAgreementDetails = useCallback((agreementId, patch) => {
    setState((prev) => ({
      ...prev,
      agreements: prev.agreements.map((a) =>
        a.id === agreementId ? { ...a, details: { ...a.details, ...patch } } : a,
      ),
    }));
  }, []);

  const updateAgreementCommercials = useCallback((agreementId, patch) => {
    setState((prev) => ({
      ...prev,
      agreements: prev.agreements.map((a) =>
        a.id === agreementId ? { ...a, commercials: { ...a.commercials, ...patch } } : a,
      ),
    }));
  }, []);

  const nextStep = useCallback(() => {
    setState((prev) => ({ ...prev, step: Math.min(prev.step + 1, 2) }));
  }, []);

  const prevStep = useCallback(() => {
    setState((prev) => ({ ...prev, step: Math.max(prev.step - 1, 0) }));
  }, []);

  const reset = useCallback(() => setState({
    step: 0,
    companyId: null,
    companyName: '',
    vendorIds: [],
    productRules: { manufacturers: [], divisionRules: [], productRules: [] },
    agreements: [createBlankAgreement()],
  }), []);

  const hydrateFromEdit = useCallback((agreement) => {
    if (!agreement) return;
    setState({
      step: 0,
      companyId: agreement.companyId ?? null,
      companyName: agreement.companyName ?? '',
      vendorIds: agreement.vendors?.map((v) => v.vendorId) ?? [],
      productRules: {
        manufacturers: agreement.manufacturerIds ?? [],
        divisionRules: agreement.divisionRules?.map((r) => ({ id: r.id, ruleType: r.ruleType })) ?? [],
        productRules: agreement.productRules?.map((r) => ({ id: r.id, ruleType: r.ruleType })) ?? [],
      },
      agreements: [{
        id: `agr-edit-${Date.now()}`,
        details: {
          incomeTypeId: agreement.incomeTypeId ?? null,
          agreementTypeId: agreement.agreementTypeId ?? null,
          startDate: agreement.startDate ?? null,
          expiryDate: agreement.expiryDate ?? null,
          notes: agreement.notes ?? '',
          documents: [],
        },
        commercials: {
          commercialStructure: agreement.commercialStructure ?? 'FLAT',
          commercialValue: agreement.commercialValue ?? '',
          calculationFormula: agreement.calculationFormula ?? '',
          slabs: [],
          timeDistribution: 'MONTHLY',
        },
      }],
    });
  }, []);

  const hydrateFromClone = useCallback((clonedData) => {
    if (!clonedData) return;
    setState((prev) => ({
      ...prev,
      step: 0,
      companyId: clonedData.companyId ?? null,
      companyName: clonedData.companyName ?? '',
      vendorIds: clonedData.vendorIds ?? [],
      productRules: {
        manufacturers: clonedData.productRules?.manufacturers ?? [],
        divisionRules: clonedData.productRules?.divisionRules ?? [],
        productRules: clonedData.productRules?.productRules ?? [],
      },
      agreements: [createBlankAgreement()],
    }));
  }, []);

  return {
    state,
    updateStep,
    updateField,
    updateFields,
    updateProductRules,
    addAgreement,
    removeAgreement,
    updateAgreementDetails,
    updateAgreementCommercials,
    nextStep,
    prevStep,
    reset,
    hydrateFromClone,
    hydrateFromEdit,
  };
}
