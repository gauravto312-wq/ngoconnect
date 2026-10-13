import React, { useState, useEffect } from 'react';
import { useOutletContext } from 'react-router-dom';
import api from '../../services/api';
import { Bell, CheckCheck, Clock, AlertCircle, Loader2 } from 'lucide-react';

export const NotificationsView = () => {
  const outletCtx = useOutletContext();
  const setUnreadCount = outletCtx?.setUnreadCount;
  const refreshUnreadCount = outletCtx?.refreshUnreadCount;

  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [processingIds, setProcessingIds] = useState([]);
  const [markingAll, setMarkingAll] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const syncUnreadCount = (notifList) => {
    const unread = notifList.filter((n) => !n.isRead).length;
    if (setUnreadCount) {
      setUnreadCount(unread);
    }
    window.dispatchEvent(new CustomEvent('notifications-updated', { detail: { unreadCount: unread } }));
  };

  useEffect(() => {
    fetchNotifications();
  }, []);

  const fetchNotifications = async () => {
    setLoading(true);
    setErrorMessage('');
    try {
      const res = await api.get('/notifications');
      const data = res.data || [];
      setNotifications(data);
      syncUnreadCount(data);
    } catch (e) {
      console.error(e);
      setErrorMessage('Failed to load notifications. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleMarkAsRead = async (id) => {
    if (processingIds.includes(id)) return;

    setProcessingIds((prev) => [...prev, id]);
    setErrorMessage('');

    try {
      await api.patch(`/notifications/${id}/read`);
      setNotifications((prev) => {
        const next = prev.map((n) => (n.id === id ? { ...n, isRead: true } : n));
        syncUnreadCount(next);
        return next;
      });
    } catch (e) {
      console.error(e);
      setErrorMessage('Failed to mark notification as read. Please try again.');
    } finally {
      setProcessingIds((prev) => prev.filter((item) => item !== id));
    }
  };

  const handleMarkAllAsRead = async () => {
    const hasUnread = notifications.some((n) => !n.isRead);
    if (markingAll || !hasUnread) return;

    setMarkingAll(true);
    setErrorMessage('');

    try {
      await api.patch('/notifications/read-all');
      setNotifications((prev) => {
        const next = prev.map((n) => ({ ...n, isRead: true }));
        syncUnreadCount(next);
        return next;
      });
    } catch (e) {
      console.error(e);
      setErrorMessage('Failed to mark all notifications as read. Please try again.');
    } finally {
      setMarkingAll(false);
    }
  };

  const unreadCountLocal = notifications.filter((n) => !n.isRead).length;

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Notifications</h1>
          <p className="text-xs text-slate-500 mt-1">Platform updates, claim notifications, task dispatches & food drives.</p>
        </div>

        <button
          onClick={handleMarkAllAsRead}
          disabled={markingAll || unreadCountLocal === 0}
          className={`px-3.5 py-1.5 border rounded-lg text-xs font-semibold flex items-center gap-1.5 transition ${
            markingAll || unreadCountLocal === 0
              ? 'border-slate-200 text-slate-400 bg-slate-50 cursor-not-allowed opacity-60'
              : 'border-slate-300 text-slate-700 hover:bg-slate-100'
          }`}
        >
          {markingAll ? (
            <Loader2 className="w-4 h-4 text-emerald-600 animate-spin" />
          ) : (
            <CheckCheck className="w-4 h-4 text-emerald-600" />
          )}
          <span>{markingAll ? 'Marking All...' : 'Mark All Read'}</span>
        </button>
      </div>

      {errorMessage && (
        <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 rounded-xl text-xs flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 flex-shrink-0 text-rose-600" />
            <span>{errorMessage}</span>
          </div>
          <button
            onClick={() => setErrorMessage('')}
            className="text-rose-500 hover:text-rose-800 text-xs font-semibold underline ml-3"
          >
            Dismiss
          </button>
        </div>
      )}

      {loading ? (
        <div className="text-xs text-slate-500 text-center py-12 flex items-center justify-center gap-2">
          <Loader2 className="w-4 h-4 animate-spin text-emerald-600" />
          <span>Loading notifications...</span>
        </div>
      ) : notifications.length === 0 ? (
        <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center text-slate-500 text-xs">
          No notifications in your inbox.
        </div>
      ) : (
        <div className="space-y-3">
          {notifications.map((n) => {
            const isProcessing = processingIds.includes(n.id);
            return (
              <div
                key={n.id}
                onClick={() => !n.isRead && !isProcessing && handleMarkAsRead(n.id)}
                className={`p-4 rounded-xl border transition flex items-start justify-between gap-4 ${
                  n.isRead
                    ? 'bg-white border-slate-200 text-slate-600 cursor-default'
                    : isProcessing
                    ? 'bg-emerald-50/40 border-emerald-300 opacity-70 cursor-wait'
                    : 'bg-emerald-50/60 border-emerald-200 text-slate-900 shadow-sm cursor-pointer hover:border-emerald-300'
                }`}
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className={`w-2 h-2 rounded-full ${n.isRead ? 'bg-slate-300' : 'bg-emerald-600'}`}></span>
                    <h4 className="font-bold text-sm">{n.title}</h4>
                  </div>
                  <p className="text-xs text-slate-600 pl-4 whitespace-pre-line">{n.message}</p>
                  <div className="text-[10px] text-slate-400 pl-4 flex items-center gap-1 pt-1">
                    <Clock className="w-3 h-3" />
                    <span>{new Date(n.createdAt).toLocaleString()}</span>
                  </div>
                </div>

                {!n.isRead && (
                  <span className="text-[10px] font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded flex-shrink-0 flex items-center gap-1">
                    {isProcessing ? (
                      <>
                        <Loader2 className="w-2.5 h-2.5 animate-spin" />
                        <span>Updating...</span>
                      </>
                    ) : (
                      'NEW'
                    )}
                  </span>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
