import { Box, Divider, TextField } from '@mui/material';
import Step1CompanyVendors from './Step1CompanyVendors';
import Step2Products from './Step2Products';

export default function Step1Setup({ state, updateFields, updateProductRules }) {
  return (
    <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      <TextField
        label="Agreement Name"
        value={state.agreementName || ''}
        onChange={(e) => updateFields({ agreementName: e.target.value })}
        required
        fullWidth
        inputProps={{ maxLength: 255 }}
        sx={{ mb: 3 }}
      />
      <Step1CompanyVendors state={state} updateFields={updateFields} />
      <Divider sx={{ my: 3 }} />
      <Step2Products state={state} updateProductRules={updateProductRules} />
    </Box>
  );
}
