import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { PublicNavbar } from '../components/PublicNavbar';
import { Footer } from '../components/Footer';
import { VerificationBadge } from '../components/VerificationBadge';
import api, { getUploadUrl } from '../services/api';
import { Search, MapPin, Heart, Phone, Mail, ExternalLink, X, MessageSquare, Building, HeartHandshake } from 'lucide-react';

export const NgoDiscovery = () => {
  const [ngos, setNgos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [city, setCity] = useState('');
  const [selectedNgo, setSelectedNgo] = useState(null);
  const [contactMsg, setContactMsg] = useState('');
  const [msgSent, setMsgSent] = useState(false);

  useEffect(() => {
    fetchNgos();
  }, []);

  const fetchNgos = async (searchQuery = search, cityQuery = city) => {
    setLoading(true);
    try {
      const res = await api.get('/ngos/public', {
        params: { search: searchQuery, city: cityQuery }
      });
      setNgos(res.data);
    } catch (e) {
      console.error('Failed to fetch NGOs', e);
    } finally {
      setLoading(false);
    }
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    fetchNgos(search, city);
  };

  const [senderName, setSenderName] = useState('');
  const [senderContact, setSenderContact] = useState('');
  const [sending, setSending] = useState(false);

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!contactMsg.trim() || !selectedNgo) return;
    setSending(true);
    try {
      await api.post(`/ngos/public/${selectedNgo.id}/contact`, {
        senderName: senderName || 'Website Visitor',
        senderContact: senderContact || 'N/A',
        message: contactMsg
      });
      setMsgSent(true);
      setTimeout(() => {
        setMsgSent(false);
        setContactMsg('');
        setSenderName('');
        setSenderContact('');
        setSelectedNgo(null);
      }, 2000);
    } catch (err) {
      alert('Failed to send message to NGO.');
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <PublicNavbar />

      {/* Hero Header */}
      <section className="bg-gradient-to-r from-emerald-900 via-slate-900 to-emerald-950 text-white py-14 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto text-center space-y-4">
          <div className="inline-flex items-center space-x-2 bg-emerald-800/50 border border-emerald-600/50 px-3.5 py-1.5 rounded-full text-xs font-semibold text-emerald-300">
            <HeartHandshake className="w-4 h-4 text-emerald-400" />
            <span>NGO Directory & Social Impact Partners</span>
          </div>
          <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight">
            Discover <span className="text-emerald-400">Verified NGOs</span>
          </h1>
          <p className="max-w-2xl mx-auto text-slate-300 text-sm sm:text-base">
            Explore registered non-governmental organizations dedicated to hunger relief, food recovery, and community welfare.
          </p>

          {/* Search Engine Bar */}
          <form onSubmit={handleSearchSubmit} className="max-w-3xl mx-auto mt-6 bg-white p-2 sm:p-3 rounded-2xl shadow-xl flex flex-col sm:flex-row gap-2 text-slate-800">
            <div className="flex-1 flex items-center px-3 bg-slate-100 rounded-xl border border-slate-200">
              <Search className="w-4 h-4 text-slate-400 shrink-0" />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search NGO name, cause, address..."
                className="w-full px-2.5 py-2.5 bg-transparent text-xs sm:text-sm outline-none font-medium"
              />
            </div>
            <div className="w-full sm:w-48 flex items-center px-3 bg-slate-100 rounded-xl border border-slate-200">
              <MapPin className="w-4 h-4 text-slate-400 shrink-0" />
              <input
                type="text"
                value={city}
                onChange={(e) => setCity(e.target.value)}
                placeholder="City (e.g. Lucknow)"
                className="w-full px-2.5 py-2.5 bg-transparent text-xs sm:text-sm outline-none font-medium"
              />
            </div>
            <button
              type="submit"
              className="px-6 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs sm:text-sm rounded-xl transition shadow"
            >
              Search NGOs
            </button>
          </form>
        </div>
      </section>

      {/* Main NGO Grid */}
      <main className="max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-12 flex-1">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-xl font-bold text-slate-900">
            Registered Organizations <span className="text-slate-500 text-sm font-normal">({ngos.length})</span>
          </h2>
        </div>

        {loading ? (
          <div className="text-center py-16 text-slate-500 text-sm">Loading NGOs...</div>
        ) : ngos.length === 0 ? (
          <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center text-slate-500 space-y-3">
            <Building className="w-12 h-12 text-slate-300 mx-auto" />
            <h3 className="text-base font-bold text-slate-700">No NGOs Found</h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">Try clearing your search keyword or city filter to view available organizations.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {ngos.map((ngo) => (
              <div key={ngo.id} className="bg-white rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition flex flex-col overflow-hidden">
                {/* NGO Card Header / Photo */}
                <div className="h-44 bg-slate-800 relative overflow-hidden flex items-center justify-center">
                  {ngo.profileImageUrl ? (
                    <img src={getUploadUrl(ngo.profileImageUrl)} alt={ngo.ngoName} className="w-full h-full object-cover" onError={(e) => { e.target.style.display = 'none'; }} />
                  ) : (
                    <div className="w-full h-full bg-gradient-to-br from-emerald-800 to-slate-900 flex flex-col items-center justify-center p-4 text-center">
                      <HeartHandshake className="w-12 h-12 text-emerald-400 opacity-80 mb-1" />
                      <span className="text-white font-bold text-sm line-clamp-1">{ngo.ngoName}</span>
                    </div>
                  )}

                  <div className="absolute top-3 right-3">
                    <VerificationBadge status={ngo.verificationStatus} />
                  </div>
                </div>

                {/* Card Body */}
                <div className="p-5 flex-1 flex flex-col justify-between space-y-4">
                  <div className="space-y-2">
                    <div className="flex items-center gap-1.5 text-xs font-semibold text-emerald-600 uppercase tracking-wide">
                      <Heart className="w-3.5 h-3.5" />
                      <span>{ngo.cause || 'Community Relief'}</span>
                    </div>
                    <h3 className="font-bold text-slate-900 text-lg line-clamp-1">{ngo.ngoName}</h3>
                    <p className="text-xs text-slate-600 line-clamp-2 leading-relaxed">
                      {ngo.description || 'Dedicated non-profit organization serving local communities.'}
                    </p>
                  </div>

                  <div className="text-xs text-slate-500 space-y-1 bg-slate-50 p-3 rounded-xl border border-slate-100">
                    <div className="flex items-center gap-1.5 font-medium text-slate-700">
                      <MapPin className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                      <span className="truncate">{ngo.address}, {ngo.city}, {ngo.state}</span>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="pt-2 flex items-center gap-2 border-t border-slate-100">
                    <Link
                      to={`/ngos/${ngo.id}`}
                      className="flex-1 py-2 px-3 bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs rounded-xl text-center transition flex items-center justify-center gap-1"
                    >
                      <span>View Profile</span>
                      <ExternalLink className="w-3.5 h-3.5" />
                    </Link>
                    <button
                      onClick={() => setSelectedNgo(ngo)}
                      className="py-2 px-3 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 font-bold text-xs rounded-xl border border-emerald-200 transition flex items-center justify-center gap-1"
                    >
                      <Phone className="w-3.5 h-3.5" />
                      <span>Contact</span>
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      {/* Contact Modal */}
      {selectedNgo && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white max-w-md w-full rounded-2xl shadow-2xl p-6 space-y-5 relative">
            <button
              onClick={() => setSelectedNgo(null)}
              className="absolute top-4 right-4 text-slate-400 hover:text-slate-600 p-1"
            >
              <X className="w-5 h-5" />
            </button>

            <div>
              <div className="flex items-center gap-2 mb-1">
                <VerificationBadge status={selectedNgo.verificationStatus} />
              </div>
              <h3 className="text-xl font-bold text-slate-900">{selectedNgo.ngoName}</h3>
              <p className="text-xs text-slate-500">Public Contact & Direct Inquiry</p>
            </div>

            <div className="bg-slate-50 p-4 rounded-xl space-y-2.5 text-xs text-slate-700 border border-slate-200">
              <div className="flex items-center gap-2">
                <Phone className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>Phone: <strong>{selectedNgo.phone || selectedNgo.contactNumber}</strong></span>
              </div>
              <div className="flex items-center gap-2">
                <Mail className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>Email: <strong>{selectedNgo.email || 'Contact via platform'}</strong></span>
              </div>
              <div className="flex items-start gap-2">
                <MapPin className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                <span>Address: <strong>{selectedNgo.address}, {selectedNgo.city}, {selectedNgo.state}</strong></span>
              </div>
            </div>

            {msgSent ? (
              <div className="p-3 bg-emerald-100 border border-emerald-300 text-emerald-800 text-xs font-bold rounded-xl text-center">
                ✓ Message sent successfully to {selectedNgo.ngoName}! It will appear in their notifications.
              </div>
            ) : (
              <form onSubmit={handleSendMessage} className="space-y-3">
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">Your Name</label>
                    <input
                      type="text"
                      value={senderName}
                      onChange={(e) => setSenderName(e.target.value)}
                      placeholder="e.g. Rahul Sharma"
                      className="w-full p-2 border rounded-xl text-xs outline-none focus:border-emerald-500"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">Your Phone / Email</label>
                    <input
                      type="text"
                      value={senderContact}
                      onChange={(e) => setSenderContact(e.target.value)}
                      placeholder="e.g. 9876543210"
                      className="w-full p-2 border rounded-xl text-xs outline-none focus:border-emerald-500"
                      required
                    />
                  </div>
                </div>
                <div>
                  <label className="block text-[10px] font-bold text-slate-600 uppercase mb-1">Inquiry Message</label>
                  <textarea
                    rows={3}
                    value={contactMsg}
                    onChange={(e) => setContactMsg(e.target.value)}
                    placeholder="Type your message or inquiry for food support/collaboration..."
                    className="w-full p-2.5 border rounded-xl text-xs outline-none focus:border-emerald-500"
                    required
                  />
                </div>
                <button
                  type="submit"
                  disabled={sending}
                  className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow transition flex items-center justify-center gap-1.5"
                >
                  <MessageSquare className="w-4 h-4" />
                  <span>{sending ? 'Sending...' : 'Send Message'}</span>
                </button>
              </form>
            )}
          </div>
        </div>
      )}

      <Footer />
    </div>
  );
};
