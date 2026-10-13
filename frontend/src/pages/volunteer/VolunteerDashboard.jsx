import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import { Power, Truck, CheckCircle2, Clock, CalendarHeart, IndianRupee, AlertTriangle, ShieldCheck } from 'lucide-react';
import { Link } from 'react-router-dom';

export const VolunteerDashboard = () => {
  const { user } = useAuth();
  const [profile, setProfile] = useState(null);
  const [tasks, setTasks] = useState([]);
  const [earnings, setEarnings] = useState(null);
  const [earningsFilter, setEarningsFilter] = useState('all'); // 'all' | 'paid' | 'pending' | 'disputed'
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [profRes, taskRes, earnRes] = await Promise.all([
        api.get('/volunteers/profile'),
        api.get('/tasks/my-volunteer-tasks'),
        api.get('/volunteers/earnings')
      ]);
      setProfile(profRes.data);
      setTasks(taskRes.data);
      setEarnings(earnRes.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleStatus = async () => {
    if (!profile) return;
    try {
      const nextStatus = profile.availabilityStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
      const res = await api.patch('/volunteers/status', { availabilityStatus: nextStatus });
      setProfile(res.data);
    } catch (e) {
      alert('Failed to update availability status.');
    }
  };

  const getFilteredPaidTasks = () => {
    if (!earnings) return [];
    if (earningsFilter === 'paid') return earnings.paidTasks || [];
    if (earningsFilter === 'pending') return earnings.pendingTasks || [];
    if (earningsFilter === 'disputed') return earnings.disputedTasks || [];
    return earnings.allPaidTasks || [];
  };

  const filteredPaidTasks = getFilteredPaidTasks();

  return (
    <div className="space-y-6">
      {/* Volunteer Identity Card */}
      <div className="bg-gradient-to-r from-slate-900 via-emerald-950 to-slate-900 text-white p-6 rounded-2xl shadow-md border border-slate-800 flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="space-y-2">
          <div className="inline-block px-3 py-1 bg-amber-500/20 text-amber-300 border border-amber-500/40 rounded-lg text-xs font-mono font-bold tracking-widest uppercase">
            Volunteer ID: {profile?.volunteerCode || 'VOL-LOADING'}
          </div>
          <h1 className="text-2xl font-extrabold tracking-tight">{user?.name}</h1>
          <p className="text-xs text-slate-300">
            Location: {profile?.city}, {profile?.state} • Registered Volunteer
          </p>
        </div>

        {/* Availability Toggle */}
        <div className="bg-white/10 p-4 rounded-xl backdrop-blur border border-white/10 flex items-center space-x-4">
          <div>
            <div className="text-[11px] text-slate-300 font-semibold uppercase tracking-wider">Current Availability</div>
            <div className={`text-sm font-bold mt-0.5 ${profile?.availabilityStatus === 'ACTIVE' ? 'text-emerald-400' : 'text-rose-400'}`}>
              {profile?.availabilityStatus}
            </div>
          </div>

          <button
            onClick={handleToggleStatus}
            className={`px-4 py-2.5 rounded-xl font-bold text-xs flex items-center gap-1.5 shadow transition ${
              profile?.availabilityStatus === 'ACTIVE'
                ? 'bg-rose-600 hover:bg-rose-700 text-white'
                : 'bg-emerald-600 hover:bg-emerald-700 text-white'
            }`}
          >
            <Power className="w-4 h-4" />
            <span>Set {profile?.availabilityStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'}</span>
          </button>
        </div>
      </div>

      {/* Task Summary Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Tasks Assigned</div>
          <div className="text-2xl font-extrabold text-slate-900 mt-1">{tasks.length}</div>
        </div>
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Pending / In-Progress</div>
          <div className="text-2xl font-extrabold text-amber-600 mt-1">
            {tasks.filter((t) => t.status === 'ASSIGNED' || t.status === 'ACCEPTED' || t.status === 'IN_PROGRESS').length}
          </div>
        </div>
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Completed Tasks</div>
          <div className="text-2xl font-extrabold text-emerald-600 mt-1">
            {tasks.filter((t) => t.status === 'COMPLETED').length}
          </div>
        </div>
      </div>

      {/* Volunteer Earnings & Payment Tracking Section */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 space-y-6">
        <div>
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-extrabold text-slate-900 text-lg flex items-center gap-2">
                <IndianRupee className="w-5 h-5 text-emerald-600" />
                Volunteer Earnings & Payment Tracking
              </h3>
              <p className="text-xs text-slate-500 mt-0.5">
                Earnings calculated directly from persisted backend payment records. Offline payments are confirmed by the assigner.
              </p>
            </div>
          </div>
        </div>

        {/* Earnings Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-emerald-50/70 border border-emerald-200 rounded-xl p-4">
            <div className="text-xs font-bold text-emerald-800 uppercase tracking-wider">Total Received Earnings</div>
            <div className="text-2xl font-extrabold text-emerald-700 mt-1">
              ₹{Number(earnings?.totalEarnings || 0).toFixed(2)}
            </div>
            <div className="text-[11px] text-emerald-700 font-semibold mt-1">
              {earnings?.paidTasksCount || 0} Confirmed PAID tasks
            </div>
          </div>

          <div className="bg-amber-50/70 border border-amber-200 rounded-xl p-4">
            <div className="text-xs font-bold text-amber-800 uppercase tracking-wider">Pending Payments</div>
            <div className="text-2xl font-extrabold text-amber-700 mt-1">
              ₹{Number(earnings?.pendingEarnings || 0).toFixed(2)}
            </div>
            <div className="text-[11px] text-amber-700 font-semibold mt-1">
              {earnings?.pendingTasksCount || 0} Completed tasks awaiting payment
            </div>
          </div>

          <div className="bg-rose-50/70 border border-rose-200 rounded-xl p-4">
            <div className="text-xs font-bold text-rose-800 uppercase tracking-wider">Disputed Payments</div>
            <div className="text-2xl font-extrabold text-rose-700 mt-1">
              ₹{Number(earnings?.disputedEarnings || 0).toFixed(2)}
            </div>
            <div className="text-[11px] text-rose-700 font-semibold mt-1">
              {earnings?.disputedTasksCount || 0} Disputed tasks
            </div>
          </div>
        </div>

        {/* Filter Tabs for Paid Tasks */}
        <div className="space-y-3">
          <div className="flex flex-wrap items-center gap-2 border-b border-slate-100 pb-2">
            <button
              onClick={() => setEarningsFilter('all')}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition ${
                earningsFilter === 'all'
                  ? 'bg-slate-900 text-white shadow-sm'
                  : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              All Paid Tasks ({earnings?.allPaidTasks?.length || 0})
            </button>
            <button
              onClick={() => setEarningsFilter('paid')}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition ${
                earningsFilter === 'paid'
                  ? 'bg-emerald-600 text-white shadow-sm'
                  : 'text-emerald-800 hover:bg-emerald-50'
              }`}
            >
              Confirmed Paid ({earnings?.paidTasks?.length || 0})
            </button>
            <button
              onClick={() => setEarningsFilter('pending')}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition ${
                earningsFilter === 'pending'
                  ? 'bg-amber-600 text-white shadow-sm'
                  : 'text-amber-800 hover:bg-amber-50'
              }`}
            >
              Pending ({earnings?.pendingTasks?.length || 0})
            </button>
            <button
              onClick={() => setEarningsFilter('disputed')}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition ${
                earningsFilter === 'disputed'
                  ? 'bg-rose-600 text-white shadow-sm'
                  : 'text-rose-800 hover:bg-rose-50'
              }`}
            >
              Disputed ({earnings?.disputedTasks?.length || 0})
            </button>
          </div>

          {/* Paid Tasks List */}
          {filteredPaidTasks.length === 0 ? (
            <div className="text-xs text-slate-500 py-6 text-center border border-dashed border-slate-200 rounded-xl">
              No tasks found in this payment category.
            </div>
          ) : (
            <div className="space-y-3">
              {filteredPaidTasks.map((t) => (
                <div
                  key={t.id}
                  className="p-4 bg-slate-50 rounded-xl border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-slate-900 text-sm">{t.title}</span>
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                        t.paymentStatus === 'PAID' ? 'bg-emerald-100 text-emerald-800 border border-emerald-300' :
                        t.paymentStatus === 'PENDING' ? 'bg-amber-100 text-amber-800 border border-amber-300' :
                        t.paymentStatus === 'DISPUTED' ? 'bg-rose-100 text-rose-800 border border-rose-300' :
                        'bg-slate-200 text-slate-800'
                      }`}>
                        {t.paymentStatus}
                      </span>
                    </div>

                    <p className="text-slate-600">
                      Assigned by: <strong>{t.assignedByName}</strong> ({t.assignedByRole})
                    </p>

                    {t.paymentTerms && (
                      <p className="text-slate-500">Terms: {t.paymentTerms}</p>
                    )}

                    {t.paymentStatus === 'PAID' && t.paidAt && (
                      <p className="text-emerald-700 font-semibold">
                        Paid on: {new Date(t.paidAt).toLocaleString()}
                      </p>
                    )}

                    {t.paymentStatus === 'DISPUTED' && t.disputeReason && (
                      <p className="text-rose-700 font-semibold">
                        Dispute Reason: {t.disputeReason}
                      </p>
                    )}
                  </div>

                  <div className="text-right">
                    <div className="text-base font-extrabold text-slate-900">
                      ₹{Number(t.paymentAmount).toFixed(2)}
                    </div>
                    <div className="text-[10px] text-slate-400 uppercase font-mono">{t.currency}</div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Tasks Overview */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
        <div className="flex justify-between items-center mb-4">
          <h3 className="font-bold text-slate-900 text-lg">Your Assigned Tasks</h3>
          <Link to="/volunteer/tasks" className="text-xs font-bold text-emerald-600 hover:underline">
            View All Tasks &rarr;
          </Link>
        </div>

        {loading ? (
          <div className="text-xs text-slate-500 py-6 text-center">Loading tasks...</div>
        ) : tasks.length === 0 ? (
          <div className="text-xs text-slate-500 py-8 text-center border border-dashed border-slate-200 rounded-xl">
            No pickup or distribution tasks assigned yet. Toggle status to <strong>ACTIVE</strong> to appear in nearby NGO searches.
          </div>
        ) : (
          <div className="space-y-3">
            {tasks.slice(0, 3).map((t) => (
              <div key={t.id} className="p-4 bg-slate-50 rounded-xl border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2">
                    <h4 className="font-bold text-slate-900 text-sm">{t.title}</h4>
                    {t.isPaid && (
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-300">
                        ₹{Number(t.paymentAmount).toFixed(2)}
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-slate-600 mt-0.5">Assigned by: {t.assignedByName} ({t.assignedByRole})</p>
                  <p className="text-xs text-slate-500 mt-1">Pickup: {t.pickupAddress} &rarr; Destination: {t.destinationAddress}</p>
                </div>
                <span className={`px-3 py-1 rounded text-xs font-bold self-start sm:self-center ${
                  t.status === 'ASSIGNED' ? 'bg-amber-100 text-amber-800' :
                  t.status === 'ACCEPTED' ? 'bg-indigo-100 text-indigo-800' :
                  t.status === 'COMPLETED' ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-200 text-slate-800'
                }`}>
                  {t.status}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
