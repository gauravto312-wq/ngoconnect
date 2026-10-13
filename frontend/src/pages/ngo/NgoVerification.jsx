import React, { useState, useEffect } from 'react';
import api, { getUploadUrl } from '../../services/api';
import { ShieldCheck, Upload, FileText, CheckCircle2, Clock, ExternalLink } from 'lucide-react';

export const NgoVerification = () => {
  const [ngoProfile, setNgoProfile] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);

  const [documentType, setDocumentType] = useState('80G Certificate');
  const [documentNumber, setDocumentNumber] = useState('');
  const [documentUrl, setDocumentUrl] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [uploadingDoc, setUploadingDoc] = useState(false);

  useEffect(() => {
    fetchVerificationData();
  }, []);

  const fetchVerificationData = async () => {
    setLoading(true);
    try {
      const [profRes, docRes] = await Promise.all([
        api.get('/ngos/profile'),
        api.get('/ngos/verification-documents')
      ]);
      setNgoProfile(profRes.data);
      setDocuments(docRes.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleDocFileUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setUploadingDoc(true);
    const formData = new FormData();
    formData.append('file', file);
    try {
      const res = await api.post('/ngos/verification-documents/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      setDocumentUrl(res.data.url);
    } catch (err) {
      alert('Failed to upload document file. Please ensure it is a valid document (PDF, JPG, PNG, WEBP) under 5MB.');
    } finally {
      setUploadingDoc(false);
    }
  };

  const handleViewDocument = async (doc) => {
    try {
      const res = await api.get(`/ngos/verification-documents/${doc.id}/view-url`);
      if (res.data && res.data.url && (res.data.url.startsWith('http://') || res.data.url.startsWith('https://'))) {
        window.open(res.data.url, '_blank', 'noopener,noreferrer');
        return;
      }
    } catch (err) {
      console.warn('Could not fetch signed view url, falling back to authenticated blob stream', err);
    }

    try {
      const fileRes = await api.get(`/ngos/verification-documents/${doc.id}/file`, {
        responseType: 'blob'
      });
      const contentType = fileRes.headers['content-type'] || 'application/pdf';
      const blob = new Blob([fileRes.data], { type: contentType });
      const blobUrl = window.URL.createObjectURL(blob);
      window.open(blobUrl, '_blank', 'noopener,noreferrer');
      setTimeout(() => window.URL.revokeObjectURL(blobUrl), 120000);
    } catch (streamErr) {
      console.error('Failed to view verification document', streamErr);
      alert('Unable to load verification document. Please try again.');
    }
  };

  const handleSubmitDocument = async (e) => {
    e.preventDefault();
    if (!documentUrl.trim()) {
      alert('Please upload a document file or provide a valid document URL.');
      return;
    }
    setSubmitting(true);
    try {
      await api.post('/ngos/verification-documents', {
        documentType,
        documentNumber,
        documentUrl: documentUrl.trim()
      });
      alert('Verification document submitted to Admin for review!');
      setDocumentNumber('');
      setDocumentUrl('');
      fetchVerificationData();
    } catch (e) {
      alert('Failed to submit verification document.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">NGO Verification Center</h1>
          <p className="text-xs text-slate-500 mt-1">Upload government registration certificates for Admin verification.</p>
        </div>
        <span className={`px-3 py-1 rounded-full text-xs font-bold ${
          ngoProfile?.verificationStatus === 'APPROVED' ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
        }`}>
          {ngoProfile?.verificationStatus}
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Submit Document Form */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
          <h3 className="font-bold text-slate-900 text-base border-b pb-3 flex items-center gap-2">
            <Upload className="w-5 h-5 text-emerald-600" />
            <span>Submit Verification Document</span>
          </h3>

          <form onSubmit={handleSubmitDocument} className="space-y-3 text-xs">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Document Type *</label>
              <select
                value={documentType}
                onChange={(e) => setDocumentType(e.target.value)}
                className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none bg-white"
              >
                <option value="80G Certificate">80G Tax Exemption Certificate</option>
                <option value="12A Registration">12A Registration Certificate</option>
                <option value="FCRA Registration">FCRA Registration</option>
                <option value="NITI Aayog Darpan ID">NITI Aayog NGO Darpan ID</option>
                <option value="Society Registration">Society / Trust Registration Act</option>
              </select>
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">Document Reg / Reference Number *</label>
              <input
                type="text"
                required
                value={documentNumber}
                onChange={(e) => setDocumentNumber(e.target.value)}
                placeholder="e.g. REG-8472-UP-2024"
                className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">Upload Certificate File or Enter URL *</label>
              <div className="flex gap-2 mb-2">
                <label className="px-3 py-2 bg-slate-900 hover:bg-slate-800 text-white font-bold rounded-lg cursor-pointer inline-flex items-center gap-1.5 transition">
                  <Upload className="w-3.5 h-3.5" />
                  <span>{uploadingDoc ? 'Uploading...' : 'Choose File'}</span>
                  <input type="file" accept="image/*,application/pdf,.pdf" className="hidden" onChange={handleDocFileUpload} disabled={uploadingDoc} />
                </label>
                <input
                  type="text"
                  required
                  value={documentUrl}
                  onChange={(e) => setDocumentUrl(e.target.value)}
                  placeholder="File path or URL (e.g. /uploads/... or https://...)"
                  className="flex-1 px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting || uploadingDoc}
              className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-xs shadow transition mt-2"
            >
              {submitting ? 'Submitting...' : 'Upload & Submit for Admin Review'}
            </button>
          </form>
        </div>

        {/* Verification Document History */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
          <h3 className="font-bold text-slate-900 text-base border-b pb-3 flex items-center gap-2">
            <FileText className="w-5 h-5 text-indigo-600" />
            <span>Uploaded Verification Documents</span>
          </h3>

          {loading ? (
            <div className="text-xs text-slate-500 text-center py-6">Loading documents...</div>
          ) : documents.length === 0 ? (
            <div className="text-xs text-slate-500 text-center py-8 border border-dashed rounded-xl">
              No verification documents submitted yet.
            </div>
          ) : (
            <div className="space-y-3">
              {documents.map((doc) => (
                <div key={doc.id} className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 text-xs space-y-1">
                  <div className="flex justify-between items-center">
                    <strong className="text-slate-900">{doc.documentType}</strong>
                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      doc.verificationStatus === 'APPROVED' ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
                    }`}>
                      {doc.verificationStatus}
                    </span>
                  </div>
                  <div className="text-slate-600">Ref: {doc.documentNumber}</div>
                  {doc.documentUrl && (
                    <div className="pt-1">
                      <button
                        type="button"
                        onClick={() => handleViewDocument(doc)}
                        className="inline-flex items-center gap-1 text-emerald-700 hover:text-emerald-800 font-bold underline bg-transparent border-none p-0 cursor-pointer text-xs"
                      >
                        <ExternalLink className="w-3 h-3" />
                        <span>View Document</span>
                      </button>
                    </div>
                  )}
                  {doc.remarks && <div className="text-indigo-700">Admin Remarks: {doc.remarks}</div>}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
