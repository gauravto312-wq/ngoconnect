import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { ShieldCheck, Users, Building2, HeartHandshake, CalendarHeart, Truck, CheckCircle2, AlertTriangle, Mail } from 'lucide-react';
import { Link } from 'react-router-dom';

export const AdminDashboard = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAdminStats();
  }, []);

  const fetchAdminStats = async () => {
    try {
      const res = await api.get('/admin/stats');
      setStats(res.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="bg-slate-900 text-white p-6 rounded-2xl border border-slate-800 shadow-md flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-7 h-7 text-indigo-400" />
            <h1 className="text-2xl font-extrabold tracking-tight">Admin Portal Dashboard</h1>
          </div>
          <p className="text-xs text-slate-300 mt-1">Full platform activity, user role oversight & verification management.</p>
        </div>

        <div className="text-xs bg-slate-800 px-3 py-1.5 rounded-lg border border-slate-700 font-mono text-emerald-400 font-bold">
          ADMIN AUTHORIZED
        </div>
      </div>

      {loading ? (
        <div className="text-xs text-slate-500 text-center py-12">Loading admin metrics...</div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase">Total Users</span>
              <Users className="w-5 h-5 text-indigo-600" />
            </div>
            <div className="text-3xl font-extrabold text-slate-900 mt-2">{stats?.totalUsers || 0}</div>
            <div className="text-[11px] text-slate-500 mt-1">
              Citizens: {stats?.totalCitizens} • Volunteers: {stats?.totalVolunteers}
            </div>
          </div>

          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase">Active Volunteers</span>
              <Truck className="w-5 h-5 text-emerald-600" />
            </div>
            <div className="text-3xl font-extrabold text-emerald-600 mt-2">{stats?.activeVolunteers || 0}</div>
            <div className="text-[11px] text-slate-500 mt-1">Available for task dispatches</div>
          </div>

          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase">NGO Verifications</span>
              <Building2 className="w-5 h-5 text-amber-600" />
            </div>
            <div className="text-3xl font-extrabold text-amber-600 mt-2">{stats?.pendingNgoVerifications || 0}</div>
            <Link to="/admin/ngo-verifications" className="text-[11px] font-bold text-amber-700 hover:underline block mt-1">
              Pending Admin Review &rarr;
            </Link>
          </div>

          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase">Total Donations</span>
              <HeartHandshake className="w-5 h-5 text-emerald-600" />
            </div>
            <div className="text-3xl font-extrabold text-slate-900 mt-2">{stats?.totalDonations || 0}</div>
            <div className="text-[11px] text-slate-500 mt-1">
              Available: {stats?.availableDonations} • Claimed: {stats?.claimedDonations}
            </div>
          </div>
        </div>
      )}

      {/* Quick Action Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <Link
          to="/admin/ngo-verifications"
          className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition space-y-2 block"
        >
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-slate-900 text-base">NGO Verification Queue</h3>
            <span className="px-2.5 py-0.5 rounded text-xs font-bold bg-amber-100 text-amber-800">
              {stats?.pendingNgoVerifications || 0} Pending
            </span>
          </div>
          <p className="text-xs text-slate-600">Review uploaded 80G/12A certificates & approve NGO profiles.</p>
        </Link>

        <Link
          to="/admin/restaurant-verifications"
          className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition space-y-2 block"
        >
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-slate-900 text-base">Restaurant Oversight</h3>
            <span className="px-2.5 py-0.5 rounded text-xs font-bold bg-indigo-100 text-indigo-800">
              {stats?.totalRestaurants || 0} Total
            </span>
          </div>
          <p className="text-xs text-slate-600">Oversight & status verification for restaurant accounts.</p>
        </Link>

        <Link
          to="/admin/users"
          className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition space-y-2 block"
        >
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-slate-900 text-base">User Management</h3>
            <span className="px-2.5 py-0.5 rounded text-xs font-bold bg-slate-100 text-slate-800">
              {stats?.totalUsers || 0} Accounts
            </span>
          </div>
          <p className="text-xs text-slate-600">View user roles, manage account active/inactive statuses.</p>
        </Link>

        <Link
          to="/admin/disputes"
          className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition space-y-2 block"
        >
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-slate-900 text-base flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-amber-500" />
              <span>Payment Disputes</span>
            </h3>
            <span className="px-2.5 py-0.5 rounded text-xs font-bold bg-amber-100 text-amber-800">
              Arbitration
            </span>
          </div>
          <p className="text-xs text-slate-600">Arbitrate payment conflicts reported between volunteers and organizers.</p>
        </Link>

        <Link
          to="/admin/inquiries"
          className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition space-y-2 block"
        >
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-slate-900 text-base flex items-center gap-2">
              <Mail className="w-4 h-4 text-indigo-500" />
              <span>Contact Inquiries</span>
            </h3>
            <span className="px-2.5 py-0.5 rounded text-xs font-bold bg-indigo-100 text-indigo-800">
              Inquiries
            </span>
          </div>
          <p className="text-xs text-slate-600">Review public contact inquiries, citizen messages and partnership requests.</p>
        </Link>
      </div>
    </div>
  );
};
