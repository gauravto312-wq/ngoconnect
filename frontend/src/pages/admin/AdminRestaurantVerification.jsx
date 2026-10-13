import React, { useState, useEffect } from 'react';
import api, { getUploadUrl } from '../../services/api';
import { Building2, Check, X, ShieldCheck } from 'lucide-react';

export const AdminRestaurantVerification = () => {
  const [restaurants, setRestaurants] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchRestaurants();
  }, []);

  const fetchRestaurants = async () => {
    setLoading(true);
    try {
      const res = await api.get('/admin/restaurants');
      setRestaurants(res.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateStatus = async (restaurantId, status) => {
    try {
      await api.patch(`/admin/restaurants/${restaurantId}/status`, { status });
      alert(`Restaurant status updated to ${status}!`);
      fetchRestaurants();
    } catch (e) {
      alert('Failed to update restaurant status.');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900">Restaurant Oversight & Verification</h1>
        <p className="text-xs text-slate-500 mt-1">Review restaurant registration profiles and manage verification status.</p>
      </div>

      {loading ? (
        <div className="text-xs text-slate-500 text-center py-12">Loading restaurants...</div>
      ) : restaurants.length === 0 ? (
        <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center text-slate-500 text-xs">
          No registered restaurants found.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {restaurants.map((r) => (
            <div key={r.id} className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-4">
              <div className="flex justify-between items-center border-b border-slate-100 pb-3">
                <h3 className="font-bold text-slate-900 text-lg flex items-center gap-2">
                  <Building2 className="w-5 h-5 text-emerald-600" />
                  <span>{r.restaurantName}</span>
                </h3>
                <span className={`px-2.5 py-0.5 rounded text-[10px] font-bold ${
                  r.verificationStatus === 'APPROVED' ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'
                }`}>
                  {r.verificationStatus}
                </span>
              </div>

              <div className="bg-slate-50 p-3.5 rounded-xl text-xs space-y-2 text-slate-600">
                {r.profileImageUrl && (
                  <div className="flex items-center gap-3 pb-2 border-b border-slate-200">
                    <img src={getUploadUrl(r.profileImageUrl)} alt="Restaurant Profile" className="w-14 h-14 rounded-lg object-cover border" onError={(e) => { e.target.style.display = 'none'; }} />
                    <div>
                      <div className="font-bold text-slate-800">Uploaded Profile Photo</div>
                      <div className="text-[10px] text-slate-400">Available for public verification badge display</div>
                    </div>
                  </div>
                )}
                <div><strong>Contact Phone:</strong> {r.contactNumber}</div>
                <div><strong>Location:</strong> {r.address}, {r.city}, {r.state}</div>
                <div><strong>Description:</strong> {r.description || 'N/A'}</div>
                {r.galleryPhotos && (
                  <div className="pt-2">
                    <strong>Uploaded Gallery Photos:</strong>
                    <div className="flex gap-2 mt-1 overflow-x-auto py-1">
                      {r.galleryPhotos.split(',').map((url, i) => (
                        <img key={i} src={getUploadUrl(url.trim())} alt="Gallery" className="w-12 h-12 rounded object-cover border" onError={(e) => { e.target.style.display = 'none'; }} />
                      ))}
                    </div>
                  </div>
                )}
              </div>

              <div className="flex justify-end space-x-2 pt-2 border-t border-slate-100">
                {r.verificationStatus !== 'REJECTED' && (
                  <button
                    onClick={() => handleUpdateStatus(r.id, 'REJECTED')}
                    className="px-3.5 py-1.5 border border-rose-300 text-rose-700 hover:bg-rose-50 font-bold rounded-lg text-xs transition"
                  >
                    Reject Status
                  </button>
                )}
                {r.verificationStatus !== 'APPROVED' && (
                  <button
                    onClick={() => handleUpdateStatus(r.id, 'APPROVED')}
                    className="px-4 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg text-xs shadow transition"
                  >
                    Approve Restaurant
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
