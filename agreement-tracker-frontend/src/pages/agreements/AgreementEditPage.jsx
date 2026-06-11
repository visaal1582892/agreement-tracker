import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import {
  Box, CircularProgress, Alert, Dialog,
  DialogTitle, DialogContent, DialogActions, Button, TextField,
} from '@mui/material';
import { useSnackbar } from 'notistack';
import axiosInstance from '../../api/axiosInstance';
import { ENDPOINTS } from '../../config/endpoints';
import { ROUTES } from '../../config/routes';
import { BRAND } from '../../config/theme';
import { submitAgreementGroupForApproval } from '../../api/agreementGroupApi';
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
  const { agreementVersionId: agreementId } = useParams();
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
        const { data: loaded } = await axiosInstance.get(ENDPOINTS.AGREEMENT_VERSION_BY_ID(agreementId));

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
          ENDPOINTS.AGREEMENT_VERSIONS(loaded.agreementId),
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
        ENDPOINTS.AGREEMENT_VERSION_UPDATE(draftAgreementId),
        payload,
        { params: { validateStep1, validateStep2 } },
      );
      setSourceAgreement(data);
      if (data.agreementName) {
        updateFields({ agreementName: data.agreementName });
      }
      return data;
    }
    const sourceId = versionSourceId ?? sourceAgreement?.id;
    const { data } = await axiosInstance.post(ENDPOINTS.AGREEMENT_VERSION_CREATE_EDIT(sourceId), payload);
    setDraftAgreementId(data.id);
    setSourceAgreement(data);
    if (data.agreementName) {
      updateFields({ agreementName: data.agreementName });
    }
    navigate(buildAgreementEditPath(data.id, { step: urlStepFromInternal(state.step) }), { replace: true });
    if (validateStep1) {
      const { data: updated } = await axiosInstance.put(
        ENDPOINTS.AGREEMENT_VERSION_UPDATE(data.id),
        payload,
        { params: { validateStep1: true, validateStep2: false } },
      );
      setSourceAgreement(updated);
      if (updated.agreementName) {
        updateFields({ agreementName: updated.agreementName });
      }
      return updated;
    }
    return data;
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
      if (saved?.agreementId) {
        navigate(buildAgreementDetailPath(saved.agreementId));
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
    if (versionSourceId) {
      if (!validateAgreementForSubmit(state, enqueueSnackbar)) return;
      setSubmitModalOpen(true);
      return;
    }

    if (isFreshDraftWizard) {
      const groupId = state.companyAgreementGroupId;
      if (!groupId) {
        enqueueSnackbar('Company agreement group is required for bulk submit', { variant: 'warning' });
        return;
      }

      setSubmitting(true);
      try {
        if (state.step >= 1) {
          if (!validateStep2LoopFields(state, enqueueSnackbar)) {
            setSubmitting(false);
            return;
          }
          await persistDraft({ validateStep2: true });
        } else {
          await persistDraft();
        }

        const result = await submitAgreementGroupForApproval(groupId);
        enqueueSnackbar(
          `Submitted ${result.submittedCount} agreement(s) for approval`,
          { variant: 'success' },
        );
        reset();
        navigate(ROUTES.AGREEMENTS);
      } catch (err) {
        enqueueSnackbar(
          err.response?.data?.message || err.message || 'Bulk submit failed',
          { variant: 'error' },
        );
      } finally {
        setSubmitting(false);
      }
      return;
    }

    if (!validateAgreementForSubmit(state, enqueueSnackbar)) return;
    setSubmitting(true);
    try {
      const saved = await persistDraft();
      const targetId = saved?.id ?? draftAgreementId;
      await axiosInstance.put(ENDPOINTS.AGREEMENT_VERSION_SUBMIT(targetId));
      enqueueSnackbar('Agreement submitted for approval', { variant: 'success' });
      reset();
      navigate(saved?.agreementId
        ? buildAgreementDetailPath(saved.agreementId)
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
      await axiosInstance.put(ENDPOINTS.AGREEMENT_VERSION_SUBMIT(targetId), {
        comments: revisionComments.trim(),
      });
      enqueueSnackbar(`Version V${data.versionNumber} submitted for approval`, { variant: 'success' });
      setRevisionComments('');
      reset();
      navigate(buildAgreementDetailPath(data.agreementId));
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

  const agreementTabLabel = state.agreementName
    || sourceAgreement?.agreementName
    || 'New Agreement';

  const submitButtonLabel = isFreshDraftWizard ? 'Submit & Exit' : 'Submit for Approval';

  const STEP_COMPONENTS = [
    <Step1Setup
      state={state}
      updateFields={updateFields}
      updateProductRules={updateProductRules}
      groupFieldsLocked={Boolean(draftAgreementId)}
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
      <WizardLayout
        activeStep={state.step}
        agreementTabLabel={agreementTabLabel}
        submitButtonLabel={submitButtonLabel}
        footerMode={footerMode}
        onNext={isFreshDraftWizard ? handleSetupNext : handleRevisionNext}
        onBack={handleBack}
        onCancel={() => navigate(
          sourceAgreement.agreementId
            ? buildAgreementDetailPath(sourceAgreement.agreementId)
            : ROUTES.AGREEMENTS,
        )}
        onSaveAndCreateAnother={handleSaveAndCreateAnother}
        onFinishAndExit={handleFinishAndExit}
        onSubmitForApproval={handleSubmitForApproval}
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
