import { useCallback, useEffect, useState } from 'react';
import { Box, Typography, Grid, Divider, Alert } from '@mui/material';
import axiosInstance from '../../../api/axiosInstance';
import { ENDPOINTS } from '../../../config/endpoints';
import SearchableSelect from '../../../components/forms/SearchableSelect';
import BulkVendorInput from '../../../components/forms/BulkVendorInput';

export default function Step1CompanyVendors({ state, updateFields }) {
  const [companyOptions, setCompanyOptions] = useState([]);
  const [vendorOptions, setVendorOptions] = useState([]);
  const [selectedCompany, setSelectedCompany] = useState(null);
  const [selectedVendors, setSelectedVendors] = useState([]);
  const [loadingCompanies, setLoadingCompanies] = useState(false);
  const [loadingVendors, setLoadingVendors] = useState(false);
  const [fetchError, setFetchError] = useState(null);

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

  useEffect(() => {
    if (state.companyId && state.companyName) {
      setSelectedCompany({ id: state.companyId, companyName: state.companyName });
    }
  }, [state.companyId, state.companyName]);

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
    updateFields({ companyId: company?.id || null, companyName: company?.companyName || '' });
  };

  const handleVendorChange = (selected) => {
    setSelectedVendors(selected);
    updateFields({ vendorIds: selected.map((v) => v.id) });
  };

  return (
    <Box>
      <Typography variant="h6" fontWeight={600} mb={0.5}>
        Company & Vendor Setup
      </Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        Select the company and vendors associated with this agreement.
      </Typography>

      {fetchError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setFetchError(null)}>
          {fetchError}
        </Alert>
      )}

      <Grid container spacing={3}>
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
            required
          />
        </Grid>

        <Grid size={12}>
          <Divider sx={{ my: 1 }} />
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
          <Box sx={{ mt: 2 }}>
            <BulkVendorInput selectedVendors={selectedVendors} onChange={handleVendorChange} />
          </Box>
        </Grid>
      </Grid>
    </Box>
  );
}
