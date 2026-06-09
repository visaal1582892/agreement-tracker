import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Typography, alpha, Paper } from '@mui/material';
import { useSnackbar } from 'notistack';
import axiosInstance from '../../api/axiosInstance';
import { ENDPOINTS } from '../../config/endpoints';
import { ROUTES } from '../../config/routes';
import { BRAND } from '../../config/theme';
import WizardLayout from '../../layouts/WizardLayout';
import { useAgreementWizard } from '../../hooks/useAgreementWizard';
import { buildAgreementEditPath } from '../../utils/agreementNavigation';
import {
  buildStep1CreatePayload,
  urlStepFromInternal,
  validateStep1Fields,
} from '../../utils/agreementWizardUtils';
import Step1Setup from './wizard/Step1Setup';

export default function AgreementCreatePage() {
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const { state, updateFields, updateProductRules } = useAgreementWizard();
  const [savingDraft, setSavingDraft] = useState(false);

  const createDraft = useCallback(async () => {
    const { data } = await axiosInstance.post(ENDPOINTS.AGREEMENTS, buildStep1CreatePayload(state));
    const created = data.agreements?.[0];
    if (!created?.id) {
      throw new Error('Draft created but no agreement id returned');
    }
    return created;
  }, [state]);

  const handleSaveDraft = async () => {
    if (!validateStep1Fields(state, enqueueSnackbar)) return;
    setSavingDraft(true);
    try {
      const created = await createDraft();
      enqueueSnackbar('Draft saved', { variant: 'success' });
      navigate(buildAgreementEditPath(created.id, { step: urlStepFromInternal(0) }), { replace: true });
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || err.message || 'Failed to save draft', { variant: 'error' });
    } finally {
      setSavingDraft(false);
    }
  };

  const handleNext = async () => {
    if (!validateStep1Fields(state, enqueueSnackbar)) return;
    setSavingDraft(true);
    try {
      const created = await createDraft();
      enqueueSnackbar('Product template saved', { variant: 'success' });
      navigate(buildAgreementEditPath(created.id, { step: urlStepFromInternal(1) }), { replace: true });
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || err.message || 'Failed to save draft', { variant: 'error' });
    } finally {
      setSavingDraft(false);
    }
  };

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
          Select company, vendors, and products to build your template
        </Typography>
      </Paper>

      <WizardLayout
        activeStep={0}
        footerMode="setup"
        onNext={handleNext}
        onBack={() => {}}
        onCancel={() => navigate(ROUTES.AGREEMENTS)}
        onSaveDraft={handleSaveDraft}
        isSavingDraft={savingDraft}
      >
        <Step1Setup
          state={state}
          updateFields={updateFields}
          updateProductRules={updateProductRules}
        />
      </WizardLayout>
    </Box>
  );
}
