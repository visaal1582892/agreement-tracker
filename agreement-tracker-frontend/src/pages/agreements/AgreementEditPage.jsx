import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Box, Typography, alpha, Paper, CircularProgress, Alert, Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField } from '@mui/material';
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
  const [draftAgreementId, setDraftAgreementId] = useState(null);
  const [versionSourceId, setVersionSourceId] = useState(null);
  const [loadError, setLoadError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitModalOpen, setSubmitModalOpen] = useState(false);
  const [revisionComments, setRevisionComments] = useState('');
  const [documentErrors, setDocumentErrors] = useState({});

  useEffect(() => {
    if (!agreementId || hydratedRef.current) return;
    const load = async () => {
      try {
        const { data: loaded } = await axiosInstance.get(ENDPOINTS.AGREEMENT_BY_ID(agreementId));

        if (loaded.approvalStatus === 'DRAFT') {
          setSourceAgreement(loaded);
          setDraftAgreementId(loaded.id);
          hydrateFromEdit(loaded);
          hydratedRef.current = true;
          return;
        }

        setSourceAgreement(loaded);
        setVersionSourceId(loaded.id);

        const { data: versions } = await axiosInstance.get(
          ENDPOINTS.AGREEMENT_VERSIONS(loaded.agreementGroupId),
        );
        const existingDraft = [...versions].reverse().find((v) => v.approvalStatus === 'DRAFT');

        if (existingDraft) {
          setDraftAgreementId(existingDraft.id);
          setSourceAgreement(existingDraft);
          hydrateFromEdit(existingDraft);
        } else {
          hydrateFromEdit(loaded);
        }
        hydratedRef.current = true;
      } catch (err) {
        const msg = err.response?.data?.message || 'Failed to load agreement for editing';
        setLoadError(msg);
        enqueueSnackbar(msg, { variant: 'error' });
      }
    };
    load();
  }, [agreementId, hydrateFromEdit, enqueueSnackbar]);

  const clearDocumentError = (id) => {
    setDocumentErrors((prev) => { const n = { ...prev }; delete n[id]; return n; });
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

  const buildUpdatePayload = useCallback(() => {
    const agreement = state.agreements[0];
    const item = buildAgreementItem(agreement);
    return {
      agreementName: state.agreementName?.trim() || '',
      companyId: state.companyId,
      vendorIds: state.vendorIds ?? [],
      productRules: state.productRules ?? {},
      details: item?.details ?? {},
      commercials: item?.commercials ?? {},
    };
  }, [state]);

  const persistDraft = async ({ validateStep1 = false } = {}) => {
    const payload = buildUpdatePayload();
    if (draftAgreementId) {
      const { data } = await axiosInstance.put(
        ENDPOINTS.AGREEMENT_UPDATE(draftAgreementId),
        payload,
        { params: { validateStep1 } },
      );
      return data;
    }
    const sourceId = versionSourceId ?? sourceAgreement?.id;
    const { data } = await axiosInstance.post(ENDPOINTS.AGREEMENT_CREATE_VERSION(sourceId), payload);
    setDraftAgreementId(data.id);
    navigate(`/agreements/${data.id}/edit`, { replace: true });
    if (validateStep1) {
      const { data: updated } = await axiosInstance.put(
        ENDPOINTS.AGREEMENT_UPDATE(data.id),
        payload,
        { params: { validateStep1: true } },
      );
      return updated;
    }
    return data;
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

  const handleSubmitForApproval = async () => {
    if (!validateAgreementForSubmit(state, enqueueSnackbar)) return;
    setSubmitModalOpen(true);
  };

  const handleSubmitConfirm = async () => {
    if (!revisionComments.trim()) return;
    setSubmitModalOpen(false);
    setSubmitting(true);
    try {
      const data = await persistDraft();
      const targetId = data.id ?? draftAgreementId;
      await axiosInstance.put(ENDPOINTS.AGREEMENT_SUBMIT(targetId), {
        comments: revisionComments.trim(),
      });
      enqueueSnackbar(`Version V${data.versionNumber} submitted for approval`, { variant: 'success' });
      setRevisionComments('');
      reset();
      navigate(`/agreements/groups/${data.agreementGroupId}`);
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Failed to submit edited version', { variant: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  const activeAgreement = sourceAgreement;
  const versionLabel = activeAgreement
    ? `V${activeAgreement.versionNumber} — ${activeAgreement.approvalStatus}`
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
    return <Alert severity="error" sx={{ m: 3 }}>{loadError}</Alert>;
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
          {state.agreementName || sourceAgreement.agreementName || sourceAgreement.agreementNumber}
        </Typography>
        <Typography sx={{ mt: 0.5, fontSize: '0.9rem', color: '#64748B' }}>
          {sourceAgreement.agreementNumber} · {versionLabel} — submit when ready for approval
        </Typography>
      </Paper>

      <WizardLayout
        activeStep={state.step}
        onNext={handleNext}
        onBack={prevStep}
        onCancel={() => navigate(`/agreements/groups/${sourceAgreement.agreementGroupId}`)}
        onSubmitForApproval={handleSubmitForApproval}
        isLastStep={state.step === 2}
        isSubmitting={submitting}
      >
        {STEP_COMPONENTS[state.step]}
      </WizardLayout>

      <Dialog
        open={submitModalOpen}
        onClose={() => {
          setSubmitModalOpen(false);
          setRevisionComments('');
        }}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle fontWeight={700}>Submit for Approval</DialogTitle>
        <DialogContent>
          <Alert severity="info" sx={{ mb: 2, mt: 1 }}>
            Explain why this edit or revision is being submitted. Approvers will see your reason in the timeline.
          </Alert>
          <TextField
            label="Reason for Edit / Revision *"
            multiline
            rows={4}
            fullWidth
            value={revisionComments}
            onChange={(e) => setRevisionComments(e.target.value)}
            placeholder="Describe what changed and why…"
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button
            onClick={() => {
              setSubmitModalOpen(false);
              setRevisionComments('');
            }}
            variant="outlined"
          >
            Cancel
          </Button>
          <Button
            onClick={handleSubmitConfirm}
            variant="contained"
            sx={{ bgcolor: BRAND.red }}
            disabled={!revisionComments.trim() || submitting}
          >
            {submitting ? 'Submitting…' : 'Confirm Submit'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
