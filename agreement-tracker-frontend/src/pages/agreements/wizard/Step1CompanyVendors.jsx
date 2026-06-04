import { useEffect, useState } from 'react';
import { Box, Typography, Grid, Divider, Alert } from '@mui/material';
import axiosInstance from '../../../api/axiosInstance';
import { ENDPOINTS } from '../../../config/endpoints';
import SearchableSelect from '../../../components/forms/SearchableSelect';
import BulkVendorInput from '../../../components/forms/BulkVendorInput';

export default function Step1CompanyVendors({ state, updateFields }) {
  const [companies, setCompanies] = useState([]);
  const [vendors, setVendors] = useState([]);
  const [selectedCompany, setSelectedCompany] = useState(null);
  const [selectedVendors, setSelectedVendors] = useState([]);
  const [loadingCompanies, setLoadingCompanies] = useState(false);
  const [loadingVendors, setLoadingVendors] = useState(false);
  const [fetchError, setFetchError] = useState(null);

  useEffect(() => {
    setLoadingCompanies(true);
    axiosInstance.get(ENDPOINTS.COMPANIES)
      .then(({ data }) => setCompanies(Array.isArray(data) ? data : []))
      .catch((err) => {
        console.error('Failed to load companies:', err);
        setFetchError('Failed to load companies. Check API connection.');
      })
      .finally(() => setLoadingCompanies(false));

    setLoadingVendors(true);
    axiosInstance.get(ENDPOINTS.VENDORS)
      .then(({ data }) => setVendors(Array.isArray(data) ? data : []))
      .catch((err) => {
        console.error('Failed to load vendors:', err);
        setFetchError('Failed to load vendors. Check API connection.');
      })
      .finally(() => setLoadingVendors(false));
  }, []);

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
      <Typography variant="h6" fontWeight={600} mb={0.5}>Company & Vendor Setup</Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        Select the company and vendors associated with this agreement.
      </Typography>

      {fetchError && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setFetchError(null)}>{fetchError}</Alert>}

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Typography variant="subtitle2" gutterBottom>Company *</Typography>
          <SearchableSelect
            label="Search & select company"
            options={companies}
            value={selectedCompany}
            onChange={handleCompanyChange}
            getOptionLabel={(o) => o.companyName || ''}
            isOptionEqualToValue={(o, v) => o.id === v.id}
            loading={loadingCompanies}
            required
          />
        </Grid>

        <Grid size={12}>
          <Divider sx={{ my: 1 }} />
          <Typography variant="subtitle2" gutterBottom>Vendors *</Typography>
          <Box sx={{ mb: 2 }}>
            <SearchableSelect
              label="Search vendors by name or code"
              options={vendors.filter((v) => !selectedVendors.find((s) => s.id === v.id))}
              value={null}
              onChange={(v) => v && handleVendorChange([...selectedVendors, v])}
              getOptionLabel={(o) => `${o.vendorCode} — ${o.vendorName}`}
              isOptionEqualToValue={(o, v) => o.id === v.id}
              loading={loadingVendors}
            />
          </Box>
          <BulkVendorInput selectedVendors={selectedVendors} onChange={handleVendorChange} />
        </Grid>
      </Grid>
    </Box>
  );
}
