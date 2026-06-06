import { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Box, Typography, alpha, Paper, CircularProgress, Alert } from '@mui/material';
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

export default function AgreementEditPage() {
  const { agreementId } = useParams();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const hydratedRef = useRef(false);

  const {
    state,
    updateFields,
    updateProductRules,
    updateAgreementDetails,
    updateAgreementCommercials,
    nextStep,
    prevStep,
    reset,
    hydrateFromEdit,
  } = useAgreementWizard();

  const [sourceAgreement, setSourceAgreement] = useState(null);
  const [loadError, setLoadError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [documentErrors, setDocumentErrors] = useState({});

  useEffect(() => {
    if (!agreementId || hydratedRef.current) return;
    axiosInstance.get(ENDPOINTS.AGREEMENT_BY_ID(agreementId))
      .then(({ data }) => {
        setSourceAgreement(data);
        hydrateFromEdit(data);
        hydratedRef.current = true;
      })
      .catch((err) => {
        const msg = err.response?.data?.message || 'Failed to load agreement for editing';
        setLoadError(msg);
        enqueueSnackbar(msg, { variant: 'error' });
      });
  }, [agreementId, hydrateFromEdit, enqueueSnackbar]);

  const clearDocumentError = (id) => {
    setDocumentErrors((prev) => { const n = { ...prev }; delete n[id]; return n; });
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
        const agreement = state.agreements[0];
        if (!agreement) return false;
        const { details, commercials } = agreement;
        if (!details.incomeTypeId) { enqueueSnackbar('Select income type', { variant: 'warning' }); return false; }
        if (!details.agreementTypeId) { enqueueSnackbar('Select agreement type', { variant: 'warning' }); return false; }
        if (!details.startDate || !details.expiryDate) { enqueueSnackbar('Dates are required', { variant: 'warning' }); return false; }
        if (commercials.commercialStructure === 'FLAT' && !commercials.commercialValue) {
          enqueueSnackbar('Enter commercial value', { variant: 'warning' }); return false;
        }
        return true;
      }
      default:
        return true;
    }
  };

  const handleNext = () => {
    if (!validate()) return;
    if (state.step === 2) {
      handleSubmit();
    } else {
      nextStep();
    }
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    const agreement = state.agreements[0];
    try {
      const payload = {
        vendorIds: state.vendorIds,
        productRules: {
          manufacturers: state.productRules.manufacturers,
          divisionRules: state.productRules.divisionRules,
          productRules: state.productRules.productRules,
        },
        details: {
          incomeTypeId: agreement.details.incomeTypeId,
          agreementTypeId: agreement.details.agreementTypeId,
          startDate: agreement.details.startDate?.split('T')[0],
          expiryDate: agreement.details.expiryDate?.split('T')[0],
          notes: agreement.details.notes || null,
        },
        commercials: {
          commercialStructure: agreement.commercials.commercialStructure,
          commercialValue: agreement.commercials.commercialValue || null,
          calculationFormula: agreement.commercials.calculationFormula || null,
        },
      };

      const { data } = await axiosInstance.post(
        ENDPOINTS.AGREEMENT_CREATE_VERSION(agreementId),
        payload,
      );
      enqueueSnackbar('New version submitted for approval', { variant: 'success' });
      reset();
      navigate(`/agreements/groups/${data.agreementGroupId}`);
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to submit edited version';
      enqueueSnackbar(msg, { variant: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  const versionLabel = sourceAgreement
    ? `V${sourceAgreement.versionNumber} — ${sourceAgreement.approvalStatus}`
    : '';

  const STEP_COMPONENTS = [
    <Step1Setup
      state={state}
      updateFields={updateFields}
      updateProductRules={updateProductRules}
    />,
    <Step2Agreements
      state={state}
      addAgreement={() => {}}
      removeAgreement={() => {}}
      updateAgreementDetails={updateAgreementDetails}
      updateAgreementCommercials={updateAgreementCommercials}
      documentErrors={documentErrors}
      onClearDocumentError={clearDocumentError}
      disableAdd
    />,
    <Step5Review state={state} />,
  ];

  if (loadError) {
    return (
      <Alert severity="error" sx={{ m: 3 }}>{loadError}</Alert>
    );
  }

  if (!sourceAgreement) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

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
          Edit Agreement
        </Typography>
        <Typography sx={{ fontSize: { xs: '1.35rem', md: '1.6rem' }, fontWeight: 800, color: BRAND.textPrimary, letterSpacing: '-0.5px' }}>
          {sourceAgreement.agreementNumber}
        </Typography>
        <Typography sx={{ mt: 0.5, fontSize: '0.9rem', color: '#64748B' }}>
          {versionLabel} — changes will create a new version pending approval
        </Typography>
      </Paper>

      <WizardLayout
        activeStep={state.step}
        onNext={handleNext}
        onBack={prevStep}
        onCancel={() => navigate(`/agreements/groups/${sourceAgreement.agreementGroupId}`)}
        isLastStep={state.step === 2}
        isSubmitting={submitting}
      >
        {STEP_COMPONENTS[state.step]}
      </WizardLayout>
    </Box>
  );
}
