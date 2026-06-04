import { useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Box, Button, Chip } from '@mui/material';
import { fetchAgreementGroups } from '../../store/slices/agreementSlice';
import { ROUTES } from '../../config/routes';
import { BRAND } from '../../config/theme';
import PageHeader from '../../components/ui/PageHeader';
import DataTable from '../../components/ui/DataTable';
import StatusBadge from '../../components/ui/StatusBadge';
import { useDataTable } from '../../hooks/useDataTable';
import { useAuth } from '../../hooks/useAuth';

const COLUMNS = [
  { field: 'agreementNumber', header: 'Agreement No.', sortable: true },
  { field: 'companyName', header: 'Company', sortable: true },
  { field: 'currentVersionNumber', header: 'Version', sortable: false, render: (v) => `V${v || 1}` },
  {
    field: 'currentStatus', header: 'Status', sortable: false,
    render: (v) => <StatusBadge status={v || 'DRAFT'} />,
  },
  { field: 'createdAt', header: 'Created', sortable: true, render: (v) => v ? new Date(v).toLocaleDateString('en-IN') : '—' },
];

export default function AgreementListPage() {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { isAdmin, isAccountManager } = useAuth();

  const { groups, totalElements, loading } = useSelector((s) => s.agreements);
  const { page, rowsPerPage, search, sortBy, sortDir, handlePageChange, handleRowsPerPageChange, handleSearch, handleSort } = useDataTable();

  const loadData = useCallback(() => {
    dispatch(fetchAgreementGroups({ page, size: rowsPerPage, search }));
  }, [dispatch, page, rowsPerPage, search]);

  useEffect(() => { loadData(); }, [loadData]);

  return (
    <Box>
      <PageHeader
        title="Agreements"
        subtitle="All commercial agreements across companies"
        actionLabel="New Agreement"
        onAction={(isAdmin || isAccountManager) ? () => navigate(ROUTES.AGREEMENT_CREATE) : undefined}
      />
      <DataTable
        columns={COLUMNS}
        rows={groups}
        loading={loading}
        totalCount={totalElements}
        page={page}
        rowsPerPage={rowsPerPage}
        onPageChange={handlePageChange}
        onRowsPerPageChange={handleRowsPerPageChange}
        onRowClick={(row) => navigate(`/agreements/groups/${row.id}`)}
        searchValue={search}
        onSearch={handleSearch}
        sortBy={sortBy}
        sortDir={sortDir}
        onSort={handleSort}
        emptyMessage="No agreements found. Create your first agreement."
      />
    </Box>
  );
}
