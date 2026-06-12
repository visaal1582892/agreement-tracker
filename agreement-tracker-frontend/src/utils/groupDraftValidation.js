import axiosInstance from '../api/axiosInstance';
import { ENDPOINTS } from '../config/endpoints';
import { fetchCommercialTargetsPreview, fetchSlabs } from '../api/commercialApi';

export function getDraftDetailsGaps(version, { slabCount = 0, targetCount = 0 } = {}) {
  const gaps = [];
  if (!version?.incomeTypeId) gaps.push('Income Type');
  if (!version?.agreementTypeId) gaps.push('Agreement Type');
  if (!version?.startDate) gaps.push('Start Date');
  if (!version?.expiryDate) gaps.push('Expiry Date');
  if (!version?.commercialStructure) gaps.push('Commercial Structure');
  if (version?.commercialStructure === 'FLAT' && version.commercialValue == null) {
    gaps.push('Commercial Value');
  }
  if (version?.commercialStructure === 'SLAB') {
    if (slabCount === 0) gaps.push('Slabs');
    if (targetCount === 0) gaps.push('Commercial Targets');
  }
  return gaps;
}

export async function loadGroupDraftReviewData(drafts) {
  return Promise.all((drafts ?? []).map(async (row) => {
    const { data: version } = await axiosInstance.get(
      ENDPOINTS.AGREEMENT_VERSION_BY_ID(row.latestVersionId),
    );

    let slabs = [];
    let targetCount = 0;

    if (version.commercialStructure === 'SLAB') {
      slabs = await fetchSlabs(row.latestVersionId);
      try {
        const preview = await fetchCommercialTargetsPreview(row.latestVersionId);
        targetCount = Array.isArray(preview) ? preview.length : 0;
      } catch {
        targetCount = 0;
      }
    }

    const gaps = getDraftDetailsGaps(version, {
      slabCount: slabs.length,
      targetCount,
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
