import axiosInstance from './axiosInstance';
import { ENDPOINTS } from '../config/endpoints';

export async function fetchPurchaseSlabs(agreementId) {
  const { data } = await axiosInstance.get(ENDPOINTS.AGREEMENT_SLABS(agreementId));
  return data;
}

export async function createPurchaseSlab(agreementId, payload) {
  const { data } = await axiosInstance.post(ENDPOINTS.AGREEMENT_SLABS(agreementId), payload);
  return data;
}

export async function updatePurchaseSlab(agreementId, slabId, payload) {
  const { data } = await axiosInstance.put(ENDPOINTS.AGREEMENT_SLAB(agreementId, slabId), payload);
  return data;
}

export async function deletePurchaseSlab(agreementId, slabId) {
  await axiosInstance.delete(ENDPOINTS.AGREEMENT_SLAB(agreementId, slabId));
}

export async function generateCommercialTemplate(agreementId, payload) {
  const { data } = await axiosInstance.post(
    ENDPOINTS.COMMERCIAL_TEMPLATE(agreementId),
    payload,
    { responseType: 'blob' },
  );
  return data;
}

export async function uploadCommercialTargets(agreementId, file) {
  const formData = new FormData();
  formData.append('file', file);
  const { data } = await axiosInstance.post(
    ENDPOINTS.COMMERCIAL_UPLOAD(agreementId),
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  );
  return data;
}

export async function fetchCommercialTargetsPreview(agreementId) {
  const { data } = await axiosInstance.get(ENDPOINTS.COMMERCIAL_TARGETS_PREVIEW(agreementId));
  return data;
}

export async function upsertSaleTarget(agreementId, payload) {
  await axiosInstance.put(ENDPOINTS.COMMERCIAL_TARGETS(agreementId), payload);
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

export function toSlabPayload(slab) {
  return {
    fromValue: Number(slab.fromValue),
    toValue: Number(slab.toValue),
    valueType: slab.valueType,
    commercialValue: Number(slab.commercialValue),
  };
}

export function formatSlabLabel(slab) {
  const range = `${slab.fromValue} - ${slab.toValue}`;
  if (slab.valueType === 'PERCENTAGE') {
    return `${range} (${slab.commercialValue}%)`;
  }
  return `${range} (Fixed: ${slab.commercialValue})`;
}
