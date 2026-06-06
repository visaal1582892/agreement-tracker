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
import { validateAgreementForSubmit } from '../../utils/agreementSubmitValidation';
import Step1Setup from './wizard/Step1Setup';
import Step2Agreements from './wizard/Step2Agreements';
import Step5Review from './wizard/Step5Review';

function buildAgreementItem(agreement) {
  if (!agreement) return null;
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
  const [draftAgreementId, setDraftAgreementId] = useState(null);
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

  const validateDraftSave = () => {
    if (!state.agreementName?.trim()) {
      enqueueSnackbar('Agreement name is required to save draft', { variant: 'warning' });
      return false;
    }
    return true;
  };

  const validate = () => {
    switch (state.step) {
      case 0:
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

  const buildCreatePayload = useCallback(() => ({
    agreementName: state.agreementName?.trim() || '',
    companyId: state.companyId,
    vendorIds: state.vendorIds ?? [],
    productRules: state.productRules ?? {},
    agreements: state.agreements.map(buildAgreementItem).filter(Boolean),
  }), [state]);

  const buildUpdatePayload = useCallback(() => {
    const agreement = state.agreements[0];
    return {
      agreementName: state.agreementName?.trim() || '',
      companyId: state.companyId,
      vendorIds: state.vendorIds ?? [],
      productRules: state.productRules ?? {},
      details: buildAgreementItem(agreement)?.details ?? {},
      commercials: buildAgreementItem(agreement)?.commercials ?? {},
    };
  }, [state]);

  const persistDraft = async ({ validateStep1 = false } = {}) => {
    const updatePayload = buildUpdatePayload();
    if (draftAgreementId) {
      const { data } = await axiosInstance.put(
        ENDPOINTS.AGREEMENT_UPDATE(draftAgreementId),
        updatePayload,
        { params: { validateStep1 } },
      );
      return data;
    }
    const { data } = await axiosInstance.post(ENDPOINTS.AGREEMENTS, buildCreatePayload());
    const created = data.agreements?.[0];
    if (created?.id) {
      setDraftAgreementId(created.id);
      if (validateStep1) {
        const { data: updated } = await axiosInstance.put(
          ENDPOINTS.AGREEMENT_UPDATE(created.id),
          updatePayload,
          { params: { validateStep1: true } },
        );
        return updated;
      }
    }
    return created ?? data;
  };

  const handleNext = async () => {
    if (state.step === 0) {
      if (!state.vendorIds?.length) { enqueueSnackbar('Select at least one vendor', { variant: 'warning' }); return; }
      if (!state.productRules?.productRules?.length) {
        enqueueSnackbar('Select at least one product', { variant: 'warning' }); return;
      }
      try {
        await persistDraft({ validateStep1: true });
      } catch (err) {
        enqueueSnackbar(err.response?.data?.message || 'Complete required step 1 fields', { variant: 'error' });
        return;
      }
      nextStep();
      return;
    }
    if (!validate()) return;
    nextStep();
  };

  const handleSaveDraft = async () => {
    if (!validateDraftSave()) return;
    setSavingDraft(true);
    try {
      const wasNew = !draftAgreementId;
      const saved = await persistDraft();
      const label = saved?.versionNumber ? `V${saved.versionNumber}` : 'draft';
      enqueueSnackbar(`Agreement ${label} saved`, { variant: 'success' });
      if (wasNew && saved?.id) {
        navigate(`/agreements/${saved.id}/edit`, { replace: true });
      }
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Failed to save draft', { variant: 'error' });
    } finally {
      setSavingDraft(false);
    }
  };

  const handleSubmitForApproval = async () => {
    if (!validateAgreementForSubmit(state, enqueueSnackbar)) return;
    setSubmitting(true);
    try {
      const saved = await persistDraft();
      const agreementId = saved?.id ?? draftAgreementId;
      await axiosInstance.put(ENDPOINTS.AGREEMENT_SUBMIT(agreementId));
      enqueueSnackbar('Agreement submitted for approval', { variant: 'success' });
      reset();
      navigate(saved?.agreementGroupId
        ? `/agreements/groups/${saved.agreementGroupId}`
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
          Save as draft anytime — submit when ready
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
