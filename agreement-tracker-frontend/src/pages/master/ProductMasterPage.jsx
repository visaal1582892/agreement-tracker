import { useEffect, useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import {
  Box, Button, TextField, Chip, Stack, Typography, FormControlLabel, Switch, alpha,
  MenuItem, Select, FormControl, InputLabel, FormHelperText,
} from '@mui/material';
import DataTable from '../../components/ui/DataTable';
import SlidePanel from '../../components/ui/SlidePanel';
import { MasterAddButton, MasterRowActions } from '../../components/master/MasterCrudActions';
import { productApi, manufacturerApi, divisionApi } from '../../api/masterApi';
import { useMasterPage } from '../../hooks/useMasterPage';
import { BRAND } from '../../config/theme';
import { isRecordActive } from '../../utils/masterUtils';

const STATUS_OPTS = [{ value: 'true', label: 'Active' }, { value: 'false', label: 'Inactive' }];

const COLUMNS = [
  { field: 'id',               header: '#',               width: 60,  sortable: true },
  { field: 'productCode',      header: 'Code',            width: 120, sortable: true, filterType: 'text' },
  { field: 'productName',      header: 'Product Name',                sortable: true, filterType: 'text' },
  { field: 'manufacturerName', header: 'Manufacturer',                sortable: true, filterType: 'text', filterKey: 'manufacturerName' },
  { field: 'divisionName',     header: 'Division',                    sortable: true, filterType: 'text', filterKey: 'divisionName' },
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

export default function ProductMasterPage() {
  const page = useMasterPage({ api: productApi, entityLabel: 'Product' });
  const [manufacturers, setManufacturers] = useState([]);
  const [allDivisions, setAllDivisions]   = useState([]);

  useEffect(() => {
    manufacturerApi.list().then(setManufacturers).catch(() => {});
    divisionApi.list().then(setAllDivisions).catch(() => {});
  }, []);

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
          <Typography variant="h6" sx={{ fontWeight: 700 }}>Products</Typography>
          <Typography variant="body2" color="text.secondary">{page.totalCount} record{page.totalCount !== 1 ? 's' : ''}</Typography>
        </Box>
        <MasterAddButton label="Add Product" onClick={page.openCreate} />
      </Stack>
      <DataTable columns={COLUMNS} rows={enrichedRows} loading={page.loading} totalCount={page.totalCount}
        page={page.page} rowsPerPage={page.rowsPerPage} onPageChange={page.handlePageChange}
        onRowsPerPageChange={page.handleRowsPerPageChange} sortBy={page.sortBy} sortDir={page.sortDir}
        onSort={page.handleSort} filters={page.filters} onFilterChange={page.handleFilterChange} />
      <ProductFormPanel open={page.panelOpen} onClose={page.closePanel} editingRow={page.editingRow}
        saving={page.saving} onSave={page.handleSave} manufacturers={manufacturers} allDivisions={allDivisions} />
    </Box>
  );
}

function ProductFormPanel({ open, onClose, editingRow, saving, onSave, manufacturers, allDivisions }) {
  const isEdit = Boolean(editingRow);
  const { register, handleSubmit, reset, control, watch, formState: { errors } } = useForm();
  const selectedMfrId = watch('manufacturerId');
  const filteredDivisions = selectedMfrId
    ? allDivisions.filter((d) => d.manufacturer?.id === Number(selectedMfrId))
    : allDivisions;

  useEffect(() => {
    if (!open) return;
    reset(isEdit
      ? {
          productCode: editingRow.productCode,
          productName: editingRow.productName,
          manufacturerId: editingRow.manufacturerId,
          divisionId: editingRow.divisionId,
          isActive: isRecordActive(editingRow),
        }
      : { productCode: '', productName: '', manufacturerId: '', divisionId: '', isActive: true });
  }, [open, editingRow, isEdit, reset]);

  return (
    <SlidePanel open={open} onClose={onClose} title={isEdit ? 'Edit Product' : 'Add Product'} loading={saving}>
      <Box component="form" onSubmit={handleSubmit(onSave)} noValidate
        sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
        <TextField label="Product Code" {...register('productCode')} fullWidth />
        <TextField label="Product Name" required error={!!errors.productName}
          helperText={errors.productName?.message}
          {...register('productName', { required: 'Product name is required' })} fullWidth />
        <FormControl fullWidth required error={!!errors.manufacturerId}>
          <InputLabel>Manufacturer</InputLabel>
          <Controller name="manufacturerId" control={control} rules={{ required: 'Manufacturer is required' }}
            render={({ field }) => (
              <Select {...field} label="Manufacturer">
                {manufacturers.map((m) => <MenuItem key={m.id} value={m.id}>{m.manufacturerName}</MenuItem>)}
              </Select>
            )} />
          {errors.manufacturerId && <FormHelperText>{errors.manufacturerId.message}</FormHelperText>}
        </FormControl>
        <FormControl fullWidth required error={!!errors.divisionId}>
          <InputLabel>Division</InputLabel>
          <Controller name="divisionId" control={control} rules={{ required: 'Division is required' }}
            render={({ field }) => (
              <Select {...field} label="Division">
                {filteredDivisions.map((d) => <MenuItem key={d.id} value={d.id}>{d.divisionName}</MenuItem>)}
              </Select>
            )} />
          {errors.divisionId && <FormHelperText>{errors.divisionId.message}</FormHelperText>}
        </FormControl>
        {isEdit && (
          <FormControlLabel control={<Switch {...register('isActive')} defaultChecked={isRecordActive(editingRow)} color="success" />} label="Active" />
        )}
        <Stack direction="row" spacing={1.5} sx={{ justifyContent: 'flex-end', mt: 1 }}>
          <Button variant="outlined" onClick={onClose} disabled={saving}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={saving} sx={{ background: BRAND.redGradient }}>
            {isEdit ? 'Save Changes' : 'Create Product'}
          </Button>
        </Stack>
      </Box>
    </SlidePanel>
  );
}
