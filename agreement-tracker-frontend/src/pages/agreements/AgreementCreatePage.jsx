import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Typography, alpha, Paper } from '@mui/material';
import { useSnackbar } from 'notistack';
import axiosInstance from '../../api/axiosInstance';
import { ENDPOINTS } from '../../config/endpoints';
import { ROUTES } from '../../config/routes';
import { BRAND } from '../../config/theme';
import WizardLayout from '../../layouts/WizardLayout';
import { useAgreementWizard } from '../../hooks/useAgreementWizard';
import Step1CompanyVendors from './wizard/Step1CompanyVendors';
import Step2Products from './wizard/Step2Products';
import Step3Details from './wizard/Step3Details';
import Step4Commercials from './wizard/Step4Commercials';
import Step5Review from './wizard/Step5Review';

export default function AgreementCreatePage() {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const { state, updateFields, nextStep, prevStep, reset } = useAgreementWizard();
  const [submitting, setSubmitting] = useState(false);

  const validate = () => {
    switch (state.step) {
      case 0:
        if (!state.companyId) { enqueueSnackbar('Select a company', { variant: 'warning' }); return false; }
        if (!state.vendorIds?.length) { enqueueSnackbar('Select at least one vendor', { variant: 'warning' }); return false; }
        return true;
      case 1:
        if (!state.productIds?.length) { enqueueSnackbar('Select at least one product', { variant: 'warning' }); return false; }
        return true;
      case 2:
        if (!state.incomeTypeId) { enqueueSnackbar('Select income type', { variant: 'warning' }); return false; }
        if (!state.agreementTypeId) { enqueueSnackbar('Select agreement type', { variant: 'warning' }); return false; }
        if (!state.startDate || !state.expiryDate) { enqueueSnackbar('Dates are required', { variant: 'warning' }); return false; }
        return true;
      case 3:
        if (state.commercialStructure === 'FLAT' && !state.commercialValue) {
          enqueueSnackbar('Enter commercial value', { variant: 'warning' }); return false;
        }
        return true;
      default: return true;
    }
  };

  const handleNext = () => {
    if (!validate()) return;
    if (state.step === 4) {
      handleSubmit();
    } else {
      nextStep();
    }
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      const payload = {
        companyId: state.companyId,
        vendorIds: state.vendorIds,
        productIds: state.productIds,
        incomeTypeId: state.incomeTypeId,
        agreementTypeId: state.agreementTypeId,
        commercialStructure: state.commercialStructure,
        commercialValue: state.commercialValue || null,
        calculationFormula: state.calculationFormula || null,
        startDate: state.startDate?.split('T')[0],
        expiryDate: state.expiryDate?.split('T')[0],
        notes: state.notes || null,
      };

      const { data } = await axiosInstance.post(ENDPOINTS.AGREEMENTS, payload);
      enqueueSnackbar(`Agreement ${data.agreementNumber} created as DRAFT`, { variant: 'success' });
      reset();
      navigate(`/agreements/groups/${data.agreementGroupId}`);
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to create agreement';
      enqueueSnackbar(msg, { variant: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  const STEP_COMPONENTS = [
    <Step1CompanyVendors state={state} updateFields={updateFields} />,
    <Step2Products state={state} updateFields={updateFields} />,
    <Step3Details state={state} updateFields={updateFields} />,
    <Step4Commercials state={state} updateFields={updateFields} />,
    <Step5Review state={state} />,
  ];

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100%', height: '100%' }}>
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
          New Agreement
        </Typography>
        <Typography sx={{ mt: 0.5, fontSize: '0.9rem', color: '#64748B' }}>
          Complete all steps to create a new commercial agreement draft
        </Typography>
      </Paper>

      <WizardLayout
        activeStep={state.step}
        onNext={handleNext}
        onBack={prevStep}
        onCancel={() => navigate(ROUTES.AGREEMENTS)}
        isLastStep={state.step === 4}
        isSubmitting={submitting}
      >
        {STEP_COMPONENTS[state.step]}
      </WizardLayout>
    </Box>
  );
}
