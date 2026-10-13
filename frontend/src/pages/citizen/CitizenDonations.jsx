import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { Plus, HeartHandshake, ShieldCheck, CheckCircle2, User, Key, AlertCircle, Truck } from 'lucide-react';

export const CitizenDonations = () => {
  const [donations, setDonations] = useState([]);
  const [showModal, setShowModal] = useState(false);

  const [handoverModal, setHandoverModal] = useState(null);
  const [handoverCodeInput, setHandoverCodeInput] = useState('');
  const [submittingHandover, setSubmittingHandover] = useState(false);
  const [handoverError, setHandoverError] = useState('');

  const [category, setCategory] = useState('FOOD');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [quantity, setQuantity] = useState(1);
  const [unit, setUnit] = useState('packets');
  const [address, setAddress] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('Uttar Pradesh');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    fetchDonations();
  }, []);

  const fetchDonations = async () => {
    try {
      const res = await api.get('/donations/my-donations');
      setDonations(res.data);
    } catch (e) {
      console.error(e);
    }
  };

  const handleCreateDonation = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await api.post('/donations', {
        category,
        title,
        description,
        quantity: parseInt(quantity),
        unit,
        address,
        city,
        state
      });
      setShowModal(false);
      setTitle('');
      setDescription('');
      fetchDonations();
    } catch (e) {
      alert(e.response?.data?.message || 'Failed to create donation');
    } finally {
      setSubmitting(false);
    }
  };

  const handleConfirmHandover = async (e) => {
    e.preventDefault();
    if (!handoverModal || !handoverCodeInput) return;
    setSubmittingHandover(true);
    setHandoverError('');
    try {
      await api.post(`/donations/${handoverModal.id}/confirm-handover`, {
        pickupCode: handoverCodeInput.trim()
      });
      alert('Physical handover confirmed! The food is now in transit.');
      setHandoverModal(null);
      setHandoverCodeInput('');
      fetchDonations();
    } catch (err) {
      setHandoverError(err.response?.data?.message || 'Failed to confirm handover. Please check the code.');
    } finally {
      setSubmittingHandover(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">My Donations</h1>
          <p className="text-xs text-slate-500 mt-1">Manage your posted food, clothes, and item donations.</p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-xs flex items-center gap-1.5 shadow transition"
        >
          <Plus className="w-4 h-4" />
          <span>Post New Donation</span>
        </button>
      </div>

      {/* Handover Confirmation Modal */}
      {handoverModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full p-6 space-y-4">
            <h3 className="font-bold text-slate-900 text-base flex items-center gap-2 border-b pb-3">
              <Key className="w-5 h-5 text-emerald-600" />
              <span>Confirm Food Handover — {handoverModal.title}</span>
            </h3>

            {handoverError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700 flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-red-500 flex-shrink-0" />
                <span>{handoverError}</span>
              </div>
            )}

            <p className="text-xs text-slate-600">
              When volunteer <strong>{handoverModal.assignedVolunteerName}</strong> arrives, verify the pickup code and click confirm to hand over the food.
            </p>

            <form onSubmit={handleConfirmHandover} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Pickup Verification Code *</label>
                <input
                  type="text"
                  required
                  value={handoverCodeInput}
                  onChange={(e) => setHandoverCodeInput(e.target.value)}
                  placeholder="e.g. NGC-847291"
                  className="w-full px-3.5 py-2.5 border rounded-lg focus:ring-2 focus:ring-emerald-500 font-mono text-sm uppercase tracking-wider outline-none"
                />
              </div>

              <div className="flex justify-end space-x-2 pt-3 border-t">
                <button
                  type="button"
                  onClick={() => setHandoverModal(null)}
                  className="px-4 py-2 border rounded-lg text-slate-600 hover:bg-slate-100 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingHandover}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg shadow"
                >
                  {submittingHandover ? 'Confirming...' : 'Confirm Handover'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Form */}
      {showModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-lg w-full p-6 space-y-4">
            <h3 className="font-bold text-slate-900 text-lg border-b pb-3">Post a New Donation</h3>

            <form onSubmit={handleCreateDonation} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Category *</label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none bg-white"
                >
                  <option value="FOOD">FOOD</option>
                  <option value="CLOTHES">CLOTHES</option>
                  <option value="OTHER">OTHER</option>
                </select>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Donation Title *</label>
                <input
                  type="text"
                  required
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="e.g. 20 Surplus Rice Packets"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Quantity *</label>
                  <input
                    type="number"
                    min="1"
                    required
                    value={quantity}
                    onChange={(e) => setQuantity(e.target.value)}
                    className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Unit</label>
                  <input
                    type="text"
                    value={unit}
                    onChange={(e) => setUnit(e.target.value)}
                    placeholder="packets / kg / items"
                    className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Pickup Address *</label>
                <input
                  type="text"
                  required
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  placeholder="Building/Street address"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">City *</label>
                  <input
                    type="text"
                    required
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="Lucknow"
                    className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">State *</label>
                  <input
                    type="text"
                    required
                    value={state}
                    onChange={(e) => setState(e.target.value)}
                    placeholder="Uttar Pradesh"
                    className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Additional Notes</label>
                <textarea
                  rows="2"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Freshly prepared, ready for pickup before 8 PM."
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                ></textarea>
              </div>

              <div className="flex justify-end space-x-2 pt-3 border-t">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 border rounded-lg text-slate-600 hover:bg-slate-100 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg shadow"
                >
                  {submitting ? 'Publishing...' : 'Publish Donation'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Donations List */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {donations.map((d) => (
          <div key={d.id} className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-3">
            <div className="flex items-center justify-between">
              <span className="px-2.5 py-0.5 rounded text-[11px] font-bold bg-emerald-100 text-emerald-800 uppercase">
                {d.category}
              </span>
              <span className={`px-2.5 py-0.5 rounded text-[11px] font-bold uppercase ${
                d.status === 'AVAILABLE' ? 'bg-emerald-100 text-emerald-800' :
                d.status === 'CLAIMED' ? 'bg-amber-100 text-amber-800 border border-amber-300' :
                d.status === 'PICKUP' ? 'bg-blue-100 text-blue-800 border border-blue-300' :
                d.status === 'COMPLETED' ? 'bg-emerald-100 text-emerald-800 border border-emerald-300' : 'bg-slate-200 text-slate-800'
              }`}>
                {d.status === 'PICKUP' ? 'IN TRANSIT / PICKED UP' : d.status}
              </span>
            </div>

            <div>
              <h3 className="font-bold text-slate-900 text-base">{d.title}</h3>
              <p className="text-xs text-slate-600 mt-1">{d.description || 'No description'}</p>
            </div>

            <div className="bg-slate-50 p-3 rounded-xl text-xs space-y-1 text-slate-600">
              <div><strong>Quantity:</strong> {d.quantity} {d.unit}</div>
              <div><strong>Address:</strong> {d.address}, {d.city}</div>
              {d.claimedByNgoName && (
                <div className="text-amber-800 font-bold">Claimed By: {d.claimedByNgoName}</div>
              )}
            </div>

            {/* Assigned Volunteer Details */}
            {d.assignedVolunteerName && (
              <div className="bg-indigo-50/70 border border-indigo-200 p-3 rounded-xl text-xs space-y-1 text-indigo-950">
                <div className="flex items-center gap-1.5 font-bold text-indigo-900">
                  <User className="w-3.5 h-3.5 text-indigo-600" />
                  <span>Assigned Volunteer: {d.assignedVolunteerName}</span>
                </div>
                {d.assignedVolunteerContact && (
                  <div><strong>Phone:</strong> {d.assignedVolunteerContact}</div>
                )}
                <div><strong>Task Status:</strong> <span className="font-bold uppercase">{d.taskStatus || 'ASSIGNED'}</span></div>
              </div>
            )}

            {/* Private Pickup Code Display */}
            {d.pickupCode && (
              <div className="bg-amber-50 border border-amber-200 p-2.5 rounded-xl text-xs flex items-center justify-between">
                <span className="text-amber-800 font-medium">Private Pickup Code:</span>
                <span className="font-mono font-bold text-amber-900 text-sm tracking-wider">{d.pickupCode}</span>
              </div>
            )}

            {/* Status Feedback & Actions */}
            {d.status === 'CLAIMED' && d.assignedVolunteerName && (
              <div className="pt-1 flex justify-end">
                <button
                  onClick={() => {
                    setHandoverModal(d);
                    setHandoverCodeInput(d.pickupCode || '');
                    setHandoverError('');
                  }}
                  className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg text-xs shadow transition flex items-center gap-1.5"
                >
                  <Key className="w-3.5 h-3.5" />
                  <span>Confirm Handover to Volunteer</span>
                </button>
              </div>
            )}

            {d.status === 'PICKUP' && (
              <div className="p-2.5 bg-blue-50 border border-blue-200 rounded-xl text-xs text-blue-800 flex items-center gap-2">
                <Truck className="w-4 h-4 text-blue-600 flex-shrink-0" />
                <span>Food picked up by volunteer. In transit to {d.claimedByNgoName}.</span>
              </div>
            )}

            {d.status === 'COMPLETED' && (
              <div className="p-2.5 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-800 flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                <span>Successfully delivered to {d.claimedByNgoName}.</span>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};
