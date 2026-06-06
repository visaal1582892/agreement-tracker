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

export default function WizardLayout({ activeStep, children, onNext, onBack, onCancel, isLastStep, isSubmitting }) {
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
        {/* Stepper */}
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

        {/* Step content */}
        <Box sx={{ flex: 1, p: { xs: 2.5, md: 3.5 }, display: 'flex', flexDirection: 'column' }}>
          {children}
        </Box>

        {/* Footer actions */}
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
          <Box sx={{ display: 'flex', gap: 1.5 }}>
            <Button
              variant="outlined"
              onClick={onBack}
              disabled={activeStep === 0}
              sx={{ borderRadius: 2.5, px: 2.5, minWidth: 90 }}
            >
              Back
            </Button>
            <Button
              variant="contained"
              onClick={onNext}
              disabled={isSubmitting}
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
              {isLastStep ? (isSubmitting ? 'Submitting…' : 'Submit for Approval') : 'Next'}
            </Button>
          </Box>
        </Box>
      </Paper>
    </Box>
  );
}
