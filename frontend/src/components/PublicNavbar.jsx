import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { HeartHandshake, LogIn, UserPlus, ShieldCheck, ShoppingBag, LayoutDashboard, LogOut, Building2, CalendarHeart } from 'lucide-react';

export const PublicNavbar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <nav className="bg-white border-b border-slate-200 sticky top-0 z-50 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <Link to="/" className="flex items-center space-x-2">
          <div className="w-10 h-10 rounded-lg bg-emerald-600 flex items-center justify-center text-white font-bold text-xl shadow-md">
            <HeartHandshake className="w-6 h-6" />
          </div>
          <span className="text-xl font-extrabold text-slate-800 tracking-tight">NGO<span className="text-emerald-600">Connect</span></span>
        </Link>

        <div className="hidden md:flex items-center space-x-6 text-sm font-medium text-slate-600">
          <Link to="/" className="hover:text-emerald-600 transition-colors">Home</Link>
          <Link to="/ngos" className="hover:text-emerald-600 transition-colors flex items-center gap-1">
            <HeartHandshake className="w-4 h-4 text-emerald-600" /> NGOs
          </Link>
          <Link to="/restaurants" className="hover:text-emerald-600 transition-colors flex items-center gap-1">
            <Building2 className="w-4 h-4 text-emerald-600" /> Restaurants
          </Link>
          <Link to="/marketplace" className="hover:text-emerald-600 transition-colors flex items-center gap-1.5">
            <ShoppingBag className="w-4 h-4 text-emerald-600" /> Marketplace
          </Link>
          <Link to="/food-drives" className="hover:text-emerald-600 transition-colors flex items-center gap-1.5">
            <CalendarHeart className="w-4 h-4 text-emerald-600" /> Food Drives
          </Link>
          <Link to="/about" className="hover:text-emerald-600 transition-colors">About Us</Link>
          <Link to="/contact" className="hover:text-emerald-600 transition-colors">Contact</Link>
        </div>

        <div className="flex items-center space-x-3">
          {user ? (
            <>
              <Link
                to={`/${user.role.toLowerCase()}`}
                className="px-4 py-2 text-sm font-semibold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 rounded-lg border border-emerald-200 flex items-center gap-1.5 transition"
              >
                <LayoutDashboard className="w-4 h-4" /> Dashboard ({user.role})
              </Link>
              <button
                onClick={() => { logout(); navigate('/'); }}
                className="p-2 text-slate-500 hover:text-red-600 rounded-lg transition"
                title="Logout"
              >
                <LogOut className="w-5 h-5" />
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="px-4 py-2 text-sm font-medium text-slate-700 hover:text-emerald-600 flex items-center gap-1.5 transition"
              >
                <LogIn className="w-4 h-4" /> Login
              </Link>
              <Link
                to="/register"
                className="px-4 py-2 text-sm font-semibold text-white bg-emerald-600 hover:bg-emerald-700 rounded-lg shadow-sm flex items-center gap-1.5 transition"
              >
                <UserPlus className="w-4 h-4" /> Join Profile
              </Link>
              <Link
                to="/admin/login"
                className="p-2 text-slate-400 hover:text-indigo-600 transition"
                title="Admin Portal"
              >
                <ShieldCheck className="w-5 h-5" />
              </Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
};
