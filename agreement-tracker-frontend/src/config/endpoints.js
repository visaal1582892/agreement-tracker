const BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const API_BASE = BASE;

export const ENDPOINTS = {
  // Auth
  LOGIN: `${BASE}/auth/login`,
  AUTH_ME: `${BASE}/auth/me`,

  // Users
  USERS: `${BASE}/users`,
  USER_BY_ID: (id) => `${BASE}/users/${id}`,
  USER_ME: `${BASE}/users/me`,
  USER_SEARCH: `${BASE}/users/search`,
  USER_ROLES: (id) => `${BASE}/users/${id}/roles`,

  // Agreements
  AGREEMENTS: `${BASE}/agreements`,
  AGREEMENT_BY_ID: (id) => `${BASE}/agreements/${id}`,
  AGREEMENT_GROUPS: `${BASE}/agreements/groups`,
  AGREEMENT_GROUP_BY_ID: (id) => `${BASE}/agreements/groups/${id}`,
  AGREEMENT_VERSIONS: (groupId) => `${BASE}/agreements/groups/${groupId}/versions`,
  AGREEMENT_NEW_VERSION: (groupId) => `${BASE}/agreements/groups/${groupId}/new-version`,
  AGREEMENT_SUBMIT: (id) => `${BASE}/agreements/${id}/submit`,
  AGREEMENT_APPROVE: (id) => `${BASE}/agreements/${id}/approve`,
  AGREEMENT_REJECT: (id) => `${BASE}/agreements/${id}/reject`,
  AGREEMENT_TERMINATE: (id) => `${BASE}/agreements/${id}/terminate`,
  AGREEMENT_IN_PROGRESS: (id) => `${BASE}/agreements/${id}/in-progress`,
  AGREEMENT_TIMELINE: (id) => `${BASE}/agreements/${id}/timeline`,
  AGREEMENT_PENDING_APPROVALS: `${BASE}/agreements/pending-approvals`,
  AGREEMENT_BULK_TRANSFER: `${BASE}/agreements/bulk-transfer`,

  // Dashboard
  DASHBOARD_STATS: `${BASE}/dashboard/stats`,

  // Master Data — simple dropdowns (backward compat with wizard)
  COMPANIES: `${BASE}/master/companies`,
  INCOME_TYPES: `${BASE}/master/income-types`,
  AGREEMENT_TYPES: `${BASE}/master/agreement-types`,
  VENDORS: `${BASE}/master/vendors`,
  MANUFACTURERS: `${BASE}/master/manufacturers`,
  DIVISIONS: (mfrId) => `${BASE}/master/manufacturers/${mfrId}/divisions`,
  PRODUCTS: `${BASE}/master/products`,

  // Master Data — full CRUD + paginated search
  MASTER_COMPANIES: `${BASE}/master/companies`,
  MASTER_COMPANIES_SEARCH: `${BASE}/master/companies/search`,
  MASTER_COMPANY_BY_ID: (id) => `${BASE}/master/companies/${id}`,
  MASTER_COMPANY_TOGGLE: (id) => `${BASE}/master/companies/${id}/toggle-status`,

  MASTER_VENDORS: `${BASE}/master/vendors`,
  MASTER_VENDORS_SEARCH: `${BASE}/master/vendors/search`,
  MASTER_VENDOR_BY_ID: (id) => `${BASE}/master/vendors/${id}`,
  MASTER_VENDOR_TOGGLE: (id) => `${BASE}/master/vendors/${id}/toggle-status`,

  MASTER_MANUFACTURERS: `${BASE}/master/manufacturers`,
  MASTER_MANUFACTURERS_SEARCH: `${BASE}/master/manufacturers/search`,
  MASTER_MANUFACTURER_BY_ID: (id) => `${BASE}/master/manufacturers/${id}`,
  MASTER_MANUFACTURER_TOGGLE: (id) => `${BASE}/master/manufacturers/${id}/toggle-status`,

  MASTER_DIVISIONS: `${BASE}/master/divisions`,
  MASTER_DIVISIONS_SEARCH: `${BASE}/master/divisions/search`,
  MASTER_DIVISION_BY_ID: (id) => `${BASE}/master/divisions/${id}`,
  MASTER_DIVISION_TOGGLE: (id) => `${BASE}/master/divisions/${id}/toggle-status`,

  MASTER_PRODUCTS: `${BASE}/master/products`,
  MASTER_PRODUCTS_SEARCH: `${BASE}/master/products/search`,
  MASTER_PRODUCT_BY_ID: (id) => `${BASE}/master/products/${id}`,
  MASTER_PRODUCT_TOGGLE: (id) => `${BASE}/master/products/${id}/toggle-status`,

  MASTER_INCOME_TYPES: `${BASE}/master/income-types`,
  MASTER_INCOME_TYPES_SEARCH: `${BASE}/master/income-types/search`,
  MASTER_INCOME_TYPE_BY_ID: (id) => `${BASE}/master/income-types/${id}`,
  MASTER_INCOME_TYPE_TOGGLE: (id) => `${BASE}/master/income-types/${id}/toggle-status`,

  MASTER_AGREEMENT_TYPES: `${BASE}/master/agreement-types`,
  MASTER_AGREEMENT_TYPES_SEARCH: `${BASE}/master/agreement-types/search`,
  MASTER_AGREEMENT_TYPE_BY_ID: (id) => `${BASE}/master/agreement-types/${id}`,
  MASTER_AGREEMENT_TYPE_TOGGLE: (id) => `${BASE}/master/agreement-types/${id}/toggle-status`,

  MASTER_ROLES: `${BASE}/master/roles`,
  MASTER_ROLES_SEARCH: `${BASE}/master/roles/search`,
  MASTER_ROLE_BY_ID: (id) => `${BASE}/master/roles/${id}`,
  MASTER_ROLE_TOGGLE: (id) => `${BASE}/master/roles/${id}/toggle-status`,

  MASTER_RIGHTS: `${BASE}/master/rights`,
  MASTER_RIGHTS_SEARCH: `${BASE}/master/rights/search`,
  MASTER_RIGHT_BY_ID: (id) => `${BASE}/master/rights/${id}`,
  MASTER_RIGHT_TOGGLE: (id) => `${BASE}/master/rights/${id}/toggle-status`,
};
