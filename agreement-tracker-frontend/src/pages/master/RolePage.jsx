import { useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import {
  Box, Button, TextField, Chip, Stack, Typography, FormControlLabel, Switch, alpha,
  MenuItem, Select, FormControl, InputLabel, FormHelperText,
} from '@mui/material';
import DataTable from '../../components/ui/DataTable';
import SlidePanel from '../../components/ui/SlidePanel';
import { MasterAddButton, MasterRowActions } from '../../components/master/MasterCrudActions';
import { roleApi } from '../../api/masterApi';
import { useMasterPage } from '../../hooks/useMasterPage';
import { BRAND } from '../../config/theme';
import { isRecordActive } from '../../utils/masterUtils';

const ROLE_NAMES = ['ADMIN', 'ACCOUNT_MANAGER', 'APPROVER', 'LEADERSHIP', 'FINANCE'];
const STATUS_OPTS = [{ value: 'true', label: 'Active' }, { value: 'false', label: 'Inactive' }];

const COLUMNS = [
  { field: 'id',          header: '#',           width: 60, sortable: true },
  { field: 'name',        header: 'Role',                   sortable: true, filterType: 'text' },
  { field: 'description', header: 'Description',            sortable: true, filterType: 'text' },
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

export default function RolePage() {
  const page = useMasterPage({ api: roleApi, entityLabel: 'Role' });

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
          <Typography variant="h6" sx={{ fontWeight: 700 }}>Roles</Typography>
          <Typography variant="body2" color="text.secondary">{page.totalCount} record{page.totalCount !== 1 ? 's' : ''}</Typography>
        </Box>
        <MasterAddButton label="Add Role" onClick={page.openCreate} />
      </Stack>
      <DataTable columns={COLUMNS} rows={enrichedRows} loading={page.loading} totalCount={page.totalCount}
        page={page.page} rowsPerPage={page.rowsPerPage} onPageChange={page.handlePageChange}
        onRowsPerPageChange={page.handleRowsPerPageChange} sortBy={page.sortBy} sortDir={page.sortDir}
        onSort={page.handleSort} filters={page.filters} onFilterChange={page.handleFilterChange} />
      <RoleFormPanel open={page.panelOpen} onClose={page.closePanel} editingRow={page.editingRow}
        saving={page.saving} onSave={page.handleSave} />
    </Box>
  );
}

function RoleFormPanel({ open, onClose, editingRow, saving, onSave }) {
  const isEdit = Boolean(editingRow);
  const { register, handleSubmit, reset, control, formState: { errors } } = useForm();

  useEffect(() => {
    if (!open) return;
    reset(isEdit
      ? { name: editingRow.name, description: editingRow.description, isActive: isRecordActive(editingRow) }
      : { name: '', description: '', isActive: true });
  }, [open, editingRow, isEdit, reset]);

  return (
    <SlidePanel open={open} onClose={onClose} title={isEdit ? 'Edit Role' : 'Add Role'} loading={saving}>
      <Box component="form" onSubmit={handleSubmit(onSave)} noValidate
        sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
        <FormControl fullWidth required error={!!errors.name}>
          <InputLabel>Role Name</InputLabel>
          <Controller name="name" control={control} rules={{ required: 'Role name is required' }}
            render={({ field }) => (
              <Select {...field} label="Role Name" disabled={isEdit}>
                {ROLE_NAMES.map((r) => <MenuItem key={r} value={r}>{r}</MenuItem>)}
              </Select>
            )} />
          {errors.name && <FormHelperText>{errors.name.message}</FormHelperText>}
        </FormControl>
        <TextField label="Description" multiline rows={3} {...register('description')} fullWidth />
        {isEdit && (
          <FormControlLabel control={<Switch {...register('isActive')} defaultChecked={isRecordActive(editingRow)} color="success" />} label="Active" />
        )}
        <Stack direction="row" spacing={1.5} sx={{ justifyContent: 'flex-end', mt: 1 }}>
          <Button variant="outlined" onClick={onClose} disabled={saving}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={saving} sx={{ background: BRAND.redGradient }}>
            {isEdit ? 'Save Changes' : 'Create Role'}
          </Button>
        </Stack>
      </Box>
    </SlidePanel>
  );
}
