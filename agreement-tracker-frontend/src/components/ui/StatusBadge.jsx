import { Box, Typography, alpha } from '@mui/material';

const STATUS_CONFIG = {
  SUPERSEDED:       { label: 'Superseded',        bg: '#F1F5F9', color: '#94A3B8', dot: '#CBD5E1' },
  ACTIVE:           { label: 'Active',           bg: '#DCFCE7', color: '#15803D', dot: '#22C55E' },
  EXPIRED:          { label: 'Expired',           bg: '#FEE2E2', color: '#B91C1C', dot: '#EF4444' },
  TERMINATED:       { label: 'Terminated',        bg: '#FEE2E2', color: '#B91C1C', dot: '#EF4444' },
  IN_PROGRESS:      { label: 'In Progress',       bg: '#FEF3C7', color: '#B45309', dot: '#F59E0B' },
  DRAFT:            { label: 'Draft',             bg: '#F1F5F9', color: '#475569', dot: '#94A3B8' },
  PENDING_APPROVAL: { label: 'Pending Approval',  bg: '#FEF3C7', color: '#B45309', dot: '#F59E0B' },
  APPROVED:         { label: 'Approved',          bg: '#DCFCE7', color: '#15803D', dot: '#22C55E' },
  REJECTED:         { label: 'Rejected',          bg: '#FEE2E2', color: '#B91C1C', dot: '#EF4444' },
};

export default function StatusBadge({ status, size = 'small' }) {
  const cfg = STATUS_CONFIG[status] || { label: status || '—', bg: '#F1F5F9', color: '#64748B', dot: '#94A3B8' };
  const isSmall = size === 'small';

  return (
    <Box sx={{
      display: 'inline-flex', alignItems: 'center', gap: 0.6,
      px: isSmall ? 1 : 1.5, py: isSmall ? 0.3 : 0.5,
      bgcolor: cfg.bg, borderRadius: 1.5,
      border: `1px solid ${alpha(cfg.color, 0.2)}`,
    }}>
      <Box sx={{
        width: isSmall ? 6 : 7, height: isSmall ? 6 : 7,
        borderRadius: '50%', bgcolor: cfg.dot, flexShrink: 0,
      }} />
      <Typography sx={{
        fontSize: isSmall ? '0.72rem' : '0.8rem',
        fontWeight: 600, color: cfg.color, lineHeight: 1,
        whiteSpace: 'nowrap',
      }}>
        {cfg.label}
      </Typography>
    </Box>
  );
}
