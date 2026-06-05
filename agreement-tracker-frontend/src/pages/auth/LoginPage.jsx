import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { useForm } from 'react-hook-form';
import {
  Box, TextField, Button, Typography,
  Alert, CircularProgress, InputAdornment, IconButton, alpha,
} from '@mui/material';
import { Visibility, VisibilityOff, LockOutlined, PersonOutlined } from '@mui/icons-material';
import { login, clearError, selectAuthLoading, selectAuthError, selectIsAuthenticated, selectUserRights } from '../../store/slices/authSlice';
import { defaultRouteForRights } from '../../config/rights';
import { BRAND } from '../../config/theme';

export default function LoginPage() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const loading = useSelector(selectAuthLoading);
  const error = useSelector(selectAuthError);
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const rights = useSelector(selectUserRights);
  const [showPassword, setShowPassword] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm();

  useEffect(() => {
    if (isAuthenticated) navigate(defaultRouteForRights(rights));
  }, [isAuthenticated, rights, navigate]);

  useEffect(() => {
    return () => { dispatch(clearError()); };
  }, [dispatch]);

  const onSubmit = (data) => { dispatch(login(data)); };

  return (
    <Box sx={{
      minHeight: '100vh',
      display: 'flex',
      background: BRAND.bgGray,
    }}>
      {/* Left brand panel */}
      <Box sx={{
        display: { xs: 'none', md: 'flex' },
        flex: '0 0 45%',
        background: `linear-gradient(150deg, #8B0000 0%, #C2181D 45%, #E53935 100%)`,
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'flex-start',
        p: 8,
        position: 'relative',
        overflow: 'hidden',
      }}>
        {/* background circles */}
        <Box sx={{
          position: 'absolute', top: -80, right: -80,
          width: 320, height: 320, borderRadius: '50%',
          background: alpha('#fff', 0.06),
        }} />
        <Box sx={{
          position: 'absolute', bottom: -40, left: -60,
          width: 240, height: 240, borderRadius: '50%',
          background: alpha('#fff', 0.05),
        }} />
        <Box sx={{
          position: 'absolute', top: '40%', right: 40,
          width: 120, height: 120, borderRadius: '50%',
          background: alpha('#fff', 0.08),
        }} />

        {/* Logo */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 6 }}>
          <Box sx={{
            width: 52, height: 52, borderRadius: 3,
            bgcolor: alpha('#fff', 0.15), display: 'flex',
            alignItems: 'center', justifyContent: 'center',
            border: `2px solid ${alpha('#fff', 0.3)}`,
            overflow: 'hidden',
          }}>
            <Box component="img" src="/images/medplus_logo.png" alt="MedPlus" sx={{ width: 40, height: 40, objectFit: 'contain' }} />
          </Box>
          <Typography sx={{ color: '#fff', fontWeight: 800, fontSize: 22, letterSpacing: '-0.5px' }}>
            MedPlus
          </Typography>
        </Box>

        <Typography variant="h3" sx={{
          color: '#fff', fontWeight: 800, lineHeight: 1.15,
          mb: 2.5, letterSpacing: '-1px', maxWidth: 360,
        }}>
          Commercial Agreement Tracker
        </Typography>
        <Typography sx={{ color: alpha('#fff', 0.75), fontSize: '1.05rem', maxWidth: 340, lineHeight: 1.7 }}>
          Manage vendor agreements, approvals, and commercial structures — all in one place.
        </Typography>

        <Box sx={{ mt: 6, display: 'flex', flexDirection: 'column', gap: 2 }}>
          {['End-to-end agreement lifecycle', 'Multi-level approval workflow', 'Expiry alerts & reminders'].map((f) => (
            <Box key={f} sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
              <Box sx={{
                width: 8, height: 8, borderRadius: '50%',
                bgcolor: BRAND.greenLight, flexShrink: 0,
              }} />
              <Typography sx={{ color: alpha('#fff', 0.85), fontSize: '0.9rem' }}>{f}</Typography>
            </Box>
          ))}
        </Box>
      </Box>

      {/* Right form panel */}
      <Box sx={{
        flex: 1,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        p: { xs: 3, md: 6 },
      }}>
        <Box sx={{ width: '100%', maxWidth: 420 }}>
          {/* Mobile logo */}
          <Box sx={{ display: { xs: 'flex', md: 'none' }, alignItems: 'center', gap: 1.5, mb: 4 }}>
            <Box sx={{
              width: 44, height: 44, borderRadius: 2, overflow: 'hidden',
            }}>
              <Box component="img" src="/images/medplus_logo.png" alt="MedPlus" sx={{ width: 44, height: 44, objectFit: 'contain' }} />
            </Box>
            <Typography sx={{ fontWeight: 800, fontSize: 18, color: BRAND.textPrimary }}>
              MedPlus — Agreement Tracker
            </Typography>
          </Box>

          <Typography variant="h4" sx={{ mb: 1, color: BRAND.textPrimary }}>
            Welcome back
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
            Sign in to your account to continue
          </Typography>

          {error && (
            <Alert severity="error" sx={{ mb: 3, borderRadius: 2 }} onClose={() => dispatch(clearError())}>
              {error}
            </Alert>
          )}

          <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
            <TextField
              label="Username"
              fullWidth
              autoFocus
              autoComplete="username"
              {...register('username', { required: 'Username is required' })}
              error={!!errors.username}
              helperText={errors.username?.message}
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <PersonOutlined sx={{ color: 'text.secondary', fontSize: 20 }} />
                    </InputAdornment>
                  ),
                },
              }}
            />
            <TextField
              label="Password"
              type={showPassword ? 'text' : 'password'}
              fullWidth
              autoComplete="current-password"
              {...register('password', { required: 'Password is required' })}
              error={!!errors.password}
              helperText={errors.password?.message}
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <LockOutlined sx={{ color: 'text.secondary', fontSize: 20 }} />
                    </InputAdornment>
                  ),
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton onClick={() => setShowPassword((v) => !v)} edge="end" size="small">
                        {showPassword ? <VisibilityOff fontSize="small" /> : <Visibility fontSize="small" />}
                      </IconButton>
                    </InputAdornment>
                  ),
                },
              }}
            />

            <Button
              type="submit"
              fullWidth
              variant="contained"
              size="large"
              disabled={loading}
              sx={{ mt: 1, py: 1.5, fontSize: '0.95rem', borderRadius: 2.5 }}
            >
              {loading ? <CircularProgress size={22} color="inherit" /> : 'Sign In'}
            </Button>
          </Box>

          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 4, textAlign: 'center' }}>
            © {new Date().getFullYear()} MedPlus Healthcare. All rights reserved.
          </Typography>
        </Box>
      </Box>
    </Box>
  );
}
