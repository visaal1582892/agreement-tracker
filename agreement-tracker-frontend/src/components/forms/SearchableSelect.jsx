import { Autocomplete, TextField, CircularProgress } from '@mui/material';

export default function SearchableSelect({
  label, options = [], value, onChange, multiple = false,
  loading = false, required = false, disabled = false,
  getOptionLabel = (o) => o.label || o.name || String(o),
  isOptionEqualToValue = (o, v) => o.id === v.id,
  placeholder,
  error,
  helperText,
}) {
  return (
    <Autocomplete
      multiple={multiple}
      options={options}
      value={value}
      onChange={(_e, newVal) => onChange(newVal)}
      getOptionLabel={getOptionLabel}
      isOptionEqualToValue={isOptionEqualToValue}
      loading={loading}
      disabled={disabled}
      renderInput={({ InputProps: inputProps, ...params }) => (
        <TextField
          {...params}
          label={label}
          required={required}
          placeholder={placeholder}
          error={!!error}
          helperText={helperText}
          slotProps={{
            input: {
              ...inputProps,
              endAdornment: (
                <>
                  {loading && <CircularProgress size={16} />}
                  {inputProps?.endAdornment}
                </>
              ),
            },
          }}
        />
      )}
    />
  );
}
