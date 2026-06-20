import { isAdHocIncomeType, isAssetRentalIncomeType } from './incomeTypeUtils';
import { ADHOC_SUB_TYPES } from '../constants/adhocSubTypes';

const BLANK_ASSET = {
  assetCategory: 'PHYSICAL_ASSET',
  assetType: '',
  storeCount: '',
  payoutMode: 'FLAT',
  flatPayout: '',
  payoutPerStore: '',
  remarks: '',
};

export function resolveIncomeTypeProfile(incomeTypes, incomeTypeId, incomeTypeName = null) {
  if (isAssetRentalIncomeType(incomeTypes, incomeTypeId, incomeTypeName)) return 'ASSET_RENTAL';
  if (isAdHocIncomeType(incomeTypes, incomeTypeId, incomeTypeName)) return 'AD_HOC';
  return 'STANDARD';
}

export function buildIncomeTypeSwitchUpdates(incomeTypes, previousDetails, nextDetails) {
  const prevId = previousDetails?.incomeTypeId;
  const nextId = nextDetails?.incomeTypeId;
  if (prevId == null || String(prevId) === String(nextId)) {
    return null;
  }

  const prevProfile = resolveIncomeTypeProfile(
    incomeTypes,
    previousDetails.incomeTypeId,
    previousDetails.incomeTypeName,
  );
  const nextProfile = resolveIncomeTypeProfile(
    incomeTypes,
    nextDetails.incomeTypeId,
    nextDetails.incomeTypeName,
  );

  const updates = {
    details: {},
    resetAsset: false,
    resetCommercials: false,
    clearProductRules: false,
  };

  if (nextProfile !== 'AD_HOC') {
    updates.details.adhocSubType = null;
    updates.details.quantityCap = '';
  }
  if (nextProfile !== 'ASSET_RENTAL') {
    updates.details.storeOutletList = null;
    updates.resetAsset = true;
  }
  if (nextProfile === 'ASSET_RENTAL') {
    updates.clearProductRules = true;
  }
  if (prevProfile !== nextProfile) {
    updates.details.stateIds = [];
  }
  if (nextProfile === 'AD_HOC' || prevProfile === 'AD_HOC') {
    updates.resetCommercials = true;
  }

  return updates;
}

export function sanitizeAgreementPayload(payload, incomeTypes, incomeTypeId, incomeTypeName = null) {
  const profile = resolveIncomeTypeProfile(incomeTypes, incomeTypeId, incomeTypeName);
  const sanitized = {
    ...payload,
    details: payload.details ? { ...payload.details } : {},
    commercials: payload.commercials ? { ...payload.commercials } : {},
  };

  if (profile === 'ASSET_RENTAL') {
    sanitized.productRules = { manufacturers: [], divisionRules: [], productRules: [] };
    sanitized.details.adhocSubType = null;
    sanitized.details.quantityCap = null;
    if (!sanitized.asset?.assetType?.trim()) {
      sanitized.asset = null;
    }
  } else {
    sanitized.asset = null;
    if (profile !== 'AD_HOC') {
      sanitized.details.adhocSubType = null;
      sanitized.details.quantityCap = null;
    } else if (sanitized.details.adhocSubType === ADHOC_SUB_TYPES.QPS) {
      sanitized.details.quantityCap = null;
    }
  }

  return sanitized;
}

export { BLANK_ASSET };
