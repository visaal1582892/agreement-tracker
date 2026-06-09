import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import {
  Box, Typography, alpha, Paper, CircularProgress, Alert, Dialog,
  DialogTitle, DialogContent, DialogActions, Button, TextField,
} from '@mui/material';
import { useSnackbar } from 'notistack';
import axiosInstance from '../../api/axiosInstance';
import { ENDPOINTS } from '../../config/endpoints';
import { ROUTES } from '../../config/routes';
import { BRAND } from '../../config/theme';
import WizardLayout from '../../layouts/WizardLayout';
import { useAgreementWizard } from '../../hooks/useAgreementWizard';
import { validateAgreementForSubmit } from '../../utils/agreementSubmitValidation';
import { cloneAgreementOnServer } from '../../utils/agreementClone';
import {
  buildAgreementEditPath,
  buildAgreementDetailPath,
} from '../../utils/agreementNavigation';
import {
  buildStep1UpdatePayload,
  internalStepFromUrl,
  urlStepFromInternal,
  validateStep1Fields,
  validateStep2LoopFields,
  validateContractDetailsFields,
} from '../../utils/agreementWizardUtils';
import Step1Setup from './wizard/Step1Setup';
import Step2Agreements from './wizard/Step2Agreements';
import Step5Review from './wizard/Step5Review';

export default function AgreementEditPage() {
  const { agreementId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const hydratedRef = useRef(false);

  const {
    state,
    updateFields,
    updateProductRules,
    updateAgreementDetails,
    updateAgreementCommercials,
    reset,
    hydrateFromEdit,
    applyCloneResponse,
    updateStep,
  } = useAgreementWizard();

  const [sourceAgreement, setSourceAgreement] = useState(null);
  const [draftAgreementId, setDraftAgreementId] = useState(null);
  const [versionSourceId, setVersionSourceId] = useState(null);
  const [loadError, setLoadError] = useState(null);
  const [savingDraft, setSavingDraft] = useState(false);
  const [savingLoop, setSavingLoop] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [submitModalOpen, setSubmitModalOpen] = useState(false);
  const [revisionComments, setRevisionComments] = useState('');
  const [documentErrors, setDocumentErrors] = useState({});

  const isFreshDraftWizard = sourceAgreement?.approvalStatus === 'DRAFT' && !versionSourceId;

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

  useEffect(() => {
    if (!sourceAgreement) return;
    const rawStep = searchParams.get('step');
    if (rawStep == null || rawStep === '') return;
    const internalStep = internalStepFromUrl(rawStep);
    if (internalStep == null) return;
    updateStep(internalStep);
  }, [sourceAgreement, searchParams, updateStep]);

  const syncStepToUrl = useCallback((internalStep, id = draftAgreementId) => {
    if (!id) return;
    const nextParams = new URLSearchParams(searchParams);
    nextParams.set('step', String(urlStepFromInternal(internalStep)));
    setSearchParams(nextParams, { replace: true });
    updateStep(internalStep);
  }, [draftAgreementId, searchParams, setSearchParams, updateStep]);

  const clearDocumentError = (id) => {
    setDocumentErrors((prev) => { const n = { ...prev }; delete n[id]; return n; });
  };

  const validateRevisionStep = () => {
    switch (state.step) {
      case 0:
        return validateStep1Fields(state, enqueueSnackbar);
      case 1: {
        const { details, commercials } = state.agreement ?? {};
        if (!details?.incomeTypeId) { enqueueSnackbar('Select income type', { variant: 'warning' }); return false; }
        if (!details?.agreementTypeId) { enqueueSnackbar('Select agreement type', { variant: 'warning' }); return false; }
        if (!details?.startDate || !details?.expiryDate) { enqueueSnackbar('Dates are required', { variant: 'warning' }); return false; }
        if (commercials?.commercialStructure === 'FLAT' && !commercials?.commercialValue) {
          enqueueSnackbar('Enter commercial value', { variant: 'warning' }); return false;
        }
        return true;
      }
      default:
        return true;
    }
  };

  const buildUpdatePayload = useCallback(() => buildStep1UpdatePayload(state), [state]);

  const persistDraft = async ({ validateStep1 = false, validateStep2 = false } = {}) => {
    const payload = buildUpdatePayload();
    if (draftAgreementId) {
      const { data } = await axiosInstance.put(
        ENDPOINTS.AGREEMENT_UPDATE(draftAgreementId),
        payload,
        { params: { validateStep1, validateStep2 } },
      );
      setSourceAgreement(data);
      return data;
    }
    const sourceId = versionSourceId ?? sourceAgreement?.id;
    const { data } = await axiosInstance.post(ENDPOINTS.AGREEMENT_CREATE_VERSION(sourceId), payload);
    setDraftAgreementId(data.id);
    setSourceAgreement(data);
    navigate(buildAgreementEditPath(data.id, { step: urlStepFromInternal(state.step) }), { replace: true });
    if (validateStep1) {
      const { data: updated } = await axiosInstance.put(
        ENDPOINTS.AGREEMENT_UPDATE(data.id),
        payload,
        { params: { validateStep1: true, validateStep2: false } },
      );
      setSourceAgreement(updated);
      return updated;
    }
    return data;
  };

  const handleSaveDraft = async () => {
    if (state.step === 0 && !validateStep1Fields(state, enqueueSnackbar)) return;
    setSavingDraft(true);
    try {
      const saved = await persistDraft({ validateStep1: state.step === 0 });
      const label = saved?.versionNumber ? `V${saved.versionNumber}` : 'draft';
      enqueueSnackbar(`Agreement ${label} saved`, { variant: 'success' });
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Failed to save draft', { variant: 'error' });
    } finally {
      setSavingDraft(false);
    }
  };

  const handleSetupNext = async () => {
    if (!validateStep1Fields(state, enqueueSnackbar)) return;
    setSavingDraft(true);
    try {
      await persistDraft({ validateStep1: true });
      enqueueSnackbar('Product template saved', { variant: 'success' });
      syncStepToUrl(1);
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Complete required step 1 fields', { variant: 'error' });
    } finally {
      setSavingDraft(false);
    }
  };

  const handleSaveAndCreateAnother = async () => {
    if (!validateStep2LoopFields(state, enqueueSnackbar)) return;
    setSavingLoop(true);
    try {
      await persistDraft({ validateStep2: true });
      const cloned = await cloneAgreementOnServer(axiosInstance, ENDPOINTS, draftAgreementId);
      applyCloneResponse(cloned);
      setSourceAgreement(cloned);
      setDraftAgreementId(cloned.id);
      hydratedRef.current = true;
      navigate(buildAgreementEditPath(cloned.id, { step: urlStepFromInternal(1) }), { replace: true });
      enqueueSnackbar('Agreement saved — new draft ready', { variant: 'success' });
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Failed to save and create another', { variant: 'error' });
    } finally {
      setSavingLoop(false);
    }
  };

  const handleFinishAndExit = async () => {
    setSavingDraft(true);
    try {
      const saved = await persistDraft();
      enqueueSnackbar('Agreement saved', { variant: 'success' });
      if (saved?.agreementGroupId) {
        navigate(buildAgreementDetailPath(saved.agreementGroupId));
      } else {
        navigate(ROUTES.AGREEMENTS);
      }
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Failed to save agreement', { variant: 'error' });
    } finally {
      setSavingDraft(false);
    }
  };

  const handleRevisionNext = async () => {
    if (state.step === 0) {
      if (!validateStep1Fields(state, enqueueSnackbar)) return;
      setSavingDraft(true);
      try {
        await persistDraft({ validateStep1: true });
        syncStepToUrl(1);
      } catch (err) {
        enqueueSnackbar(err.response?.data?.message || 'Complete required step 1 fields', { variant: 'error' });
      } finally {
        setSavingDraft(false);
      }
      return;
    }
    if (!validateRevisionStep()) return;
    syncStepToUrl(state.step + 1);
  };

  const handleBack = () => {
    if (state.step === 0) return;
    syncStepToUrl(state.step - 1);
  };

  const handleSubmitForApproval = async () => {
    if (!validateAgreementForSubmit(state, enqueueSnackbar)) return;
    if (versionSourceId) {
      setSubmitModalOpen(true);
      return;
    }
    setSubmitting(true);
    try {
      const saved = await persistDraft();
      const targetId = saved?.id ?? draftAgreementId;
      await axiosInstance.put(ENDPOINTS.AGREEMENT_SUBMIT(targetId));
      enqueueSnackbar('Agreement submitted for approval', { variant: 'success' });
      reset();
      navigate(saved?.agreementGroupId
        ? buildAgreementDetailPath(saved.agreementGroupId)
        : ROUTES.AGREEMENTS);
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Failed to submit agreement', { variant: 'error' });
    } finally {
      setSubmitting(false);
    }
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
      navigate(buildAgreementDetailPath(data.agreementGroupId));
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Failed to submit edited version', { variant: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  const handleSaveContractDetails = useCallback(async () => {
    const { details } = state.agreement ?? {};
    if (!validateContractDetailsFields(details, enqueueSnackbar)) {
      throw new Error('Contract details validation failed');
    }
    return persistDraft();
  }, [state, enqueueSnackbar, persistDraft]);

  const footerMode = (() => {
    if (!isFreshDraftWizard) {
      if (state.step === 2) return 'review';
      return 'revision';
    }
    if (state.step === 0) return 'setup';
    if (state.step === 1) return 'details';
    return 'review';
  })();

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
      updateFields={updateFields}
      updateAgreementDetails={updateAgreementDetails}
      updateAgreementCommercials={updateAgreementCommercials}
      documentErrors={documentErrors}
      onClearDocumentError={clearDocumentError}
      serverAgreementId={draftAgreementId}
      sourceAgreement={sourceAgreement}
      onSaveContractDetails={handleSaveContractDetails}
    />,
    <Step5Review state={state} serverAgreementId={draftAgreementId} />,
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
          {isFreshDraftWizard ? 'New Draft' : 'Edit Agreement'}
        </Typography>
        <Typography sx={{ fontSize: { xs: '1.35rem', md: '1.6rem' }, fontWeight: 800, color: BRAND.textPrimary, letterSpacing: '-0.5px' }}>
          {state.agreementName || sourceAgreement.agreementName || sourceAgreement.agreementNumber}
        </Typography>
        <Typography sx={{ mt: 0.5, fontSize: '0.9rem', color: '#64748B' }}>
          {sourceAgreement.agreementNumber} · {versionLabel}
          {isFreshDraftWizard ? ' — complete details or create another' : ' — submit when ready for approval'}
        </Typography>
      </Paper>

      <WizardLayout
        activeStep={state.step}
        footerMode={footerMode}
        onNext={isFreshDraftWizard ? handleSetupNext : handleRevisionNext}
        onBack={handleBack}
        onCancel={() => navigate(
          sourceAgreement.agreementGroupId
            ? buildAgreementDetailPath(sourceAgreement.agreementGroupId)
            : ROUTES.AGREEMENTS,
        )}
        onSaveDraft={handleSaveDraft}
        onSaveAndCreateAnother={handleSaveAndCreateAnother}
        onFinishAndExit={handleFinishAndExit}
        onSubmitForApproval={handleSubmitForApproval}
        showSaveDraft={sourceAgreement.approvalStatus === 'DRAFT'}
        isSavingDraft={savingDraft}
        isSavingLoop={savingLoop}
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
