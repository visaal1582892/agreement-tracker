import { Box, Step, StepLabel, Stepper, Typography, Paper, Button, alpha } from '@mui/material';
import { BRAND } from '../config/theme';

const STEPS = [
  'Company, Vendors & Products',
  'Agreement Details',
  'Review & Submit',
];

const cardSx = {
  borderRadius: 3.5,
  border: '1px solid rgba(226, 232, 240, 0.8)',
  boxShadow: '0 4px 24px rgba(15, 23, 42, 0.05)',
  bgcolor: '#fff',
};

export default function WizardLayout({
  activeStep,
  children,
  onBack,
  onCancel,
  footerMode = 'setup',
  onSaveDraft,
  onNext,
  onSaveAndCreateAnother,
  onFinishAndExit,
  onSubmitForApproval,
  showSaveDraft = true,
  isSavingDraft,
  isSubmitting,
  isSavingLoop,
}) {
  const busy = isSavingDraft || isSubmitting || isSavingLoop;

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', flex: 1, minHeight: 0 }}>
      <Paper
        elevation={0}
        sx={{
          ...cardSx,
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          minHeight: 0,
        }}
      >
        <Box sx={{
          px: { xs: 2, md: 3 },
          py: 2.5,
          background: 'linear-gradient(180deg, #FAFBFC 0%, #fff 100%)',
          borderBottom: '1px solid #F1F5F9',
        }}>
          <Stepper activeStep={activeStep} alternativeLabel>
            {STEPS.map((label, index) => (
              <Step key={label}>
                <StepLabel
                  slotProps={{
                    stepIcon: {
                      sx: {
                        width: 32,
                        height: 32,
                        '&.Mui-active': {
                          color: BRAND.red,
                          boxShadow: `0 0 0 4px ${alpha(BRAND.red, 0.12)}`,
                          borderRadius: '50%',
                        },
                        '&.Mui-completed': { color: BRAND.green },
                      },
                    },
                  }}
                >
                  <Typography
                    variant="caption"
                    fontWeight={activeStep === index ? 700 : 500}
                    color={activeStep === index ? BRAND.red : 'text.secondary'}
                    sx={{ display: { xs: 'none', sm: 'block' } }}
                  >
                    {label}
                  </Typography>
                </StepLabel>
              </Step>
            ))}
          </Stepper>
        </Box>

        <Box sx={{ flex: 1, p: { xs: 2.5, md: 3.5 }, display: 'flex', flexDirection: 'column' }}>
          {children}
        </Box>

        <Box sx={{
          px: { xs: 2.5, md: 3.5 },
          py: 2.5,
          borderTop: '1px solid #F1F5F9',
          background: '#FAFBFC',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: 2,
          flexShrink: 0,
          flexWrap: 'wrap',
        }}>
          <Button
            variant="outlined"
            onClick={onCancel}
            sx={{
              color: '#64748B',
              borderColor: '#E2E8F0',
              borderRadius: 2.5,
              px: 2.5,
              '&:hover': { borderColor: '#CBD5E1', bgcolor: '#fff' },
            }}
          >
            Cancel
          </Button>
          <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
            <Button
              variant="outlined"
              onClick={onBack}
              disabled={activeStep === 0 || busy}
              sx={{ borderRadius: 2.5, px: 2.5, minWidth: 90 }}
            >
              Back
            </Button>

            {footerMode === 'setup' && (
              <>
                {onSaveDraft && (
                  <Button
                    variant="outlined"
                    onClick={onSaveDraft}
                    disabled={busy}
                    sx={{
                      borderRadius: 2.5,
                      px: 2.5,
                      minWidth: 130,
                      fontWeight: 600,
                      borderColor: '#CBD5E1',
                      color: '#475569',
                    }}
                  >
                    {isSavingDraft ? 'Saving…' : 'Save as Draft'}
                  </Button>
                )}
                <Button
                  variant="contained"
                  onClick={onNext}
                  disabled={busy}
                  sx={{
                    borderRadius: 2.5,
                    px: 3,
                    minWidth: 120,
                    fontWeight: 700,
                    background: BRAND.redGradient,
                    boxShadow: `0 4px 14px ${alpha(BRAND.red, 0.3)}`,
                    '&:hover': { background: BRAND.redGradient },
                  }}
                >
                  Next
                </Button>
              </>
            )}

            {footerMode === 'details' && (
              <>
                {showSaveDraft && onSaveDraft && (
                  <Button
                    variant="outlined"
                    onClick={onSaveDraft}
                    disabled={busy}
                    sx={{
                      borderRadius: 2.5,
                      px: 2.5,
                      minWidth: 130,
                      fontWeight: 600,
                      borderColor: '#CBD5E1',
                      color: '#475569',
                    }}
                  >
                    {isSavingDraft ? 'Saving…' : 'Save as Draft'}
                  </Button>
                )}
                <Button
                  variant="outlined"
                  onClick={onFinishAndExit}
                  disabled={busy}
                  sx={{ borderRadius: 2.5, px: 2.5, minWidth: 130, fontWeight: 600 }}
                >
                  {isSavingDraft ? 'Saving…' : 'Finish & Exit'}
                </Button>
                <Button
                  variant="contained"
                  onClick={onSaveAndCreateAnother}
                  disabled={busy}
                  sx={{
                    borderRadius: 2.5,
                    px: 3,
                    minWidth: 190,
                    fontWeight: 700,
                    background: BRAND.redGradient,
                    boxShadow: `0 4px 14px ${alpha(BRAND.red, 0.3)}`,
                    '&:hover': { background: BRAND.redGradient },
                  }}
                >
                  {isSavingLoop ? 'Saving…' : 'Save & Create Another'}
                </Button>
              </>
            )}

            {footerMode === 'review' && (
              <Button
                variant="contained"
                onClick={onSubmitForApproval}
                disabled={busy}
                sx={{
                  borderRadius: 2.5,
                  px: 3,
                  minWidth: 160,
                  fontWeight: 700,
                  background: BRAND.redGradient,
                  boxShadow: `0 4px 14px ${alpha(BRAND.red, 0.3)}`,
                  '&:hover': { background: BRAND.redGradient },
                }}
              >
                {isSubmitting ? 'Submitting…' : 'Submit for Approval'}
              </Button>
            )}

            {footerMode === 'revision' && (
              <>
                {showSaveDraft && onSaveDraft && activeStep < 2 && (
                  <Button
                    variant="outlined"
                    onClick={onSaveDraft}
                    disabled={busy}
                    sx={{
                      borderRadius: 2.5,
                      px: 2.5,
                      minWidth: 130,
                      fontWeight: 600,
                      borderColor: '#CBD5E1',
                      color: '#475569',
                    }}
                  >
                    {isSavingDraft ? 'Saving…' : 'Save as Draft'}
                  </Button>
                )}
                {activeStep < 2 && (
                  <Button
                    variant="contained"
                    onClick={onNext}
                    disabled={busy}
                    sx={{
                      borderRadius: 2.5,
                      px: 3,
                      minWidth: 120,
                      fontWeight: 700,
                      background: BRAND.redGradient,
                      boxShadow: `0 4px 14px ${alpha(BRAND.red, 0.3)}`,
                      '&:hover': { background: BRAND.redGradient },
                    }}
                  >
                    Next
                  </Button>
                )}
                {activeStep === 2 && (
                  <Button
                    variant="contained"
                    onClick={onSubmitForApproval}
                    disabled={busy}
                    sx={{
                      borderRadius: 2.5,
                      px: 3,
                      minWidth: 160,
                      fontWeight: 700,
                      background: BRAND.redGradient,
                      boxShadow: `0 4px 14px ${alpha(BRAND.red, 0.3)}`,
                      '&:hover': { background: BRAND.redGradient },
                    }}
                  >
                    {isSubmitting ? 'Submitting…' : 'Submit for Approval'}
                  </Button>
                )}
              </>
            )}
          </Box>
        </Box>
      </Paper>
    </Box>
  );
}
