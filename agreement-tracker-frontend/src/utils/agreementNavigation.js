/**
 * Draft missing agreement name — route to edit wizard, not read-only detail.
 */
export function isIncompleteDraft(agreement) {
  const name = agreement?.agreementName?.trim();
  if (!name) return true;
  const number = agreement?.agreementNumber?.trim();
  if (number && name === number) return true;
  return false;
}

export function buildAgreementEditPath(agreementId, { step } = {}) {
  const base = `/agreements/${agreementId}/edit`;
  if (step != null && step !== '') {
    return `${base}?step=${step}`;
  }
  return base;
}

export function buildAgreementDetailPath(groupId) {
  return `/agreements/groups/${groupId}`;
}

/**
 * Row click / primary navigation from agreements list.
 * Incomplete draft → edit wizard step 2 (agreement details).
 * Complete → group detail page.
 */
export function navigateToAgreement(row, navigate) {
  if (!row?.latestVersionId) return;

  if (isIncompleteDraft(row)) {
    navigate(buildAgreementEditPath(row.latestVersionId, { step: 2 }));
    return;
  }

  if (row.id) {
    navigate(buildAgreementDetailPath(row.id));
  }
}
