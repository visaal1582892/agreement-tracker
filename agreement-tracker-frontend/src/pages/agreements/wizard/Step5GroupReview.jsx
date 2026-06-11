import { useEffect, useState } from 'react';
import {
  Box, Typography, Grid, Paper, Divider, CircularProgress, Alert,
} from '@mui/material';
import dayjs from 'dayjs';
import { useSnackbar } from 'notistack';
import { formatTenureFromDates } from '../../../components/forms/DateRangeFields';
import { loadGroupDraftReviewData } from '../../../utils/groupDraftValidation';
import CommercialsUploadModal from './CommercialsUploadModal';

function ReviewRow({ label, value }) {
  return (
    <Box sx={{ display: 'flex', py: 0.75 }}>
      <Typography variant="body2" color="text.secondary" sx={{ width: 160, flexShrink: 0 }}>{label}</Typography>
      <Typography variant="body2" fontWeight={500}>{value || '—'}</Typography>
    </Box>
  );
}

export default function Step5GroupReview({ sharedState, groupDrafts, activeAgreementId }) {
  const { enqueueSnackbar } = useSnackbar();
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setLoading(true);
      try {
        const data = await loadGroupDraftReviewData(groupDrafts);
        if (!cancelled) setReviews(data);
      } catch (err) {
        if (!cancelled) {
          enqueueSnackbar(err.response?.data?.message || 'Failed to load review data', { variant: 'error' });
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    if (groupDrafts?.length) load();
    return () => { cancelled = true; };
  }, [groupDrafts, enqueueSnackbar]);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  const activeReview = reviews.find(
    (item) => item.row.id === activeAgreementId || item.version.agreementId === activeAgreementId,
  );
  const sharedRules = sharedState?.productRules || {};

  return (
    <Box>
      <Typography variant="h6" fontWeight={600} mb={0.5}>Review & Submit</Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        Review each draft agreement using the tabs above before submitting for approval.
      </Typography>

      <Paper variant="outlined" sx={{ p: 2, mb: 3, borderRadius: 2 }}>
        <Typography variant="subtitle2" fontWeight={700} mb={1} color="primary">Shared Setup</Typography>
        <ReviewRow label="Company" value={sharedState?.companyName} />
        <ReviewRow label="Agreement Group" value={sharedState?.companyAgreementGroupName} />
        <ReviewRow label="Vendors" value={`${sharedState?.vendorIds?.length || 0} selected`} />
        <ReviewRow label="Manufacturers" value={`${sharedRules.manufacturers?.length || 0} selected`} />
        <ReviewRow label="Division Rules" value={`${sharedRules.divisionRules?.length || 0} rule(s)`} />
        <ReviewRow label="Product Rules" value={`${sharedRules.productRules?.length || 0} rule(s)`} />
      </Paper>

      {!activeReview && (
        <Alert severity="warning">Select a draft agreement tab to review.</Alert>
      )}

      {activeReview && (
        <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 2 }}>
          <Typography variant="subtitle1" fontWeight={700} mb={2}>
            {activeReview.version.agreementName || activeReview.row.agreementName || 'Agreement'}
          </Typography>

          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 6 }}>
              <Typography variant="subtitle2" fontWeight={700} mb={1}>Details</Typography>
              <ReviewRow label="Income Type" value={activeReview.version.incomeTypeName} />
              <ReviewRow label="Agreement Type" value={activeReview.version.agreementTypeName} />
              <ReviewRow
                label="Start Date"
                value={activeReview.version.startDate
                  ? dayjs(activeReview.version.startDate).format('DD MMM YYYY')
                  : ''}
              />
              <ReviewRow
                label="Expiry Date"
                value={activeReview.version.expiryDate
                  ? dayjs(activeReview.version.expiryDate).format('DD MMM YYYY')
                  : ''}
              />
              <ReviewRow
                label="Tenure"
                value={formatTenureFromDates(
                  activeReview.version.startDate,
                  activeReview.version.expiryDate,
                )}
              />
              {activeReview.version.notes && (
                <ReviewRow label="Notes" value={activeReview.version.notes} />
              )}
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <Typography variant="subtitle2" fontWeight={700} mb={1}>Commercials</Typography>
              <ReviewRow label="Structure" value={activeReview.version.commercialStructure} />
              {activeReview.version.commercialStructure === 'FLAT' && (
                <ReviewRow
                  label="Value"
                  value={activeReview.version.commercialValue != null
                    ? `₹${Number(activeReview.version.commercialValue).toLocaleString('en-IN')}`
                    : ''}
                />
              )}
              {activeReview.version.commercialStructure === 'SLAB' && (
                <ReviewRow label="Purchase Slabs" value={`${activeReview.slabs.length} tier(s)`} />
              )}
            </Grid>
          </Grid>

          {activeReview.version.commercialStructure === 'SLAB' && (
            <Box sx={{ mt: 2 }}>
              <Divider sx={{ mb: 2 }} />
              <Typography variant="subtitle2" fontWeight={700} mb={1.5}>
                Commercial Targets Matrix
              </Typography>
              <CommercialsUploadModal
                embedded
                readOnly
                agreementId={activeReview.version.id}
                slabs={activeReview.slabs}
                startDate={activeReview.version.startDate}
                expiryDate={activeReview.version.expiryDate}
              />
            </Box>
          )}
        </Paper>
      )}

      {reviews.length === 0 && (
        <Alert severity="warning">No draft agreements to review.</Alert>
      )}
    </Box>
  );
}
