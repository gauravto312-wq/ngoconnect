import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { PublicNavbar } from '../components/PublicNavbar';
import { Footer } from '../components/Footer';
import api from '../services/api';
import { HeartHandshake, ShoppingBag, Users, Building2, Truck, ArrowRight, ShieldCheck, CheckCircle2 } from 'lucide-react';

export const Home = () => {
  const [recentDonations, setRecentDonations] = useState([]);
  const [foodDrives, setFoodDrives] = useState([]);

  useEffect(() => {
    fetchMarketplace();
    fetchDrives();
  }, []);

  const fetchMarketplace = async () => {
    try {
      const res = await api.get('/donations/marketplace');
      setRecentDonations(res.data.slice(0, 4));
    } catch (e) {
      // ignore
    }
  };

  const fetchDrives = async () => {
    try {
      const res = await api.get('/food-drives/public');
      setFoodDrives(res.data.slice(0, 3));
    } catch (e) {
      // ignore
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <PublicNavbar />

      {/* Hero Banner */}
      <section className="bg-gradient-to-br from-slate-900 via-emerald-950 to-slate-900 text-white py-20 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-12 items-center">
          <div className="space-y-6">
            <div className="inline-flex items-center space-x-2 bg-emerald-900/60 border border-emerald-700/60 px-3.5 py-1.5 rounded-full text-xs font-semibold text-emerald-300">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <span>Verified NGO & Surplus Food Recovery Network</span>
            </div>
            <h1 className="text-4xl sm:text-5xl font-extrabold tracking-tight leading-tight">
              Bridge Surplus Food to <span className="text-emerald-400">Communities in Need</span>.
            </h1>
            <p className="text-slate-300 text-base sm:text-lg leading-relaxed">
              NGOConnect connects Citizens, Restaurants, NGOs, and Volunteers in a localized ecosystem to facilitate food pickup, item donations, and community food drives.
            </p>
            <div className="flex flex-wrap gap-3 pt-2">
              <Link
                to="/register"
                className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-sm rounded-xl shadow-lg flex items-center space-x-2 transition"
              >
                <span>Join NGOConnect</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
              <Link
                to="/marketplace"
                className="px-4 py-2.5 bg-slate-800 hover:bg-slate-700 text-white font-semibold text-sm rounded-xl border border-slate-700 flex items-center space-x-1.5 transition"
              >
                <ShoppingBag className="w-4 h-4 text-emerald-400" />
                <span>Marketplace</span>
              </Link>
              <Link
                to="/ngos"
                className="px-4 py-2.5 bg-slate-800 hover:bg-slate-700 text-white font-semibold text-sm rounded-xl border border-slate-700 flex items-center space-x-1.5 transition"
              >
                <HeartHandshake className="w-4 h-4 text-emerald-400" />
                <span>Discover NGOs</span>
              </Link>
              <Link
                to="/restaurants"
                className="px-4 py-2.5 bg-slate-800 hover:bg-slate-700 text-white font-semibold text-sm rounded-xl border border-slate-700 flex items-center space-x-1.5 transition"
              >
                <Building2 className="w-4 h-4 text-emerald-400" />
                <span>Discover Restaurants</span>
              </Link>
            </div>
          </div>

          <div className="bg-white/5 border border-white/10 rounded-2xl p-8 backdrop-blur shadow-2xl">
            <h3 className="text-xl font-bold text-white mb-6 border-b border-white/10 pb-4">Ecosystem Architecture</h3>
            <div className="space-y-4 text-sm">
              <div className="flex items-start space-x-3">
                <div className="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold">1</div>
                <div>
                  <h4 className="font-semibold text-slate-200">Donors & Restaurants</h4>
                  <p className="text-xs text-slate-400">Publish surplus food availability & item donations tied to local branches.</p>
                </div>
              </div>
              <div className="flex items-start space-x-3">
                <div className="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold">2</div>
                <div>
                  <h4 className="font-semibold text-slate-200">Verified NGOs</h4>
                  <p className="text-xs text-slate-400">Browse nearby donations & claim in 1-click. Receive private pickup code.</p>
                </div>
              </div>
              <div className="flex items-start space-x-3">
                <div className="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold">3</div>
                <div>
                  <h4 className="font-semibold text-slate-200">Active Volunteers</h4>
                  <p className="text-xs text-slate-400">Assigned pickup tasks based on active status & city location.</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Statistics / Impact Preview */}
      <section className="py-12 bg-white border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid grid-cols-2 md:grid-cols-4 gap-6 text-center">
          <div className="p-4">
            <div className="text-3xl font-extrabold text-slate-900">5 Roles</div>
            <div className="text-xs text-slate-500 uppercase tracking-wider font-semibold mt-1">Citizen, Volunteer, NGO, Restaurant, Admin</div>
          </div>
          <div className="p-4">
            <div className="text-3xl font-extrabold text-emerald-600">Secure</div>
            <div className="text-xs text-slate-500 uppercase tracking-wider font-semibold mt-1">Private Pickup Verification Codes</div>
          </div>
          <div className="p-4">
            <div className="text-3xl font-extrabold text-slate-900">Local First</div>
            <div className="text-xs text-slate-500 uppercase tracking-wider font-semibold mt-1">City-based Location Matching</div>
          </div>
          <div className="p-4">
            <div className="text-3xl font-extrabold text-emerald-600">Drives</div>
            <div className="text-xs text-slate-500 uppercase tracking-wider font-semibold mt-1">Community Food Distribution</div>
          </div>
        </div>
      </section>

      {/* Live Marketplace Highlights */}
      <section className="py-16 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full">
        <div className="flex justify-between items-end mb-8">
          <div>
            <h2 className="text-2xl font-bold text-slate-900">Active Donation Marketplace</h2>
            <p className="text-slate-600 text-sm mt-1">Public listing of available donations. NGO claim required to view pickup code.</p>
          </div>
          <Link to="/marketplace" className="text-sm font-semibold text-emerald-600 hover:text-emerald-700 flex items-center gap-1">
            <span>View All</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>

        {recentDonations.length === 0 ? (
          <div className="bg-white p-8 rounded-xl border border-slate-200 text-center text-slate-500">
            No active donations posted yet. Register as a Donor or Restaurant to create the first donation!
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {recentDonations.map((d) => (
              <div key={d.id} className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm hover:shadow-md transition">
                <div className="flex items-center justify-between mb-3">
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 uppercase">
                    {d.category}
                  </span>
                  <span className="text-xs font-semibold text-slate-500">{d.city}</span>
                </div>
                <h3 className="font-bold text-slate-900 text-base mb-1 truncate">{d.title}</h3>
                <p className="text-xs text-slate-600 mb-4 line-clamp-2">{d.description || 'No additional details provided.'}</p>
                <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
                  <span>Quantity: <strong className="text-slate-800">{d.quantity} {d.unit}</strong></span>
                  <span className="text-emerald-600 font-bold">{d.status}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Community Food Drives Highlight */}
      <section className="py-16 bg-slate-100 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto">
          <div className="mb-8">
            <h2 className="text-2xl font-bold text-slate-900">Upcoming Community Food Drives</h2>
            <p className="text-slate-600 text-sm mt-1">Joint food distribution events hosted by Citizens and NGOs.</p>
          </div>

          {foodDrives.length === 0 ? (
            <div className="bg-white p-8 rounded-xl border border-slate-200 text-center text-slate-500">
              No food drives scheduled at the moment.
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {foodDrives.map((fd) => (
                <div key={fd.id} className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
                  <div className="text-xs font-bold text-emerald-600 uppercase tracking-wider mb-2">{fd.city}, {fd.state}</div>
                  <h3 className="font-bold text-slate-900 text-lg mb-2">{fd.title}</h3>
                  <p className="text-xs text-slate-600 mb-4 line-clamp-2">{fd.purpose || fd.description}</p>
                  <div className="bg-slate-50 p-3 rounded-lg text-xs space-y-1 text-slate-600 mb-4">
                    <div><strong>Date:</strong> {fd.eventDate}</div>
                    <div><strong>Time:</strong> {fd.startTime} - {fd.endTime}</div>
                    <div><strong>Host:</strong> {fd.createdByName} ({fd.createdByRole})</div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      <Footer />
    </div>
  );
};
