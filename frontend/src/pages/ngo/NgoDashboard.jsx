import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import { Building2, ShieldCheck, ShoppingBag, CheckCircle2, Users, AlertCircle } from 'lucide-react';
import { Link } from 'react-router-dom';
import { PhotoUploadManager } from '../../components/PhotoUploadManager';

export const NgoDashboard = () => {
  const { user } = useAuth();
  const [ngoProfile, setNgoProfile] = useState(null);
  const [claimedDonations, setClaimedDonations] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchNgoData();
  }, []);

  const fetchNgoData = async () => {
    try {
      const [profRes, claimRes] = await Promise.all([
        api.get('/ngos/profile'),
        api.get('/donations/claimed')
      ]);
      setNgoProfile(profRes.data);
      setClaimedDonations(claimRes.data);
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
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-extrabold text-slate-900">{ngoProfile?.ngoName || user?.name}</h1>
            <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${
              ngoProfile?.verificationStatus === 'APPROVED' ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
            }`}>
              Verification: {ngoProfile?.verificationStatus}
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-1">NGO Dashboard — Claim marketplace donations & assign active volunteers.</p>
        </div>

        <div className="flex gap-2">
          <Link
            to="/ngo/marketplace"
            className="px-4 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-xs flex items-center gap-2 shadow transition"
          >
            <ShoppingBag className="w-4 h-4" />
            <span>Browse Marketplace</span>
          </Link>
        </div>
      </div>

      {/* Verification Prompt Warning Banner */}
      {ngoProfile?.verificationStatus !== 'APPROVED' && (
        <div className="bg-amber-50 border border-amber-200 p-4 rounded-xl text-xs text-amber-900 flex items-start justify-between gap-3">
          <div className="flex items-start gap-2">
            <AlertCircle className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
            <div>
              <strong>Verification Document Required:</strong> Your NGO account is currently in {ngoProfile?.verificationStatus} status. Please upload government registration certificates to get verified by Admin.
            </div>
          </div>
          <Link to="/ngo/verification" className="px-3 py-1.5 bg-amber-600 text-white font-bold rounded-lg hover:bg-amber-700 flex-shrink-0">
            Upload Docs
          </Link>
        </div>
      )}

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Donations Claimed</div>
          <div className="text-2xl font-extrabold text-slate-900 mt-1">{claimedDonations.length}</div>
        </div>
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Pending Pickup</div>
          <div className="text-2xl font-extrabold text-amber-600 mt-1">
            {claimedDonations.filter((d) => d.status === 'CLAIMED').length}
          </div>
        </div>
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Completed Handovers</div>
          <div className="text-2xl font-extrabold text-emerald-600 mt-1">
            {claimedDonations.filter((d) => d.status === 'COMPLETED').length}
          </div>
        </div>
      </div>

      {/* Photo Manager Component */}
      <PhotoUploadManager
        endpoint="/ngos/photos"
        initialProfileImage={ngoProfile?.profileImageUrl}
        initialGalleryPhotos={ngoProfile?.galleryPhotos}
        onUpdateSuccess={fetchNgoData}
      />

      {/* Active Claimed Donations */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
        <div className="flex justify-between items-center mb-4">
          <h3 className="font-bold text-slate-900 text-lg">Your Claimed Donations</h3>
          <Link to="/ngo/claimed-donations" className="text-xs font-bold text-emerald-600 hover:underline">
            Manage All Claims &rarr;
          </Link>
        </div>

        {loading ? (
          <div className="text-xs text-slate-500 py-6 text-center">Loading claimed donations...</div>
        ) : claimedDonations.length === 0 ? (
          <div className="text-xs text-slate-500 py-8 text-center border border-dashed border-slate-200 rounded-xl">
            No donations claimed yet. Visit the Marketplace to claim available surplus food & item donations!
          </div>
        ) : (
          <div className="space-y-3">
            {claimedDonations.slice(0, 3).map((d) => (
              <div key={d.id} className="p-4 bg-slate-50 rounded-xl border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <h4 className="font-bold text-slate-900 text-sm">{d.title}</h4>
                  <p className="text-xs text-slate-600 mt-0.5">Donor: {d.donorName} ({d.donorRole}) • Location: {d.city}</p>
                  {d.pickupCode && (
                    <div className="mt-2 text-xs font-mono font-bold text-amber-800 bg-amber-100 px-2.5 py-1 rounded inline-block border border-amber-300">
                      Private Pickup Code: {d.pickupCode}
                    </div>
                  )}
                </div>
                <div className="flex items-center gap-2">
                  <span className={`px-3 py-1 rounded text-xs font-bold ${
                    d.status === 'CLAIMED' ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-800'
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
