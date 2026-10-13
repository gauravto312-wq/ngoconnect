import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { ShoppingBag, Plus, Clock } from 'lucide-react';

export const RestaurantFoodAvailability = () => {
  const [availabilities, setAvailabilities] = useState([]);
  const [branches, setBranches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);

  const [branchId, setBranchId] = useState('');
  const [foodType, setFoodType] = useState('');
  const [description, setDescription] = useState('');
  const [quantity, setQuantity] = useState(25);
  const [unit, setUnit] = useState('meals');
  const [availableFrom, setAvailableFrom] = useState('');
  const [availableUntil, setAvailableUntil] = useState('');
  const [pickupDeadline, setPickupDeadline] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [availRes, branchRes] = await Promise.all([
        api.get('/restaurants/food-availability'),
        api.get('/restaurants/branches')
      ]);
      setAvailabilities(availRes.data);
      setBranches(branchRes.data);
      if (branchRes.data.length > 0) setBranchId(branchRes.data[0].id);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handlePostAvailability = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const now = new Date();
      await api.post('/restaurants/food-availability', {
        branchId: branchId ? parseInt(branchId) : undefined,
        foodType,
        description,
        quantity: parseInt(quantity),
        unit,
        availableFrom: availableFrom || now.toISOString(),
        availableUntil: availableUntil || new Date(now.getTime() + 7200000).toISOString(),
        pickupDeadline: pickupDeadline || new Date(now.getTime() + 10800000).toISOString()
      });
      setShowModal(false);
      setFoodType('');
      setDescription('');
      fetchData();
    } catch (e) {
      alert('Failed to post food availability.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Surplus Food Availability</h1>
          <p className="text-xs text-slate-500 mt-1">Post immediate surplus food availability for local NGOs.</p>
        </div>

        <button
          onClick={() => setShowModal(true)}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-xs flex items-center gap-1.5 shadow transition"
        >
          <Plus className="w-4 h-4" />
          <span>Post Food Availability</span>
        </button>
      </div>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full p-6 space-y-4">
            <h3 className="font-bold text-slate-900 text-base border-b pb-3">Post Food Availability</h3>

            <form onSubmit={handlePostAvailability} className="space-y-3 text-xs">
              {branches.length > 0 && (
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Select Branch</label>
                  <select
                    value={branchId}
                    onChange={(e) => setBranchId(e.target.value)}
                    className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none bg-white"
                  >
                    {branches.map((b) => (
                      <option key={b.id} value={b.id}>{b.branchName} ({b.city})</option>
                    ))}
                  </select>
                </div>
              )}

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Food Type / Description *</label>
                <input
                  type="text"
                  required
                  value={foodType}
                  onChange={(e) => setFoodType(e.target.value)}
                  placeholder="e.g. 30 Prepared Lunch Thalis"
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
                    placeholder="meals / thalis"
                    className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Pickup Deadline *</label>
                <input
                  type="datetime-local"
                  required
                  value={pickupDeadline}
                  onChange={(e) => setPickupDeadline(e.target.value)}
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                />
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
                  {submitting ? 'Publishing...' : 'Publish Availability'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Availability Grid */}
      {loading ? (
        <div className="text-xs text-slate-500 text-center py-12">Loading availability listings...</div>
      ) : availabilities.length === 0 ? (
        <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center text-slate-500 text-xs">
          No food availability posted yet. Click 'Post Food Availability' to broadcast surplus food to nearby NGOs.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {availabilities.map((fa) => (
            <div key={fa.id} className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-3">
              <div className="flex items-center justify-between">
                <span className="px-2.5 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800">
                  {fa.status}
                </span>
                {fa.branchName && <span className="text-xs font-semibold text-slate-500">{fa.branchName}</span>}
              </div>

              <h3 className="font-bold text-slate-900 text-lg">{fa.foodType}</h3>
              <p className="text-xs text-slate-600">{fa.description || 'Surplus food ready for pickup.'}</p>

              <div className="bg-slate-50 p-3 rounded-xl text-xs space-y-1 text-slate-600">
                <div><strong>Quantity:</strong> {fa.quantity} {fa.unit}</div>
                <div className="flex items-center gap-1 text-slate-500 pt-1">
                  <Clock className="w-3.5 h-3.5" />
                  <span>Pickup Deadline: {new Date(fa.pickupDeadline).toLocaleString()}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
