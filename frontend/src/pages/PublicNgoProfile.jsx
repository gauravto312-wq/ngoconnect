import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { PublicNavbar } from '../components/PublicNavbar';
import { Footer } from '../components/Footer';
import { VerificationBadge } from '../components/VerificationBadge';
import api, { getUploadUrl } from '../services/api';
import { MapPin, Phone, Mail, Heart, HeartHandshake, ArrowLeft, MessageSquare, Image, ShieldCheck } from 'lucide-react';

export const PublicNgoProfile = () => {
  const { id } = useParams();
  const [ngo, getNgo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [contactMsg, setContactMsg] = useState('');
  const [msgSent, setMsgSent] = useState(false);

  useEffect(() => {
    fetchProfile();
  }, [id]);

  const fetchProfile = async () => {
    setLoading(true);
    try {
      const res = await api.get(`/ngos/public/${id}`);
      getNgo(res.data);
    } catch (e) {
      setError('NGO profile not found.');
    } finally {
      setLoading(false);
    }
  };

  const [senderName, setSenderName] = useState('');
  const [senderContact, setSenderContact] = useState('');
  const [sending, setSending] = useState(false);

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!contactMsg.trim() || !ngo) return;
    setSending(true);
    try {
      await api.post(`/ngos/public/${ngo.id}/contact`, {
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
      }, 2500);
    } catch (err) {
      alert('Failed to send message to NGO.');
    } finally {
      setSending(false);
    }
  };

  const galleryList = ngo?.galleryPhotos ? ngo.galleryPhotos.split(',').map(s => s.trim()).filter(Boolean) : [];

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <PublicNavbar />

      <main className="max-w-5xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-10 flex-1 space-y-8">
        <Link to="/ngos" className="inline-flex items-center gap-1 text-xs font-bold text-slate-600 hover:text-emerald-600 transition">
          <ArrowLeft className="w-4 h-4" />
          <span>Back to NGO Directory</span>
        </Link>

        {loading ? (
          <div className="text-center py-20 text-slate-500 text-sm">Loading NGO profile...</div>
        ) : error || !ngo ? (
          <div className="bg-white p-12 rounded-2xl border text-center text-rose-600 font-bold text-sm">
            {error || 'Profile not found.'}
          </div>
        ) : (
          <div className="space-y-8">
            {/* Header Card */}
            <div className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden">
              {/* Profile Cover / Header */}
              <div className="h-56 bg-gradient-to-r from-emerald-900 via-slate-900 to-emerald-950 relative p-6 flex items-end">
                {ngo.profileImageUrl && (
                  <img src={getUploadUrl(ngo.profileImageUrl)} alt={ngo.ngoName} className="absolute inset-0 w-full h-full object-cover opacity-40" onError={(e) => { e.target.style.display = 'none'; }} />
                )}
                <div className="relative z-10 space-y-2">
                  <div className="flex items-center gap-2">
                    <VerificationBadge status={ngo.verificationStatus} />
                    <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-white/20 text-emerald-200 backdrop-blur">
                      {ngo.cause || 'Community Welfare'}
                    </span>
                  </div>
                  <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">{ngo.ngoName}</h1>
                  <p className="text-xs sm:text-sm text-slate-300 flex items-center gap-1.5">
                    <MapPin className="w-4 h-4 text-emerald-400" />
                    <span>{ngo.address}, {ngo.city}, {ngo.state}</span>
                  </p>
                </div>
              </div>

              {/* Quick Info Grid */}
              <div className="p-6 grid grid-cols-1 md:grid-cols-3 gap-6 border-b border-slate-100 bg-slate-50/50">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold">
                    <Phone className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Phone</div>
                    <div className="text-xs font-bold text-slate-900">{ngo.phone || ngo.contactNumber}</div>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold">
                    <Mail className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Email</div>
                    <div className="text-xs font-bold text-slate-900">{ngo.email || 'Contact via platform'}</div>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold">
                    <ShieldCheck className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Verification Status</div>
                    <div className="text-xs font-bold text-slate-900">{ngo.verificationStatus}</div>
                  </div>
                </div>
              </div>

              {/* NGO Description & Details */}
              <div className="p-8 space-y-6">
                <div>
                  <h3 className="text-lg font-bold text-slate-900 mb-2">About {ngo.ngoName}</h3>
                  <p className="text-slate-600 text-sm leading-relaxed whitespace-pre-line">
                    {ngo.description || 'No detailed description provided yet for this NGO profile.'}
                  </p>
                </div>

                {/* Photo Gallery if any */}
                {galleryList.length > 0 && (
                  <div className="space-y-3 pt-4 border-t border-slate-100">
                    <h4 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                      <Image className="w-4 h-4 text-emerald-600" />
                      <span>Photo Gallery ({galleryList.length})</span>
                    </h4>
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
                      {galleryList.map((url, idx) => (
                        <div key={idx} className="h-36 rounded-xl overflow-hidden bg-slate-100 border border-slate-200">
                          <img src={getUploadUrl(url)} alt={`Gallery ${idx + 1}`} className="w-full h-full object-cover hover:scale-105 transition" onError={(e) => { e.target.style.display = 'none'; }} />
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Direct Contact Form Card */}
            <div className="bg-white p-8 rounded-3xl border border-slate-200 shadow-sm space-y-4">
              <h3 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                <MessageSquare className="w-5 h-5 text-emerald-600" />
                <span>Contact {ngo.ngoName}</span>
              </h3>
              <p className="text-xs text-slate-500">Send an inquiry or collaboration proposal directly to this non-profit organization.</p>

              {msgSent ? (
                <div className="p-4 bg-emerald-100 border border-emerald-300 text-emerald-800 text-xs font-bold rounded-xl text-center">
                  ✓ Your message has been sent directly to {ngo.ngoName}! It will appear in their notifications inbox.
                </div>
              ) : (
                <form onSubmit={handleSendMessage} className="space-y-4">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Your Name</label>
                      <input
                        type="text"
                        value={senderName}
                        onChange={(e) => setSenderName(e.target.value)}
                        placeholder="e.g. Rahul Sharma"
                        className="w-full p-3 border rounded-xl text-xs outline-none focus:border-emerald-500"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Your Phone / Email</label>
                      <input
                        type="text"
                        value={senderContact}
                        onChange={(e) => setSenderContact(e.target.value)}
                        placeholder="e.g. 9876543210 / rahul@gmail.com"
                        className="w-full p-3 border rounded-xl text-xs outline-none focus:border-emerald-500"
                        required
                      />
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Your Message</label>
                    <textarea
                      rows={4}
                      value={contactMsg}
                      onChange={(e) => setContactMsg(e.target.value)}
                      placeholder="Write your message or proposal..."
                      className="w-full p-3 border rounded-xl text-xs outline-none focus:border-emerald-500"
                      required
                    />
                  </div>
                  <button
                    type="submit"
                    disabled={sending}
                    className="px-6 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow transition"
                  >
                    {sending ? 'Sending...' : 'Send Message'}
                  </button>
                </form>
              )}
            </div>
          </div>
        )}
      </main>

      <Footer />
    </div>
  );
};
