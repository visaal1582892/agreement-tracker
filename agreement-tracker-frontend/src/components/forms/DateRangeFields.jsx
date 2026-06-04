import { Grid, TextField } from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs from 'dayjs';

export default function DateRangeFields({ startDate, expiryDate, tenureMonths, onStartChange, onExpiryChange, onTenureChange }) {
  const handleStartChange = (date) => {
    onStartChange(date);
    if (tenureMonths && date) {
      onExpiryChange(date.add(Number(tenureMonths), 'month').subtract(1, 'day'));
    }
  };

  const handleTenureChange = (e) => {
    const months = e.target.value;
    onTenureChange(months);
    if (startDate && months) {
      onExpiryChange(dayjs(startDate).add(Number(months), 'month').subtract(1, 'day'));
    }
  };

  const handleExpiryChange = (date) => {
    onExpiryChange(date);
    if (startDate && date) {
      const diff = date.diff(dayjs(startDate), 'month');
      onTenureChange(String(diff));
    }
  };

  return (
    <Grid container spacing={2}>
      <Grid size={{ xs: 12, sm: 4 }}>
        <DatePicker
          label="Start Date *"
          value={startDate ? dayjs(startDate) : null}
          onChange={handleStartChange}
          slotProps={{ textField: { fullWidth: true, size: 'small' } }}
        />
      </Grid>
      <Grid size={{ xs: 12, sm: 4 }}>
        <TextField
          label="Tenure (Months)"
          type="number"
          value={tenureMonths}
          onChange={handleTenureChange}
          fullWidth
          inputProps={{ min: 1 }}
        />
      </Grid>
      <Grid size={{ xs: 12, sm: 4 }}>
        <DatePicker
          label="Expiry Date *"
          value={expiryDate ? dayjs(expiryDate) : null}
          onChange={handleExpiryChange}
          minDate={startDate ? dayjs(startDate) : undefined}
          slotProps={{ textField: { fullWidth: true, size: 'small' } }}
        />
      </Grid>
    </Grid>
  );
}
