import { useEffect, useState } from 'react';
import { Box, Typography, Grid, Chip } from '@mui/material';
import dayjs from 'dayjs';
import { useSnackbar } from 'notistack';
import { formatTenureFromDates } from '../../../components/forms/DateRangeFields';
import { fetchPurchaseSlabs } from '../../../api/commercialApi';

function ReviewRow({ label, value }) {
  return (
    <Box sx={{ display: 'flex', py: 0.75 }}>
      <Typography variant="body2" color="text.secondary" sx={{ width: 180, flexShrink: 0 }}>{label}</Typography>
      <Typography variant="body2" fontWeight={500}>{value || '—'}</Typography>
    </Box>
  );
}

export default function Step5Review({ state, serverAgreementId }) {
  const { enqueueSnackbar } = useSnackbar();
  const sharedRules = state.productRules || {};
  const agreement = state.agreement;
  const [slabCount, setSlabCount] = useState(null);
  const [loadingSlabs, setLoadingSlabs] = useState(false);

  useEffect(() => {
    if (!serverAgreementId || agreement?.commercials?.commercialStructure !== 'SLAB') {
      setSlabCount(null);
      return;
    }

    let cancelled = false;
    const load = async () => {
      setLoadingSlabs(true);
      try {
        const data = await fetchPurchaseSlabs(serverAgreementId);
        if (!cancelled) {
          setSlabCount(data.length);
        }
      } catch (err) {
        if (!cancelled) {
          enqueueSnackbar(err.response?.data?.message || 'Failed to load slabs for review', { variant: 'error' });
          setSlabCount(0);
        }
      } finally {
        if (!cancelled) {
          setLoadingSlabs(false);
        }
      }
    };
    load();
    return () => { cancelled = true; };
  }, [serverAgreementId, agreement?.commercials?.commercialStructure, enqueueSnackbar]);

  if (!agreement) {
    return null;
  }

  return (
    <Box>
      <Typography variant="h6" fontWeight={600} mb={0.5}>Review & Submit</Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        Review setup and agreement details before submitting.
      </Typography>

      <Grid container spacing={3}>
        <Grid size={12}>
          <Typography variant="subtitle2" fontWeight={700} mb={1} color="primary">Shared Setup</Typography>
          <ReviewRow label="Company" value={state.companyName} />
          <ReviewRow label="Vendors" value={`${state.vendorIds?.length || 0} selected`} />
          <ReviewRow label="Manufacturers" value={`${sharedRules.manufacturers?.length || 0} selected`} />
          <ReviewRow label="Division Rules" value={`${sharedRules.divisionRules?.length || 0} rule(s)`} />
          <ReviewRow label="Product Rules" value={`${sharedRules.productRules?.length || 0} rule(s)`} />
        </Grid>

        <Grid size={12}>
          <Typography variant="subtitle1" fontWeight={700} mb={1.5} color="primary">
            Agreement Details
          </Typography>
          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 6 }}>
              <Typography variant="subtitle2" fontWeight={700} mb={1}>Details</Typography>
              <ReviewRow label="Agreement Name" value={state.agreementName} />
              <ReviewRow label="Income Type" value={agreement.details.incomeTypeId ? `ID: ${agreement.details.incomeTypeId}` : ''} />
              <ReviewRow label="Agreement Type" value={agreement.details.agreementTypeId ? `ID: ${agreement.details.agreementTypeId}` : ''} />
              <ReviewRow label="Start Date" value={agreement.details.startDate ? dayjs(agreement.details.startDate).format('DD MMM YYYY') : ''} />
              <ReviewRow label="Expiry Date" value={agreement.details.expiryDate ? dayjs(agreement.details.expiryDate).format('DD MMM YYYY') : ''} />
              <ReviewRow label="Tenure" value={formatTenureFromDates(agreement.details.startDate, agreement.details.expiryDate)} />
              {agreement.details.notes && <ReviewRow label="Notes" value={agreement.details.notes} />}
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <Typography variant="subtitle2" fontWeight={700} mb={1}>Commercials</Typography>
              <ReviewRow label="Structure" value={agreement.commercials.commercialStructure} />
              {agreement.commercials.commercialStructure === 'FLAT' && (
                <ReviewRow label="Value" value={agreement.commercials.commercialValue ? `₹${Number(agreement.commercials.commercialValue).toLocaleString('en-IN')}` : ''} />
              )}
              {agreement.commercials.commercialStructure === 'SLAB' && (
                <>
                  <ReviewRow
                    label="Frequencies"
                    value={agreement.commercials.selectedFrequencies?.join(', ') || '—'}
                  />
                  <ReviewRow
                    label="Slabs"
                    value={loadingSlabs
                      ? 'Loading…'
                      : `${slabCount ?? 0} tier(s)`}
                  />
                </>
              )}

              <Typography variant="subtitle2" fontWeight={700} mb={1} mt={1.5}>Documents</Typography>
              {agreement.details.documents?.length ? (
                agreement.details.documents.map((d, i) => (
                  <Box key={i} sx={{ display: 'flex', gap: 1, mb: 0.5 }}>
                    <Chip label={d.documentType} size="small" variant="outlined" />
                    <Typography variant="body2">{d.fileName}</Typography>
                  </Box>
                ))
              ) : (
                <Typography variant="body2" color="text.secondary">No documents uploaded</Typography>
              )}
            </Grid>
          </Grid>
        </Grid>
      </Grid>
    </Box>
  );
}
