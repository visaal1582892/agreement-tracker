import { Box, Typography, Grid, Divider, Chip } from '@mui/material';
import dayjs from 'dayjs';

function ReviewRow({ label, value }) {
  return (
    <Box sx={{ display: 'flex', py: 0.75 }}>
      <Typography variant="body2" color="text.secondary" sx={{ width: 180, flexShrink: 0 }}>{label}</Typography>
      <Typography variant="body2" fontWeight={500}>{value || '—'}</Typography>
    </Box>
  );
}

export default function Step5Review({ state }) {
  return (
    <Box>
      <Typography variant="h6" fontWeight={600} mb={0.5}>Review & Submit</Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        Review all details before submitting for approval.
      </Typography>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Typography variant="subtitle2" fontWeight={700} mb={1} color="primary">Company & Vendors</Typography>
          <ReviewRow label="Company" value={state.companyName} />
          <ReviewRow label="Vendors" value={`${state.vendorIds?.length || 0} selected`} />
          <Divider sx={{ my: 1.5 }} />

          <Typography variant="subtitle2" fontWeight={700} mb={1} color="primary">Agreement Details</Typography>
          <ReviewRow label="Income Type" value={state.incomeTypeId ? `ID: ${state.incomeTypeId}` : ''} />
          <ReviewRow label="Agreement Type" value={state.agreementTypeId ? `ID: ${state.agreementTypeId}` : ''} />
          <ReviewRow label="Start Date" value={state.startDate ? dayjs(state.startDate).format('DD MMM YYYY') : ''} />
          <ReviewRow label="Expiry Date" value={state.expiryDate ? dayjs(state.expiryDate).format('DD MMM YYYY') : ''} />
          <ReviewRow label="Tenure" value={state.tenureMonths ? `${state.tenureMonths} months` : ''} />
          {state.notes && <ReviewRow label="Notes" value={state.notes} />}
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <Typography variant="subtitle2" fontWeight={700} mb={1} color="primary">Commercial Structure</Typography>
          <ReviewRow label="Structure" value={state.commercialStructure} />
          {state.commercialStructure === 'FLAT' && (
            <ReviewRow label="Value" value={state.commercialValue ? `₹${Number(state.commercialValue).toLocaleString('en-IN')}` : ''} />
          )}
          {state.commercialStructure === 'SLAB' && (
            <>
              <ReviewRow label="Distribution" value={state.timeDistribution} />
              <ReviewRow label="Slabs" value={`${state.slabs?.length || 0} tier(s)`} />
              {state.slabs?.map((s, i) => (
                <ReviewRow key={i} label={`  Tier ${i + 1}`} value={`${s.slabName} (₹${s.fromValue} – ₹${s.toValue})`} />
              ))}
            </>
          )}

          <Divider sx={{ my: 1.5 }} />

          <Typography variant="subtitle2" fontWeight={700} mb={1} color="primary">Products</Typography>
          <ReviewRow label="Products" value={`${state.productIds?.length || 0} selected`} />

          <Divider sx={{ my: 1.5 }} />

          <Typography variant="subtitle2" fontWeight={700} mb={1} color="primary">Documents</Typography>
          {state.documents?.length ? (
            state.documents.map((d, i) => (
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
    </Box>
  );
}
