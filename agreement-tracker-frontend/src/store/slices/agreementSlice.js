import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axiosInstance from '../../api/axiosInstance';
import { ENDPOINTS } from '../../config/endpoints';
import { normalizePageResponse } from '../../utils/pageResponse';

export const fetchAgreementGroups = createAsyncThunk(
  'agreements/fetchGroups',
  async ({ page = 0, size = 20, scope = 'MY', sortBy, sortDir, ...params } = {}, { rejectWithValue }) => {
    try {
      const query = { page, size, scope, ...params };
      if (sortBy) {
        query.sort = `${sortBy},${sortDir || 'asc'}`;
      }
      const { data } = await axiosInstance.get(ENDPOINTS.AGREEMENT_GROUPS, { params: query });
      return normalizePageResponse(data);
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to fetch agreements');
    }
  }
);

export const submitAgreementForApproval = createAsyncThunk(
  'agreements/submit',
  async (agreementId, { rejectWithValue }) => {
    try {
      const { data } = await axiosInstance.put(ENDPOINTS.AGREEMENT_SUBMIT(agreementId));
      return data;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to submit agreement');
    }
  }
);

export const approveAgreement = createAsyncThunk(
  'agreements/approve',
  async ({ agreementId, remarks = 'Approved' }, { rejectWithValue }) => {
    try {
      const { data } = await axiosInstance.post(ENDPOINTS.AGREEMENT_APPROVE(agreementId), { remarks });
      return data;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Approval failed');
    }
  }
);

export const rejectAgreement = createAsyncThunk(
  'agreements/reject',
  async ({ agreementId, remarks }, { rejectWithValue }) => {
    try {
      const { data } = await axiosInstance.post(ENDPOINTS.AGREEMENT_REJECT(agreementId), { remarks });
      return data;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Rejection failed');
    }
  }
);

export const fetchPendingApprovals = createAsyncThunk(
  'agreements/fetchPending',
  async ({ page = 0, size = 10, search = '' } = {}, { rejectWithValue }) => {
    try {
      const params = { page, size };
      if (search?.trim()) params.search = search.trim();
      const { data } = await axiosInstance.get(ENDPOINTS.AGREEMENT_PENDING_APPROVALS, { params });
      return normalizePageResponse(data);
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to fetch pending approvals');
    }
  }
);

function applyAgreementToGroups(state, agreement) {
  if (!agreement?.agreementGroupId) return;

  const idx = state.groups.findIndex((g) => g.id === agreement.agreementGroupId);
  if (idx === -1) return;

  const group = state.groups[idx];
  state.groups[idx] = {
    ...group,
    currentStatus: agreement.derivedStatus ?? agreement.approvalStatus,
    latestVersionId: agreement.id,
    currentVersionNumber: agreement.versionNumber ?? group.currentVersionNumber,
    currentVersionId: agreement.approvalStatus === 'APPROVED'
      ? agreement.id
      : group.currentVersionId,
    startDate: agreement.startDate ?? group.startDate,
    expiryDate: agreement.expiryDate ?? group.expiryDate,
    incomeTypeName: agreement.incomeTypeName ?? group.incomeTypeName,
  };
}

function removeFromPendingApprovals(state, agreement) {
  if (!agreement) return;

  const before = state.pendingApprovals.length;
  state.pendingApprovals = state.pendingApprovals.filter(
    (a) => a.id !== agreement.id && a.agreementGroupId !== agreement.agreementGroupId,
  );
  const removed = before - state.pendingApprovals.length;
  if (removed > 0 && state.pendingTotal > 0) {
    state.pendingTotal -= removed;
  }
}

const agreementSlice = createSlice({
  name: 'agreements',
  initialState: {
    groups: [],
    totalElements: 0,
    totalPages: 0,
    currentPage: 0,
    pendingApprovals: [],
    pendingTotal: 0,
    pendingTotalPages: 0,
    pendingPage: 0,
    pendingPageSize: 10,
    loading: false,
    error: null,
  },
  reducers: {
    clearAgreementError(state) {
      state.error = null;
    },
    updateAgreementStatusLocal(state, { payload }) {
      const { agreementId, groupId, newStatus } = payload;
      const idx = state.groups.findIndex(
        (g) => g.id === groupId || g.latestVersionId === agreementId,
      );
      if (idx !== -1 && newStatus) {
        state.groups[idx].currentStatus = newStatus;
      }
    },
    updateAgreementGroupFromResponse(state, { payload: agreement }) {
      applyAgreementToGroups(state, agreement);
      if (agreement?.approvalStatus !== 'PENDING_APPROVAL') {
        removeFromPendingApprovals(state, agreement);
      }
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchAgreementGroups.pending, (state) => { state.loading = true; })
      .addCase(fetchAgreementGroups.fulfilled, (state, { payload }) => {
        state.loading = false;
        state.groups = payload.content;
        state.totalElements = payload.totalElements;
        state.totalPages = payload.totalPages;
        state.currentPage = payload.number;
      })
      .addCase(fetchAgreementGroups.rejected, (state, { payload }) => {
        state.loading = false;
        state.error = payload;
      })
      .addCase(submitAgreementForApproval.fulfilled, (state, { payload }) => {
        applyAgreementToGroups(state, payload);
      })
      .addCase(approveAgreement.fulfilled, (state, { payload }) => {
        applyAgreementToGroups(state, payload);
        removeFromPendingApprovals(state, payload);
      })
      .addCase(rejectAgreement.fulfilled, (state, { payload }) => {
        applyAgreementToGroups(state, payload);
        removeFromPendingApprovals(state, payload);
      })
      .addCase(fetchPendingApprovals.pending, (state) => { state.loading = true; })
      .addCase(fetchPendingApprovals.fulfilled, (state, { payload }) => {
        state.loading = false;
        state.pendingApprovals = payload.content ?? [];
        state.pendingTotal = payload.totalElements ?? 0;
        state.pendingTotalPages = payload.totalPages ?? 0;
        state.pendingPage = payload.number ?? 0;
        state.pendingPageSize = payload.size ?? state.pendingPageSize;
      })
      .addCase(fetchPendingApprovals.rejected, (state, { payload }) => {
        state.loading = false;
        state.error = payload;
      });
  },
});

export const {
  clearAgreementError,
  updateAgreementStatusLocal,
  updateAgreementGroupFromResponse,
} = agreementSlice.actions;
export default agreementSlice.reducer;
