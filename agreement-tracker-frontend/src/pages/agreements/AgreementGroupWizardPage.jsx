import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Box, CircularProgress, Alert } from '@mui/material';
import { useSnackbar } from 'notistack';
import axiosInstance from '../../api/axiosInstance';
import { ENDPOINTS } from '../../config/endpoints';
import { ROUTES } from '../../config/routes';
import { fetchGroupDraftAgreements, submitAgreementGroupForApproval } from '../../api/agreementGroupApi';
import WizardLayout from '../../layouts/WizardLayout';
import { useAgreementWizard } from '../../hooks/useAgreementWizard';
import { useAuth } from '../../hooks/useAuth';
import { resolveAgreementListScope } from '../../utils/authUtils';
import { cloneAgreementOnServer } from '../../utils/agreementClone';
import {
  buildGroupDetailPath,
  buildGroupWizardPath,
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
import Step5GroupReview from './wizard/Step5GroupReview';
import {
  incompleteDraftLabels,
  loadGroupDraftReviewData,
} from '../../utils/groupDraftValidation';

function draftTabLabel(row) {
  return row.agreementName
    || row.incomeTypeName
    || `Draft #${row.id}`;
}

export default function AgreementGroupWizardPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const { hasRight } = useAuth();
  const agreementScope = resolveAgreementListScope(hasRight);

  const groupId = searchParams.get('groupId');
  const activeAgreementId = searchParams.get('activeAgreementId');
  const parsedGroupId = Number(groupId);
  const parsedActiveAgreementId = Number(activeAgreementId);

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

  const [groupDrafts, setGroupDrafts] = useState([]);
  const [sourceAgreement, setSourceAgreement] = useState(null);
  const [draftAgreementId, setDraftAgreementId] = useState(null);
  const [loadError, setLoadError] = useState(null);
  const [loadingGroup, setLoadingGroup] = useState(true);
  const [loadingDraft, setLoadingDraft] = useState(false);
  const [savingDraft, setSavingDraft] = useState(false);
  const [savingLoop, setSavingLoop] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [documentErrors, setDocumentErrors] = useState({});

  const loadedVersionRef = useRef(null);

  const refreshGroupDrafts = useCallback(async () => {
    if (!parsedGroupId || Number.isNaN(parsedGroupId)) return [];
    const rows = await fetchGroupDraftAgreements(parsedGroupId, agreementScope);
    setGroupDrafts(rows);
    return rows;
  }, [parsedGroupId, agreementScope]);

  const draftTabs = useMemo(
    () => groupDrafts.map((row) => ({
      agreementId: row.id,
      latestVersionId: row.latestVersionId,
      label: draftTabLabel(row),
    })),
    [groupDrafts],
  );

  const loadActiveDraft = useCallback(async (agreementId, drafts) => {
    const rows = drafts ?? groupDrafts;
    const row = rows.find((d) => d.id === agreementId);
    if (!row?.latestVersionId) {
      throw new Error('Draft agreement version not found');
    }
    setLoadingDraft(true);
    try {
      const { data: loaded } = await axiosInstance.get(
        ENDPOINTS.AGREEMENT_VERSION_BY_ID(row.latestVersionId),
      );
      if (loaded.approvalStatus !== 'DRAFT') {
        throw new Error('Only draft agreements can be edited in the group wizard');
      }
      setSourceAgreement(loaded);
      setDraftAgreementId(loaded.id);
      hydrateFromEdit(loaded);
      loadedVersionRef.current = loaded.id;
    } finally {
      setLoadingDraft(false);
    }
  }, [groupDrafts, hydrateFromEdit]);

  useEffect(() => {
    if (!parsedGroupId || Number.isNaN(parsedGroupId)) {
      setLoadError('Invalid group wizard link');
      setLoadingGroup(false);
      return;
    }

    const initGroup = async () => {
      setLoadingGroup(true);
      setLoadError(null);
      try {
        const rows = await refreshGroupDrafts();
        if (rows.length === 0) {
          setLoadError('No draft agreements found in this group');
          return;
        }

        const hasValidActive = !Number.isNaN(parsedActiveAgreementId)
          && rows.some((r) => r.id === parsedActiveAgreementId);
        if (!hasValidActive) {
          setSearchParams((prev) => {
            const next = new URLSearchParams(prev);
            next.set('activeAgreementId', String(rows[0].id));
            return next;
          }, { replace: true });
        }
      } catch (err) {
        const msg = err.response?.data?.message || err.message || 'Failed to load group drafts';
        setLoadError(msg);
        enqueueSnackbar(msg, { variant: 'error' });
      } finally {
        setLoadingGroup(false);
      }
    };

    initGroup();
  }, [parsedGroupId, parsedActiveAgreementId, refreshGroupDrafts, setSearchParams, enqueueSnackbar]);

  useEffect(() => {
    if (!parsedGroupId || Number.isNaN(parsedGroupId) || loadingGroup) return;
    if (!activeAgreementId || Number.isNaN(parsedActiveAgreementId)) return;
    if (groupDrafts.length === 0) return;

    const row = groupDrafts.find((d) => d.id === parsedActiveAgreementId);
    if (!row) return;
    if (loadedVersionRef.current === row.latestVersionId) return;

    const load = async () => {
      try {
        await loadActiveDraft(parsedActiveAgreementId, groupDrafts);
      } catch (err) {
        const msg = err.response?.data?.message || err.message || 'Failed to load draft';
        setLoadError(msg);
        enqueueSnackbar(msg, { variant: 'error' });
      }
    };
    load();
  }, [
    parsedGroupId,
    parsedActiveAgreementId,
    activeAgreementId,
    groupDrafts,
    loadingGroup,
    loadActiveDraft,
    enqueueSnackbar,
  ]);

  useEffect(() => {
    if (!sourceAgreement) return;
    const rawStep = searchParams.get('step');
    if (rawStep == null || rawStep === '') return;
    const internalStep = internalStepFromUrl(rawStep);
    if (internalStep == null) return;
    updateStep(internalStep);
  }, [sourceAgreement, searchParams, updateStep]);

  const syncStepToUrl = useCallback((internalStep) => {
    const params = new URLSearchParams(searchParams);
    params.set('step', String(urlStepFromInternal(internalStep)));
    setSearchParams(params, { replace: true });
    updateStep(internalStep);
  }, [searchParams, setSearchParams, updateStep]);

  const handleDraftTabChange = useCallback((agreementId) => {
    const path = buildGroupWizardPath(parsedGroupId, agreementId, {
      step: searchParams.get('step') || undefined,
    });
    if (path) navigate(path);
  }, [parsedGroupId, searchParams, navigate]);

  const clearDocumentError = (id) => {
    setDocumentErrors((prev) => { const n = { ...prev }; delete n[id]; return n; });
  };

  const buildUpdatePayload = useCallback(() => buildStep1UpdatePayload(state), [state]);

  const persistDraft = async ({ validateStep1 = false, validateStep2 = false } = {}) => {
    if (!draftAgreementId) {
      throw new Error('No draft version loaded');
    }
    const { data } = await axiosInstance.put(
      ENDPOINTS.AGREEMENT_VERSION_UPDATE(draftAgreementId),
      buildUpdatePayload(),
      { params: { validateStep1, validateStep2 } },
    );
    setSourceAgreement(data);
    if (data.agreementName) {
      updateFields({ agreementName: data.agreementName });
    }
    await refreshGroupDrafts();
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
      const rows = await refreshGroupDrafts();
      const newAgreementId = cloned.agreementId;
      applyCloneResponse(cloned);
      loadedVersionRef.current = null;
      const path = buildGroupWizardPath(parsedGroupId, newAgreementId, { step: 2 });
      if (path) {
        navigate(path, { replace: true });
      } else {
        await loadActiveDraft(newAgreementId, rows);
      }
      enqueueSnackbar('Agreement saved — new draft ready', { variant: 'success' });
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Failed to save and create another', { variant: 'error' });
    } finally {
      setSavingLoop(false);
    }
  };

  const validateCurrentAgreementDetails = () => {
    if (!validateStep2LoopFields(state, enqueueSnackbar)) return false;
    const { details, commercials } = state.agreement ?? {};
    if (!validateContractDetailsFields(details, enqueueSnackbar)) return false;
    if (!commercials?.commercialStructure) {
      enqueueSnackbar('Commercial structure is required', { variant: 'warning' });
      return false;
    }
    if (commercials.commercialStructure === 'FLAT' && !commercials.commercialValue) {
      enqueueSnackbar('Commercial value is required for FLAT structure', { variant: 'warning' });
      return false;
    }
    return true;
  };

  const handleDetailsNext = async () => {
    if (!validateCurrentAgreementDetails()) return;
    setSavingDraft(true);
    try {
      await persistDraft({ validateStep2: true });
      const rows = await refreshGroupDrafts();
      const reviewData = await loadGroupDraftReviewData(rows);
      const incomplete = reviewData.filter((item) => !item.isComplete);
      if (incomplete.length > 0) {
        enqueueSnackbar(
          `Complete details for: ${incompleteDraftLabels(reviewData).join(', ')}`,
          { variant: 'warning' },
        );
        return;
      }
      syncStepToUrl(2);
    } catch (err) {
      enqueueSnackbar(err.response?.data?.message || 'Failed to validate group drafts', { variant: 'error' });
    } finally {
      setSavingDraft(false);
    }
  };

  const handleBack = () => {
    if (state.step === 0) return;
    syncStepToUrl(state.step - 1);
  };

  const handleSubmitForApproval = async () => {
    setSubmitting(true);
    try {
      const rows = await refreshGroupDrafts();
      const reviewData = await loadGroupDraftReviewData(rows);
      const incomplete = reviewData.filter((item) => !item.isComplete);
      if (incomplete.length > 0) {
        enqueueSnackbar(
          `Cannot submit — incomplete: ${incompleteDraftLabels(reviewData).join(', ')}`,
          { variant: 'warning' },
        );
        setSubmitting(false);
        return;
      }

      const result = await submitAgreementGroupForApproval(parsedGroupId);
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
  };

  const handleSaveContractDetails = useCallback(async () => {
    const { details } = state.agreement ?? {};
    if (!validateContractDetailsFields(details, enqueueSnackbar)) {
      throw new Error('Contract details validation failed');
    }
    return persistDraft();
  }, [state, enqueueSnackbar]);

  const footerMode = (() => {
    if (state.step === 0) return 'setup';
    if (state.step === 1) return 'details';
    return 'review';
  })();

  const STEP_COMPONENTS = [
    <Step1Setup
      key={`step1-${draftAgreementId}`}
      state={state}
      updateFields={updateFields}
      updateProductRules={updateProductRules}
      groupFieldsLocked
    />,
    <Step2Agreements
      key={`step2-${draftAgreementId}`}
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
    <Step5GroupReview
      key={`step5-group-${parsedActiveAgreementId}`}
      sharedState={state}
      groupDrafts={groupDrafts}
      activeAgreementId={parsedActiveAgreementId}
    />,
  ];

  if (loadError) {
    return <Alert severity="error" sx={{ m: 3 }}>{loadError}</Alert>;
  }

  if (loadingGroup || !sourceAgreement) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column' }}>
      {loadingDraft && (
        <Box sx={{ position: 'fixed', inset: 0, bgcolor: 'rgba(255,255,255,0.45)', zIndex: 10, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <CircularProgress />
        </Box>
      )}
      <WizardLayout
        activeStep={state.step}
        draftTabs={draftTabs}
        activeDraftId={parsedActiveAgreementId}
        onDraftTabChange={handleDraftTabChange}
        showDraftTabs
        submitButtonLabel="Submit for Approval"
        footerMode={footerMode}
        onNext={handleSetupNext}
        onBack={handleBack}
        onCancel={() => navigate(buildGroupDetailPath(parsedGroupId) || ROUTES.AGREEMENTS)}
        onSaveAndCreateAnother={handleSaveAndCreateAnother}
        onDetailsNext={handleDetailsNext}
        onSubmitForApproval={handleSubmitForApproval}
        isSavingDraft={savingDraft}
        isSavingLoop={savingLoop}
        isSubmitting={submitting}
      >
        {STEP_COMPONENTS[state.step]}
      </WizardLayout>
    </Box>
  );
}
