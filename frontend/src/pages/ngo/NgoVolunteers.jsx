import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../../services/api';
import { Users, Search, MapPin, Send, CheckCircle2, UserCheck, IndianRupee, Clock, Check, X, AlertTriangle, ShieldCheck } from 'lucide-react';

export const NgoVolunteers = () => {
  const [searchParams] = useSearchParams();
  const initialCity = searchParams.get('city') || '';
  const initialDonationId = searchParams.get('donationId') || '';

  const [activeTab, setActiveTab] = useState('volunteers'); // 'volunteers' | 'assigned'

  // Volunteers Search State
  const [volunteers, setVolunteers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cityFilter, setCityFilter] = useState(initialCity);

  // Task Creation Modal State
  const [selectedVolunteer, setSelectedVolunteer] = useState(null);
  const [taskTitle, setTaskTitle] = useState('');
  const [taskDescription, setTaskDescription] = useState('');
  const [pickupAddress, setPickupAddress] = useState('');
  const [destinationAddress, setDestinationAddress] = useState('');
  const [scheduledTime, setScheduledTime] = useState('');
  const [isPaidTask, setIsPaidTask] = useState(false);
  const [paymentAmount, setPaymentAmount] = useState('');
  const [paymentTerms, setPaymentTerms] = useState('');
  const [currency, setCurrency] = useState('INR');
  const [assigning, setAssigning] = useState(false);

  // Assigned Tasks & Payment State
  const [assignedTasks, setAssignedTasks] = useState([]);
  const [loadingTasks, setLoadingTasks] = useState(false);
  const [confirmModal, setConfirmModal] = useState(null); // task to confirm payment for
  const [disputeModal, setDisputeModal] = useState(null); // { task, reason }
  const [actionProcessing, setActionProcessing] = useState(false);

  useEffect(() => {
    if (activeTab === 'volunteers') {
      fetchActiveVolunteers();
    } else {
      fetchAssignedTasks();
    }
  }, [cityFilter, activeTab]);

  const fetchActiveVolunteers = async () => {
    setLoading(true);
    try {
      const params = {};
      if (cityFilter) params.city = cityFilter;
      const res = await api.get('/volunteers/active-search', { params });
      setVolunteers(res.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const fetchAssignedTasks = async () => {
    setLoadingTasks(true);
    try {
      const res = await api.get('/tasks/assigned-by-me');
      setAssignedTasks(res.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoadingTasks(false);
    }
  };

  const handleAssignTask = async (e) => {
    e.preventDefault();
    if (!selectedVolunteer) return;

    if (isPaidTask) {
      const numAmount = parseFloat(paymentAmount);
      if (isNaN(numAmount) || numAmount <= 0) {
        alert('Please enter a valid positive payment amount for the paid task.');
        return;
      }
    }

    setAssigning(true);
    try {
      await api.post('/tasks/assign', {
        volunteerId: selectedVolunteer.id,
        donationId: initialDonationId ? parseInt(initialDonationId) : undefined,
        taskType: 'PICKUP_DONATION',
        title: taskTitle,
        description: taskDescription,
        pickupAddress,
        destinationAddress,
        scheduledTime: scheduledTime || new Date(Date.now() + 3600000).toISOString(),
        isPaid: isPaidTask,
        paymentAmount: isPaidTask ? parseFloat(paymentAmount) : null,
        currency: isPaidTask ? (currency || 'INR') : null,
        paymentTerms: isPaidTask ? paymentTerms : null
      });

      alert(`Task assigned successfully to Volunteer ${selectedVolunteer.name} (${selectedVolunteer.volunteerCode})!`);
      setSelectedVolunteer(null);
      setTaskTitle('');
      setTaskDescription('');
      setPickupAddress('');
      setDestinationAddress('');
      setIsPaidTask(false);
      setPaymentAmount('');
      setPaymentTerms('');
      if (activeTab === 'assigned') {
        fetchAssignedTasks();
      }
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to assign task to volunteer.');
    } finally {
      setAssigning(false);
    }
  };

  const handleConfirmPayment = async () => {
    if (!confirmModal) return;
    setActionProcessing(true);
    try {
      await api.post(`/tasks/${confirmModal.id}/confirm-payment`);
      alert(`Payment for task "${confirmModal.title}" has been confirmed!`);
      setConfirmModal(null);
      fetchAssignedTasks();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to confirm payment.');
    } finally {
      setActionProcessing(false);
    }
  };

  const handleDisputePayment = async (e) => {
    e.preventDefault();
    if (!disputeModal || !disputeModal.reason.trim()) {
      alert('Please provide a reason for disputing this payment.');
      return;
    }
    setActionProcessing(true);
    try {
      await api.post(`/tasks/${disputeModal.task.id}/dispute-payment`, {
        reason: disputeModal.reason.trim()
      });
      alert(`Payment for task "${disputeModal.task.title}" has been marked as disputed.`);
      setDisputeModal(null);
      fetchAssignedTasks();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to dispute payment.');
    } finally {
      setActionProcessing(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Header & Tab Navigation */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Volunteer Operations & Tasks</h1>
          <p className="text-xs text-slate-500 mt-1">Assign pickup tasks, set compensation, and track task payments.</p>
        </div>

        <div className="flex items-center gap-2 bg-slate-200/70 p-1 rounded-xl">
          <button
            onClick={() => setActiveTab('volunteers')}
            className={`px-4 py-2 rounded-lg text-xs font-bold transition flex items-center gap-1.5 ${
              activeTab === 'volunteers'
                ? 'bg-white text-emerald-800 shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <Users className="w-3.5 h-3.5" />
            <span>Search & Assign</span>
          </button>
          <button
            onClick={() => setActiveTab('assigned')}
            className={`px-4 py-2 rounded-lg text-xs font-bold transition flex items-center gap-1.5 ${
              activeTab === 'assigned'
                ? 'bg-white text-emerald-800 shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Assigned Tasks & Payments</span>
          </button>
        </div>
      </div>

      {/* TAB 1: Search & Assign Volunteers */}
      {activeTab === 'volunteers' && (
        <div className="space-y-6">
          <div className="flex justify-end">
            <div className="flex items-center gap-2">
              <MapPin className="w-4 h-4 text-slate-400" />
              <input
                type="text"
                value={cityFilter}
                onChange={(e) => setCityFilter(e.target.value)}
                placeholder="Search city (e.g. Lucknow)"
                className="px-3.5 py-2 text-xs border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none bg-white"
              />
            </div>
          </div>

          {loading ? (
            <div className="text-xs text-slate-500 text-center py-12">Searching active volunteers...</div>
          ) : volunteers.length === 0 ? (
            <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center text-slate-500 text-xs">
              No active volunteers found in {cityFilter || 'all cities'}. Only ACTIVE volunteers appear in search.
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {volunteers.map((v) => (
                <div key={v.id} className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm flex flex-col justify-between">
                  <div>
                    <div className="flex items-center justify-between mb-3">
                      <span className="px-2.5 py-0.5 rounded text-[11px] font-mono font-bold bg-amber-100 text-amber-900 border border-amber-300">
                        {v.volunteerCode}
                      </span>
                      <span className="px-2.5 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 uppercase flex items-center gap-1">
                        <UserCheck className="w-3 h-3" /> ACTIVE
                      </span>
                    </div>

                    <h3 className="font-bold text-slate-900 text-lg mb-1">{v.name}</h3>
                    <p className="text-xs text-slate-600 flex items-center gap-1 mb-3">
                      <MapPin className="w-3.5 h-3.5 text-slate-400" />
                      {v.city}, {v.state}
                    </p>

                    <div className="bg-slate-50 p-3 rounded-xl text-xs space-y-1 text-slate-600">
                      <div><strong>Phone:</strong> {v.phone}</div>
                      <div><strong>Email:</strong> {v.email}</div>
                      <div><strong>Address:</strong> {v.address}</div>
                    </div>
                  </div>

                  <div className="pt-4 border-t border-slate-100 mt-4">
                    <button
                      onClick={() => setSelectedVolunteer(v)}
                      className="w-full py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-xs shadow transition flex items-center justify-center gap-1.5"
                    >
                      <Send className="w-4 h-4" />
                      <span>Assign Pickup Task</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TAB 2: Assigned Tasks & Payments */}
      {activeTab === 'assigned' && (
        <div className="space-y-6">
          <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-4 text-xs text-emerald-900">
            <h4 className="font-bold mb-1 flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-emerald-700" />
              Manual Payment Confirmation Workflow
            </h4>
            <p>
              When a volunteer completes a paid task, its payment status automatically becomes <strong>PENDING</strong>.
              After completing the offline/direct transfer (e.g. UPI, cash, or bank deposit), click <strong>Confirm Payment</strong> below to record the confirmation.
            </p>
          </div>

          {loadingTasks ? (
            <div className="text-xs text-slate-500 text-center py-12">Loading assigned tasks...</div>
          ) : assignedTasks.length === 0 ? (
            <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center text-slate-500 text-xs">
              No tasks assigned yet. Search active volunteers in the first tab to assign pickup tasks.
            </div>
          ) : (
            <div className="space-y-4">
              {assignedTasks.map((t) => (
                <div key={t.id} className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-3">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-800 uppercase">
                          {t.taskType}
                        </span>
                        <h3 className="font-bold text-slate-900 text-base">{t.title}</h3>
                      </div>
                      <p className="text-xs text-slate-500 mt-0.5">
                        Assigned to: <strong>{t.volunteerName}</strong> ({t.volunteerCode})
                      </p>
                    </div>

                    <div className="flex items-center gap-2">
                      <span className={`px-2.5 py-1 rounded text-xs font-bold uppercase ${
                        t.status === 'ASSIGNED' ? 'bg-amber-100 text-amber-800 border border-amber-300' :
                        t.status === 'ACCEPTED' ? 'bg-indigo-100 text-indigo-800 border border-indigo-300' :
                        t.status === 'IN_PROGRESS' ? 'bg-blue-100 text-blue-800 border border-blue-300' :
                        t.status === 'COMPLETED' ? 'bg-emerald-100 text-emerald-800 border border-emerald-300' :
                        'bg-slate-200 text-slate-800'
                      }`}>
                        Task: {t.status}
                      </span>
                    </div>
                  </div>

                  <div className="bg-slate-50 p-4 rounded-xl text-xs space-y-2 text-slate-700">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                      <div><strong>Pickup:</strong> {t.pickupAddress}</div>
                      <div><strong>Destination:</strong> {t.destinationAddress}</div>
                    </div>
                    {t.description && <div><strong>Instructions:</strong> {t.description}</div>}
                    <div className="text-slate-500 flex items-center gap-1 pt-1">
                      <Clock className="w-3.5 h-3.5" />
                      <span>Scheduled: {new Date(t.scheduledTime).toLocaleString()}</span>
                    </div>
                  </div>

                  {/* Compensation & Payment Status Bar */}
                  <div className="p-4 bg-slate-100/60 rounded-xl flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                      {t.isPaid ? (
                        <div className="space-y-1">
                          <div className="flex items-center gap-2">
                            <span className="font-extrabold text-sm text-slate-900">
                              ₹{Number(t.paymentAmount).toFixed(2)} {t.currency}
                            </span>
                            <span className={`px-2.5 py-0.5 rounded text-[11px] font-bold uppercase ${
                              t.paymentStatus === 'PAID' ? 'bg-emerald-100 text-emerald-800 border border-emerald-300' :
                              t.paymentStatus === 'PENDING' ? 'bg-amber-100 text-amber-800 border border-amber-300' :
                              t.paymentStatus === 'DISPUTED' ? 'bg-rose-100 text-rose-800 border border-rose-300' :
                              'bg-slate-200 text-slate-700'
                            }`}>
                              Payment: {t.paymentStatus}
                            </span>
                          </div>
                          {t.paymentTerms && (
                            <p className="text-xs text-slate-600">Terms: {t.paymentTerms}</p>
                          )}
                          {t.paymentStatus === 'PAID' && t.paidAt && (
                            <p className="text-[11px] text-emerald-700 font-semibold">
                              Confirmed on: {new Date(t.paidAt).toLocaleString()}
                            </p>
                          )}
                          {t.paymentStatus === 'DISPUTED' && t.disputeReason && (
                            <p className="text-[11px] text-rose-700 font-semibold">
                              Dispute Reason: {t.disputeReason}
                            </p>
                          )}
                        </div>
                      ) : (
                        <div className="text-xs text-slate-500 font-semibold">
                          Unpaid Voluntary Task (Payment status: NOT_APPLICABLE)
                        </div>
                      )}
                    </div>

                    {/* Actions for Paid Tasks */}
                    {t.isPaid && t.status === 'COMPLETED' && (
                      <div className="flex items-center gap-2">
                        {t.paymentStatus === 'PENDING' && (
                          <>
                            <button
                              onClick={() => setDisputeModal({ task: t, reason: '' })}
                              className="px-3 py-1.5 text-xs font-bold text-rose-700 border border-rose-300 hover:bg-rose-50 rounded-lg transition"
                            >
                              Dispute
                            </button>
                            <button
                              onClick={() => setConfirmModal(t)}
                              className="px-4 py-1.5 text-xs font-bold bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg shadow transition flex items-center gap-1"
                            >
                              <Check className="w-3.5 h-3.5" />
                              <span>Confirm Payment (Mark as PAID)</span>
                            </button>
                          </>
                        )}

                        {t.paymentStatus === 'DISPUTED' && (
                          <button
                            onClick={() => setConfirmModal(t)}
                            className="px-4 py-1.5 text-xs font-bold bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg shadow transition flex items-center gap-1"
                          >
                            <Check className="w-3.5 h-3.5" />
                            <span>Resolve & Confirm Payment</span>
                          </button>
                        )}

                        {t.paymentStatus === 'PAID' && (
                          <div className="text-xs font-bold text-emerald-700 flex items-center gap-1">
                            <CheckCircle2 className="w-4 h-4" />
                            <span>Payment Confirmed</span>
                          </div>
                        )}
                      </div>
                    )}

                    {t.isPaid && t.status !== 'COMPLETED' && (
                      <div className="text-[11px] text-slate-500 italic">
                        Payment confirmation unlocks once the volunteer completes the task.
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Task Assignment Modal */}
      {selectedVolunteer && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-lg w-full p-6 space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="border-b pb-3">
              <h3 className="font-bold text-slate-900 text-base">Assign Pickup Task</h3>
              <p className="text-xs text-emerald-700 font-semibold mt-0.5">
                Assigning to: {selectedVolunteer.name} ({selectedVolunteer.volunteerCode}) — {selectedVolunteer.city}
              </p>
            </div>

            <form onSubmit={handleAssignTask} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Task Title *</label>
                <input
                  type="text"
                  required
                  value={taskTitle}
                  onChange={(e) => setTaskTitle(e.target.value)}
                  placeholder="e.g. Surplus Food Pickup from ABC Restaurant"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Pickup Address *</label>
                <input
                  type="text"
                  required
                  value={pickupAddress}
                  onChange={(e) => setPickupAddress(e.target.value)}
                  placeholder="Donor/Restaurant pickup location"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Destination Address *</label>
                <input
                  type="text"
                  required
                  value={destinationAddress}
                  onChange={(e) => setDestinationAddress(e.target.value)}
                  placeholder="NGO shelter or distribution address"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Scheduled Time *</label>
                <input
                  type="datetime-local"
                  required
                  value={scheduledTime}
                  onChange={(e) => setScheduledTime(e.target.value)}
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              {/* Compensation Type Selector */}
              <div className="bg-slate-50 p-3.5 rounded-xl border border-slate-200 space-y-2.5">
                <label className="block font-bold text-slate-800">Task Compensation</label>
                <div className="flex items-center gap-4">
                  <label className="flex items-center gap-1.5 cursor-pointer font-medium text-slate-700">
                    <input
                      type="radio"
                      name="compensationType"
                      checked={!isPaidTask}
                      onChange={() => setIsPaidTask(false)}
                      className="text-emerald-600 focus:ring-emerald-500"
                    />
                    <span>Unpaid Voluntary Task</span>
                  </label>
                  <label className="flex items-center gap-1.5 cursor-pointer font-bold text-emerald-800">
                    <input
                      type="radio"
                      name="compensationType"
                      checked={isPaidTask}
                      onChange={() => setIsPaidTask(true)}
                      className="text-emerald-600 focus:ring-emerald-500"
                    />
                    <span>Paid Task (Stipend / Delivery Fee)</span>
                  </label>
                </div>

                {isPaidTask && (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-2 border-t border-slate-200">
                    <div>
                      <label className="block font-semibold text-slate-700 mb-1">Amount (₹ INR) *</label>
                      <input
                        type="number"
                        min="1"
                        step="0.01"
                        required={isPaidTask}
                        value={paymentAmount}
                        onChange={(e) => setPaymentAmount(e.target.value)}
                        placeholder="e.g. 500"
                        className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none bg-white font-mono"
                      />
                    </div>
                    <div>
                      <label className="block font-semibold text-slate-700 mb-1">Payment Terms</label>
                      <input
                        type="text"
                        value={paymentTerms}
                        onChange={(e) => setPaymentTerms(e.target.value)}
                        placeholder="e.g. Pay on verified handover"
                        className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none bg-white"
                      />
                    </div>
                  </div>
                )}
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Special Instructions</label>
                <textarea
                  rows="2"
                  value={taskDescription}
                  onChange={(e) => setTaskDescription(e.target.value)}
                  placeholder="Verify pickup code upon arrival."
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                ></textarea>
              </div>

              <div className="flex justify-end space-x-2 pt-3 border-t">
                <button
                  type="button"
                  onClick={() => setSelectedVolunteer(null)}
                  className="px-4 py-2 border rounded-lg text-slate-600 hover:bg-slate-100 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={assigning}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg shadow"
                >
                  {assigning ? 'Assigning...' : 'Confirm Assignment'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Manual Payment Confirmation Modal */}
      {confirmModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full p-6 space-y-4 text-xs">
            <div className="border-b pb-3">
              <h3 className="font-bold text-slate-900 text-base flex items-center gap-1.5">
                <ShieldCheck className="w-5 h-5 text-emerald-600" />
                Confirm Task Payment
              </h3>
              <p className="text-slate-500 mt-1">
                You are confirming manual payment for: <strong>{confirmModal.title}</strong>
              </p>
            </div>

            <div className="bg-slate-50 p-3.5 rounded-xl space-y-1.5">
              <div><strong>Volunteer:</strong> {confirmModal.volunteerName} ({confirmModal.volunteerCode})</div>
              <div><strong>Amount:</strong> <span className="font-bold text-emerald-800 text-sm">₹{Number(confirmModal.paymentAmount).toFixed(2)} {confirmModal.currency}</span></div>
              {confirmModal.paymentTerms && <div><strong>Terms:</strong> {confirmModal.paymentTerms}</div>}
            </div>

            <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-amber-900 leading-relaxed">
              <strong>Notice:</strong> NGOConnect does not process banking transactions or hold funds. By confirming, you verify that offline payment (via UPI, cash, or bank deposit) has been completed.
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t">
              <button
                type="button"
                onClick={() => setConfirmModal(null)}
                className="px-4 py-2 border rounded-lg text-slate-600 hover:bg-slate-100 font-semibold"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={actionProcessing}
                onClick={handleConfirmPayment}
                className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg shadow flex items-center gap-1"
              >
                <Check className="w-4 h-4" />
                <span>{actionProcessing ? 'Confirming...' : 'Yes, Confirm Payment'}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Dispute Modal */}
      {disputeModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full p-6 space-y-4 text-xs">
            <div className="border-b pb-3">
              <h3 className="font-bold text-slate-900 text-base flex items-center gap-1.5">
                <AlertTriangle className="w-5 h-5 text-rose-600" />
                Dispute Task Payment
              </h3>
              <p className="text-slate-500 mt-1">
                Dispute payment for: <strong>{disputeModal.task.title}</strong>
              </p>
            </div>

            <form onSubmit={handleDisputePayment} className="space-y-3">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Dispute Reason *</label>
                <textarea
                  rows="3"
                  required
                  value={disputeModal.reason}
                  onChange={(e) => setDisputeModal({ ...disputeModal, reason: e.target.value })}
                  placeholder="Explain why payment is disputed (e.g. damaged goods, incomplete handover, etc.)."
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-rose-500 outline-none"
                ></textarea>
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t">
                <button
                  type="button"
                  onClick={() => setDisputeModal(null)}
                  className="px-4 py-2 border rounded-lg text-slate-600 hover:bg-slate-100 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionProcessing}
                  className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white font-bold rounded-lg shadow"
                >
                  {actionProcessing ? 'Submitting...' : 'Mark as Disputed'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
