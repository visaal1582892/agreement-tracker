import { Button, Stack } from '@mui/material';
import { Add, Edit, PowerSettingsNew } from '@mui/icons-material';
import { BRAND } from '../../config/theme';
import { isRecordActive } from '../../utils/masterUtils';
import { useAuth } from '../../hooks/useAuth';
import { RIGHTS } from '../../config/rights';

export function MasterAddButton({ label, onClick }) {
  const { hasRight } = useAuth();
  if (!hasRight(RIGHTS.MASTER_MANAGE)) return null;

  return (
    <Button
      variant="contained"
      startIcon={<Add />}
      onClick={onClick}
      sx={{ background: BRAND.redGradient }}
    >
      {label}
    </Button>
  );
}

export function MasterRowActions({ row, onEdit, onToggleStatus }) {
  const { hasRight } = useAuth();
  if (!hasRight(RIGHTS.MASTER_MANAGE)) return null;

  const active = isRecordActive(row);

  return (
    <Stack direction="row" spacing={0.5}>
      <Button
        size="small"
        variant="outlined"
        startIcon={<Edit sx={{ fontSize: 14 }} />}
        onClick={(e) => { e.stopPropagation(); onEdit(row); }}
        sx={{ fontSize: '0.72rem', py: 0.3, px: 1 }}
      >
        Edit
      </Button>
      <Button
        size="small"
        variant="outlined"
        color={active ? 'error' : 'success'}
        startIcon={<PowerSettingsNew sx={{ fontSize: 14 }} />}
        onClick={(e) => { e.stopPropagation(); onToggleStatus(row); }}
        sx={{ fontSize: '0.72rem', py: 0.3, px: 1 }}
      >
        {active ? 'Deactivate' : 'Activate'}
      </Button>
    </Stack>
  );
}
