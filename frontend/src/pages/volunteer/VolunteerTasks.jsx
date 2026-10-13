import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { Truck, Check, X, ArrowRight, Clock, IndianRupee, ShieldCheck, AlertTriangle, Key, AlertCircle } from 'lucide-react';

export const VolunteerTasks = () => {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);

  const [disputeModal, setDisputeModal] = useState(null);
  const [disputeReasonInput, setDisputeReasonInput] = useState('');
  const [disputing, setDisputing] = useState(false);
  const [disputeError, setDisputeError] = useState('');

  useEffect(() => {
    fetchTasks();
  }, []);

  const fetchTasks = async () => {
    setLoading(true);
    try {
      const res = await api.get('/tasks/my-volunteer-tasks');
      setTasks(res.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateStatus = async (taskId, newStatus) => {
    try {
      await api.patch(`/tasks/${taskId}/status`, { status: newStatus });
      fetchTasks();
    } catch (e) {
      alert(e.response?.data?.message || 'Failed to update task status.');
    }
  };

  const handleDisputePayment = async (e) => {
    e.preventDefault();
    if (!disputeModal || !disputeReasonInput.trim()) return;
    setDisputing(true);
    setDisputeError('');
    try {
      await api.post(`/tasks/${disputeModal.id}/dispute-payment`, {
        reason: disputeReasonInput.trim()
      });
      alert('Payment issue reported successfully. The assigner and administrator have been notified.');
      setDisputeModal(null);
      setDisputeReasonInput('');
      fetchTasks();
    } catch (err) {
      setDisputeError(err.response?.data?.message || 'Failed to submit payment dispute.');
    } finally {
      setDisputing(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900">Volunteer Tasks</h1>
        <p className="text-xs text-slate-500 mt-1">Accept, manage, and complete assigned pickup & food drive tasks.</p>
      </div>

      {/* Dispute Modal */}
      {disputeModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full p-6 space-y-4">
            <h3 className="font-bold text-slate-900 text-base flex items-center gap-2 border-b pb-3">
              <AlertTriangle className="w-5 h-5 text-rose-600" />
              <span>Report Payment Issue — {disputeModal.title}</span>
            </h3>

            {disputeError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700 flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-red-500 flex-shrink-0" />
                <span>{disputeError}</span>
              </div>
            )}

            <p className="text-xs text-slate-600">
              Please provide details on why you are disputing the payment for this completed task (e.g., unpaid amount, incorrect transfer, or delay).
            </p>

            <form onSubmit={handleDisputePayment} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Dispute Reason *</label>
                <textarea
                  rows="3"
                  required
                  value={disputeReasonInput}
                  onChange={(e) => setDisputeReasonInput(e.target.value)}
                  placeholder="e.g. Payment not received after delivery confirmation."
                  className="w-full px-3.5 py-2.5 border rounded-lg focus:ring-2 focus:ring-rose-500 outline-none"
                ></textarea>
              </div>

              <div className="flex justify-end space-x-2 pt-3 border-t">
                <button
                  type="button"
                  onClick={() => setDisputeModal(null)}
                  className="px-4 py-2 border rounded-lg text-slate-600 hover:bg-slate-100 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={disputing}
                  className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white font-bold rounded-lg shadow"
                >
                  {disputing ? 'Submitting...' : 'Submit Payment Dispute'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {loading ? (
        <div className="text-xs text-slate-500 text-center py-12">Loading assigned tasks...</div>
      ) : tasks.length === 0 ? (
        <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center text-slate-500 text-xs">
          No tasks assigned yet. Toggle your availability status to <strong>ACTIVE</strong> so nearby NGOs & Restaurants can assign pickup tasks.
        </div>
      ) : (
        <div className="space-y-4">
          {tasks.map((t) => (
            <div key={t.id} className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-100 pb-3">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="px-2.5 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-800 uppercase">
                      {t.taskType}
                    </span>
                    <h3 className="font-bold text-slate-900 text-base">{t.title}</h3>
                  </div>
                  <p className="text-xs text-slate-600 mt-1">
                    Payment From: <strong>{t.assignedByName}</strong> ({t.assignedByRole})
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <span className={`px-3 py-1 rounded text-xs font-bold uppercase self-start sm:self-center ${
                    t.status === 'ASSIGNED' ? 'bg-amber-100 text-amber-800 border border-amber-300' :
                    t.status === 'ACCEPTED' ? 'bg-indigo-100 text-indigo-800 border border-indigo-300' :
                    t.status === 'IN_PROGRESS' ? 'bg-blue-100 text-blue-800 border border-blue-300' :
                    t.status === 'COMPLETED' ? 'bg-emerald-100 text-emerald-800 border border-emerald-300' : 'bg-slate-200 text-slate-800'
                  }`}>
                    {t.status}
                  </span>
                </div>
              </div>

              <div className="bg-slate-50 p-4 rounded-xl text-xs space-y-2 text-slate-700">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                  <div><strong>Pickup Address:</strong> {t.pickupAddress}</div>
                  <div><strong>Destination:</strong> {t.destinationAddress}</div>
                </div>
                {t.description && <div><strong>Instructions:</strong> {t.description}</div>}
                <div className="text-slate-500 pt-1 flex items-center gap-1">
                  <Clock className="w-3.5 h-3.5" />
                  <span>Scheduled Time: {new Date(t.scheduledTime).toLocaleString()}</span>
                </div>
              </div>

              {/* Pickup Verification Code Display for Volunteer */}
              {t.pickupCode && (
                <div className="bg-amber-50 border border-amber-200 p-3.5 rounded-xl flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
                  <div>
                    <div className="text-[11px] font-bold text-amber-900 uppercase">Authorized Pickup Verification Code</div>
                    <div className="font-mono text-lg font-extrabold text-amber-950 mt-0.5 tracking-wider">{t.pickupCode}</div>
                    <p className="text-[11px] text-amber-800 mt-0.5">Provide this code to the donor upon pickup, and to the NGO upon delivery.</p>
                  </div>
                </div>
              )}

              {/* Compensation & Payment Details */}
              <div className="p-3.5 bg-slate-100/70 rounded-xl flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
                <div>
                  {t.isPaid ? (
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-extrabold text-sm text-slate-900">
                          ₹{Number(t.paymentAmount).toFixed(2)} {t.currency}
                        </span>
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                          t.paymentStatus === 'PAID' ? 'bg-emerald-100 text-emerald-800 border border-emerald-300' :
                          t.paymentStatus === 'PENDING' ? 'bg-amber-100 text-amber-800 border border-amber-300' :
                          t.paymentStatus === 'DISPUTED' ? 'bg-rose-100 text-rose-800 border border-rose-300' :
                          'bg-slate-200 text-slate-700'
                        }`}>
                          Payment: {t.paymentStatus}
                        </span>
                      </div>
                      <p className="text-slate-600">Payer: <strong>{t.assignedByName}</strong> ({t.assignedByRole})</p>
                      {t.paymentTerms && (
                        <p className="text-slate-600">Payment Terms: {t.paymentTerms}</p>
                      )}
                      {t.paymentStatus === 'PAID' && t.paidAt && (
                        <p className="text-emerald-700 font-semibold text-[11px]">
                          Payment confirmed on {new Date(t.paidAt).toLocaleString()}
                        </p>
                      )}
                      {t.paymentStatus === 'DISPUTED' && t.disputeReason && (
                        <p className="text-rose-700 font-semibold text-[11px]">
                          Dispute Reason: {t.disputeReason}
                        </p>
                      )}
                    </div>
                  ) : (
                    <span className="text-slate-500 font-medium">Unpaid Voluntary Task</span>
                  )}
                </div>

                {t.isPaid && t.status === 'COMPLETED' && t.paymentStatus === 'PENDING' && (
                  <div className="text-[11px] text-amber-800 font-medium bg-amber-50 px-3 py-1.5 rounded-lg border border-amber-200">
                    Delivery verified. Awaiting payment confirmation from {t.assignedByName}.
                  </div>
                )}
              </div>

              {/* Task Action Controls */}
              <div className="flex flex-wrap items-center justify-end gap-2 pt-2">
                {t.status === 'ASSIGNED' && (
                  <>
                    <button
                      onClick={() => handleUpdateStatus(t.id, 'REJECTED')}
                      className="px-3.5 py-2 border border-slate-300 text-slate-700 hover:bg-slate-100 rounded-lg text-xs font-bold transition flex items-center gap-1"
                    >
                      <X className="w-4 h-4 text-rose-500" /> Decline
                    </button>
                    <button
                      onClick={() => handleUpdateStatus(t.id, 'ACCEPTED')}
                      className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold shadow transition flex items-center gap-1"
                    >
                      <Check className="w-4 h-4" /> Accept Task
                    </button>
                  </>
                )}

                {t.status === 'ACCEPTED' && (
                  <button
                    onClick={() => handleUpdateStatus(t.id, 'IN_PROGRESS')}
                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-bold shadow transition flex items-center gap-1"
                  >
                    <span>Mark IN_PROGRESS</span>
                    <ArrowRight className="w-4 h-4" />
                  </button>
                )}

                {t.status === 'IN_PROGRESS' && (
                  t.donationId ? (
                    <div className="text-[11px] font-medium text-slate-600 bg-slate-100 px-3 py-1.5 rounded-lg border border-slate-200 flex items-center gap-1.5">
                      <Truck className="w-4 h-4 text-indigo-600 flex-shrink-0" />
                      <span>Food in transit. Receiving NGO will confirm delivery receipt using code.</span>
                    </div>
                  ) : (
                    <button
                      onClick={() => handleUpdateStatus(t.id, 'COMPLETED')}
                      className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold shadow transition flex items-center gap-1"
                    >
                      <Check className="w-4 h-4" /> Mark Task COMPLETED
                    </button>
                  )
                )}

                {/* Dispute Payment Action */}
                {t.isPaid && t.status === 'COMPLETED' && (t.paymentStatus === 'PENDING' || t.paymentStatus === 'PAID') && (
                  <button
                    onClick={() => {
                      setDisputeModal(t);
                      setDisputeReasonInput('');
                      setDisputeError('');
                    }}
                    className="px-3 py-1.5 border border-rose-300 hover:bg-rose-50 text-rose-700 rounded-lg text-xs font-bold transition flex items-center gap-1"
                  >
                    <AlertTriangle className="w-3.5 h-3.5 text-rose-600" />
                    <span>Report Payment Issue</span>
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
