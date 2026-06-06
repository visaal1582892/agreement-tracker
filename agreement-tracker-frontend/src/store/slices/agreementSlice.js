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

export const { clearAgreementError } = agreementSlice.actions;
export default agreementSlice.reducer;
