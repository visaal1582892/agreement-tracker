import { Box, Step, StepLabel, Stepper, Typography, Paper, Button, Divider } from '@mui/material';
import { BRAND } from '../config/theme';

const STEPS = [
  'Company & Vendors',
  'Products',
  'Agreement Details',
  'Commercials',
  'Review & Submit',
];

export default function WizardLayout({ activeStep, children, onNext, onBack, onCancel, isLastStep, isSubmitting }) {
  return (
    <Box>
      {/* Step Indicator */}
      <Paper elevation={0} sx={{ p: 3, mb: 3, borderRadius: 3 }}>
        <Stepper activeStep={activeStep} alternativeLabel>
          {STEPS.map((label, index) => (
            <Step key={label}>
              <StepLabel
                slotProps={{
                  stepIcon: {
                    sx: {
                      '&.Mui-active': { color: BRAND.red },
                      '&.Mui-completed': { color: BRAND.green },
                    },
                  },
                }}
              >
                <Typography
                  variant="caption"
                  fontWeight={activeStep === index ? 700 : 400}
                  color={activeStep === index ? BRAND.red : 'text.secondary'}
                >
                  {label}
                </Typography>
              </StepLabel>
            </Step>
          ))}
        </Stepper>
      </Paper>

      {/* Step Content */}
      <Paper elevation={0} sx={{ p: 3, borderRadius: 3, minHeight: 400 }}>
        {children}
      </Paper>

      {/* Navigation Buttons */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 3 }}>
        <Button variant="outlined" color="inherit" onClick={onCancel} sx={{ color: 'text.secondary' }}>
          Cancel
        </Button>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button
            variant="outlined"
            onClick={onBack}
            disabled={activeStep === 0}
          >
            Back
          </Button>
          <Button
            variant="contained"
            onClick={onNext}
            disabled={isSubmitting}
            sx={{ bgcolor: BRAND.red, '&:hover': { bgcolor: BRAND.redDark }, minWidth: 120 }}
          >
            {isLastStep ? (isSubmitting ? 'Submitting…' : 'Submit for Approval') : 'Next'}
          </Button>
        </Box>
      </Box>
    </Box>
  );
}
