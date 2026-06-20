import { Box } from '@mui/material';
import Step1CompanyVendors from './Step1CompanyVendors';
import Step1FoundationalFields from './Step1FoundationalFields';

export default function Step1Setup({
  state,
  updateFields,
  updateAgreementDetails,
  updateAgreementAsset,
  updateAgreementCommercials,
  updateProductRules,
  groupFieldsLocked = false,
}) {
  return (
    <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      <Step1CompanyVendors
        state={state}
        updateFields={updateFields}
        groupFieldsLocked={groupFieldsLocked}
      />
      <Step1FoundationalFields
        agreement={state.agreement}
        onUpdateDetails={updateAgreementDetails}
        onUpdateAsset={updateAgreementAsset}
        onUpdateCommercials={updateAgreementCommercials}
        updateProductRules={updateProductRules}
      />
    </Box>
  );
}
