import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { PublicNavbar } from '../components/PublicNavbar';
import { Footer } from '../components/Footer';
import { HeartHandshake, LogIn, AlertCircle } from 'lucide-react';

export const Login = () => {
  const { login, loading } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const user = await login(email.trim(), password);
      navigate(`/${user.role.toLowerCase()}`);
    } catch (err) {
      const data = err.response?.data;
      let msg = data?.message;
      if ((!msg || msg === 'Validation failed for request parameters') && data?.validationErrors) {
        msg = Object.values(data.validationErrors).join(', ');
      }
      setError(msg || 'Invalid email or password credentials.');
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <PublicNavbar />

      <div className="flex-1 flex items-center justify-center p-4">
        <div className="max-w-md w-full bg-white rounded-2xl border border-slate-200 shadow-md p-8">
          <div className="text-center mb-8">
            <div className="w-12 h-12 rounded-xl bg-emerald-600 text-white flex items-center justify-center mx-auto mb-3 shadow">
              <HeartHandshake className="w-7 h-7" />
            </div>
            <h2 className="text-2xl font-extrabold text-slate-900">User Login</h2>
            <p className="text-xs text-slate-500 mt-1">Access Citizen, Volunteer, NGO, or Restaurant Account</p>
          </div>

          {error && (
            <div className="mb-6 p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700 flex items-center gap-2">
              <AlertCircle className="w-4 h-4 text-red-500 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4 text-xs">
            <div>
              <label className="block text-slate-700 font-semibold mb-1">Email Address</label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="your.email@domain.com"
                className="w-full px-3.5 py-2.5 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none text-sm"
              />
            </div>

            <div>
              <label className="block text-slate-700 font-semibold mb-1">Password</label>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full px-3.5 py-2.5 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none text-sm"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg shadow transition flex items-center justify-center space-x-2 text-sm"
            >
              <LogIn className="w-4 h-4" />
              <span>{loading ? 'Authenticating...' : 'Sign In'}</span>
            </button>
          </form>

          <div className="mt-8 pt-6 border-t border-slate-100 flex flex-col space-y-3 text-center text-xs">
            <div>
              Don't have a profile yet?{' '}
              <Link to="/register" className="font-bold text-emerald-600 hover:underline">
                Register New Account
              </Link>
            </div>
            <div>
              Administrative Access?{' '}
              <Link to="/admin/login" className="font-medium text-slate-500 hover:text-indigo-600">
                Admin Login Portal
              </Link>
            </div>
          </div>
        </div>
      </div>

      <Footer />
    </div>
  );
};
