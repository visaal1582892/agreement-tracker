import { useState } from 'react';
import { Box, Button, TextField, Chip, Typography, CircularProgress, Alert } from '@mui/material';
import { ContentPaste } from '@mui/icons-material';
import axiosInstance from '../../api/axiosInstance';
import { ENDPOINTS } from '../../config/endpoints';

export default function BulkVendorInput({ selectedVendors, onChange }) {
  const [bulkMode, setBulkMode] = useState(false);
  const [pasteText, setPasteText] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleBulkResolve = async () => {
    setLoading(true);
    setError('');
    try {
      const ids = pasteText.split(',').map((s) => s.trim()).filter(Boolean);
      const { data } = await axiosInstance.get(ENDPOINTS.VENDORS, {
        params: { ids: ids.join(',') },
      });
      const resolved = data.filter((v) => ids.includes(v.vendorCode) || ids.includes(String(v.id)));
      onChange(resolved);
      setBulkMode(false);
      setPasteText('');
    } catch {
      setError('Could not resolve vendor IDs. Check and retry.');
    } finally {
      setLoading(false);
    }
  };

  const removeVendor = (id) => onChange(selectedVendors.filter((v) => v.id !== id));

  return (
    <Box>
      <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
        <Typography variant="subtitle2" sx={{ alignSelf: 'center' }}>
          Selected Vendors ({selectedVendors.length})
        </Typography>
        <Button
          size="small"
          variant="outlined"
          startIcon={<ContentPaste />}
          onClick={() => setBulkMode((v) => !v)}
        >
          {bulkMode ? 'Cancel Bulk' : 'Bulk Paste IDs'}
        </Button>
      </Box>

      {bulkMode && (
        <Box sx={{ mb: 2 }}>
          <TextField
            multiline
            rows={3}
            fullWidth
            placeholder="Paste comma-separated Vendor IDs, e.g. V001, V002, V003"
            value={pasteText}
            onChange={(e) => setPasteText(e.target.value)}
            size="small"
          />
          {error && <Alert severity="error" sx={{ mt: 1 }}>{error}</Alert>}
          <Button
            variant="contained"
            size="small"
            sx={{ mt: 1 }}
            onClick={handleBulkResolve}
            disabled={!pasteText.trim() || loading}
          >
            {loading ? <CircularProgress size={16} /> : 'Resolve Vendors'}
          </Button>
        </Box>
      )}

      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
        {selectedVendors.map((v) => (
          <Chip key={v.id} label={v.vendorName} onDelete={() => removeVendor(v.id)} size="small" />
        ))}
      </Box>
    </Box>
  );
}
