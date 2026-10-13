import React, { useState } from 'react';
import api, { getUploadUrl } from '../services/api';
import { Camera, Image, Upload, Plus, Trash2, CheckCircle2 } from 'lucide-react';

export const PhotoUploadManager = ({ endpoint, initialProfileImage, initialGalleryPhotos, onUpdateSuccess }) => {
  const [profileImage, setProfileImage] = useState(initialProfileImage || '');
  const [gallery, setGallery] = useState(
    initialGalleryPhotos ? initialGalleryPhotos.split(',').map(s => s.trim()).filter(Boolean) : []
  );
  const [newGalleryUrl, setNewGalleryUrl] = useState('');
  const [uploading, setUploading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [msg, setMsg] = useState('');

  // Handle local file upload
  const handleFileUpload = async (e, type) => {
    const file = e.target.files[0];
    if (!file) return;

    setUploading(true);
    setMsg('');
    const formData = new FormData();
    formData.append('file', file);

    try {
      const res = await api.post('/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      const url = res.data.url;
      if (type === 'profile') {
        setProfileImage(url);
      } else if (type === 'gallery') {
        setGallery(prev => [...prev, url]);
      }
    } catch (err) {
      alert('Failed to upload file. Please try again.');
    } finally {
      setUploading(false);
    }
  };

  const handleAddGalleryUrl = () => {
    if (!newGalleryUrl.trim()) return;
    setGallery(prev => [...prev, newGalleryUrl.trim()]);
    setNewGalleryUrl('');
  };

  const handleRemoveGalleryItem = (index) => {
    setGallery(prev => prev.filter((_, i) => i !== index));
  };

  const handleSavePhotos = async () => {
    setSaving(true);
    setMsg('');
    try {
      await api.put(endpoint, {
        profileImageUrl: profileImage,
        galleryPhotos: gallery.join(',')
      });
      setMsg('Photos updated successfully!');
      if (onUpdateSuccess) onUpdateSuccess();
    } catch (e) {
      alert('Failed to save photo updates.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 space-y-6">
      <div className="flex items-center justify-between border-b border-slate-100 pb-4">
        <div>
          <h3 className="font-bold text-slate-900 text-lg flex items-center gap-2">
            <Camera className="w-5 h-5 text-emerald-600" />
            <span>Profile & Photo Gallery Management</span>
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">Upload organization profile image and showcase photos visible on public pages & admin audit.</p>
        </div>
        {msg && (
          <span className="text-xs font-bold text-emerald-700 bg-emerald-100 border border-emerald-200 px-3 py-1 rounded-lg">
            {msg}
          </span>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Profile Image Section */}
        <div className="space-y-3 bg-slate-50 p-4 rounded-xl border border-slate-200">
          <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">Main Profile Photo</label>
          <div className="flex items-center gap-4">
            <div className="w-20 h-20 rounded-xl bg-slate-200 border border-slate-300 overflow-hidden flex items-center justify-center shrink-0">
              {profileImage ? (
                <img src={getUploadUrl(profileImage)} alt="Profile Preview" className="w-full h-full object-cover" />
              ) : (
                <Camera className="w-8 h-8 text-slate-400" />
              )}
            </div>
            <div className="space-y-2 flex-1 text-xs">
              <label className="px-3 py-1.5 bg-slate-900 hover:bg-slate-800 text-white font-bold rounded-lg cursor-pointer inline-flex items-center gap-1.5 transition">
                <Upload className="w-3.5 h-3.5" />
                <span>Upload File</span>
                <input type="file" accept="image/*" className="hidden" onChange={(e) => handleFileUpload(e, 'profile')} disabled={uploading} />
              </label>
              <div className="text-[10px] text-slate-500">Or paste image URL:</div>
              <input
                type="text"
                value={profileImage}
                onChange={(e) => setProfileImage(e.target.value)}
                placeholder="https://example.com/photo.jpg"
                className="w-full p-2 border rounded-lg text-xs bg-white outline-none"
              />
            </div>
          </div>
        </div>

        {/* Gallery Section */}
        <div className="space-y-3 bg-slate-50 p-4 rounded-xl border border-slate-200">
          <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">Add Additional Photos (Gallery)</label>
          <div className="flex gap-2">
            <label className="px-3 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg cursor-pointer inline-flex items-center gap-1.5 text-xs transition">
              <Upload className="w-3.5 h-3.5" />
              <span>Upload Photo</span>
              <input type="file" accept="image/*" className="hidden" onChange={(e) => handleFileUpload(e, 'gallery')} disabled={uploading} />
            </label>
            <input
              type="text"
              value={newGalleryUrl}
              onChange={(e) => setNewGalleryUrl(e.target.value)}
              placeholder="Or paste photo URL..."
              className="flex-1 p-2 border rounded-lg text-xs bg-white outline-none"
            />
            <button
              onClick={handleAddGalleryUrl}
              className="px-3 py-2 bg-slate-800 text-white font-bold rounded-lg text-xs hover:bg-slate-700"
            >
              Add
            </button>
          </div>

          {/* Gallery Thumbnails */}
          <div className="grid grid-cols-4 gap-2 pt-2">
            {gallery.map((url, idx) => (
              <div key={idx} className="relative h-16 rounded-lg overflow-hidden border bg-white group">
                <img src={getUploadUrl(url)} alt={`Gallery ${idx}`} className="w-full h-full object-cover" />
                <button
                  onClick={() => handleRemoveGalleryItem(idx)}
                  className="absolute top-1 right-1 bg-rose-600 text-white p-0.5 rounded opacity-80 hover:opacity-100"
                >
                  <Trash2 className="w-3 h-3" />
                </button>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="flex justify-end pt-2">
        <button
          onClick={handleSavePhotos}
          disabled={saving || uploading}
          className="px-6 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow transition flex items-center gap-1.5"
        >
          <CheckCircle2 className="w-4 h-4" />
          <span>{saving ? 'Saving...' : 'Save Photos'}</span>
        </button>
      </div>
    </div>
  );
};
