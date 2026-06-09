import { Box, Divider } from '@mui/material';
import Step1CompanyVendors from './Step1CompanyVendors';
import Step2Products from './Step2Products';

export default function Step1Setup({ state, updateFields, updateProductRules }) {
  return (
    <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      <Step1CompanyVendors state={state} updateFields={updateFields} />
      <Divider sx={{ my: 3 }} />
      <Step2Products state={state} updateProductRules={updateProductRules} />
    </Box>
  );
}
