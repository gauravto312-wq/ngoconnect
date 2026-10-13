import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { AlertTriangle, CheckCircle2, XCircle, Clock, DollarSign, User, ShieldAlert, RefreshCw, MessageSquare } from 'lucide-react';

export const AdminPaymentDisputes = () => {
  const [disputes, setDisputes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedTask, setSelectedTask] = useState(null);
  const [resolution, setResolution] = useState('PAID');
  const [remarks, setRemarks] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [notification, setNotification] = useState('');

  useEffect(() => {
    fetchDisputes();
  }, []);

  const fetchDisputes = async () => {
    setLoading(true);
    try {
      const res = await api.get('/tasks/disputed');
      setDisputes(res.data || []);
    } catch (e) {
      console.error('Failed to fetch payment disputes', e);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenResolveModal = (task) => {
    setSelectedTask(task);
    setResolution('PAID');
    setRemarks('');
  };

  const handleCloseResolveModal = () => {
    setSelectedTask(null);
    setRemarks('');
  };

  const handleResolveSubmit = async (e) => {
    e.preventDefault();
    if (!selectedTask) return;
    setSubmitting(true);
    try {
      await api.post(`/tasks/${selectedTask.id}/resolve-dispute`, {
        resolution,
        remarks: remarks || `Dispute resolved as ${resolution} by Admin.`
      });
      setNotification(`Dispute for task #${selectedTask.id} (${selectedTask.title}) resolved successfully as ${resolution}!`);
      setTimeout(() => setNotification(''), 4000);
      handleCloseResolveModal();
      fetchDisputes();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to resolve payment dispute.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 flex items-center gap-2">
            <AlertTriangle className="w-6 h-6 text-amber-500" />
            <span>Volunteer Payment Disputes</span>
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Arbitrate payment issues reported between Volunteers and assigning NGOs or Restaurants.
          </p>
        </div>

        <button
          onClick={fetchDisputes}
          disabled={loading}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-white border border-slate-200 rounded-xl text-xs font-bold text-slate-700 hover:bg-slate-50 shadow-sm transition self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh Queue</span>
        </button>
      </div>

      {notification && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold rounded-xl flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>{notification}</span>
        </div>
      )}

      {/* Disputes Content */}
      {loading ? (
        <div className="text-center py-16 text-slate-400 text-xs">
          Loading payment dispute queue...
        </div>
      ) : disputes.length === 0 ? (
        <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center space-y-3">
          <CheckCircle2 className="w-12 h-12 text-emerald-500 mx-auto" />
          <h3 className="font-bold text-slate-800 text-sm">No Pending Disputes</h3>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            All volunteer task payments are in good standing. No outstanding payment arbitrations require attention.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {disputes.map((task) => (
            <div
              key={task.id}
              className="bg-white rounded-2xl border border-amber-200 p-6 shadow-sm hover:shadow-md transition space-y-4"
            >
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-100 pb-3">
                <div className="space-y-0.5">
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-100 text-amber-800 uppercase">
                      DISPUTED
                    </span>
                    <span className="text-xs font-mono font-bold text-slate-400">
                      Task #{task.id}
                    </span>
                    <span className="text-xs text-slate-500">
                      • {task.taskType}
                    </span>
                  </div>
                  <h3 className="font-bold text-slate-900 text-base">{task.title}</h3>
                </div>

                <div className="text-right">
                  <div className="text-lg font-black text-slate-900">
                    {task.currency || 'INR'} {task.paymentAmount ? Number(task.paymentAmount).toLocaleString() : '0.00'}
                  </div>
                  <div className="text-[10px] text-slate-400">Agreed Payment</div>
                </div>
              </div>

              {/* Dispute Details Grid */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 bg-slate-50 p-4 rounded-xl text-xs text-slate-600">
                <div className="space-y-1">
                  <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Assigned Volunteer</div>
                  <div className="font-bold text-slate-900">{task.volunteerName || 'Volunteer'}</div>
                  <div className="text-slate-500 text-[11px]">Code: {task.volunteerCode || 'N/A'}</div>
                </div>

                <div className="space-y-1">
                  <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Assigning Entity</div>
                  <div className="font-bold text-slate-900">{task.assignedByName || 'Assigner'}</div>
                  <div className="text-slate-500 text-[11px]">Role: {task.assignedByRole || 'ORGANIZATION'}</div>
                </div>

                <div className="space-y-1">
                  <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Payment Terms</div>
                  <div className="font-medium text-slate-800">{task.paymentTerms || 'Standard completion payout'}</div>
                  <div className="text-slate-400 text-[10px]">
                    Completed: {task.completedAt ? new Date(task.completedAt).toLocaleDateString() : 'Yes'}
                  </div>
                </div>
              </div>

              {/* Reported Reason Banner */}
              <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-xs space-y-1">
                <div className="font-bold text-amber-900 flex items-center gap-1.5">
                  <ShieldAlert className="w-3.5 h-3.5 text-amber-600" />
                  <span>Reported Dispute Reason:</span>
                </div>
                <p className="text-amber-800 italic pl-5">
                  "{task.disputeReason || 'Payment not released following task handover.'}"
                </p>
                {task.disputedAt && (
                  <div className="text-[10px] text-amber-600/80 pl-5">
                    Disputed on: {new Date(task.disputedAt).toLocaleString()}
                  </div>
                )}
              </div>

              {/* Action Button */}
              <div className="flex justify-end pt-2">
                <button
                  onClick={() => handleOpenResolveModal(task)}
                  className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs rounded-xl shadow-sm transition flex items-center gap-1.5"
                >
                  <DollarSign className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Resolve Dispute</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Resolution Modal */}
      {selectedTask && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-5 animate-in fade-in zoom-in-95">
            <div className="flex items-center justify-between border-b pb-3">
              <div>
                <h3 className="text-lg font-bold text-slate-900">Resolve Payment Dispute</h3>
                <p className="text-xs text-slate-500">Task #{selectedTask.id}: {selectedTask.title}</p>
              </div>
              <button
                onClick={handleCloseResolveModal}
                className="text-slate-400 hover:text-slate-600 p-1"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleResolveSubmit} className="space-y-4 text-xs">
              <div>
                <label className="block font-bold text-slate-700 uppercase mb-1">
                  Resolution Decision *
                </label>
                <select
                  value={resolution}
                  onChange={(e) => setResolution(e.target.value)}
                  className="w-full p-2.5 border rounded-xl bg-white font-medium outline-none focus:ring-2 focus:ring-emerald-500"
                >
                  <option value="PAID">Mark as PAID (Confirm Volunteer Payment Disbursed)</option>
                  <option value="RESOLVED">Mark as RESOLVED (Mutual Agreement Reached)</option>
                  <option value="CANCELLED">Cancel Dispute / Dismiss Claim</option>
                </select>
                <p className="text-[10px] text-slate-400 mt-1">
                  Setting PAID updates the task status to PAID and credits the volunteer's confirmed earnings.
                </p>
              </div>

              <div>
                <label className="block font-bold text-slate-700 uppercase mb-1">
                  Administrative Remarks / Notes
                </label>
                <textarea
                  rows={3}
                  value={remarks}
                  onChange={(e) => setRemarks(e.target.value)}
                  placeholder="Enter notes on arbitration, confirmation details, or instructions..."
                  className="w-full p-2.5 border rounded-xl outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t">
                <button
                  type="button"
                  onClick={handleCloseResolveModal}
                  className="px-4 py-2 border rounded-xl font-bold text-slate-600 hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl shadow transition"
                >
                  {submitting ? 'Applying Resolution...' : 'Apply Resolution'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
