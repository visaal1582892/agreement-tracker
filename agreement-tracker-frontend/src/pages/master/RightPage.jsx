import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import {
  Box, Button, TextField, Chip, Stack, Typography, FormControlLabel, Switch, alpha,
} from '@mui/material';
import DataTable from '../../components/ui/DataTable';
import SlidePanel from '../../components/ui/SlidePanel';
import { MasterAddButton, MasterRowActions } from '../../components/master/MasterCrudActions';
import { rightApi } from '../../api/masterApi';
import { useMasterPage } from '../../hooks/useMasterPage';
import { BRAND } from '../../config/theme';
import { isRecordActive } from '../../utils/masterUtils';

const STATUS_OPTS = [{ value: 'true', label: 'Active' }, { value: 'false', label: 'Inactive' }];

const COLUMNS = [
  { field: 'id',          header: '#',          width: 60,  sortable: true },
  { field: 'code',        header: 'Code',       width: 150, sortable: true, filterType: 'text' },
  { field: 'name',        header: 'Name',                   sortable: true, filterType: 'text' },
  { field: 'module',      header: 'Module',     width: 140, sortable: true, filterType: 'text' },
  { field: 'description', header: 'Description',            sortable: false, filterType: 'text' },
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

export default function RightPage() {
  const page = useMasterPage({ api: rightApi, entityLabel: 'Right' });

  const enrichedRows = page.rows.map((row) => ({
    ...row,
    _actions: (
      <MasterRowActions
        row={row}
        onEdit={page.openEdit}
        onToggleStatus={page.handleToggleStatus}
      />
    ),
  }));

  return (
    <Box>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Box>
          <Typography variant="h6" sx={{ fontWeight: 700 }}>Rights</Typography>
          <Typography variant="body2" color="text.secondary">{page.totalCount} record{page.totalCount !== 1 ? 's' : ''}</Typography>
        </Box>
        <MasterAddButton label="Add Right" onClick={page.openCreate} />
      </Stack>
      <DataTable columns={COLUMNS} rows={enrichedRows} loading={page.loading} totalCount={page.totalCount}
        page={page.page} rowsPerPage={page.rowsPerPage} onPageChange={page.handlePageChange}
        onRowsPerPageChange={page.handleRowsPerPageChange} sortBy={page.sortBy} sortDir={page.sortDir}
        onSort={page.handleSort} filters={page.filters} onFilterChange={page.handleFilterChange} />
      <RightFormPanel open={page.panelOpen} onClose={page.closePanel} editingRow={page.editingRow}
        saving={page.saving} onSave={page.handleSave} />
    </Box>
  );
}

function RightFormPanel({ open, onClose, editingRow, saving, onSave }) {
  const isEdit = Boolean(editingRow);
  const { register, handleSubmit, reset, formState: { errors } } = useForm();

  useEffect(() => {
    if (!open) return;
    reset(isEdit ? editingRow : { code: '', name: '', module: '', description: '', isActive: true });
  }, [open, editingRow, isEdit, reset]);

  return (
    <SlidePanel open={open} onClose={onClose} title={isEdit ? 'Edit Right' : 'Add Right'} loading={saving}>
      <Box component="form" onSubmit={handleSubmit(onSave)} noValidate
        sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
        <TextField label="Code" required error={!!errors.code} helperText={errors.code?.message}
          {...register('code', { required: 'Code is required' })} fullWidth />
        <TextField label="Name" required error={!!errors.name} helperText={errors.name?.message}
          {...register('name', { required: 'Name is required' })} fullWidth />
        <TextField label="Module" {...register('module')} fullWidth helperText="e.g. AGREEMENTS, MASTER, ADMIN" />
        <TextField label="Description" multiline rows={2} {...register('description')} fullWidth />
        {isEdit && (
          <FormControlLabel control={<Switch {...register('isActive')} defaultChecked={isRecordActive(editingRow)} color="success" />} label="Active" />
        )}
        <Stack direction="row" spacing={1.5} sx={{ justifyContent: 'flex-end', mt: 1 }}>
          <Button variant="outlined" onClick={onClose} disabled={saving}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={saving} sx={{ background: BRAND.redGradient }}>
            {isEdit ? 'Save Changes' : 'Create Right'}
          </Button>
        </Stack>
      </Box>
    </SlidePanel>
  );
}
