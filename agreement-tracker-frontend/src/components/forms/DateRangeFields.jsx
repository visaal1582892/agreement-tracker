import { Box, Grid, MenuItem, Select, TextField, Typography } from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs from 'dayjs';
import { BRAND } from '../../config/theme';

export const TENURE_UNITS = {
  DAYS: 'DAYS',
  MONTHS: 'MONTHS',
  YEARS: 'YEARS',
};

const UNIT_LABELS = {
  [TENURE_UNITS.DAYS]: 'Days',
  [TENURE_UNITS.MONTHS]: 'Months',
  [TENURE_UNITS.YEARS]: 'Years',
};

const toDay = (d) => dayjs(d).startOf('day');

export function isLastDayOfMonth(date) {
  const d = toDay(date);
  return d.isValid() && d.date() === d.endOf('month').date();
}

/** Inclusive tenure: expiry is last day of the covered period. */
export function calculateExpiryFromTenure(startDate, tenureValue, tenureUnit = TENURE_UNITS.MONTHS) {
  const start = toDay(startDate);
  const amount = Number(tenureValue);
  if (!start.isValid() || !amount || amount < 1) return null;

  const endOfMonthStart = isLastDayOfMonth(start);

  if (tenureUnit === TENURE_UNITS.DAYS) {
    return start.add(amount - 1, 'day');
  }

  if (tenureUnit === TENURE_UNITS.MONTHS) {
    const target = start.add(amount, 'month');
    if (endOfMonthStart) return target.endOf('month').startOf('day');
    return target.subtract(1, 'day');
  }

  if (tenureUnit === TENURE_UNITS.YEARS) {
    const target = start.add(amount, 'year');
    if (endOfMonthStart) return target.endOf('month').startOf('day');
    return target.subtract(1, 'day');
  }

  return null;
}

export function calculateTenureFromDates(startDate, expiryDate) {
  const start = toDay(startDate);
  const expiry = toDay(expiryDate);
  if (!start.isValid() || !expiry.isValid() || expiry.isBefore(start)) {
    return { tenureValue: '', tenureUnit: TENURE_UNITS.MONTHS };
  }

  for (let years = expiry.add(1, 'day').diff(start, 'year'); years >= 1; years -= 1) {
    const computed = calculateExpiryFromTenure(start, years, TENURE_UNITS.YEARS);
    if (computed?.isSame(expiry, 'day')) {
      return { tenureValue: String(years), tenureUnit: TENURE_UNITS.YEARS };
    }
  }

  const monthCount = expiry.add(1, 'day').diff(start, 'month');
  if (monthCount >= 1 && monthCount <= 11) {
    const computed = calculateExpiryFromTenure(start, monthCount, TENURE_UNITS.MONTHS);
    if (computed?.isSame(expiry, 'day')) {
      return { tenureValue: String(monthCount), tenureUnit: TENURE_UNITS.MONTHS };
    }
  }

  const days = expiry.add(1, 'day').diff(start, 'day');
  return { tenureValue: String(days), tenureUnit: TENURE_UNITS.DAYS };
}

export function formatTenureDisplay(tenureValue, tenureUnit) {
  if (!tenureValue) return '';
  const unit = UNIT_LABELS[tenureUnit] || UNIT_LABELS[TENURE_UNITS.MONTHS];
  return `${tenureValue} ${unit}`;
}

export default function DateRangeFields({
  startDate,
  expiryDate,
  tenureValue,
  tenureUnit = TENURE_UNITS.MONTHS,
  onChange,
}) {
  const emit = (patch) => {
    onChange({
      startDate: patch.startDate !== undefined ? patch.startDate : (startDate ?? null),
      expiryDate: patch.expiryDate !== undefined ? patch.expiryDate : (expiryDate ?? null),
      tenureValue: patch.tenureValue !== undefined ? patch.tenureValue : (tenureValue ?? ''),
      tenureUnit: patch.tenureUnit !== undefined ? patch.tenureUnit : (tenureUnit ?? TENURE_UNITS.MONTHS),
    });
  };

  const toIso = (d) => {
    if (!d) return null;
    const parsed = dayjs.isDayjs(d) ? d : dayjs(d);
    return parsed.isValid() ? parsed.toISOString() : null;
  };

  const hasTenure = tenureValue !== '' && tenureValue != null && Number(tenureValue) > 0;

  const applyTenureForward = (nextStartIso, nextValue, nextUnit) => {
    if (!nextStartIso || !nextValue || Number(nextValue) < 1) {
      emit({ tenureValue: nextValue, tenureUnit: nextUnit });
      return;
    }
    const nextExpiry = calculateExpiryFromTenure(nextStartIso, nextValue, nextUnit);
    emit({
      startDate: nextStartIso,
      tenureValue: nextValue,
      tenureUnit: nextUnit,
      expiryDate: toIso(nextExpiry),
    });
  };

  const handleStartChange = (date) => {
    const nextStart = date ? date.startOf('day') : null;
    if (!nextStart?.isValid()) {
      emit({ startDate: null, expiryDate: null, tenureValue: '', tenureUnit: TENURE_UNITS.MONTHS });
      return;
    }
    const nextStartIso = toIso(nextStart);
    if (hasTenure) {
      applyTenureForward(nextStartIso, tenureValue, tenureUnit);
      return;
    }
    if (expiryDate) {
      const derived = calculateTenureFromDates(nextStart, expiryDate);
      emit({ startDate: nextStartIso, ...derived });
      return;
    }
    emit({ startDate: nextStartIso });
  };

  const handleTenureValueChange = (e) => {
    const nextValue = e.target.value;
    if (nextValue === '') {
      emit({ tenureValue: '' });
      return;
    }
    if (startDate) {
      applyTenureForward(startDate, nextValue, tenureUnit);
      return;
    }
    emit({ tenureValue: nextValue });
  };

  const handleTenureUnitChange = (e) => {
    const nextUnit = e.target.value;
    if (!hasTenure) {
      emit({ tenureUnit: nextUnit });
      return;
    }
    if (startDate) {
      applyTenureForward(startDate, tenureValue, nextUnit);
      return;
    }
    emit({ tenureUnit: nextUnit });
  };

  const handleExpiryChange = (date) => {
    const nextExpiry = date ? date.startOf('day') : null;
    if (!nextExpiry?.isValid()) {
      emit({ expiryDate: null, tenureValue: '', tenureUnit: TENURE_UNITS.MONTHS });
      return;
    }
    if (startDate && nextExpiry.isBefore(toDay(startDate))) {
      return;
    }
    if (startDate) {
      const derived = calculateTenureFromDates(startDate, nextExpiry);
      emit({ expiryDate: toIso(nextExpiry), ...derived });
      return;
    }
    emit({ expiryDate: toIso(nextExpiry) });
  };

  return (
    <Grid container spacing={2.5}>
      <Grid size={{ xs: 12, md: 4 }}>
        <DatePicker
          label="Start Date *"
          value={startDate ? dayjs(startDate) : null}
          onChange={handleStartChange}
          slotProps={{ textField: { fullWidth: true, size: 'small' } }}
        />
      </Grid>

      <Grid size={{ xs: 12, md: 4 }}>
        <Typography variant="caption" sx={{ display: 'block', mb: 0.5, fontWeight: 600, color: BRAND.textSecondary }}>
          Tenure
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'stretch' }}>
          <TextField
            type="number"
            size="small"
            value={tenureValue ?? ''}
            onChange={handleTenureValueChange}
            placeholder="Value"
            inputProps={{ min: 1 }}
            sx={{
              flex: 1,
              '& .MuiOutlinedInput-root': {
                borderTopRightRadius: 0,
                borderBottomRightRadius: 0,
                '& fieldset': { borderColor: BRAND.borderLight, borderRight: 0 },
              },
            }}
          />
          <Select
            size="small"
            value={tenureUnit || TENURE_UNITS.MONTHS}
            onChange={handleTenureUnitChange}
            sx={{
              minWidth: 112,
              bgcolor: BRAND.white,
              '& .MuiOutlinedInput-root': {
                borderTopLeftRadius: 0,
                borderBottomLeftRadius: 0,
                height: '100%',
              },
              '& .MuiOutlinedInput-notchedOutline': { borderColor: BRAND.borderLight },
            }}
          >
            {Object.entries(UNIT_LABELS).map(([value, label]) => (
              <MenuItem key={value} value={value}>{label}</MenuItem>
            ))}
          </Select>
        </Box>
      </Grid>

      <Grid size={{ xs: 12, md: 4 }}>
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
