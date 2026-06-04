import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Typography } from '@mui/material';
import { useSnackbar } from 'notistack';
import axiosInstance from '../../api/axiosInstance';
import { ENDPOINTS } from '../../config/endpoints';
import { ROUTES } from '../../config/routes';
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
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h5" fontWeight={700}>New Agreement</Typography>
        <Typography variant="body2" color="text.secondary">Complete all steps to create a new commercial agreement draft</Typography>
      </Box>

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
