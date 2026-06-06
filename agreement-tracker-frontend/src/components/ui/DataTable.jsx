import {
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  TablePagination, TableSortLabel, Paper, Box, Typography,
  CircularProgress, TextField, InputAdornment, MenuItem, Select,
  FormControl,
} from '@mui/material';
import { Search } from '@mui/icons-material';
import { BRAND } from '../../config/theme';
import SearchableSelect from '../forms/SearchableSelect';

/**
 * Reusable DataTable with:
 *  - Server-side pagination  (page / rowsPerPage / totalCount / onPageChange)
 *  - Server-side sorting     (sortBy / sortDir / onSort)
 *  - Column-level filter row (column.filterType: 'text' | 'select' | 'searchable-select' | none)
 *  - Top global search       (optional)
 *
 * Column definition shape:
 *   {
 *     field: string,           // data key
 *     header: string,          // display label
 *     width?: string | number,
 *     sortable?: boolean,       // default true
 *     filterType?: 'text' | 'select' | 'searchable-select' | null,
 *     filterOptions?: [{ value, label } | object],  // for 'select' / 'searchable-select'
 *     onFilterSearch?: (query: string) => void,
 *     filterLoading?: boolean,
 *     getOptionLabel?: (option) => string,
 *     filterKey?: string,       // override filter key (default = field)
 *     render?: (value, row) => ReactNode,
 *   }
 */
export default function DataTable({
  columns,
  rows,
  loading,
  totalCount,
  page,
  rowsPerPage,
  onPageChange,
  onRowsPerPageChange,
  onRowClick,
  sortBy,
  sortDir,
  onSort,
  filters = {},
  onFilterChange,
  searchValue,
  onSearch,
  rowsPerPageOptions = [10, 20, 50],
  emptyMessage = 'No records found',
  stickyHeader = true,
}) {
  const hasFilters = columns.some((c) => c.filterType);

  const resolveSearchableValue = (col, key) => {
    const raw = filters[key];
    if (!raw) return null;
    const options = col.filterOptions || [];
    return options.find((o) => String(o.id) === String(raw)) ?? { id: raw };
  };

  return (
    <Box>
      {onSearch !== undefined && (
        <Box sx={{ mb: 2 }}>
          <TextField
            size="small"
            placeholder="Search…"
            value={searchValue}
            onChange={(e) => onSearch(e.target.value)}
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <Search fontSize="small" />
                  </InputAdornment>
                ),
              },
            }}
            sx={{ width: 280 }}
          />
        </Box>
      )}

      <TableContainer
        component={Paper}
        elevation={0}
        sx={{ borderRadius: 2, border: '1px solid', borderColor: 'divider', overflow: 'auto' }}
      >
        <Table size="small" stickyHeader={stickyHeader}>
          <TableHead>
            {/* ── Column Headers ── */}
            <TableRow>
              {columns.map((col) => (
                <TableCell
                  key={col.field}
                  width={col.width}
                  sx={{ fontWeight: 600, whiteSpace: 'nowrap', bgcolor: BRAND.bgGray }}
                >
                  {col.sortable !== false && onSort ? (
                    <TableSortLabel
                      active={sortBy === col.field}
                      direction={sortBy === col.field ? sortDir : 'asc'}
                      onClick={() => onSort(col.field)}
                    >
                      {col.header}
                    </TableSortLabel>
                  ) : (
                    col.header
                  )}
                </TableCell>
              ))}
            </TableRow>

            {/* ── Filter Row ── */}
            {hasFilters && onFilterChange && (
              <TableRow>
                {columns.map((col) => {
                  const key = col.filterKey || col.field;
                  return (
                    <TableCell
                      key={col.field}
                      sx={{ py: 0.5, px: 1, bgcolor: '#fff', borderBottom: `1px solid ${BRAND.borderLight}` }}
                    >
                      {col.filterType === 'text' && (
                        <TextField
                          size="small"
                          variant="outlined"
                          placeholder={`Filter ${col.header}`}
                          value={filters[key] ?? ''}
                          onChange={(e) => onFilterChange(key, e.target.value)}
                          sx={{
                            '& .MuiOutlinedInput-root': { fontSize: '0.78rem', borderRadius: 1.5 },
                            width: '100%',
                            minWidth: 80,
                          }}
                        />
                      )}
                      {col.filterType === 'select' && (
                        <FormControl size="small" fullWidth>
                          <Select
                            value={filters[key] ?? ''}
                            onChange={(e) => onFilterChange(key, e.target.value)}
                            displayEmpty
                            sx={{ fontSize: '0.78rem', borderRadius: 1.5 }}
                          >
                            <MenuItem value="">
                              <em>All</em>
                            </MenuItem>
                            {(col.filterOptions || []).map((opt) => (
                              <MenuItem key={opt.value} value={opt.value}>
                                {opt.label}
                              </MenuItem>
                            ))}
                          </Select>
                        </FormControl>
                      )}
                      {col.filterType === 'searchable-select' && (
                        <SearchableSelect
                          value={resolveSearchableValue(col, key)}
                          onChange={(opt) => onFilterChange(key, opt?.id ?? '')}
                          options={col.filterOptions || []}
                          onSearch={col.onFilterSearch}
                          loading={col.filterLoading}
                          getOptionLabel={col.getOptionLabel}
                          placeholder={`Filter ${col.header}`}
                          disabled={false}
                        />
                      )}
                    </TableCell>
                  );
                })}
              </TableRow>
            )}
          </TableHead>

          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={columns.length} align="center" sx={{ py: 6 }}>
                  <CircularProgress size={28} color="primary" />
                </TableCell>
              </TableRow>
            ) : rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length} align="center" sx={{ py: 6 }}>
                  <Typography variant="body2" color="text.secondary">
                    {emptyMessage}
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              rows.map((row, idx) => (
                <TableRow
                  key={row.id ?? idx}
                  hover={!!onRowClick}
                  onClick={onRowClick ? () => onRowClick(row) : undefined}
                  sx={{ cursor: onRowClick ? 'pointer' : 'default' }}
                >
                  {columns.map((col) => (
                    <TableCell key={col.field} sx={{ py: 1.2 }}>
                      {col.render ? col.render(row[col.field], row) : row[col.field]}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {totalCount !== undefined && (
        <TablePagination
          component="div"
          count={totalCount}
          page={page}
          rowsPerPage={rowsPerPage}
          onPageChange={onPageChange}
          onRowsPerPageChange={onRowsPerPageChange}
          rowsPerPageOptions={rowsPerPageOptions}
          sx={{ borderTop: `1px solid ${BRAND.borderLight}` }}
        />
      )}
    </Box>
  );
}
