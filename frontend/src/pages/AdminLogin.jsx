import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, LogIn, AlertCircle } from 'lucide-react';

export const AdminLogin = () => {
  const { adminLogin, loading } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await adminLogin(email.trim(), password);
      navigate('/admin');
    } catch (err) {
      const data = err.response?.data;
      let msg = data?.message;
      if ((!msg || msg === 'Validation failed for request parameters') && data?.validationErrors) {
        msg = Object.values(data.validationErrors).join(', ');
      }
      setError(msg || 'Access denied: Invalid administrator credentials.');
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-slate-800 rounded-2xl border border-slate-700 shadow-2xl p-8 text-slate-100">
        <div className="text-center mb-8">
          <div className="w-14 h-14 rounded-2xl bg-indigo-600 text-white flex items-center justify-center mx-auto mb-3 shadow-lg">
            <ShieldCheck className="w-8 h-8" />
          </div>
          <h2 className="text-2xl font-extrabold text-white tracking-tight">Admin Login Portal</h2>
          <p className="text-xs text-slate-400 mt-1">Authorized Administrator Portal Access Only</p>
        </div>

        {error && (
          <div className="mb-6 p-3.5 bg-rose-950/80 border border-rose-800/80 rounded-lg text-xs text-rose-300 flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-400 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4 text-xs">
          <div>
            <label className="block text-slate-300 font-semibold mb-1">Admin Email</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="admin@ngoconnect.org"
              className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-700 text-white rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none text-sm"
            />
          </div>

          <div>
            <label className="block text-slate-300 font-semibold mb-1">Admin Security Password</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              className="w-full px-3.5 py-2.5 bg-slate-900 border border-slate-700 text-white rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none text-sm"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 bg-indigo-600 hover:bg-indigo-500 text-white font-bold rounded-lg shadow-lg transition flex items-center justify-center space-x-2 text-sm mt-2"
          >
            <LogIn className="w-4 h-4" />
            <span>{loading ? 'Verifying Admin Token...' : 'Authenticate Admin'}</span>
          </button>
        </form>

        <div className="mt-8 pt-6 border-t border-slate-700 text-center text-xs text-slate-400">
          <Link to="/login" className="hover:text-white transition">
            &larr; Return to Standard User Login
          </Link>
        </div>
      </div>
    </div>
  );
};
