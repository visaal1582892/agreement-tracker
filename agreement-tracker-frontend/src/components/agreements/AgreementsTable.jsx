import { useNavigate } from 'react-router-dom';
import { Box, Chip, IconButton, Menu, MenuItem } from '@mui/material';
import { MoreVert } from '@mui/icons-material';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import { useSnackbar } from 'notistack';
import DataTable from '../ui/DataTable';
import StatusBadge from '../ui/StatusBadge';
import axiosInstance from '../../api/axiosInstance';
import { ENDPOINTS } from '../../config/endpoints';
import { submitAgreementForApproval } from '../../store/slices/agreementSlice';
import { useAgreementPermissions } from '../../hooks/useAgreementPermissions';
import { ROUTES } from '../../config/routes';
import { fetchAgreementForClone } from '../../utils/agreementClone';
import TransferOwnershipModal from './TransferOwnershipModal';
import ConfirmDialog from '../ui/ConfirmDialog';
import { useModal } from '../../hooks/useModal';

const STATUS_FILTER_OPTIONS = [
  { value: 'DRAFT', label: 'Draft' },
  { value: 'PENDING_APPROVAL', label: 'Pending' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
];

const formatDate = (d) =>
  d ? new Date(d).toLocaleDateString('en-US', { month: '2-digit', day: '2-digit', year: 'numeric' }) : '—';

function rowToAgreement(row) {
  return {
    ownerId: row.ownerUserId,
    ownerUserId: row.ownerUserId,
    approvalStatus: row.approvalStatus,
    id: row.latestVersionId,
    latestVersionId: row.latestVersionId,
  };
}

function RowActionsMenu({ row, navigate, onSubmit, onClone, onTransfer }) {
  const [anchor, setAnchor] = useState(null);
  const submitModal = useModal();
  const cloneModal = useModal();
  const { getListRowActions } = useAgreementPermissions();
  const actions = getListRowActions(rowToAgreement(row));

  if (!Object.values(actions).some(Boolean)) return null;

  const goToDetail = () => {
    setAnchor(null);
    navigate(`/agreements/groups/${row.id}`);
  };

  const goToEdit = () => {
    setAnchor(null);
    navigate(`/agreements/${row.latestVersionId}/edit`);
  };

  const handleSubmitClick = () => {
    setAnchor(null);
    submitModal.open();
  };

  const handleSubmitConfirm = async () => {
    submitModal.close();
    await onSubmit(row.latestVersionId);
  };

  const handleCloneClick = () => {
    setAnchor(null);
    cloneModal.open();
  };

  const handleCloneConfirm = () => {
    cloneModal.close();
    onClone(row.latestVersionId);
  };

  const handleTransferClick = () => {
    setAnchor(null);
    onTransfer(row);
  };

  return (
    <>
      <IconButton
        size="small"
        onClick={(e) => { e.stopPropagation(); setAnchor(e.currentTarget); }}
      >
        <MoreVert fontSize="small" />
      </IconButton>
      <Menu
        anchorEl={anchor}
        open={Boolean(anchor)}
        onClose={() => setAnchor(null)}
        onClick={(e) => e.stopPropagation()}
      >
        {actions.view && <MenuItem dense onClick={goToDetail}>View</MenuItem>}
        {actions.editDraft && <MenuItem dense onClick={goToEdit}>Edit Draft</MenuItem>}
        {actions.editApproved && <MenuItem dense onClick={goToEdit}>Edit</MenuItem>}
        {actions.revise && <MenuItem dense onClick={goToEdit}>Revise & Resubmit</MenuItem>}
        {actions.submit && <MenuItem dense onClick={handleSubmitClick}>Submit for Approval</MenuItem>}
        {actions.clone && <MenuItem dense onClick={handleCloneClick}>Clone (Copy Products)</MenuItem>}
        {actions.transfer && <MenuItem dense onClick={handleTransferClick}>Transfer Ownership</MenuItem>}
        {actions.approveReject && <MenuItem dense onClick={goToDetail}>Approve / Reject</MenuItem>}
      </Menu>
      <ConfirmDialog
        open={submitModal.isOpen}
        onClose={submitModal.close}
        onConfirm={handleSubmitConfirm}
        title="Submit for Approval"
        message="Are you sure you want to submit this agreement for approval? You will no longer be able to edit the details until it is reviewed."
        confirmLabel="Confirm"
      />
      <ConfirmDialog
        open={cloneModal.isOpen}
        onClose={cloneModal.close}
        onConfirm={handleCloneConfirm}
        title="Clone Agreement"
        message="This will copy the product scope into a new draft. Proceed?"
        confirmLabel="Proceed"
      />
    </>
  );
}

const buildColumns = ({ vendorOptions, incomeTypeOptions, onVendorSearch, onIncomeTypeSearch, loadingVendors, loadingIncomeTypes }) => [
  {
    field: 'agreementNumber',
    header: 'Agreement No.',
    sortable: true,
    filterType: 'text',
    filterKey: 'agreementNumber',
  },
  {
    field: 'agreementName',
    header: 'Agreement Name',
    sortable: false,
    filterType: 'text',
    filterKey: 'agreementName',
    render: (v) => v || '—',
  },
  {
    field: 'companyName',
    header: 'Company',
    sortable: true,
    filterType: 'text',
    filterKey: 'companyName',
  },
  {
    field: 'vendors',
    header: 'Vendors',
    sortable: false,
    filterType: 'searchable-select',
    filterKey: 'vendorId',
    filterOptions: vendorOptions,
    onFilterSearch: onVendorSearch,
    filterLoading: loadingVendors,
    getOptionLabel: (o) => o?.vendorName || '',
    render: (vendors) => {
      if (!vendors?.length) return '—';
      const [first, ...rest] = vendors;
      return (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, flexWrap: 'nowrap' }}>
          <span>{first.vendorName}</span>
          {rest.length > 0 && (
            <Chip label={`+${rest.length} more`} size="small" sx={{ fontSize: '0.7rem', height: 18 }} />
          )}
        </Box>
      );
    },
  },
  {
    field: 'incomeTypeName',
    header: 'Income Type',
    sortable: false,
    filterType: 'searchable-select',
    filterKey: 'incomeTypeId',
    filterOptions: incomeTypeOptions,
    onFilterSearch: onIncomeTypeSearch,
    filterLoading: loadingIncomeTypes,
    getOptionLabel: (o) => o?.name || '',
    render: (v) => v || '—',
  },
  {
    field: 'startDate',
    header: 'Validity',
    sortable: false,
    render: (_, row) => {
      if (!row.startDate && !row.expiryDate) return '—';
      return `${formatDate(row.startDate)} – ${formatDate(row.expiryDate)}`;
    },
  },
  {
    field: 'currentVersionNumber',
    header: 'Version',
    sortable: false,
    render: (v) => `V${v || 1}`,
  },
  {
    field: 'computedStatus',
    header: 'Status',
    sortable: false,
    filterType: 'select',
    filterKey: 'status',
    filterOptions: STATUS_FILTER_OPTIONS,
    render: (v) => <StatusBadge status={v || 'DRAFT'} />,
  },
  {
    field: 'ownerName',
    header: 'Owner',
    sortable: false,
    filterType: 'text',
    filterKey: 'ownerName',
    render: (v) => v || '—',
  },
];

export default function AgreementsTable({
  rows,
  loading,
  totalCount,
  page,
  rowsPerPage,
  onPageChange,
  onRowsPerPageChange,
  onRowClick,
  sortBy,
  sortDir,
  onSort,
  filters = {},
  onFilterChange,
  emptyMessage = 'No agreements found.',
  hasRight,
  onRefresh,
}) {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { enqueueSnackbar } = useSnackbar();

  const [vendorOptions, setVendorOptions] = useState([]);
  const [incomeTypeOptions, setIncomeTypeOptions] = useState([]);
  const [loadingVendors, setLoadingVendors] = useState(false);
  const [loadingIncomeTypes, setLoadingIncomeTypes] = useState(false);
  const [transferRow, setTransferRow] = useState(null);

  const searchVendors = useCallback(async (query) => {
    setLoadingVendors(true);
    try {
      const { data } = await axiosInstance.get(ENDPOINTS.VENDORS, {
        params: query?.trim() ? { search: query.trim() } : {},
      });
      setVendorOptions(Array.isArray(data) ? data : []);
    } catch {
      setVendorOptions([]);
    } finally {
      setLoadingVendors(false);
    }
  }, []);

  const searchIncomeTypes = useCallback(async (query) => {
    setLoadingIncomeTypes(true);
    try {
      const { data } = await axiosInstance.get(ENDPOINTS.INCOME_TYPES);
      const all = Array.isArray(data) ? data : [];
      const q = query?.trim().toLowerCase();
      setIncomeTypeOptions(q ? all.filter((t) => t.name?.toLowerCase().includes(q)) : all);
    } catch {
      setIncomeTypeOptions([]);
    } finally {
      setLoadingIncomeTypes(false);
    }
  }, []);

  useEffect(() => {
    searchVendors('');
    searchIncomeTypes('');
  }, [searchVendors, searchIncomeTypes]);

  const handleSubmit = useCallback(async (agreementId) => {
    try {
      await dispatch(submitAgreementForApproval(agreementId)).unwrap();
      enqueueSnackbar('Submitted for approval', { variant: 'success' });
      onRefresh?.();
    } catch (err) {
      enqueueSnackbar(typeof err === 'string' ? err : 'Submit failed', { variant: 'error' });
    }
  }, [dispatch, enqueueSnackbar, onRefresh]);

  const handleClone = useCallback(async (agreementId) => {
    try {
      const clonedData = await fetchAgreementForClone(axiosInstance, ENDPOINTS, agreementId);
      navigate(ROUTES.AGREEMENT_CREATE, { state: { clonedData } });
    } catch {
      enqueueSnackbar('Failed to prepare clone data', { variant: 'error' });
    }
  }, [navigate, enqueueSnackbar]);

  const columns = useMemo(
    () => buildColumns({
      vendorOptions,
      incomeTypeOptions,
      onVendorSearch: searchVendors,
      onIncomeTypeSearch: searchIncomeTypes,
      loadingVendors,
      loadingIncomeTypes,
    }),
    [vendorOptions, incomeTypeOptions, searchVendors, searchIncomeTypes, loadingVendors, loadingIncomeTypes],
  );

  const columnsWithActions = [
    ...columns,
    {
      field: 'actions',
      header: '',
      sortable: false,
      render: (_, row) => (
        <RowActionsMenu
          row={row}
          navigate={navigate}
          onSubmit={handleSubmit}
          onClone={handleClone}
          onTransfer={setTransferRow}
        />
      ),
    },
  ];

  return (
    <>
    <DataTable
      columns={columnsWithActions}
      rows={rows}
      loading={loading}
      totalCount={totalCount}
      page={page}
      rowsPerPage={rowsPerPage}
      onPageChange={onPageChange}
      onRowsPerPageChange={onRowsPerPageChange}
      onRowClick={onRowClick}
      sortBy={sortBy}
      sortDir={sortDir}
      onSort={onSort}
      filters={filters}
      onFilterChange={onFilterChange}
      emptyMessage={emptyMessage}
    />
    <TransferOwnershipModal
      open={Boolean(transferRow)}
      onClose={() => setTransferRow(null)}
      agreementId={transferRow?.latestVersionId}
      agreementLabel={transferRow?.agreementNumber}
      onSuccess={() => {
        enqueueSnackbar('Ownership transferred', { variant: 'success' });
        setTransferRow(null);
        onRefresh?.();
      }}
    />
    </>
  );
}
