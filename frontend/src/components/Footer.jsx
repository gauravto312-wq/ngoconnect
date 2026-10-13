import React from 'react';
import { HeartHandshake } from 'lucide-react';
import { Link } from 'react-router-dom';

export const Footer = () => {
  return (
    <footer className="bg-slate-900 text-slate-400 border-t border-slate-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-8">
          <div className="space-y-4">
            <div className="flex items-center space-x-2">
              <div className="w-8 h-8 rounded-lg bg-emerald-600 flex items-center justify-center text-white font-bold">
                <HeartHandshake className="w-5 h-5" />
              </div>
              <span className="text-lg font-bold text-white tracking-tight">NGO<span className="text-emerald-500">Connect</span></span>
            </div>
            <p className="text-xs text-slate-400 leading-relaxed">
              Connecting Citizens, Volunteers, NGOs, and Restaurants for surplus food recovery, item donations, and community food drives.
            </p>
          </div>

          <div>
            <h4 className="text-sm font-semibold text-slate-200 mb-4">Platform Roles</h4>
            <ul className="space-y-2 text-xs">
              <li><Link to="/register?role=CITIZEN" className="hover:text-emerald-400">Citizens</Link></li>
              <li><Link to="/register?role=VOLUNTEER" className="hover:text-emerald-400">Volunteers</Link></li>
              <li><Link to="/register?role=NGO" className="hover:text-emerald-400">NGOs & Organizations</Link></li>
              <li><Link to="/register?role=RESTAURANT" className="hover:text-emerald-400">Restaurants & Donors</Link></li>
            </ul>
          </div>

          <div>
            <h4 className="text-sm font-semibold text-slate-200 mb-4">Quick Links</h4>
            <ul className="space-y-2 text-xs">
              <li><Link to="/marketplace" className="hover:text-emerald-400">Donation Marketplace</Link></li>
              <li><Link to="/about" className="hover:text-emerald-400">About NGOConnect</Link></li>
              <li><Link to="/contact" className="hover:text-emerald-400">Contact & Support</Link></li>
              <li><Link to="/admin/login" className="hover:text-emerald-400">Admin Login Portal</Link></li>
            </ul>
          </div>
        </div>

        <div className="pt-8 border-t border-slate-800 text-center text-xs text-slate-500">
          &copy; {new Date().getFullYear()} NGOConnect Platform. All rights reserved. Built for major project demonstration.
        </div>
      </div>
    </footer>
  );
};
