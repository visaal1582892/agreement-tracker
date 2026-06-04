import { Box, Typography, alpha } from '@mui/material';
import { TrendingUp, TrendingDown } from '@mui/icons-material';
import { BRAND } from '../../config/theme';

export default function KpiCard({ title, value, icon, color = BRAND.red, subtitle, trend, trendLabel }) {
  const isPositive = trend > 0;
  const isNegative = trend < 0;

  return (
    <Box sx={{
      bgcolor: '#fff',
      borderRadius: 3,
      p: 2.5,
      height: '100%',
      border: `1px solid ${BRAND.borderLight}`,
      position: 'relative',
      overflow: 'hidden',
      transition: 'box-shadow 0.2s ease, transform 0.2s ease',
      '&:hover': {
        boxShadow: BRAND.shadowMd,
        transform: 'translateY(-2px)',
      },
    }}>
      {/* Subtle top accent bar */}
      <Box sx={{
        position: 'absolute', top: 0, left: 0, right: 0,
        height: 3, bgcolor: color, borderRadius: '3px 3px 0 0',
      }} />

      <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="caption" sx={{
          color, fontWeight: 700,
          textTransform: 'uppercase', letterSpacing: '0.06em', fontSize: '0.7rem',
        }}>
          {title}
        </Typography>
        {icon && (
          <Box sx={{
            width: 38, height: 38, borderRadius: 2,
            bgcolor: alpha(color, 0.1),
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            color,
          }}>
            <Box sx={{ fontSize: 20, display: 'flex' }}>{icon}</Box>
          </Box>
        )}
      </Box>

      <Typography sx={{
        fontSize: '2rem', fontWeight: 800, color: BRAND.textPrimary,
        lineHeight: 1, mb: 1, letterSpacing: '-1px',
      }}>
        {value ?? '—'}
      </Typography>

      {(subtitle || trend !== undefined) && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mt: 1 }}>
          {trend !== undefined && (
            <Box sx={{
              display: 'flex', alignItems: 'center', gap: 0.3,
              color: isPositive ? 'success.main' : isNegative ? 'error.main' : 'text.secondary',
              fontSize: '0.75rem', fontWeight: 600,
            }}>
              {isPositive ? <TrendingUp sx={{ fontSize: 14 }} /> : isNegative ? <TrendingDown sx={{ fontSize: 14 }} /> : null}
              {Math.abs(trend)}%
            </Box>
          )}
          {subtitle && (
            <Typography variant="caption" color="text.secondary">
              {trendLabel || subtitle}
            </Typography>
          )}
        </Box>
      )}
    </Box>
  );
}
