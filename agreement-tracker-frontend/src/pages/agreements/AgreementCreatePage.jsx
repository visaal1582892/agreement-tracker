import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Box, Typography, alpha, Paper } from '@mui/material';
import { useSnackbar } from 'notistack';
import axiosInstance from '../../api/axiosInstance';
import { ENDPOINTS } from '../../config/endpoints';
import { ROUTES } from '../../config/routes';
import { BRAND } from '../../config/theme';
import WizardLayout from '../../layouts/WizardLayout';
import { useAgreementWizard } from '../../hooks/useAgreementWizard';
import Step1Setup from './wizard/Step1Setup';
import Step2Agreements from './wizard/Step2Agreements';
import Step5Review from './wizard/Step5Review';

export default function AgreementCreatePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { enqueueSnackbar } = useSnackbar();
  const clonedRef = useRef(false);
  const {
    state,
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
  } = useAgreementWizard();
  const [savingDraft, setSavingDraft] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [documentErrors, setDocumentErrors] = useState({});

  useEffect(() => {
    if (location.state?.clonedData && !clonedRef.current) {
      hydrateFromClone(location.state.clonedData);
      clonedRef.current = true;
      enqueueSnackbar('Product scope copied — complete remaining details', { variant: 'info' });
      navigate(location.pathname, { replace: true, state: {} });
    }
  }, [location.state, location.pathname, hydrateFromClone, enqueueSnackbar, navigate]);

  const clearDocumentError = (agreementId) => {
    setDocumentErrors((prev) => {
      const next = { ...prev };
      delete next[agreementId];
      return next;
    });
  };

  const validate = () => {
    switch (state.step) {
      case 0:
        if (!state.companyId) { enqueueSnackbar('Select a company', { variant: 'warning' }); return false; }
        if (!state.vendorIds?.length) { enqueueSnackbar('Select at least one vendor', { variant: 'warning' }); return false; }
        if (!state.productRules?.productRules?.length) {
          enqueueSnackbar('Select at least one product', { variant: 'warning' }); return false;
        }
        return true;
      case 1: {
        if (!state.agreements?.length) {
          enqueueSnackbar('Add at least one agreement', { variant: 'warning' }); return false;
        }

        const errors = {};
        let hasError = false;
        state.agreements.forEach((agreement, index) => {
          const { details, commercials } = agreement;
          if (!details.incomeTypeId) {
            enqueueSnackbar(`Agreement ${index + 1}: select income type`, { variant: 'warning' });
            hasError = true;
          } else if (!details.agreementTypeId) {
            enqueueSnackbar(`Agreement ${index + 1}: select agreement type`, { variant: 'warning' });
            hasError = true;
          } else if (!details.startDate || !details.expiryDate) {
            enqueueSnackbar(`Agreement ${index + 1}: dates are required`, { variant: 'warning' });
            hasError = true;
          } else if (commercials.commercialStructure === 'FLAT' && !commercials.commercialValue) {
            enqueueSnackbar(`Agreement ${index + 1}: enter commercial value`, { variant: 'warning' });
            hasError = true;
          } else if (!details.documents?.length) {
            errors[agreement.id] = 'Upload at least one document before continuing.';
            enqueueSnackbar(`Agreement ${index + 1}: upload at least one document`, { variant: 'warning' });
            hasError = true;
          }
        });
        setDocumentErrors(errors);
        return !hasError;
      }
      default:
        return true;
    }
  };

  const buildPayload = useCallback(() => ({
    companyId: state.companyId,
    vendorIds: state.vendorIds,
    productRules: {
      manufacturers: state.productRules.manufacturers,
      divisionRules: state.productRules.divisionRules,
      productRules: state.productRules.productRules,
    },
    agreements: state.agreements.map(({ details, commercials }) => ({
      details: {
        incomeTypeId: details.incomeTypeId,
        agreementTypeId: details.agreementTypeId,
        startDate: details.startDate?.split('T')[0],
        expiryDate: details.expiryDate?.split('T')[0],
        notes: details.notes || null,
      },
      commercials: {
        commercialStructure: commercials.commercialStructure,
        commercialValue: commercials.commercialValue || null,
        calculationFormula: commercials.calculationFormula || null,
      },
    })),
  }), [state]);

  const createAgreements = async () => {
    const { data } = await axiosInstance.post(ENDPOINTS.AGREEMENTS, buildPayload());
    return data;
  };

  const submitCreatedAgreements = async (createdAgreements) => {
    await Promise.all(
      (createdAgreements || []).map((agreement) =>
        axiosInstance.put(ENDPOINTS.AGREEMENT_SUBMIT(agreement.id)),
      ),
    );
  };

  const handleNext = () => {
    if (!validate()) return;
    nextStep();
  };

  const handleSaveDraft = async () => {
    setSavingDraft(true);
    try {
      const data = await createAgreements();
      const count = data.agreements?.length || 1;
      enqueueSnackbar(`${count} agreement(s) saved as draft`, { variant: 'success' });
      reset();
      navigate(ROUTES.AGREEMENTS);
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Failed to save draft', { variant: 'error' });
    } finally {
      setSavingDraft(false);
    }
  };

  const handleSubmitForApproval = async () => {
    setSubmitting(true);
    try {
      const data = await createAgreements();
      await submitCreatedAgreements(data.agreements);
      const count = data.agreements?.length || 1;
      enqueueSnackbar(`${count} agreement(s) submitted for approval`, { variant: 'success' });
      reset();
      navigate(data.primaryAgreementGroupId
        ? `/agreements/groups/${data.primaryAgreementGroupId}`
        : ROUTES.AGREEMENTS);
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Failed to submit agreement', { variant: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  const STEP_COMPONENTS = [
    <Step1Setup
      state={state}
      updateFields={updateFields}
      updateProductRules={updateProductRules}
    />,
    <Step2Agreements
      state={state}
      addAgreement={addAgreement}
      removeAgreement={removeAgreement}
      updateAgreementDetails={updateAgreementDetails}
      updateAgreementCommercials={updateAgreementCommercials}
      documentErrors={documentErrors}
      onClearDocumentError={clearDocumentError}
    />,
    <Step5Review state={state} />,
  ];

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column' }}>
      <Paper
        elevation={0}
        sx={{
          mb: 3,
          p: { xs: 2.5, md: 3 },
          borderRadius: 3.5,
          border: '1px solid rgba(226, 232, 240, 0.8)',
          boxShadow: '0 4px 24px rgba(15, 23, 42, 0.05)',
          background: `linear-gradient(120deg, #fff 40%, ${alpha(BRAND.red, 0.03)} 100%)`,
        }}
      >
        <Typography sx={{
          fontSize: '0.72rem', fontWeight: 600, color: BRAND.red,
          textTransform: 'uppercase', letterSpacing: '0.1em', mb: 0.75,
        }}>
          New Draft
        </Typography>
        <Typography sx={{ fontSize: { xs: '1.35rem', md: '1.6rem' }, fontWeight: 800, color: BRAND.textPrimary, letterSpacing: '-0.5px' }}>
          Bulk Agreement
        </Typography>
        <Typography sx={{ mt: 0.5, fontSize: '0.9rem', color: '#64748B' }}>
          Shared company, vendors, and products — unique details per agreement
        </Typography>
      </Paper>

      <WizardLayout
        activeStep={state.step}
        onNext={handleNext}
        onBack={prevStep}
        onCancel={() => navigate(ROUTES.AGREEMENTS)}
        onSaveDraft={handleSaveDraft}
        onSubmitForApproval={handleSubmitForApproval}
        isLastStep={state.step === 2}
        isSavingDraft={savingDraft}
        isSubmitting={submitting}
      >
        {STEP_COMPONENTS[state.step]}
      </WizardLayout>
    </Box>
  );
}
