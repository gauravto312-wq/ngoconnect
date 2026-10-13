import React, { useState } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { PublicNavbar } from '../components/PublicNavbar';
import { Footer } from '../components/Footer';
import { HeartHandshake, UserPlus, AlertCircle, Building2, User, Users, UtensilsCrossed } from 'lucide-react';

export const Register = () => {
  const { register, loading } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [role, setRole] = useState(searchParams.get('role') || 'CITIZEN');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [address, setAddress] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('Uttar Pradesh');

  // NGO Specific
  const [ngoName, setNgoName] = useState('');
  const [description, setDescription] = useState('');
  const [cause, setCause] = useState('');

  // Restaurant Specific
  const [restaurantName, setRestaurantName] = useState('');

  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const payload = {
      name,
      email,
      phone,
      password,
      role,
      address,
      city,
      state,
      description,
      cause,
      ngoName: role === 'NGO' ? (ngoName || name) : undefined,
      restaurantName: role === 'RESTAURANT' ? (restaurantName || name) : undefined,
    };

    try {
      const user = await register(payload);
      navigate(`/${user.role.toLowerCase()}`);
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed. Please check form details.');
    }
  };

  const roleOptions = [
    { id: 'CITIZEN', title: 'Citizen', desc: 'Donate food/items & participate in food drives', icon: User },
    { id: 'VOLUNTEER', title: 'Volunteer', desc: 'Receive Volunteer ID & complete pickup tasks', icon: Users },
    { id: 'NGO', title: 'NGO Organization', desc: 'Claim donations & assign active volunteers', icon: Building2 },
    { id: 'RESTAURANT', title: 'Restaurant', desc: 'Post surplus food availability & donations', icon: UtensilsCrossed },
  ];

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <PublicNavbar />

      <div className="flex-1 max-w-3xl mx-auto w-full px-4 py-12">
        <div className="bg-white rounded-2xl border border-slate-200 shadow-md p-6 sm:p-10">
          <div className="text-center mb-8">
            <div className="w-12 h-12 rounded-xl bg-emerald-600 text-white flex items-center justify-center mx-auto mb-3 shadow">
              <UserPlus className="w-6 h-6" />
            </div>
            <h2 className="text-2xl font-extrabold text-slate-900">Create Your Profile</h2>
            <p className="text-xs text-slate-500 mt-1">Select your role to join the NGOConnect network</p>
          </div>

          {error && (
            <div className="mb-6 p-3.5 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700 flex items-center gap-2">
              <AlertCircle className="w-4 h-4 text-red-500 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Role Selector Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-8">
            {roleOptions.map((opt) => {
              const Icon = opt.icon;
              const isSelected = role === opt.id;
              return (
                <button
                  key={opt.id}
                  type="button"
                  onClick={() => setRole(opt.id)}
                  className={`p-3 rounded-xl border text-left transition flex flex-col justify-between ${
                    isSelected
                      ? 'border-emerald-600 bg-emerald-50/70 text-emerald-900 ring-2 ring-emerald-500/20'
                      : 'border-slate-200 bg-white hover:bg-slate-50 text-slate-700'
                  }`}
                >
                  <Icon className={`w-5 h-5 mb-2 ${isSelected ? 'text-emerald-600' : 'text-slate-400'}`} />
                  <div>
                    <div className="font-bold text-xs">{opt.title}</div>
                    <div className="text-[10px] text-slate-500 mt-0.5 line-clamp-2">{opt.desc}</div>
                  </div>
                </button>
              );
            })}
          </div>

          <form onSubmit={handleSubmit} className="space-y-4 text-xs">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-slate-700 font-semibold mb-1">Full Contact Name *</label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Rahul Kumar"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div>
                <label className="block text-slate-700 font-semibold mb-1">Email Address *</label>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div>
                <label className="block text-slate-700 font-semibold mb-1">Phone Number *</label>
                <input
                  type="text"
                  required
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="9876543210"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div>
                <label className="block text-slate-700 font-semibold mb-1">Password *</label>
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="At least 6 characters"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>
            </div>

            {/* Role-Specific Fields */}
            {role === 'NGO' && (
              <div className="p-4 bg-emerald-50/50 border border-emerald-100 rounded-xl space-y-3">
                <h4 className="font-bold text-emerald-900 text-xs">NGO Organization Details</h4>
                <div>
                  <label className="block text-slate-700 font-semibold mb-1">NGO Legal Name</label>
                  <input
                    type="text"
                    value={ngoName}
                    onChange={(e) => setNgoName(e.target.value)}
                    placeholder="XYZ Welfare Foundation"
                    className="w-full px-3 py-2 border rounded-lg bg-white focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
                <div>
                  <label className="block text-slate-700 font-semibold mb-1">Primary Cause / Mission</label>
                  <input
                    type="text"
                    value={cause}
                    onChange={(e) => setCause(e.target.value)}
                    placeholder="Hunger Relief & Food Distribution"
                    className="w-full px-3 py-2 border rounded-lg bg-white focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
              </div>
            )}

            {role === 'RESTAURANT' && (
              <div className="p-4 bg-emerald-50/50 border border-emerald-100 rounded-xl space-y-3">
                <h4 className="font-bold text-emerald-900 text-xs">Restaurant Business Details</h4>
                <div>
                  <label className="block text-slate-700 font-semibold mb-1">Restaurant / Brand Name</label>
                  <input
                    type="text"
                    value={restaurantName}
                    onChange={(e) => setRestaurantName(e.target.value)}
                    placeholder="ABC Restaurant & Bakery"
                    className="w-full px-3 py-2 border rounded-lg bg-white focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
              </div>
            )}

            {/* Location Information */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2">
              <div className="sm:col-span-3">
                <label className="block text-slate-700 font-semibold mb-1">Street Address *</label>
                <input
                  type="text"
                  required
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  placeholder="Gomti Nagar Main Road"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div>
                <label className="block text-slate-700 font-semibold mb-1">City *</label>
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
                <label className="block text-slate-700 font-semibold mb-1">State *</label>
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

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg shadow transition flex items-center justify-center space-x-2 text-sm mt-4"
            >
              <UserPlus className="w-4 h-4" />
              <span>{loading ? 'Creating Profile...' : `Register as ${role}`}</span>
            </button>
          </form>

          <div className="mt-6 text-center text-xs text-slate-500">
            Already have an account?{' '}
            <Link to="/login" className="font-bold text-emerald-600 hover:underline">
              Log in here
            </Link>
          </div>
        </div>
      </div>

      <Footer />
    </div>
  );
};
