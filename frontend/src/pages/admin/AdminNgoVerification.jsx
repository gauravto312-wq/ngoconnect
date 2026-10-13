import React, { useState, useEffect } from 'react';
import api, { getUploadUrl } from '../../services/api';
import { ShieldCheck, Check, X, FileText, ExternalLink, Building, HeartHandshake, RefreshCw } from 'lucide-react';

export const AdminNgoVerification = () => {
  const [activeTab, setActiveTab] = useState('pending'); // 'pending' | 'all'
  const [verifications, setVerifications] = useState([]);
  const [allNgos, setAllNgos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [reviewingId, setReviewingId] = useState(null);
  const [remarks, setRemarks] = useState('');

  useEffect(() => {
    if (activeTab === 'pending') {
      fetchPending();
    } else {
      fetchAllNgos();
    }
  }, [activeTab]);

  const fetchPending = async () => {
    setLoading(true);
    try {
      const res = await api.get('/admin/verifications/ngo/pending');
      setVerifications(res.data || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const fetchAllNgos = async () => {
    setLoading(true);
    try {
      const res = await api.get('/admin/ngos');
      setAllNgos(res.data || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleReview = async (verificationId, status) => {
    try {
      await api.post(`/admin/verifications/ngo/${verificationId}/review`, {
        status,
        remarks: remarks || `Reviewed and ${status.toLowerCase()} by Admin.`
      });
      alert(`NGO Verification ${status} successfully!`);
      setReviewingId(null);
      setRemarks('');
      fetchPending();
    } catch (e) {
      alert('Failed to review NGO verification.');
    }
  };

  const handleUpdateNgoStatus = async (ngoId, status) => {
    try {
      await api.patch(`/admin/ngos/${ngoId}/status`, { status });
      alert(`NGO verification status updated to ${status}!`);
      fetchAllNgos();
    } catch (e) {
      alert('Failed to update NGO status.');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">NGO Verification & Directory Oversight</h1>
          <p className="text-xs text-slate-500 mt-1">
            Review government registration documents and manage verification statuses across all registered non-profits.
          </p>
        </div>

        <button
          onClick={activeTab === 'pending' ? fetchPending : fetchAllNgos}
          disabled={loading}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-white border border-slate-200 rounded-xl text-xs font-bold text-slate-700 hover:bg-slate-50 shadow-sm transition self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Navigation Tabs */}
      <div className="flex gap-2 border-b border-slate-200 pb-2">
        <button
          onClick={() => setActiveTab('pending')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 ${
            activeTab === 'pending'
              ? 'bg-slate-900 text-white shadow-sm'
              : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
          }`}
        >
          <ShieldCheck className="w-4 h-4 text-emerald-400" />
          <span>Pending Document Queue</span>
          {verifications.length > 0 && activeTab !== 'pending' && (
            <span className="bg-amber-500 text-white text-[10px] px-1.5 py-0.2 rounded-full font-bold">
              {verifications.length}
            </span>
          )}
        </button>

        <button
          onClick={() => setActiveTab('all')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 ${
            activeTab === 'all'
              ? 'bg-slate-900 text-white shadow-sm'
              : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
          }`}
        >
          <HeartHandshake className="w-4 h-4 text-indigo-400" />
          <span>All Registered NGOs</span>
        </button>
      </div>

      {/* Tab 1: Pending Queue */}
      {activeTab === 'pending' && (
        <>
          {loading ? (
            <div className="text-xs text-slate-500 text-center py-12">Loading pending verification requests...</div>
          ) : verifications.length === 0 ? (
            <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center text-slate-500 text-xs">
              No pending NGO verification requests in queue.
            </div>
          ) : (
            <div className="space-y-4">
              {verifications.map((v) => (
                <div key={v.id} className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-100 pb-3">
                    <div>
                      <h3 className="font-bold text-slate-900 text-lg">{v.ngoName}</h3>
                      <p className="text-xs text-slate-500">Document Type: <strong>{v.documentType}</strong></p>
                    </div>

                    <span className="px-3 py-1 rounded text-xs font-bold bg-amber-100 text-amber-800 uppercase self-start sm:self-center">
                      {v.verificationStatus}
                    </span>
                  </div>

                  <div className="bg-slate-50 p-4 rounded-xl text-xs space-y-2 text-slate-700">
                    <div><strong>Reg / Certificate Number:</strong> {v.documentNumber}</div>
                    <div className="flex items-center gap-1 text-indigo-700">
                      <ExternalLink className="w-3.5 h-3.5" />
                      <a
                        href={getUploadUrl(v.documentUrl)}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="underline font-semibold hover:text-indigo-900"
                      >
                        View Uploaded Document ({v.documentUrl})
                      </a>
                    </div>
                    <div className="text-slate-400 text-[10px]">Submitted At: {new Date(v.createdAt).toLocaleString()}</div>
                  </div>

                  {reviewingId === v.id ? (
                    <div className="bg-slate-100 p-4 rounded-xl space-y-3">
                      <label className="block text-xs font-semibold text-slate-800">Admin Remarks / Notes:</label>
                      <input
                        type="text"
                        value={remarks}
                        onChange={(e) => setRemarks(e.target.value)}
                        placeholder="e.g. Valid 80G tax exemption certificate verified."
                        className="w-full px-3 py-2 border rounded-lg text-xs outline-none bg-white"
                      />
                      <div className="flex justify-end space-x-2 pt-1">
                        <button
                          onClick={() => setReviewingId(null)}
                          className="px-3 py-1.5 border rounded-lg text-xs font-semibold text-slate-600 hover:bg-slate-200"
                        >
                          Cancel
                        </button>
                        <button
                          onClick={() => handleReview(v.id, 'REJECTED')}
                          className="px-3.5 py-1.5 bg-rose-600 hover:bg-rose-700 text-white font-bold rounded-lg text-xs shadow flex items-center gap-1"
                        >
                          <X className="w-4 h-4" /> Reject
                        </button>
                        <button
                          onClick={() => handleReview(v.id, 'APPROVED')}
                          className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg text-xs shadow flex items-center gap-1"
                        >
                          <Check className="w-4 h-4" /> Approve NGO
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="flex justify-end pt-2">
                      <button
                        onClick={() => setReviewingId(v.id)}
                        className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl text-xs shadow transition flex items-center gap-1.5"
                      >
                        <ShieldCheck className="w-4 h-4" />
                        <span>Review & Audit Document</span>
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {/* Tab 2: All Registered NGOs */}
      {activeTab === 'all' && (
        <>
          {loading ? (
            <div className="text-xs text-slate-500 text-center py-12">Loading registered NGOs...</div>
          ) : allNgos.length === 0 ? (
            <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center text-slate-500 text-xs">
              No registered NGOs found.
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {allNgos.map((ngo) => (
                <div key={ngo.id} className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-4">
                  <div className="flex justify-between items-center border-b border-slate-100 pb-3">
                    <h3 className="font-bold text-slate-900 text-lg flex items-center gap-2">
                      <HeartHandshake className="w-5 h-5 text-emerald-600" />
                      <span>{ngo.ngoName}</span>
                    </h3>
                    <span className={`px-2.5 py-0.5 rounded text-[10px] font-bold ${
                      ngo.verificationStatus === 'APPROVED' ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'
                    }`}>
                      {ngo.verificationStatus}
                    </span>
                  </div>

                  <div className="bg-slate-50 p-3.5 rounded-xl text-xs space-y-2 text-slate-600">
                    {ngo.profileImageUrl && (
                      <div className="flex items-center gap-3 pb-2 border-b border-slate-200">
                        <img
                          src={getUploadUrl(ngo.profileImageUrl)}
                          alt="NGO Profile"
                          className="w-14 h-14 rounded-lg object-cover border"
                          onError={(e) => { e.target.style.display = 'none'; }}
                        />
                        <div>
                          <div className="font-bold text-slate-800">Uploaded Profile Photo</div>
                          <div className="text-[10px] text-slate-400">Displayed on directory and public profile</div>
                        </div>
                      </div>
                    )}
                    <div><strong>Cause:</strong> {ngo.cause || 'General Welfare'}</div>
                    <div><strong>Contact Phone:</strong> {ngo.contactNumber}</div>
                    <div><strong>Location:</strong> {ngo.address}, {ngo.city}, {ngo.state}</div>
                    <div><strong>Description:</strong> {ngo.description || 'N/A'}</div>
                    {ngo.galleryPhotos && (
                      <div className="pt-2">
                        <strong>Uploaded Gallery Photos:</strong>
                        <div className="flex gap-2 mt-1 overflow-x-auto py-1">
                          {ngo.galleryPhotos.split(',').map((url, i) => (
                            <img
                              key={i}
                              src={getUploadUrl(url.trim())}
                              alt="Gallery"
                              className="w-12 h-12 rounded object-cover border"
                              onError={(e) => { e.target.style.display = 'none'; }}
                            />
                          ))}
                        </div>
                      </div>
                    )}
                  </div>

                  <div className="flex justify-end space-x-2 pt-2 border-t border-slate-100">
                    {ngo.verificationStatus !== 'REJECTED' && (
                      <button
                        onClick={() => handleUpdateNgoStatus(ngo.id, 'REJECTED')}
                        className="px-3.5 py-1.5 border border-rose-300 text-rose-700 hover:bg-rose-50 font-bold rounded-lg text-xs transition"
                      >
                        Reject Status
                      </button>
                    )}
                    {ngo.verificationStatus !== 'APPROVED' && (
                      <button
                        onClick={() => handleUpdateNgoStatus(ngo.id, 'APPROVED')}
                        className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg text-xs shadow transition"
                      >
                        Approve NGO
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
};
