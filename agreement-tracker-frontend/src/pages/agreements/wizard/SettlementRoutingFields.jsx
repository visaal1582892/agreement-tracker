import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Box, Typography, Grid, TextField, RadioGroup, FormControlLabel, Radio, Divider,
} from '@mui/material';
import axiosInstance from '../../../api/axiosInstance';
import { ENDPOINTS } from '../../../config/endpoints';
import SearchableSelect from '../../../components/forms/SearchableSelect';
import { isAssetRentalIncomeType } from '../../../utils/incomeTypeUtils';
import {
  CALCULATION_BASIS,
  CALCULATION_BASIS_LABELS,
  INVOICE_ROUTING_MODE,
} from '../../../constants/calculationBasis';
import {
  PAYMENT_REALIZATION_OPTIONS,
  PAYMENT_REALIZATION_TYPE,
} from '../../../constants/commercialStructure';

function resolveInitialRoutingMode({ isAssetRental, invoiceVendorId, vendorIds }) {
  if (isAssetRental) {
    return INVOICE_ROUTING_MODE.OTHER_VENDOR;
  }
  if (invoiceVendorId && vendorIds.includes(invoiceVendorId)) {
    return INVOICE_ROUTING_MODE.SUPPLY_VENDOR;
  }
  if (invoiceVendorId) {
    return INVOICE_ROUTING_MODE.OTHER_VENDOR;
  }
  return INVOICE_ROUTING_MODE.SUPPLY_VENDOR;
}

export default function SettlementRoutingFields({
  vendorIds = [],
  invoiceVendorId,
  payoutBufferDays,
  calculationBasis,
  paymentRealizationType,
  incomeTypeId,
  incomeTypeName,
  onUpdateDetails,
}) {
  const isAssetRental = isAssetRentalIncomeType([], incomeTypeId, incomeTypeName);
  const [supplyVendorOptions, setSupplyVendorOptions] = useState([]);
  const [searchVendorOptions, setSearchVendorOptions] = useState([]);
  const [selectedInvoiceVendor, setSelectedInvoiceVendor] = useState(null);
  const [loadingSupplyVendors, setLoadingSupplyVendors] = useState(false);
  const [loadingSearchVendors, setLoadingSearchVendors] = useState(false);
  const [invoiceRoutingMode, setInvoiceRoutingMode] = useState(
    resolveInitialRoutingMode({ isAssetRental, invoiceVendorId, vendorIds }),
  );

  const activePaymentRealization = paymentRealizationType || PAYMENT_REALIZATION_TYPE.DIRECT_PAYMENT_INVOICE;
  const showInvoiceVendor = activePaymentRealization === PAYMENT_REALIZATION_TYPE.DIRECT_PAYMENT_INVOICE;
  const activeBasis = calculationBasis || CALCULATION_BASIS.VENDOR_INVOICE;
  const useManualVendorSearch = isAssetRental || invoiceRoutingMode === INVOICE_ROUTING_MODE.OTHER_VENDOR;

  const loadSupplyVendors = useCallback(async () => {
    if (!vendorIds.length) {
      setSupplyVendorOptions([]);
      return;
    }
    setLoadingSupplyVendors(true);
    try {
      const { data } = await axiosInstance.get(ENDPOINTS.VENDORS, {
        params: { ids: vendorIds.join(',') },
      });
      setSupplyVendorOptions(
        (Array.isArray(data) ? data : []).filter((vendor) => vendorIds.includes(vendor.id)),
      );
    } catch (err) {
      console.error('Failed to load supply vendor options:', err);
      setSupplyVendorOptions([]);
    } finally {
      setLoadingSupplyVendors(false);
    }
  }, [vendorIds]);

  const searchVendors = useCallback(async (query) => {
    setLoadingSearchVendors(true);
    try {
      const { data } = await axiosInstance.get(ENDPOINTS.VENDORS, {
        params: query?.trim() ? { search: query.trim() } : {},
      });
      setSearchVendorOptions(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to search vendors:', err);
      setSearchVendorOptions([]);
    } finally {
      setLoadingSearchVendors(false);
    }
  }, []);

  const hydrateInvoiceVendor = useCallback(async () => {
    if (!invoiceVendorId) {
      setSelectedInvoiceVendor(null);
      return;
    }
    try {
      const { data } = await axiosInstance.get(ENDPOINTS.VENDORS, {
        params: { ids: String(invoiceVendorId) },
      });
      const match = (Array.isArray(data) ? data : []).find((vendor) => vendor.id === invoiceVendorId);
      setSelectedInvoiceVendor(match || null);
    } catch (err) {
      console.error('Failed to hydrate invoice vendor:', err);
      setSelectedInvoiceVendor(null);
    }
  }, [invoiceVendorId]);

  useEffect(() => {
    loadSupplyVendors();
  }, [loadSupplyVendors]);

  useEffect(() => {
    hydrateInvoiceVendor();
  }, [hydrateInvoiceVendor]);

  useEffect(() => {
    setInvoiceRoutingMode(resolveInitialRoutingMode({ isAssetRental, invoiceVendorId, vendorIds }));
  }, [isAssetRental, incomeTypeId, incomeTypeName]);

  const invoiceVendorOptions = useMemo(
    () => (useManualVendorSearch ? searchVendorOptions : supplyVendorOptions),
    [useManualVendorSearch, searchVendorOptions, supplyVendorOptions],
  );

  const handleRoutingModeChange = (mode) => {
    setInvoiceRoutingMode(mode);
    setSelectedInvoiceVendor(null);
    onUpdateDetails({ invoiceVendorId: null });
  };

  return (
    <Box>
      <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 2 }}>
        Settlement & Payment Routing
      </Typography>

      <Grid container spacing={3}>
        <Grid size={12}>
          <Typography variant="body2" fontWeight={600} sx={{ mb: 1 }}>
            Payment Realization Type *
          </Typography>
          <RadioGroup
            row
            value={activePaymentRealization}
            onChange={(e) => {
              const nextType = e.target.value;
              onUpdateDetails({
                paymentRealizationType: nextType,
                ...(nextType !== PAYMENT_REALIZATION_TYPE.DIRECT_PAYMENT_INVOICE
                  ? { invoiceVendorId: null }
                  : {}),
              });
            }}
          >
            {PAYMENT_REALIZATION_OPTIONS.map((option) => (
              <FormControlLabel
                key={option.value}
                value={option.value}
                control={<Radio size="small" />}
                label={option.label}
              />
            ))}
          </RadioGroup>
        </Grid>

        {!isAssetRental && (
          <Grid size={12}>
            <Typography variant="body2" fontWeight={600} sx={{ mb: 1 }}>
              Calculation Basis *
            </Typography>
            <RadioGroup
              row
              value={activeBasis}
              onChange={(e) => onUpdateDetails({ calculationBasis: e.target.value })}
            >
              <FormControlLabel
                value={CALCULATION_BASIS.VENDOR_INVOICE}
                control={<Radio size="small" />}
                label={CALCULATION_BASIS_LABELS.VENDOR_INVOICE}
              />
              <FormControlLabel
                value={CALCULATION_BASIS.VENDOR_INWARD}
                control={<Radio size="small" />}
                label={CALCULATION_BASIS_LABELS.VENDOR_INWARD}
              />
            </RadioGroup>
          </Grid>
        )}

        <Grid size={12}>
          <Divider sx={{ my: 0.5 }} />
        </Grid>

        {showInvoiceVendor && (
          <Grid size={12}>
            <Typography variant="body2" fontWeight={600} sx={{ mb: 1 }}>
              Invoice Vendor Routing *
            </Typography>
          {!isAssetRental && (
            <RadioGroup
              row
              value={invoiceRoutingMode}
              onChange={(e) => handleRoutingModeChange(e.target.value)}
              sx={{ mb: 2 }}
            >
              <FormControlLabel
                value={INVOICE_ROUTING_MODE.SUPPLY_VENDOR}
                control={<Radio size="small" />}
                label="Route to Supply Vendor"
                disabled={!vendorIds.length}
              />
              <FormControlLabel
                value={INVOICE_ROUTING_MODE.OTHER_VENDOR}
                control={<Radio size="small" />}
                label="Different Non-Trade Vendor"
              />
            </RadioGroup>
          )}
          {isAssetRental && (
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1.5 }}>
              Asset rentals require a manual Finance / Non-Trade vendor for invoice routing.
            </Typography>
          )}
          <SearchableSelect
            label="Invoice Vendor"
            placeholder={
              useManualVendorSearch
                ? 'Search Finance / Non-Trade vendor…'
                : (vendorIds.length ? 'Select supply vendor…' : 'Select supply vendors above first')
            }
            isMulti={false}
            options={invoiceVendorOptions}
            value={selectedInvoiceVendor}
            onChange={(vendor) => {
              setSelectedInvoiceVendor(vendor);
              onUpdateDetails({ invoiceVendorId: vendor?.id || null });
            }}
            onSearch={useManualVendorSearch ? searchVendors : loadSupplyVendors}
            getOptionLabel={(vendor) => `${vendor.vendorCode} — ${vendor.vendorName}`}
            isOptionEqualToValue={(option, value) => option.id === value.id}
            loading={useManualVendorSearch ? loadingSearchVendors : loadingSupplyVendors}
            disabled={!useManualVendorSearch && !vendorIds.length}
            required
          />
          </Grid>
        )}

        <Grid size={{ xs: 12, md: 6 }}>
          <TextField
            label="Payout Buffer (days)"
            type="number"
            fullWidth
            size="small"
            value={payoutBufferDays ?? ''}
            onChange={(e) => onUpdateDetails({ payoutBufferDays: e.target.value })}
            helperText="Days after invoice before payout is released"
            slotProps={{ htmlInput: { min: 0 } }}
          />
        </Grid>
      </Grid>
    </Box>
  );
}
