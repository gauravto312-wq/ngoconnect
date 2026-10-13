import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import { HeartHandshake, PlusCircle, ShoppingBag, CalendarHeart, Clock, CheckCircle2 } from 'lucide-react';
import { Link } from 'react-router-dom';

export const CitizenDashboard = () => {
  const { user } = useAuth();
  const [myDonations, setMyDonations] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchMyDonations();
  }, []);

  const fetchMyDonations = async () => {
    try {
      const res = await api.get('/donations/my-donations');
      setMyDonations(res.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Welcome, {user?.name}!</h1>
          <p className="text-xs text-slate-500 mt-1">Citizen Dashboard — Post donations & support community food drives.</p>
        </div>
        <Link
          to="/citizen/donations"
          className="px-4 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-xs flex items-center gap-2 shadow transition self-start md:self-auto"
        >
          <PlusCircle className="w-4 h-4" />
          <span>Create New Donation</span>
        </Link>
      </div>

      {/* Quick Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Donations Posted</div>
          <div className="text-2xl font-extrabold text-slate-900 mt-1">{myDonations.length}</div>
        </div>
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Available in Marketplace</div>
          <div className="text-2xl font-extrabold text-emerald-600 mt-1">
            {myDonations.filter((d) => d.status === 'AVAILABLE').length}
          </div>
        </div>
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Claimed / Completed</div>
          <div className="text-2xl font-extrabold text-indigo-600 mt-1">
            {myDonations.filter((d) => d.status === 'CLAIMED' || d.status === 'COMPLETED').length}
          </div>
        </div>
      </div>

      {/* Recent Donations */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
        <h3 className="font-bold text-slate-900 text-lg mb-4">Your Recent Donations</h3>
        {loading ? (
          <div className="text-xs text-slate-500 py-6 text-center">Loading donations...</div>
        ) : myDonations.length === 0 ? (
          <div className="text-xs text-slate-500 py-8 text-center border border-dashed border-slate-200 rounded-xl">
            You haven't posted any donations yet. Click 'Create New Donation' above to get started!
          </div>
        ) : (
          <div className="space-y-3">
            {myDonations.map((d) => (
              <div key={d.id} className="p-4 bg-slate-50 rounded-xl border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="px-2 py-0.5 text-[10px] font-bold bg-emerald-100 text-emerald-800 rounded uppercase">
                      {d.category}
                    </span>
                    <h4 className="font-bold text-slate-900 text-sm">{d.title}</h4>
                  </div>
                  <p className="text-xs text-slate-600">{d.quantity} {d.unit} • Location: {d.city}</p>
                  {d.pickupCode && (
                    <div className="mt-2 text-xs font-mono font-bold text-amber-700 bg-amber-50 px-2.5 py-1 rounded inline-block border border-amber-200">
                      Private Pickup Code: {d.pickupCode}
                    </div>
                  )}
                </div>
                <div className="flex items-center gap-2">
                  <span className={`px-3 py-1 rounded text-xs font-bold ${
                    d.status === 'AVAILABLE' ? 'bg-emerald-100 text-emerald-800' :
                    d.status === 'CLAIMED' ? 'bg-amber-100 text-amber-800' : 'bg-slate-200 text-slate-800'
                  }`}>
                    {d.status}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
