import axiosInstance from '../api/axiosInstance';
import { ENDPOINTS } from '../config/endpoints';
import { fetchSlabs } from '../api/commercialApi';
import { INCOME_TYPE_NAMES } from '../constants/incomeTypeNames';

function getAssetRentalPayoutGaps(asset) {
  const gaps = [];
  if (!asset?.assetCategory) gaps.push('Asset Category');
  if (!asset?.assetType) gaps.push('Asset Type');
  if (!asset?.storeCount) gaps.push('Store Count');
  const hasFlatPayout = asset?.flatPayout != null && Number(asset.flatPayout) > 0;
  const hasPerStorePayout = asset?.payoutPerStore != null && Number(asset.payoutPerStore) > 0;
  if (!hasFlatPayout && !hasPerStorePayout) {
    gaps.push('Asset Payout');
  }
  return gaps;
}

export function getDraftDetailsGaps(version, { slabCount = 0, targetCount = 0 } = {}) {
  const gaps = [];
  if (!version?.incomeTypeId) gaps.push('Income Type');
  if (!version?.agreementTypeId) gaps.push('Agreement Type');
  if (!version?.startDate) gaps.push('Start Date');
  if (!version?.expiryDate) gaps.push('Expiry Date');

  if (version?.incomeTypeName === INCOME_TYPE_NAMES.ASSET_RENTALS) {
    gaps.push(...getAssetRentalPayoutGaps(version.asset));
    if (!version?.invoiceVendorId) gaps.push('Invoice Vendor');
    return gaps;
  }

  const structure = version?.commercialStructure;
  const requiresFlat = structure === 'FLAT' || structure === 'HYBRID';
  if (!structure) gaps.push('Commercial Structure');
  if (requiresFlat && version.commercialValue == null) gaps.push('Flat Baseline Value');
  if (requiresFlat && !version.flatBaselineFrequency) gaps.push('Flat Baseline Frequency');
  if (structure === 'SLAB' && slabCount === 0) gaps.push('Slabs');
  if (structure === 'HYBRID' && slabCount === 0) gaps.push('Slabs');
  if (!version?.paymentRealizationType) gaps.push('Payment Realization Type');
  return gaps;
}

export async function loadGroupDraftReviewData(drafts) {
  return Promise.all((drafts ?? []).map(async (row) => {
    const { data: version } = await axiosInstance.get(
      ENDPOINTS.AGREEMENT_VERSION_BY_ID(row.latestVersionId),
    );

    let slabs = [];

    if (version.commercialStructure === 'SLAB' || version.commercialStructure === 'HYBRID') {
      slabs = await fetchSlabs(row.latestVersionId);
    }

    const gaps = getDraftDetailsGaps(version, {
      slabCount: slabs.length,
    });

    return {
      row,
      version,
      slabs,
      gaps,
      isComplete: gaps.length === 0,
    };
  }));
}

export function incompleteDraftLabels(reviewData) {
  return reviewData
    .filter((item) => !item.isComplete)
    .map((item) => item.version.agreementName || item.row.agreementName || `Draft #${item.row.id}`);
}
