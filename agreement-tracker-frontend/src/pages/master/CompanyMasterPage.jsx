import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import {
  Box, Button, TextField, Chip, Stack, Typography,
  FormControlLabel, Switch, alpha,
} from '@mui/material';
import { Add, Edit, PowerSettingsNew } from '@mui/icons-material';
import DataTable from '../../components/ui/DataTable';
import SlidePanel from '../../components/ui/SlidePanel';
import { companyApi } from '../../api/masterApi';
import { useMasterPage } from '../../hooks/useMasterPage';
import { BRAND } from '../../config/theme';
import { isRecordActive } from '../../utils/masterUtils';

const COLUMNS = [
  { field: 'id',          header: '#',            width: 60,  sortable: true, filterType: null },
  { field: 'companyName', header: 'Company Name',             sortable: true, filterType: 'text' },
  {
    field: 'isActive',
    header: 'Status',
    width: 110,
    sortable: true,
    filterType: 'select',
    filterOptions: [
      { value: 'true',  label: 'Active' },
      { value: 'false', label: 'Inactive' },
    ],
    render: (_, row) => (
      <Chip
        label={isRecordActive(row) ? 'Active' : 'Inactive'}
        size="small"
        sx={{
          bgcolor: isRecordActive(row) ? alpha(BRAND.green, 0.12) : alpha('#EF4444', 0.10),
          color: isRecordActive(row) ? BRAND.greenDark : '#DC2626',
          fontWeight: 600,
          fontSize: '0.72rem',
        }}
      />
    ),
  },
  { field: '_actions', header: 'Actions', width: 120, sortable: false, filterType: null },
];

export default function CompanyMasterPage() {
  const page = useMasterPage({ api: companyApi, entityLabel: 'Company' });

  const enrichedRows = page.rows.map((row) => ({
    ...row,
    _actions: (
      <Stack direction="row" spacing={0.5}>
        <Button
          size="small"
          variant="outlined"
          startIcon={<Edit sx={{ fontSize: 14 }} />}
          onClick={(e) => { e.stopPropagation(); page.openEdit(row); }}
          sx={{ fontSize: '0.72rem', py: 0.3, px: 1 }}
        >
          Edit
        </Button>
        <Button
          size="small"
          variant="outlined"
          color={isRecordActive(row) ? 'error' : 'success'}
          startIcon={<PowerSettingsNew sx={{ fontSize: 14 }} />}
          onClick={(e) => { e.stopPropagation(); page.handleToggleStatus(row); }}
          sx={{ fontSize: '0.72rem', py: 0.3, px: 1 }}
        >
          {isRecordActive(row) ? 'Deactivate' : 'Activate'}
        </Button>
      </Stack>
    ),
  }));

  return (
    <Box>
      {/* Toolbar */}
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Box>
          <Typography variant="h6" sx={{ fontWeight: 700 }}>Companies</Typography>
          <Typography variant="body2" color="text.secondary">
            {page.totalCount} record{page.totalCount !== 1 ? 's' : ''}
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={page.openCreate}
          sx={{ background: BRAND.redGradient }}
        >
          Add Company
        </Button>
      </Stack>

      <DataTable
        columns={COLUMNS}
        rows={enrichedRows}
        loading={page.loading}
        totalCount={page.totalCount}
        page={page.page}
        rowsPerPage={page.rowsPerPage}
        onPageChange={page.handlePageChange}
        onRowsPerPageChange={page.handleRowsPerPageChange}
        sortBy={page.sortBy}
        sortDir={page.sortDir}
        onSort={page.handleSort}
        filters={page.filters}
        onFilterChange={page.handleFilterChange}
      />

      <CompanyFormPanel
        open={page.panelOpen}
        onClose={page.closePanel}
        editingRow={page.editingRow}
        saving={page.saving}
        onSave={page.handleSave}
      />
    </Box>
  );
}

function CompanyFormPanel({ open, onClose, editingRow, saving, onSave }) {
  const isEdit = Boolean(editingRow);
  const { register, handleSubmit, reset, formState: { errors } } = useForm();

  useEffect(() => {
    if (!open) return;
    reset(isEdit
      ? { companyName: editingRow.companyName, isActive: isRecordActive(editingRow) }
      : { companyName: '', isActive: true });
  }, [open, editingRow, isEdit, reset]);

  return (
    <SlidePanel
      open={open}
      onClose={onClose}
      title={isEdit ? 'Edit Company' : 'Add Company'}
      loading={saving}
    >
      <Box
        component="form"
        onSubmit={handleSubmit(onSave)}
        noValidate
        sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}
      >
        <TextField
          label="Company Name"
          required
          error={!!errors.companyName}
          helperText={errors.companyName?.message}
          {...register('companyName', { required: 'Company name is required' })}
          fullWidth
        />

        {isEdit && (
          <FormControlLabel
            control={
              <Switch
                {...register('isActive')}
                defaultChecked={isRecordActive(editingRow)}
                color="success"
              />
            }
            label="Active"
          />
        )}

        <Stack direction="row" spacing={1.5} sx={{ justifyContent: 'flex-end', mt: 1 }}>
          <Button variant="outlined" onClick={onClose} disabled={saving}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={saving}
            sx={{ background: BRAND.redGradient }}
          >
            {isEdit ? 'Save Changes' : 'Create Company'}
          </Button>
        </Stack>
      </Box>
    </SlidePanel>
  );
}
