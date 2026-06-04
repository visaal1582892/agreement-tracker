import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axiosInstance from '../../api/axiosInstance';
import { ENDPOINTS } from '../../config/endpoints';

export const fetchAgreementGroups = createAsyncThunk(
  'agreements/fetchGroups',
  async ({ page = 0, size = 20, ...params } = {}, { rejectWithValue }) => {
    try {
      const { data } = await axiosInstance.get(ENDPOINTS.AGREEMENT_GROUPS, {
        params: { page, size, ...params },
      });
      return data;
    } catch (err) {
      return rejectWithValue(err.response?.data?.message || 'Failed to fetch agreements');
    }
  }
);

export const fetchPendingApprovals = createAsyncThunk(
  'agreements/fetchPending',
  async ({ page = 0, size = 20 } = {}, { rejectWithValue }) => {
    try {
      const { data } = await axiosInstance.get(ENDPOINTS.AGREEMENT_PENDING_APPROVALS, {
        params: { page, size },
      });
      return data;
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
        state.pendingApprovals = payload.content;
        state.pendingTotal = payload.totalElements;
      })
      .addCase(fetchPendingApprovals.rejected, (state, { payload }) => {
        state.loading = false;
        state.error = payload;
      });
  },
});

export const { clearAgreementError } = agreementSlice.actions;
export default agreementSlice.reducer;
