import { useCallback, useEffect, useState } from 'react';
import { Box, Typography, Grid, Alert, TextField } from '@mui/material';
import { BRAND } from '../../../config/theme';
import axiosInstance from '../../../api/axiosInstance';
import { ENDPOINTS } from '../../../config/endpoints';
import SearchableSelect from '../../../components/forms/SearchableSelect';
import BulkVendorInput from '../../../components/forms/BulkVendorInput';

export default function Step1CompanyVendors({ state, updateFields, groupFieldsLocked = false }) {
  const [companyOptions, setCompanyOptions] = useState([]);
  const [groupOptions, setGroupOptions] = useState([]);
  const [vendorOptions, setVendorOptions] = useState([]);
  const [selectedCompany, setSelectedCompany] = useState(null);
  const [selectedGroup, setSelectedGroup] = useState(null);
  const [selectedVendors, setSelectedVendors] = useState([]);
  const [loadingCompanies, setLoadingCompanies] = useState(false);
  const [loadingGroups, setLoadingGroups] = useState(false);
  const [loadingVendors, setLoadingVendors] = useState(false);
  const [fetchError, setFetchError] = useState(null);
  const [createNewGroup, setCreateNewGroup] = useState(false);

  const searchCompanies = useCallback(async (query) => {
    setLoadingCompanies(true);
    try {
      const { data } = await axiosInstance.get(ENDPOINTS.COMPANIES, {
        params: query?.trim() ? { search: query.trim() } : {},
      });
      setCompanyOptions(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to search companies:', err);
      setFetchError('Failed to load companies. Check API connection.');
      setCompanyOptions([]);
    } finally {
      setLoadingCompanies(false);
    }
  }, []);

  const loadGroups = useCallback(async (companyId) => {
    if (!companyId) {
      setGroupOptions((prev) => (prev.length === 0 ? prev : []));
      return;
    }
    setLoadingGroups(true);
    try {
      const { data } = await axiosInstance.get(ENDPOINTS.COMPANY_AGREEMENT_GROUPS(companyId));
      setGroupOptions(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to load company agreement groups:', err);
      setGroupOptions([]);
    } finally {
      setLoadingGroups(false);
    }
  }, []);

  const searchVendors = useCallback(async (query) => {
    setLoadingVendors(true);
    try {
      const { data } = await axiosInstance.get(ENDPOINTS.VENDORS, {
        params: query?.trim() ? { search: query.trim() } : {},
      });
      setVendorOptions(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to search vendors:', err);
      setFetchError('Failed to load vendors. Check API connection.');
      setVendorOptions([]);
    } finally {
      setLoadingVendors(false);
    }
  }, []);

  const handleGroupSearch = useCallback(() => {
    loadGroups(state.companyId);
  }, [loadGroups, state.companyId]);

  useEffect(() => {
    if (state.companyId && state.companyName) {
      setSelectedCompany({ id: state.companyId, companyName: state.companyName });
    }
  }, [state.companyId, state.companyName]);

  useEffect(() => {
    if (state.companyId) {
      loadGroups(state.companyId);
    }
  }, [state.companyId, loadGroups]);

  useEffect(() => {
    if (state.newCompanyAgreementGroupName?.trim()) {
      setCreateNewGroup(true);
      setSelectedGroup(null);
      return;
    }
    if (state.companyAgreementGroupId && state.companyAgreementGroupName) {
      setCreateNewGroup(false);
      setSelectedGroup({ id: state.companyAgreementGroupId, name: state.companyAgreementGroupName });
    }
  }, [
    state.companyAgreementGroupId,
    state.companyAgreementGroupName,
    state.newCompanyAgreementGroupName,
  ]);

  useEffect(() => {
    if (state.vendorIds?.length > 0 && selectedVendors.length === 0) {
      axiosInstance
        .get(ENDPOINTS.VENDORS, { params: { ids: state.vendorIds.join(',') } })
        .then(({ data }) => {
          const resolved = Array.isArray(data)
            ? data.filter((v) => state.vendorIds.includes(v.id))
            : [];
          if (resolved.length) setSelectedVendors(resolved);
        })
        .catch((err) => console.error('Failed to hydrate vendors:', err));
    }
  }, [state.vendorIds, selectedVendors.length]);

  const handleCompanyChange = (company) => {
    setSelectedCompany(company);
    setSelectedGroup(null);
    setCreateNewGroup(false);
    updateFields({
      companyId: company?.id || null,
      companyName: company?.companyName || '',
      companyAgreementGroupId: null,
      companyAgreementGroupName: '',
      newCompanyAgreementGroupName: '',
    });
  };

  const handleGroupChange = (group) => {
    setSelectedGroup(group);
    setCreateNewGroup(false);
    updateFields({
      companyAgreementGroupId: group?.id || null,
      companyAgreementGroupName: group?.name || '',
      newCompanyAgreementGroupName: '',
    });
  };

  const handleNewGroupNameChange = (e) => {
    const value = e.target.value;
    setCreateNewGroup(true);
    setSelectedGroup(null);
    updateFields({
      companyAgreementGroupId: null,
      companyAgreementGroupName: '',
      newCompanyAgreementGroupName: value,
    });
  };

  const handleVendorChange = (selected) => {
    setSelectedVendors(selected);
    updateFields({ vendorIds: selected.map((v) => v.id) });
  };

  return (
    <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      <Typography sx={{ fontSize: '1rem', fontWeight: 700, color: BRAND.textPrimary, mb: 0.5 }}>
        Company & Vendor Setup
      </Typography>
      <Typography sx={{ fontSize: '0.875rem', color: '#64748B', mb: 3 }}>
        Select company, agreement group, and vendors for this agreement.
      </Typography>

      {fetchError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setFetchError(null)}>
          {fetchError}
        </Alert>
      )}

      {groupFieldsLocked && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Company and agreement group are locked after the draft is saved. To use a different company or group,
          deactivate this group and create a new one.
        </Alert>
      )}

      <Grid container spacing={3} sx={{ flex: 1 }}>
        <Grid size={{ xs: 12, md: 6 }}>
          <SearchableSelect
            label="Company"
            placeholder="Search company name…"
            isMulti={false}
            options={companyOptions}
            value={selectedCompany}
            onChange={handleCompanyChange}
            onSearch={searchCompanies}
            getOptionLabel={(o) => o.companyName || ''}
            isOptionEqualToValue={(o, v) => o.id === v.id}
            loading={loadingCompanies}
            disabled={groupFieldsLocked}
            required
          />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <SearchableSelect
            label="Company Agreement Group"
            placeholder={state.companyId ? 'Select existing group…' : 'Select company first'}
            isMulti={false}
            options={groupOptions}
            value={createNewGroup ? null : selectedGroup}
            onChange={handleGroupChange}
            onSearch={handleGroupSearch}
            getOptionLabel={(o) => o.name || ''}
            isOptionEqualToValue={(o, v) => o.id === v.id}
            loading={loadingGroups}
            disabled={groupFieldsLocked || !state.companyId}
            required={!createNewGroup}
          />
          {!groupFieldsLocked && (
            <>
              <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
                Or create a new group
              </Typography>
              <TextField
                label="New group name"
                value={state.newCompanyAgreementGroupName || ''}
                onChange={handleNewGroupNameChange}
                fullWidth
                size="small"
                disabled={!state.companyId}
                sx={{ mt: 1 }}
                slotProps={{ htmlInput: { maxLength: 255 } }}
              />
            </>
          )}
        </Grid>

        <Grid size={{ xs: 12 }}>
          <SearchableSelect
            label="Vendors"
            placeholder="Search vendors by name or code…"
            isMulti
            options={vendorOptions}
            value={selectedVendors}
            onChange={handleVendorChange}
            onSearch={searchVendors}
            getOptionLabel={(o) => `${o.vendorCode} — ${o.vendorName}`}
            isOptionEqualToValue={(o, v) => o.id === v.id}
            loading={loadingVendors}
            maxVisibleChips={2}
            required
          />
          <BulkVendorInput selectedVendors={selectedVendors} onChange={handleVendorChange} />
        </Grid>
      </Grid>
    </Box>
  );
}
