import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Link, useLocation, useNavigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import {
  HeartHandshake,
  LayoutDashboard,
  User,
  ShoppingBag,
  Truck,
  CheckCircle2,
  Users,
  UtensilsCrossed,
  Bell,
  LogOut,
  Building2,
  ShieldAlert,
  ShieldCheck,
  CalendarHeart,
  Power,
  AlertTriangle,
  Mail
} from 'lucide-react';

export const RoleDashboardLayout = () => {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const [unreadCount, setUnreadCount] = useState(0);
  const [volunteerInfo, setVolunteerInfo] = useState(null);
  const [togglingStatus, setTogglingStatus] = useState(false);
  const lastFetchRef = useRef(0);

  const fetchUnreadCount = useCallback(async (force = false) => {
    if (!user) {
      setUnreadCount(0);
      return;
    }
    const now = Date.now();
    if (!force && now - lastFetchRef.current < 2000) {
      return;
    }
    lastFetchRef.current = now;
    try {
      const res = await api.get('/notifications/unread-count');
      const count = typeof res.data?.unreadCount === 'number' ? Math.max(0, res.data.unreadCount) : 0;
      setUnreadCount(count);
    } catch (e) {
      // keep existing unread count on network failure
    }
  }, [user]);

  useEffect(() => {
    if (user) {
      fetchUnreadCount(true);
      if (user.role === 'VOLUNTEER') {
        fetchVolunteerProfile();
      }
    } else {
      setUnreadCount(0);
    }
  }, [user, fetchUnreadCount]);

  useEffect(() => {
    if (!user) return;

    const handleVisibilityOrFocus = () => {
      if (document.visibilityState === 'visible') {
        fetchUnreadCount(false);
      }
    };

    const handleCustomUpdate = (event) => {
      if (event.detail && typeof event.detail.unreadCount === 'number') {
        setUnreadCount(Math.max(0, event.detail.unreadCount));
      } else {
        fetchUnreadCount(true);
      }
    };

    window.addEventListener('visibilitychange', handleVisibilityOrFocus);
    window.addEventListener('focus', handleVisibilityOrFocus);
    window.addEventListener('notifications-updated', handleCustomUpdate);

    return () => {
      window.removeEventListener('visibilitychange', handleVisibilityOrFocus);
      window.removeEventListener('focus', handleVisibilityOrFocus);
      window.removeEventListener('notifications-updated', handleCustomUpdate);
    };
  }, [user, fetchUnreadCount]);

  const fetchVolunteerProfile = async () => {
    try {
      const res = await api.get('/volunteers/profile');
      setVolunteerInfo(res.data);
    } catch (e) {
      // ignore
    }
  };

  const handleToggleVolunteerStatus = async () => {
    if (!volunteerInfo || togglingStatus) return;
    setTogglingStatus(true);
    try {
      const nextStatus = volunteerInfo.availabilityStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
      const res = await api.patch('/volunteers/status', { availabilityStatus: nextStatus });
      setVolunteerInfo(res.data);
    } catch (e) {
      alert('Failed to update status');
    } finally {
      setTogglingStatus(false);
    }
  };

  const role = user?.role || 'CITIZEN';

  const navItems = {
    CITIZEN: [
      { label: 'Dashboard', path: '/citizen', icon: LayoutDashboard },
      { label: 'My Profile', path: '/citizen/profile', icon: User },
      { label: 'My Donations', path: '/citizen/donations', icon: HeartHandshake },
      { label: 'Marketplace', path: '/citizen/marketplace', icon: ShoppingBag },
      { label: 'Food Drives', path: '/citizen/food-drives', icon: CalendarHeart },
      { label: 'Notifications', path: '/citizen/notifications', icon: Bell, badge: unreadCount },
    ],
    VOLUNTEER: [
      { label: 'Dashboard', path: '/volunteer', icon: LayoutDashboard },
      { label: 'My Profile', path: '/volunteer/profile', icon: User },
      { label: 'Assigned Tasks', path: '/volunteer/tasks', icon: Truck },
      { label: 'Food Drives', path: '/volunteer/food-drives', icon: CalendarHeart },
      { label: 'Notifications', path: '/volunteer/notifications', icon: Bell, badge: unreadCount },
    ],
    NGO: [
      { label: 'Dashboard', path: '/ngo', icon: LayoutDashboard },
      { label: 'NGO Profile', path: '/ngo/profile', icon: Building2 },
      { label: 'Verification', path: '/ngo/verification', icon: ShieldCheck },
      { label: 'Marketplace', path: '/ngo/marketplace', icon: ShoppingBag },
      { label: 'Claimed Donations', path: '/ngo/claimed-donations', icon: CheckCircle2 },
      { label: 'Assign Volunteers', path: '/ngo/volunteers', icon: Users },
      { label: 'Food Drives', path: '/ngo/food-drives', icon: CalendarHeart },
      { label: 'Notifications', path: '/ngo/notifications', icon: Bell, badge: unreadCount },
    ],
    RESTAURANT: [
      { label: 'Dashboard', path: '/restaurant', icon: LayoutDashboard },
      { label: 'Profile', path: '/restaurant/profile', icon: Building2 },
      { label: 'Branches', path: '/restaurant/branches', icon: UtensilsCrossed },
      { label: 'Food Availability', path: '/restaurant/availability', icon: ShoppingBag },
      { label: 'Donations', path: '/restaurant/donations', icon: HeartHandshake },
      { label: 'Marketplace', path: '/restaurant/marketplace', icon: ShoppingBag },
      { label: 'Assign Volunteers', path: '/restaurant/volunteers', icon: Users },
      { label: 'Food Drives', path: '/restaurant/food-drives', icon: CalendarHeart },
      { label: 'Notifications', path: '/restaurant/notifications', icon: Bell, badge: unreadCount },
    ],
    ADMIN: [
      { label: 'Admin Dashboard', path: '/admin', icon: LayoutDashboard },
      { label: 'User Management', path: '/admin/users', icon: Users },
      { label: 'NGO Verifications', path: '/admin/ngo-verifications', icon: ShieldCheck },
      { label: 'Restaurant Verifications', path: '/admin/restaurant-verifications', icon: Building2 },
      { label: 'Payment Disputes', path: '/admin/disputes', icon: AlertTriangle },
      { label: 'Contact Inquiries', path: '/admin/inquiries', icon: Mail },
      { label: 'Food Drives', path: '/admin/food-drives', icon: CalendarHeart },
      { label: 'Notifications', path: '/admin/notifications', icon: Bell, badge: unreadCount },
    ]
  };

  const currentNav = navItems[role] || navItems.CITIZEN;

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col md:flex-row">
      {/* Sidebar */}
      <aside className="w-full md:w-64 bg-slate-900 text-slate-300 flex-shrink-0 flex flex-col justify-between border-r border-slate-800">
        <div>
          {/* Brand Header */}
          <div className="p-5 border-b border-slate-800 flex items-center justify-between">
            <Link to="/" className="flex items-center space-x-2">
              <div className="w-9 h-9 rounded-lg bg-emerald-600 flex items-center justify-center text-white font-bold text-lg shadow">
                <HeartHandshake className="w-5 h-5" />
              </div>
              <span className="text-lg font-bold text-white tracking-tight">NGO<span className="text-emerald-500">Connect</span></span>
            </Link>
          </div>

          {/* User Profile Summary */}
          <div className="p-4 border-b border-slate-800 bg-slate-800/40">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 rounded-full bg-emerald-700 text-white font-bold flex items-center justify-center text-sm uppercase">
                {user?.name?.charAt(0) || 'U'}
              </div>
              <div className="overflow-hidden">
                <h4 className="text-sm font-semibold text-white truncate">{user?.name}</h4>
                <div className="flex items-center space-x-1.5 mt-0.5">
                  <span className="inline-block px-2 py-0.5 text-[10px] font-bold rounded bg-emerald-950 text-emerald-400 border border-emerald-800 uppercase tracking-wider">
                    {user?.role}
                  </span>
                </div>
              </div>
            </div>

            {/* Volunteer Unique ID & Active Toggle Badge */}
            {role === 'VOLUNTEER' && volunteerInfo && (
              <div className="mt-3 pt-3 border-t border-slate-700/60 flex flex-col space-y-2">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-slate-400 font-medium">Volunteer ID:</span>
                  <span className="font-mono font-bold text-amber-400 bg-amber-950/60 px-2 py-0.5 rounded border border-amber-800/60">
                    {volunteerInfo.volunteerCode}
                  </span>
                </div>
                <div className="flex items-center justify-between text-xs">
                  <span className="text-slate-400 font-medium">Status:</span>
                  <button
                    onClick={handleToggleVolunteerStatus}
                    disabled={togglingStatus}
                    className={`px-2.5 py-1 rounded text-[11px] font-bold flex items-center gap-1 transition ${
                      volunteerInfo.availabilityStatus === 'ACTIVE'
                        ? 'bg-emerald-600 hover:bg-emerald-700 text-white'
                        : 'bg-rose-600 hover:bg-rose-700 text-white'
                    }`}
                  >
                    <Power className="w-3 h-3" />
                    {volunteerInfo.availabilityStatus}
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Navigation Links */}
          <nav className="p-3 space-y-1">
            {currentNav.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname === item.path;
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`flex items-center justify-between px-3.5 py-2.5 rounded-lg text-sm font-medium transition ${
                    isActive
                      ? 'bg-emerald-600 text-white shadow-sm'
                      : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                  }`}
                >
                  <div className="flex items-center space-x-3">
                    <Icon className="w-4 h-4" />
                    <span>{item.label}</span>
                  </div>
                  {item.badge > 0 && (
                    <span className="px-2 py-0.5 text-xs font-bold bg-rose-500 text-white rounded-full">
                      {item.badge}
                    </span>
                  )}
                </Link>
              );
            })}
          </nav>
        </div>

        {/* Footer / Logout */}
        <div className="p-4 border-t border-slate-800">
          <button
            onClick={() => { logout(); navigate('/login'); }}
            className="w-full flex items-center justify-center space-x-2 px-4 py-2.5 rounded-lg text-sm font-medium text-slate-400 hover:bg-red-950/40 hover:text-red-400 border border-slate-800 hover:border-red-900/60 transition"
          >
            <LogOut className="w-4 h-4" />
            <span>Logout Session</span>
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="flex-1 min-w-0 p-4 sm:p-6 lg:p-8 overflow-y-auto">
        <Outlet context={{ unreadCount, setUnreadCount, refreshUnreadCount: fetchUnreadCount }} />
      </main>
    </div>
  );
};
