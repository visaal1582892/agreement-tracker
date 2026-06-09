import { useEffect, useCallback, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Box, Tab, Tabs } from '@mui/material';
import { fetchAgreementGroups } from '../../store/slices/agreementSlice';
import { ROUTES } from '../../config/routes';
import { navigateToAgreement } from '../../utils/agreementNavigation';
import { BRAND } from '../../config/theme';
import PageHeader from '../../components/ui/PageHeader';
import AgreementsTable from '../../components/agreements/AgreementsTable';
import { useDataTable } from '../../hooks/useDataTable';
import { useDebounce } from '../../hooks/useDebounce';
import { useAuth } from '../../hooks/useAuth';
import { RIGHTS } from '../../config/rights';

const FETCH_MODE = { MY: 'MY', ALL: 'ALL' };

const EMPTY_FILTERS = {};

export default function AgreementListPage() {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { hasRight } = useAuth();

  const canViewMy = hasRight(RIGHTS.AGREEMENT_VIEW);
  const canViewAll = hasRight(RIGHTS.AGREEMENT_VIEW_ALL);
  const showTabs = canViewMy && canViewAll;
  const defaultMode = canViewMy ? FETCH_MODE.MY : FETCH_MODE.ALL;

  const [fetchMode, setFetchMode] = useState(defaultMode);
  const [filters, setFilters] = useState(EMPTY_FILTERS);

  const { groups, totalElements, loading } = useSelector((s) => s.agreements);
  const {
    page, rowsPerPage, sortBy, sortDir,
    handlePageChange, handleRowsPerPageChange, handleSort,
  } = useDataTable();

  const debouncedFilters = useDebounce(filters, 600);
  const activeMode = showTabs ? fetchMode : defaultMode;

  const pageMeta = useMemo(() => {
    if (activeMode === FETCH_MODE.MY) {
      return {
        title: 'My Agreements',
        subtitle: 'Agreements you own and manage',
        emptyMessage: 'No agreements found. Create your first agreement.',
      };
    }
    return {
      title: 'All Agreements',
      subtitle: 'All commercial agreements across companies',
      emptyMessage: 'No agreements found.',
    };
  }, [activeMode]);

  const loadData = useCallback(() => {
    const { agreementNumber, companyName, status, ownerName, vendorId, incomeTypeId } = debouncedFilters;
    dispatch(fetchAgreementGroups({
      page,
      size: rowsPerPage,
      scope: activeMode,
      sortBy,
      sortDir,
      ...(agreementNumber && { agreementNumber }),
      ...(companyName && { companyName }),
      ...(status && { status }),
      ...(ownerName && { ownerName }),
      ...(vendorId && { vendorId }),
      ...(incomeTypeId && { incomeTypeId }),
    }));
  }, [dispatch, page, rowsPerPage, sortBy, sortDir, activeMode, debouncedFilters]);

  useEffect(() => { loadData(); }, [loadData]);

  const handleTabChange = (_, value) => {
    setFetchMode(value);
    handlePageChange(null, 0);
  };

  const handleFilterChange = useCallback((key, value) => {
    setFilters((prev) => {
      const next = { ...prev };
      if (value) {
        next[key] = value;
      } else {
        delete next[key];
      }
      return next;
    });
    handlePageChange(null, 0);
  }, [handlePageChange]);

  return (
    <Box>
      {showTabs && (
        <Tabs
          value={fetchMode}
          onChange={handleTabChange}
          sx={{
            mb: 2,
            minHeight: 40,
            '& .MuiTab-root': { minHeight: 40, textTransform: 'none', fontWeight: 600, fontSize: '0.875rem' },
            '& .Mui-selected': { color: BRAND.red },
            '& .MuiTabs-indicator': { bgcolor: BRAND.red },
          }}
        >
          <Tab label="My Agreements" value={FETCH_MODE.MY} />
          <Tab label="All Agreements" value={FETCH_MODE.ALL} />
        </Tabs>
      )}

      <PageHeader
        title={pageMeta.title}
        subtitle={pageMeta.subtitle}
        actionLabel="New Agreement"
        onAction={hasRight(RIGHTS.AGREEMENT_CREATE) ? () => navigate(ROUTES.AGREEMENT_CREATE) : undefined}
      />

      <AgreementsTable
        rows={groups}
        loading={loading}
        totalCount={totalElements}
        page={page}
        rowsPerPage={rowsPerPage}
        onPageChange={handlePageChange}
        onRowsPerPageChange={handleRowsPerPageChange}
        onRowClick={(row) => navigateToAgreement(row, navigate)}
        sortBy={sortBy}
        sortDir={sortDir}
        onSort={handleSort}
        filters={filters}
        onFilterChange={handleFilterChange}
        emptyMessage={pageMeta.emptyMessage}
        hasRight={hasRight}
        onRefresh={loadData}
      />
    </Box>
  );
}
