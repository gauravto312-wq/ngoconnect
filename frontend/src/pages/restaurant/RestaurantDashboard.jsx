import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import { UtensilsCrossed, PlusCircle, Building2, ShoppingBag, Users, CheckCircle2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { PhotoUploadManager } from '../../components/PhotoUploadManager';

export const RestaurantDashboard = () => {
  const { user } = useAuth();
  const [profile, setProfile] = useState(null);
  const [branches, setBranches] = useState([]);
  const [donations, setDonations] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchRestaurantData();
  }, []);

  const fetchRestaurantData = async () => {
    try {
      const [profRes, branchRes, donRes] = await Promise.all([
        api.get('/restaurants/profile'),
        api.get('/restaurants/branches'),
        api.get('/donations/my-donations')
      ]);
      setProfile(profRes.data);
      setBranches(branchRes.data);
      setDonations(donRes.data);
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
          <h1 className="text-2xl font-extrabold text-slate-900">{profile?.restaurantName || user?.name}</h1>
          <p className="text-xs text-slate-500 mt-1">Restaurant Dashboard — Manage branch surplus food recovery & volunteer dispatches.</p>
        </div>

        <div className="flex flex-wrap gap-2">
          <Link
            to="/restaurant/donations"
            className="px-4 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-xs flex items-center gap-1.5 shadow transition"
          >
            <PlusCircle className="w-4 h-4" />
            <span>Post Food Donation</span>
          </Link>
          <Link
            to="/restaurant/branches"
            className="px-4 py-2.5 bg-slate-800 hover:bg-slate-700 text-white font-bold rounded-xl text-xs flex items-center gap-1.5 shadow transition"
          >
            <UtensilsCrossed className="w-4 h-4" />
            <span>Manage Branches</span>
          </Link>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Active Restaurant Branches</div>
          <div className="text-2xl font-extrabold text-slate-900 mt-1">{branches.length}</div>
        </div>
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Food Donations Posted</div>
          <div className="text-2xl font-extrabold text-emerald-600 mt-1">{donations.length}</div>
        </div>
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Claimed by NGOs</div>
          <div className="text-2xl font-extrabold text-indigo-600 mt-1">
            {donations.filter((d) => d.status === 'CLAIMED' || d.status === 'COMPLETED').length}
          </div>
        </div>
      </div>

      {/* Photo Manager */}
      <PhotoUploadManager
        endpoint="/restaurants/photos"
        initialProfileImage={profile?.profileImageUrl}
        initialGalleryPhotos={profile?.galleryPhotos}
        onUpdateSuccess={fetchRestaurantData}
      />

      {/* Recent Donations */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
        <div className="flex justify-between items-center mb-4">
          <h3 className="font-bold text-slate-900 text-lg">Your Posted Surplus Food Donations</h3>
          <Link to="/restaurant/donations" className="text-xs font-bold text-emerald-600 hover:underline">
            View All Donations &rarr;
          </Link>
        </div>

        {loading ? (
          <div className="text-xs text-slate-500 py-6 text-center">Loading donations...</div>
        ) : donations.length === 0 ? (
          <div className="text-xs text-slate-500 py-8 text-center border border-dashed border-slate-200 rounded-xl">
            No food donations posted yet. Click 'Post Food Donation' above to list surplus food packets for nearby NGOs.
          </div>
        ) : (
          <div className="space-y-3">
            {donations.slice(0, 3).map((d) => (
              <div key={d.id} className="p-4 bg-slate-50 rounded-xl border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <h4 className="font-bold text-slate-900 text-sm">{d.title}</h4>
                  <p className="text-xs text-slate-600 mt-0.5">Quantity: {d.quantity} {d.unit} • Branch: {d.branchName || 'Main'}</p>
                  {d.pickupCode && (
                    <div className="mt-2 text-xs font-mono font-bold text-amber-800 bg-amber-100 px-2.5 py-1 rounded inline-block border border-amber-300">
                      Private Pickup Code: {d.pickupCode}
                    </div>
                  )}
                </div>
                <span className={`px-3 py-1 rounded text-xs font-bold self-start sm:self-center ${
                  d.status === 'AVAILABLE' ? 'bg-emerald-100 text-emerald-800' :
                  d.status === 'CLAIMED' ? 'bg-amber-100 text-amber-800' : 'bg-slate-200 text-slate-800'
                }`}>
                  {d.status}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
