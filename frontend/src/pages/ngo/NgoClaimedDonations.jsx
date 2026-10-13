import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { CheckCircle2, ShieldCheck, Users, Key, AlertCircle, Truck, User } from 'lucide-react';
import { Link } from 'react-router-dom';

export const NgoClaimedDonations = () => {
  const [claimed, setClaimed] = useState([]);
  const [loading, setLoading] = useState(true);
  const [verifyModal, setVerifyModal] = useState(null);
  const [pickupCodeInput, setPickupCodeInput] = useState('');
  const [verifying, setVerifying] = useState(false);
  const [verifyError, setVerifyError] = useState('');

  useEffect(() => {
    fetchClaimed();
  }, []);

  const fetchClaimed = async () => {
    setLoading(true);
    try {
      const res = await api.get('/donations/claimed');
      setClaimed(res.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyPickupCode = async (e) => {
    e.preventDefault();
    if (!verifyModal || !pickupCodeInput) return;
    setVerifying(true);
    setVerifyError('');
    try {
      await api.post(`/donations/${verifyModal.id}/confirm-receipt`, {
        pickupCode: pickupCodeInput.trim()
      });
      alert('Delivery receipt confirmed! Donation marked COMPLETED.');
      setVerifyModal(null);
      setPickupCodeInput('');
      fetchClaimed();
    } catch (err) {
      setVerifyError(err.response?.data?.message || 'Invalid pickup verification code.');
    } finally {
      setVerifying(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900">Claimed Donations</h1>
        <p className="text-xs text-slate-500 mt-1">Track active volunteer assignments, pickup progress, & confirm final delivery receipts.</p>
      </div>

      {/* Verify Delivery Receipt Modal */}
      {verifyModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full p-6 space-y-4">
            <h3 className="font-bold text-slate-900 text-base flex items-center gap-2 border-b pb-3">
              <Key className="w-5 h-5 text-amber-600" />
              <span>Confirm Food Delivery Receipt — {verifyModal.title}</span>
            </h3>

            {verifyError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700 flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-red-500 flex-shrink-0" />
                <span>{verifyError}</span>
              </div>
            )}

            <p className="text-xs text-slate-600">
              Enter the pickup verification code provided by the volunteer upon delivery to verify physical receipt and complete the donation.
            </p>

            <form onSubmit={handleVerifyPickupCode} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Enter Pickup Verification Code *</label>
                <input
                  type="text"
                  required
                  value={pickupCodeInput}
                  onChange={(e) => setPickupCodeInput(e.target.value)}
                  placeholder="e.g. NGC-847291"
                  className="w-full px-3.5 py-2.5 border rounded-lg focus:ring-2 focus:ring-emerald-500 font-mono text-sm uppercase tracking-wider outline-none"
                />
              </div>

              <div className="flex justify-end space-x-2 pt-3 border-t">
                <button
                  type="button"
                  onClick={() => setVerifyModal(null)}
                  className="px-4 py-2 border rounded-lg text-slate-600 hover:bg-slate-100 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={verifying}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg shadow"
                >
                  {verifying ? 'Verifying...' : 'Confirm Delivery Receipt'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {loading ? (
        <div className="text-xs text-slate-500 text-center py-12">Loading claimed donations...</div>
      ) : claimed.length === 0 ? (
        <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center text-slate-500 text-xs">
          No claimed donations found. Browse the Marketplace to claim surplus food donations.
        </div>
      ) : (
        <div className="space-y-4">
          {claimed.map((d) => (
            <div key={d.id} className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-100 pb-3">
                <div>
                  <span className="px-2.5 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 uppercase">
                    {d.category}
                  </span>
                  <h3 className="font-bold text-slate-900 text-lg mt-1">{d.title}</h3>
                </div>

                <span className={`px-3 py-1 rounded text-xs font-bold uppercase self-start sm:self-center ${
                  d.status === 'CLAIMED' ? 'bg-amber-100 text-amber-800 border border-amber-300' :
                  d.status === 'PICKUP' ? 'bg-blue-100 text-blue-800 border border-blue-300' :
                  d.status === 'COMPLETED' ? 'bg-emerald-100 text-emerald-800 border border-emerald-300' :
                  'bg-slate-200 text-slate-800'
                }`}>
                  {d.status === 'PICKUP' ? 'IN TRANSIT / PICKED UP' : d.status}
                </span>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs bg-slate-50 p-4 rounded-xl text-slate-700">
                <div>
                  <div><strong>Donor:</strong> {d.donorName} ({d.donorRole})</div>
                  <div><strong>Contact:</strong> {d.donorContact}</div>
                  <div><strong>Quantity:</strong> {d.quantity} {d.unit}</div>
                </div>
                <div>
                  <div><strong>Pickup Address:</strong> {d.address}, {d.city}</div>
                  {d.branchName && <div><strong>Branch:</strong> {d.branchName}</div>}
                  <div><strong>Claimed At:</strong> {d.claimedAt ? new Date(d.claimedAt).toLocaleString() : 'N/A'}</div>
                </div>
              </div>

              {/* Assigned Volunteer Details */}
              {d.assignedVolunteerName ? (
                <div className="bg-indigo-50/70 border border-indigo-200 p-3 rounded-xl text-xs space-y-1 text-indigo-950">
                  <div className="flex items-center gap-1.5 font-bold text-indigo-900">
                    <User className="w-3.5 h-3.5 text-indigo-600" />
                    <span>Assigned Volunteer: {d.assignedVolunteerName}</span>
                  </div>
                  {d.assignedVolunteerContact && (
                    <div><strong>Volunteer Contact:</strong> {d.assignedVolunteerContact}</div>
                  )}
                  <div><strong>Task Status:</strong> <span className="font-bold uppercase">{d.taskStatus || 'ASSIGNED'}</span></div>
                </div>
              ) : (
                <div className="p-3 bg-amber-50/70 border border-amber-200 rounded-xl text-xs flex items-center justify-between gap-2">
                  <span className="text-amber-800">No volunteer assigned yet for pickup.</span>
                  {d.status !== 'COMPLETED' && (
                    <Link
                      to={`/ngo/volunteers?donationId=${d.id}&city=${d.city}`}
                      className="px-3 py-1 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-lg text-xs shadow transition flex items-center gap-1"
                    >
                      <Users className="w-3.5 h-3.5" />
                      <span>Assign Volunteer</span>
                    </Link>
                  )}
                </div>
              )}

              {/* Private Code Display Box */}
              {d.pickupCode && (
                <div className="bg-amber-50 border border-amber-200 p-3.5 rounded-xl flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div>
                    <div className="text-[11px] font-bold text-amber-900 uppercase tracking-wider">Authorized Pickup Verification Code</div>
                    <div className="font-mono text-xl font-extrabold text-amber-950 tracking-widest mt-0.5">{d.pickupCode}</div>
                  </div>

                  {d.status === 'PICKUP' && (
                    <button
                      onClick={() => {
                        setVerifyModal(d);
                        setPickupCodeInput(d.pickupCode || '');
                        setVerifyError('');
                      }}
                      className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-xs shadow transition flex items-center gap-1.5 self-start sm:self-center"
                    >
                      <Key className="w-4 h-4" />
                      <span>Confirm Food Delivery Receipt</span>
                    </button>
                  )}

                  {d.status === 'CLAIMED' && (
                    <span className="text-[11px] font-medium text-amber-800 bg-amber-100/70 px-3 py-1.5 rounded-lg border border-amber-300">
                      Awaiting donor handover to volunteer
                    </span>
                  )}

                  {d.status === 'COMPLETED' && (
                    <span className="text-[11px] font-semibold text-emerald-800 bg-emerald-100 px-3 py-1.5 rounded-lg border border-emerald-300 flex items-center gap-1">
                      <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                      <span>Delivered & Verified</span>
                    </span>
                  )}
                </div>
              )}

              {/* Re-assign Action Button */}
              {d.status === 'CLAIMED' && d.assignedVolunteerName && (
                <div className="flex justify-end pt-1">
                  <Link
                    to={`/ngo/volunteers?donationId=${d.id}&city=${d.city}`}
                    className="px-3.5 py-1.5 border border-slate-300 hover:bg-slate-50 text-slate-700 font-bold rounded-lg text-xs transition flex items-center gap-1"
                  >
                    <Users className="w-3.5 h-3.5" />
                    <span>Reassign Volunteer</span>
                  </Link>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
