import { useState, useCallback } from 'react';

const INITIAL_STATE = {
  step: 0,
  companyId: null,
  companyName: '',
  vendorIds: [],
  productIds: [],
  incomeTypeId: null,
  agreementTypeId: null,
  commercialStructure: 'FLAT',
  commercialValue: '',
  startDate: null,
  expiryDate: null,
  tenureMonths: '',
  notes: '',
  slabs: [],
  timeDistribution: 'MONTHLY',
  documents: [],
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

  const nextStep = useCallback(() => {
    setState((prev) => ({ ...prev, step: Math.min(prev.step + 1, 4) }));
  }, []);

  const prevStep = useCallback(() => {
    setState((prev) => ({ ...prev, step: Math.max(prev.step - 1, 0) }));
  }, []);

  const reset = useCallback(() => setState(INITIAL_STATE), []);

  return { state, updateStep, updateField, updateFields, nextStep, prevStep, reset };
}
