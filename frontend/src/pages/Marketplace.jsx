import React, { useState, useEffect } from 'react';
import { PublicNavbar } from '../components/PublicNavbar';
import { Footer } from '../components/Footer';
import api from '../services/api';
import { ShoppingBag, Search, MapPin, Tag, Clock, ShieldAlert } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';

export const Marketplace = () => {
  const { user } = useAuth();
  const [donations, setDonations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cityFilter, setCityFilter] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');

  useEffect(() => {
    fetchMarketplace();
  }, [cityFilter, categoryFilter]);

  const fetchMarketplace = async () => {
    setLoading(true);
    try {
      const params = {};
      if (cityFilter) params.city = cityFilter;
      if (categoryFilter) params.category = categoryFilter;

      const res = await api.get('/donations/marketplace', { params });
      setDonations(res.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <PublicNavbar />

      <div className="bg-slate-900 text-white py-10 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight flex items-center gap-2">
              <ShoppingBag className="w-8 h-8 text-emerald-400" />
              <span>Public Donation Marketplace</span>
            </h1>
            <p className="text-xs sm:text-sm text-slate-300 mt-1">
              Browse food, clothes, and essential item donations available for NGO pickup across cities.
            </p>
          </div>

          {!user && (
            <Link
              to="/register?role=NGO"
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-lg shadow transition self-start md:self-auto"
            >
              Are you an NGO? Claim Donations
            </Link>
          )}
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full flex-1">
        {/* Filters */}
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm mb-8 flex flex-col sm:flex-row gap-4 items-center justify-between">
          <div className="flex flex-1 flex-col sm:flex-row gap-4 w-full">
            <div className="relative flex-1">
              <MapPin className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
              <input
                type="text"
                value={cityFilter}
                onChange={(e) => setCityFilter(e.target.value)}
                placeholder="Filter by city (e.g. Lucknow)"
                className="w-full pl-9 pr-3 py-2 text-xs border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
              />
            </div>

            <div className="relative w-full sm:w-48">
              <Tag className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
              <select
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value)}
                className="w-full pl-9 pr-3 py-2 text-xs border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none bg-white"
              >
                <option value="">All Categories</option>
                <option value="FOOD">FOOD</option>
                <option value="CLOTHES">CLOTHES</option>
                <option value="OTHER">OTHER</option>
              </select>
            </div>
          </div>

          <button
            onClick={fetchMarketplace}
            className="w-full sm:w-auto px-4 py-2 bg-slate-800 text-white text-xs font-semibold rounded-lg hover:bg-slate-700 transition"
          >
            Refresh List
          </button>
        </div>

        {/* Security Banner */}
        <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 text-xs text-amber-800 mb-6 flex items-center gap-2">
          <ShieldAlert className="w-4 h-4 text-amber-600 flex-shrink-0" />
          <span>
            <strong>Security Notice:</strong> Private pickup verification codes are securely hidden from public view. Only the donor and authorized claiming NGO can access the code upon claim.
          </span>
        </div>

        {/* Listings */}
        {loading ? (
          <div className="text-center py-12 text-slate-500 text-sm">Loading available donations...</div>
        ) : donations.length === 0 ? (
          <div className="bg-white p-12 rounded-xl border border-slate-200 text-center text-slate-500">
            No active donations found matching your filters.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {donations.map((d) => (
              <div key={d.id} className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm hover:shadow-md transition flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-100 text-emerald-800 uppercase">
                      {d.category}
                    </span>
                    <span className="text-xs font-semibold text-slate-500 flex items-center gap-1">
                      <MapPin className="w-3.5 h-3.5 text-slate-400" />
                      {d.city}, {d.state}
                    </span>
                  </div>

                  <h3 className="font-bold text-slate-900 text-lg mb-2">{d.title}</h3>
                  <p className="text-xs text-slate-600 mb-4 line-clamp-3">{d.description || 'No detailed description provided.'}</p>

                  <div className="bg-slate-50 p-3 rounded-lg text-xs space-y-1 text-slate-600 mb-4">
                    <div><strong>Posted By:</strong> {d.donorName} ({d.donorRole})</div>
                    {d.branchName && <div><strong>Branch:</strong> {d.branchName}</div>}
                    <div><strong>Address:</strong> {d.address}</div>
                    <div className="flex items-center gap-1 text-slate-500 pt-1">
                      <Clock className="w-3.5 h-3.5" />
                      <span>Available until: {d.availabilityEnd ? new Date(d.availabilityEnd).toLocaleString() : 'N/A'}</span>
                    </div>
                  </div>
                </div>

                <div className="pt-4 border-t border-slate-100 flex items-center justify-between text-xs">
                  <div>
                    <span className="text-slate-500">Quantity:</span>{' '}
                    <strong className="text-slate-900 text-sm font-bold">{d.quantity} {d.unit}</strong>
                  </div>

                  {user?.role === 'NGO' ? (
                    <Link
                      to="/ngo/marketplace"
                      className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg shadow-sm transition"
                    >
                      Claim in Dashboard
                    </Link>
                  ) : (
                    <span className="px-2.5 py-1 bg-emerald-50 text-emerald-700 font-bold rounded border border-emerald-200">
                      {d.status}
                    </span>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <Footer />
    </div>
  );
};
