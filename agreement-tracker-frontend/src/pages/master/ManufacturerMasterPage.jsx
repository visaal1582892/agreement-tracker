import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import {
  Box, Button, TextField, Chip, Stack, Typography, FormControlLabel, Switch, alpha,
} from '@mui/material';
import { Add, Edit, PowerSettingsNew } from '@mui/icons-material';
import DataTable from '../../components/ui/DataTable';
import SlidePanel from '../../components/ui/SlidePanel';
import { manufacturerApi } from '../../api/masterApi';
import { useMasterPage } from '../../hooks/useMasterPage';
import { BRAND } from '../../config/theme';
import { isRecordActive } from '../../utils/masterUtils';

const STATUS_OPTS = [{ value: 'true', label: 'Active' }, { value: 'false', label: 'Inactive' }];

const COLUMNS = [
  { field: 'id',                 header: '#',                  width: 60,  sortable: true },
  { field: 'manufacturerCode',   header: 'Code',               width: 130, sortable: true, filterType: 'text' },
  { field: 'manufacturerName',   header: 'Manufacturer Name',              sortable: true, filterType: 'text' },
  {
    field: 'isActive', header: 'Status', width: 110, sortable: true,
    filterType: 'select', filterOptions: STATUS_OPTS,
    render: (_, row) => (
      <Chip label={isRecordActive(row) ? 'Active' : 'Inactive'} size="small"
        sx={{ bgcolor: isRecordActive(row) ? alpha(BRAND.green, 0.12) : alpha('#EF4444', 0.10),
          color: isRecordActive(row) ? BRAND.greenDark : '#DC2626', fontWeight: 600, fontSize: '0.72rem' }} />
    ),
  },
  { field: '_actions', header: 'Actions', width: 160, sortable: false },
];

export default function ManufacturerMasterPage() {
  const page = useMasterPage({ api: manufacturerApi, entityLabel: 'Manufacturer' });

  const enrichedRows = page.rows.map((row) => ({
    ...row,
    _actions: (
      <Stack direction="row" spacing={0.5}>
        <Button size="small" variant="outlined" startIcon={<Edit sx={{ fontSize: 14 }} />}
          onClick={(e) => { e.stopPropagation(); page.openEdit(row); }}
          sx={{ fontSize: '0.72rem', py: 0.3, px: 1 }}>Edit</Button>
        <Button size="small" variant="outlined" color={isRecordActive(row) ? 'error' : 'success'}
          startIcon={<PowerSettingsNew sx={{ fontSize: 14 }} />}
          onClick={(e) => { e.stopPropagation(); page.handleToggleStatus(row); }}
          sx={{ fontSize: '0.72rem', py: 0.3, px: 1 }}>{isRecordActive(row) ? 'Deactivate' : 'Activate'}</Button>
      </Stack>
    ),
  }));

  return (
    <Box>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Box>
          <Typography variant="h6" sx={{ fontWeight: 700 }}>Manufacturers</Typography>
          <Typography variant="body2" color="text.secondary">{page.totalCount} record{page.totalCount !== 1 ? 's' : ''}</Typography>
        </Box>
        <Button variant="contained" startIcon={<Add />} onClick={page.openCreate} sx={{ background: BRAND.redGradient }}>Add Manufacturer</Button>
      </Stack>
      <DataTable columns={COLUMNS} rows={enrichedRows} loading={page.loading} totalCount={page.totalCount}
        page={page.page} rowsPerPage={page.rowsPerPage} onPageChange={page.handlePageChange}
        onRowsPerPageChange={page.handleRowsPerPageChange} sortBy={page.sortBy} sortDir={page.sortDir}
        onSort={page.handleSort} filters={page.filters} onFilterChange={page.handleFilterChange} />
      <MfgFormPanel open={page.panelOpen} onClose={page.closePanel} editingRow={page.editingRow}
        saving={page.saving} onSave={page.handleSave} />
    </Box>
  );
}

function MfgFormPanel({ open, onClose, editingRow, saving, onSave }) {
  const isEdit = Boolean(editingRow);
  const { register, handleSubmit, reset, formState: { errors } } = useForm();

  useEffect(() => {
    if (!open) return;
    reset(isEdit ? editingRow : { manufacturerCode: '', manufacturerName: '', isActive: true });
  }, [open, editingRow, isEdit, reset]);

  return (
    <SlidePanel open={open} onClose={onClose} title={isEdit ? 'Edit Manufacturer' : 'Add Manufacturer'} loading={saving}>
      <Box component="form" onSubmit={handleSubmit(onSave)} noValidate
        sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
        <TextField label="Manufacturer Code" {...register('manufacturerCode')} fullWidth helperText="Optional unique code" />
        <TextField label="Manufacturer Name" required error={!!errors.manufacturerName}
          helperText={errors.manufacturerName?.message}
          {...register('manufacturerName', { required: 'Manufacturer name is required' })} fullWidth />
        {isEdit && (
          <FormControlLabel control={<Switch {...register('isActive')} defaultChecked={isRecordActive(editingRow)} color="success" />} label="Active" />
        )}
        <Stack direction="row" spacing={1.5} sx={{ justifyContent: 'flex-end', mt: 1 }}>
          <Button variant="outlined" onClick={onClose} disabled={saving}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={saving} sx={{ background: BRAND.redGradient }}>
            {isEdit ? 'Save Changes' : 'Create Manufacturer'}
          </Button>
        </Stack>
      </Box>
    </SlidePanel>
  );
}
