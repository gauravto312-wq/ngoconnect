import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { ShoppingBag, MapPin, CheckCircle2, Clock } from 'lucide-react';

export const NgoMarketplace = () => {
  const [donations, setDonations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cityFilter, setCityFilter] = useState('');
  const [claimingId, setClaimingId] = useState(null);

  useEffect(() => {
    fetchMarketplace();
  }, [cityFilter]);

  const fetchMarketplace = async () => {
    setLoading(true);
    try {
      const params = {};
      if (cityFilter) params.city = cityFilter;

      const res = await api.get('/donations/marketplace', { params });
      setDonations(res.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleClaim = async (donationId) => {
    if (claimingId) return;
    setClaimingId(donationId);
    try {
      const res = await api.post(`/donations/${donationId}/claim`);
      alert(`Donation claimed successfully! Pickup Code: ${res.data.pickupCode}`);
      fetchMarketplace();
    } catch (e) {
      alert(e.response?.data?.message || 'Failed to claim donation.');
    } finally {
      setClaimingId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Donation Marketplace</h1>
          <p className="text-xs text-slate-500 mt-1">Claim available food and item donations in your city.</p>
        </div>

        <div className="flex items-center gap-2">
          <MapPin className="w-4 h-4 text-slate-400" />
          <input
            type="text"
            value={cityFilter}
            onChange={(e) => setCityFilter(e.target.value)}
            placeholder="Filter by city..."
            className="px-3 py-2 text-xs border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none bg-white"
          />
        </div>
      </div>

      {loading ? (
        <div className="text-xs text-slate-500 text-center py-12">Loading available marketplace donations...</div>
      ) : donations.length === 0 ? (
        <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center text-slate-500 text-xs">
          No available donations in the marketplace at this time.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {donations.map((d) => (
            <div key={d.id} className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="px-2.5 py-0.5 rounded text-[11px] font-bold bg-emerald-100 text-emerald-800 uppercase">
                    {d.category}
                  </span>
                  <span className="text-xs font-semibold text-slate-500">{d.city}, {d.state}</span>
                </div>

                <h3 className="font-bold text-slate-900 text-lg mb-2">{d.title}</h3>
                <p className="text-xs text-slate-600 mb-4 line-clamp-3">{d.description || 'No description provided.'}</p>

                <div className="bg-slate-50 p-3.5 rounded-xl text-xs space-y-1.5 text-slate-600 mb-4">
                  <div><strong>Donor:</strong> {d.donorName} ({d.donorRole})</div>
                  {d.branchName && <div><strong>Branch:</strong> {d.branchName}</div>}
                  <div><strong>Address:</strong> {d.address}</div>
                  <div className="text-slate-500 pt-1 flex items-center gap-1">
                    <Clock className="w-3.5 h-3.5" />
                    <span>Available until: {d.availabilityEnd ? new Date(d.availabilityEnd).toLocaleString() : 'N/A'}</span>
                  </div>
                </div>
              </div>

              <div className="pt-4 border-t border-slate-100 flex items-center justify-between">
                <div className="text-xs">
                  <span className="text-slate-500">Qty:</span>{' '}
                  <strong className="text-slate-900 font-bold">{d.quantity} {d.unit}</strong>
                </div>

                <button
                  onClick={() => handleClaim(d.id)}
                  disabled={claimingId === d.id}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-xs shadow transition flex items-center gap-1"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  <span>{claimingId === d.id ? 'Claiming...' : 'Claim Donation'}</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
