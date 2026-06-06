import { useCallback, useEffect, useRef, useState } from 'react';
import { Box, Typography, Button, alpha, Chip } from '@mui/material';
import { Add } from '@mui/icons-material';
import { BRAND } from '../../../config/theme';
import { getScrollParent, scrollElementIntoView } from '../../../utils/scroll';
import AgreementDetailsCard from './AgreementDetailsCard';

export default function Step2Agreements({
  state,
  addAgreement,
  removeAgreement,
  updateAgreementDetails,
  updateAgreementCommercials,
  documentErrors,
  onClearDocumentError,
  disableAdd = false,
}) {
  const [activeIndex, setActiveIndex] = useState(0);
  const navRef = useRef(null);
  const scrollParentRef = useRef(null);

  useEffect(() => {
    if (activeIndex >= state.agreements.length) {
      setActiveIndex(Math.max(0, state.agreements.length - 1));
    }
  }, [state.agreements.length, activeIndex]);

  useEffect(() => {
    const firstCard = state.agreements[0]
      ? document.getElementById(`agreement-card-${state.agreements[0].id}`)
      : null;
    scrollParentRef.current = firstCard ? getScrollParent(firstCard) : null;
  }, [state.agreements]);

  useEffect(() => {
    if (state.agreements.length <= 1) return undefined;

    let observer;
    const frame = requestAnimationFrame(() => {
      const cards = state.agreements
        .map((a) => document.getElementById(`agreement-card-${a.id}`))
        .filter(Boolean);

      if (!cards.length) return;

      const scrollRoot = scrollParentRef.current ?? getScrollParent(cards[0]);

      observer = new IntersectionObserver(
        (entries) => {
          const visible = entries
            .filter((entry) => entry.isIntersecting)
            .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);

          if (!visible.length) return;

          const id = visible[0].target.id;
          const idx = state.agreements.findIndex((a) => `agreement-card-${a.id}` === id);
          if (idx >= 0) setActiveIndex(idx);
        },
        {
          root: scrollRoot,
          rootMargin: '-72px 0px -55% 0px',
          threshold: [0, 0.1, 0.25],
        },
      );

      cards.forEach((card) => observer.observe(card));
    });

    return () => {
      cancelAnimationFrame(frame);
      observer?.disconnect();
    };
  }, [state.agreements]);

  const scrollToAgreement = useCallback((index, agreementId) => {
    setActiveIndex(index);
    const target = document.getElementById(`agreement-card-${agreementId}`);
    const navHeight = navRef.current?.offsetHeight ?? 0;
    scrollElementIntoView(target, navHeight + 12);
  }, []);

  return (
    <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h6" fontWeight={600} mb={0.5}>Agreement Details</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Fill in details, commercial structure, and documents for each agreement in the batch.
      </Typography>

      {state.agreements.length > 1 && (
        <Box
          ref={navRef}
          sx={{
            position: 'sticky',
            top: 0,
            zIndex: 10,
            mb: 2.5,
            py: 1.5,
            px: 1,
            mx: -1,
            bgcolor: '#fff',
            borderBottom: `1px solid ${BRAND.borderLight}`,
            boxShadow: '0 4px 12px rgba(15, 23, 42, 0.06)',
          }}
        >
          <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ display: 'block', mb: 1 }}>
            Jump to agreement
          </Typography>
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            {state.agreements.map((agreement, index) => (
              <Chip
                key={agreement.id}
                label={`Agreement ${index + 1}`}
                clickable
                onClick={() => scrollToAgreement(index, agreement.id)}
                variant={activeIndex === index ? 'filled' : 'outlined'}
                sx={{
                  fontWeight: 600,
                  ...(activeIndex === index
                    ? { bgcolor: BRAND.red, color: '#fff', '&:hover': { bgcolor: BRAND.red } }
                    : { borderColor: BRAND.borderLight, '&:hover': { bgcolor: alpha(BRAND.red, 0.06) } }),
                }}
              />
            ))}
          </Box>
        </Box>
      )}

      {state.agreements.map((agreement, index) => (
        <AgreementDetailsCard
          key={agreement.id}
          cardId={`agreement-card-${agreement.id}`}
          index={index}
          agreement={agreement}
          onUpdateDetails={(patch) => updateAgreementDetails(agreement.id, patch)}
          onUpdateCommercials={(patch) => updateAgreementCommercials(agreement.id, patch)}
          onRemove={() => removeAgreement(agreement.id)}
          canRemove={state.agreements.length > 1}
          documentError={documentErrors?.[agreement.id]}
          onClearDocumentError={() => onClearDocumentError?.(agreement.id)}
        />
      ))}

      {!disableAdd && <Button
        variant="outlined"
        startIcon={<Add />}
        onClick={addAgreement}
        sx={{
          mt: 1,
          alignSelf: 'flex-start',
          borderRadius: 2.5,
          px: 2.5,
          fontWeight: 700,
          borderColor: alpha(BRAND.red, 0.4),
          color: BRAND.red,
          '&:hover': { borderColor: BRAND.red, bgcolor: alpha(BRAND.red, 0.04) },
        }}
      >
        Add New Agreement
      </Button>}
    </Box>
  );
}
