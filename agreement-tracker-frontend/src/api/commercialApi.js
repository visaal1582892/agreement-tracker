import axiosInstance from './axiosInstance';
import { ENDPOINTS } from '../config/endpoints';

export async function fetchSlabs(agreementVersionId, slabType) {
  const params = slabType ? { slabType } : {};
  const { data } = await axiosInstance.get(ENDPOINTS.AGREEMENT_VERSION_SLABS(agreementVersionId), { params });
  return data;
}

export async function createSlab(agreementVersionId, payload) {
  const { data } = await axiosInstance.post(ENDPOINTS.AGREEMENT_VERSION_SLABS(agreementVersionId), payload);
  return data;
}

export async function updateSlab(agreementVersionId, slabId, payload) {
  const { data } = await axiosInstance.put(
    ENDPOINTS.AGREEMENT_VERSION_SLAB(agreementVersionId, slabId),
    payload,
  );
  return data;
}

export async function deleteSlab(agreementVersionId, slabId) {
  await axiosInstance.delete(ENDPOINTS.AGREEMENT_VERSION_SLAB(agreementVersionId, slabId));
}

export async function generateCommercialTemplate(agreementVersionId, payload) {
  const { data } = await axiosInstance.post(
    ENDPOINTS.COMMERCIAL_TEMPLATE(agreementVersionId),
    payload,
    { responseType: 'blob' },
  );
  return data;
}

export async function uploadCommercialTargets(agreementVersionId, file, slabType) {
  const formData = new FormData();
  formData.append('file', file);
  const params = slabType ? { slabType } : {};
  const { data } = await axiosInstance.post(
    ENDPOINTS.COMMERCIAL_UPLOAD(agreementVersionId),
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' }, params },
  );
  return data;
}

export async function fetchCommercialTargetsPreview(agreementVersionId) {
  const { data } = await axiosInstance.get(ENDPOINTS.COMMERCIAL_TARGETS_PREVIEW(agreementVersionId));
  return data;
}

export async function upsertTarget(agreementVersionId, payload) {
  await axiosInstance.put(ENDPOINTS.COMMERCIAL_TARGETS(agreementVersionId), payload);
}

export async function switchCommercialType(agreementVersionId, payload) {
  await axiosInstance.put(ENDPOINTS.COMMERCIAL_TYPE_SWITCH(agreementVersionId), payload);
}

export function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

export async function extractApiErrorMessage(err, fallback) {
  const data = err.response?.data;
  if (data instanceof Blob) {
    try {
      const json = JSON.parse(await data.text());
      return json.message || fallback;
    } catch {
      return fallback;
    }
  }
  return data?.message || fallback;
}

export function toSlabPayload(slab, slabType) {
  return {
    fromValue: Number(slab.fromValue),
    toValue: Number(slab.toValue),
    valueType: slab.valueType,
    commercialValue: Number(slab.commercialValue),
    slabType: slabType || slab.slabType || 'PURCHASE',
  };
}

export function formatSlabLabel(slab) {
  const range = `${slab.fromValue} - ${slab.toValue}`;
  if (slab.valueType === 'PERCENTAGE') {
    return `${range} (${slab.commercialValue}%)`;
  }
  return `${range} (Fixed: ${slab.commercialValue})`;
}

function slabRangesOverlap(fromA, toA, fromB, toB) {
  return fromA < toB && fromB < toA;
}

function isExactSlabMatch(a, b) {
  return Number(a.fromValue) === Number(b.fromValue)
    && Number(a.toValue) === Number(b.toValue)
    && a.valueType === b.valueType
    && Number(a.commercialValue) === Number(b.commercialValue);
}

/** Returns error message if payload conflicts with existing slabs, else null. */
export function validateSlabAgainstExisting(slabs, payload, excludeSlabId = null) {
  const peers = (slabs ?? []).filter((s) => s.id !== excludeSlabId);
  const from = Number(payload.fromValue);
  const to = Number(payload.toValue);

  const duplicate = peers.find((s) => isExactSlabMatch(s, payload));
  if (duplicate) {
    return `This slab rule already exists: ${formatSlabLabel(duplicate)}`;
  }

  const overlap = peers.find((s) => slabRangesOverlap(from, to, Number(s.fromValue), Number(s.toValue)));
  if (overlap) {
    return `Slab range overlaps with existing rule: ${formatSlabLabel(overlap)}`;
  }

  return null;
}
