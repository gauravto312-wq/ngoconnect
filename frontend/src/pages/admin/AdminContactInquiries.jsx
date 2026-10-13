import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { Mail, Search, MessageSquare, Clock, User, CheckCircle2, RefreshCw, Send, ChevronRight } from 'lucide-react';

export const AdminContactInquiries = () => {
  const [inquiries, setInquiries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [selectedInquiry, setSelectedInquiry] = useState(null);

  useEffect(() => {
    fetchInquiries();
  }, []);

  const fetchInquiries = async () => {
    setLoading(true);
    try {
      const res = await api.get('/contact/admin');
      setInquiries(res.data || []);
    } catch (e) {
      console.error('Failed to load contact inquiries', e);
    } finally {
      setLoading(false);
    }
  };

  const filteredInquiries = inquiries.filter((inq) => {
    const q = search.toLowerCase();
    return (
      inq.name?.toLowerCase().includes(q) ||
      inq.email?.toLowerCase().includes(q) ||
      inq.message?.toLowerCase().includes(q) ||
      inq.status?.toLowerCase().includes(q)
    );
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 flex items-center gap-2">
            <Mail className="w-6 h-6 text-indigo-600" />
            <span>Public Contact Inquiries</span>
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Review inquiries and partnership requests submitted via the NGOConnect public contact portal.
          </p>
        </div>

        <button
          onClick={fetchInquiries}
          disabled={loading}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-white border border-slate-200 rounded-xl text-xs font-bold text-slate-700 hover:bg-slate-50 shadow-sm transition self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh Inquiries</span>
        </button>
      </div>

      {/* Search Bar */}
      <div className="bg-white p-3 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-2">
        <Search className="w-4 h-4 text-slate-400 shrink-0 ml-1" />
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Filter by sender name, email, or message keyword..."
          className="w-full text-xs outline-none bg-transparent"
        />
        {search && (
          <button
            onClick={() => setSearch('')}
            className="text-xs text-slate-400 hover:text-slate-600 font-bold px-2"
          >
            Clear
          </button>
        )}
      </div>

      {/* Inquiry List & Detail Split View */}
      {loading ? (
        <div className="text-center py-16 text-slate-400 text-xs">
          Loading contact inquiries...
        </div>
      ) : filteredInquiries.length === 0 ? (
        <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center space-y-3">
          <Mail className="w-12 h-12 text-slate-300 mx-auto" />
          <h3 className="font-bold text-slate-800 text-sm">No Inquiries Found</h3>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            {search ? 'No inquiries matched your search criteria.' : 'No messages have been submitted through the contact page yet.'}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* List Column */}
          <div className="lg:col-span-1 space-y-3 max-h-[700px] overflow-y-auto pr-1">
            {filteredInquiries.map((inq) => {
              const isSelected = selectedInquiry?.id === inq.id;
              return (
                <div
                  key={inq.id}
                  onClick={() => setSelectedInquiry(inq)}
                  className={`p-4 rounded-2xl border cursor-pointer transition text-xs space-y-2 ${
                    isSelected
                      ? 'bg-indigo-50/70 border-indigo-300 shadow-sm'
                      : 'bg-white border-slate-200 hover:border-slate-300 shadow-xs'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-slate-900 truncate max-w-[150px]">{inq.name}</span>
                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      inq.status === 'NEW' ? 'bg-indigo-100 text-indigo-800' : 'bg-slate-100 text-slate-700'
                    }`}>
                      {inq.status || 'NEW'}
                    </span>
                  </div>
                  <div className="text-slate-500 text-[11px] truncate">{inq.email}</div>
                  <p className="text-slate-600 text-[11px] line-clamp-2 leading-relaxed">
                    {inq.message}
                  </p>
                  <div className="text-[10px] text-slate-400 pt-1 flex items-center justify-between">
                    <span>{inq.createdAt ? new Date(inq.createdAt).toLocaleDateString() : 'Recent'}</span>
                    <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
                  </div>
                </div>
              );
            })}
          </div>

          {/* Detail View Column */}
          <div className="lg:col-span-2">
            {selectedInquiry ? (
              <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-sm space-y-6">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-indigo-100 text-indigo-800 uppercase">
                        {selectedInquiry.status || 'NEW'}
                      </span>
                      <span className="text-xs text-slate-400">Inquiry #{selectedInquiry.id}</span>
                    </div>
                    <h2 className="text-xl font-bold text-slate-900">{selectedInquiry.name}</h2>
                    <a
                      href={`mailto:${selectedInquiry.email}`}
                      className="text-xs text-indigo-600 hover:text-indigo-800 font-medium underline flex items-center gap-1"
                    >
                      <Mail className="w-3.5 h-3.5" />
                      <span>{selectedInquiry.email}</span>
                    </a>
                  </div>

                  <div className="text-xs text-slate-400 self-start sm:self-center">
                    Received: {selectedInquiry.createdAt ? new Date(selectedInquiry.createdAt).toLocaleString() : 'N/A'}
                  </div>
                </div>

                {/* Full Message Body */}
                <div className="space-y-2">
                  <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                    Inquiry Message
                  </div>
                  <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100 text-slate-800 text-xs sm:text-sm leading-relaxed whitespace-pre-wrap">
                    {selectedInquiry.message}
                  </div>
                </div>

                {/* Reply Actions */}
                <div className="flex items-center justify-between pt-4 border-t border-slate-100">
                  <span className="text-xs text-slate-500">
                    Direct Email Responder
                  </span>
                  <a
                    href={`mailto:${selectedInquiry.email}?subject=Re:%20NGOConnect%20Inquiry%20%23${selectedInquiry.id}&body=Hi%20${encodeURIComponent(selectedInquiry.name)},%0A%0AThank%20you%20for%20contacting%20NGOConnect.`}
                    className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-xl shadow-sm transition flex items-center gap-2"
                  >
                    <Send className="w-3.5 h-3.5" />
                    <span>Reply via Email Client</span>
                  </a>
                </div>
              </div>
            ) : (
              <div className="bg-slate-50 rounded-3xl border border-dashed border-slate-200 p-12 text-center text-slate-400 text-xs space-y-2 h-full flex flex-col items-center justify-center">
                <MessageSquare className="w-8 h-8 text-slate-300" />
                <p>Select an inquiry from the list to read the full message and respond.</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
