import React, { useState, useEffect } from 'react';
import api, { getUploadUrl } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import {
  CalendarHeart,
  Plus,
  MapPin,
  Users,
  CheckCircle2,
  Clock,
  Camera,
  Upload,
  Trash2,
  AlertTriangle,
  X,
  ExternalLink,
  Image as ImageIcon
} from 'lucide-react';

export const FoodDrivesView = () => {
  const { user } = useAuth();
  const [drives, setDrives] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cityFilter, setCityFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [joinModalDrive, setJoinModalDrive] = useState(null);
  const [selectedGalleryImage, setSelectedGalleryImage] = useState(null);

  // Form State
  const [title, setTitle] = useState('');
  const [purpose, setPurpose] = useState('');
  const [description, setDescription] = useState('');
  const [coverImageUrl, setCoverImageUrl] = useState('');
  const [address, setAddress] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('Uttar Pradesh');
  const [eventDate, setEventDate] = useState('');
  const [startTime, setStartTime] = useState('10:00 AM');
  const [endTime, setEndTime] = useState('02:00 PM');
  const [completionDeadline, setCompletionDeadline] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [uploadingCover, setUploadingCover] = useState(false);

  // Participant Contribution State
  const [contributionDesc, setContributionDesc] = useState('');
  const [contributionQty, setContributionQty] = useState(50);
  const [joining, setJoining] = useState(false);

  // Evidence Photo Upload State for Creator
  const [uploadingPhotoForDriveId, setUploadingPhotoForDriveId] = useState(null);
  const [submittingPhotosDriveId, setSubmittingPhotosDriveId] = useState(null);
  const [photoSubmitSuccess, setPhotoSubmitSuccess] = useState({});
  const [completingDriveId, setCompletingDriveId] = useState(null);
  const [actionError, setActionError] = useState('');

  useEffect(() => {
    fetchDrives();
  }, [cityFilter, statusFilter]);

  const fetchDrives = async () => {
    setLoading(true);
    setActionError('');
    try {
      const params = {};
      if (cityFilter) params.city = cityFilter;
      if (statusFilter && statusFilter !== 'ALL') params.status = statusFilter;
      const res = await api.get('/food-drives/public', { params });
      setDrives(res.data);
    } catch (e) {
      console.error('Error fetching food drives:', e);
    } finally {
      setLoading(false);
    }
  };

  const handleCoverUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setUploadingCover(true);
    const formData = new FormData();
    formData.append('file', file);
    try {
      const res = await api.post('/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      setCoverImageUrl(res.data.url);
    } catch (err) {
      alert('Failed to upload cover image.');
    } finally {
      setUploadingCover(false);
    }
  };

  const handleCreateDrive = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await api.post('/food-drives', {
        title,
        purpose,
        description,
        coverImageUrl,
        address,
        city,
        state,
        eventDate,
        startTime,
        endTime,
        completionDeadline: completionDeadline ? new Date(completionDeadline).toISOString() : null
      });
      setShowCreateModal(false);
      setTitle('');
      setPurpose('');
      setDescription('');
      setCoverImageUrl('');
      setCompletionDeadline('');
      fetchDrives();
    } catch (e) {
      const msg = e.response?.data?.message || 'Failed to create food drive.';
      alert(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleJoinDrive = async (e) => {
    e.preventDefault();
    if (!joinModalDrive) return;
    setJoining(true);
    try {
      await api.post(`/food-drives/${joinModalDrive.id}/join`, {
        contributionDescription: contributionDesc || `Participated as ${user.role}`,
        contributionQuantity: parseInt(contributionQty) || 0,
        contributionUnit: 'meals'
      });
      alert('Joined food drive successfully!');
      setJoinModalDrive(null);
      fetchDrives();
    } catch (e) {
      const msg = e.response?.data?.message || 'Failed to join food drive.';
      alert(msg);
    } finally {
      setJoining(false);
    }
  };

  const handleEvidencePhotoUpload = async (driveId, e) => {
    const file = e.target.files[0];
    if (!file) return;
    setUploadingPhotoForDriveId(driveId);
    setActionError('');
    const formData = new FormData();
    formData.append('file', file);
    try {
      // 1. Upload to server
      const uploadRes = await api.post('/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      const photoUrl = uploadRes.data.url;

      // 2. Attach evidence photo to drive
      const attachRes = await api.post(`/food-drives/${driveId}/evidence-photos`, { photoUrl });
      if (attachRes.data) {
        setDrives(prev => prev.map(d => d.id === driveId ? attachRes.data : d));
      } else {
        fetchDrives();
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to attach evidence photo.';
      setActionError(msg);
    } finally {
      setUploadingPhotoForDriveId(null);
    }
  };

  const handleRemoveEvidencePhoto = async (driveId, photoUrl) => {
    if (!window.confirm('Are you sure you want to remove this evidence photo?')) return;
    setActionError('');
    try {
      const removeRes = await api.delete(`/food-drives/${driveId}/evidence-photos`, {
        data: { photoUrl }
      });
      if (removeRes.data) {
        setDrives(prev => prev.map(d => d.id === driveId ? removeRes.data : d));
      } else {
        fetchDrives();
      }
      setPhotoSubmitSuccess(prev => {
        const copy = { ...prev };
        delete copy[driveId];
        return copy;
      });
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to remove evidence photo.';
      setActionError(msg);
    }
  };

  const handleSubmitPhotos = async (driveId, existingPhotos) => {
    if (!existingPhotos || existingPhotos.length < 4) {
      setActionError('At least 4 valid evidence photos are required to submit.');
      return;
    }
    setSubmittingPhotosDriveId(driveId);
    setActionError('');
    try {
      let res;
      try {
        res = await api.post(`/food-drives/${driveId}/evidence-photos/submit`, {
          photoUrls: existingPhotos
        });
      } catch (submitErr) {
        if (submitErr.response && submitErr.response.status === 404) {
          res = await api.post(`/food-drives/${driveId}/photos/submit`, {
            photoUrls: existingPhotos
          });
        } else {
          throw submitErr;
        }
      }
      if (res && res.data) {
        setDrives(prev => prev.map(d => d.id === driveId ? res.data : d));
      } else {
        await fetchDrives();
      }
      setPhotoSubmitSuccess(prev => ({
        ...prev,
        [driveId]: 'Evidence photos submitted successfully! You can now mark the drive as completed before the deadline expires.'
      }));
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to submit evidence photos.';
      setActionError(msg);
      alert(msg);
    } finally {
      setSubmittingPhotosDriveId(null);
    }
  };

  const handleCompleteDrive = async (driveId) => {
    if (!window.confirm('Are you sure you want to mark this food drive as Completed? This confirms that the drive was executed with at least 4 evidence photos.')) {
      return;
    }
    setCompletingDriveId(driveId);
    setActionError('');
    try {
      await api.post(`/food-drives/${driveId}/complete`);
      alert('Food drive marked as Completed successfully! Completion notifications sent to all participants.');
      fetchDrives();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to complete food drive.';
      setActionError(msg);
      alert(msg);
    } finally {
      setCompletingDriveId(null);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'COMPLETED':
        return 'bg-emerald-100 text-emerald-800 border-emerald-300';
      case 'IN_PROGRESS':
        return 'bg-amber-100 text-amber-800 border-amber-300 animate-pulse';
      case 'EXPIRED':
        return 'bg-rose-100 text-rose-800 border-rose-300';
      case 'UPCOMING':
      default:
        return 'bg-blue-100 text-blue-800 border-blue-300';
    }
  };

  const isDeadlinePassed = (deadlineStr, eventDateStr, endTimeStr) => {
    if (deadlineStr) {
      return new Date() > new Date(deadlineStr);
    }
    if (eventDateStr) {
      const end = new Date(eventDateStr);
      end.setHours(23, 59, 59, 999);
      return new Date() > end;
    }
    return false;
  };

  const isDriveStarted = (eventDateStr, startTimeStr) => {
    if (!eventDateStr) return true;
    try {
      let timePart = "09:00:00";
      if (startTimeStr) {
        const clean = startTimeStr.trim();
        const match = clean.match(/^(\d{1,2}):(\d{2})(?:\s*([AaPp][Mm]))?$/);
        if (match) {
          let hours = parseInt(match[1]);
          const mins = match[2];
          const ampm = match[3];
          if (ampm) {
            if (ampm.toUpperCase() === 'PM' && hours < 12) hours += 12;
            if (ampm.toUpperCase() === 'AM' && hours === 12) hours = 0;
          }
          timePart = `${String(hours).padStart(2, '0')}:${mins}:00`;
        }
      }
      const startDateTime = new Date(`${eventDateStr}T${timePart}`);
      return new Date() >= startDateTime;
    } catch (e) {
      return true;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900">Community Food Drives 2.0</h1>
          <p className="text-xs text-slate-500 mt-1">Joint food relief drives with verifiable evidence photos and transparent completion tracking.</p>
        </div>

        <div className="flex items-center gap-2">
          <div className="relative">
            <MapPin className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
            <input
              type="text"
              value={cityFilter}
              onChange={(e) => setCityFilter(e.target.value)}
              placeholder="Search by city..."
              className="pl-9 pr-3 py-2 text-xs border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none bg-white"
            />
          </div>

          {(user?.role === 'CITIZEN' || user?.role === 'NGO' || user?.role === 'ADMIN') && (
            <button
              onClick={() => setShowCreateModal(true)}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-xs flex items-center gap-1 shadow transition flex-shrink-0"
            >
              <Plus className="w-4 h-4" />
              <span>Create Food Drive</span>
            </button>
          )}
        </div>
      </div>

      {/* Status Filter Tabs */}
      <div className="flex flex-wrap gap-2 border-b border-slate-200 pb-3">
        {[
          { key: 'ALL', label: 'All Drives' },
          { key: 'UPCOMING', label: 'Upcoming' },
          { key: 'IN_PROGRESS', label: 'In Progress' },
          { key: 'COMPLETED', label: 'Completed' },
          { key: 'EXPIRED', label: 'Expired' }
        ].map((tab) => (
          <button
            key={tab.key}
            onClick={() => setStatusFilter(tab.key)}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition ${
              statusFilter === tab.key
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {actionError && (
        <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 flex items-center gap-2">
          <AlertTriangle className="w-4 h-4 flex-shrink-0 text-red-600" />
          <span>{actionError}</span>
        </div>
      )}

      {/* Create Drive Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-xl w-full p-6 space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="font-bold text-slate-900 text-base">Organize a Community Food Drive</h3>
              <button onClick={() => setShowCreateModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateDrive} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Event Title *</label>
                <input
                  type="text"
                  required
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="e.g. Birthday Food Drive at BBD Lucknow"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Drive Purpose *</label>
                <input
                  type="text"
                  required
                  value={purpose}
                  onChange={(e) => setPurpose(e.target.value)}
                  placeholder="e.g. Slum area meal distribution"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Detailed Description</label>
                <textarea
                  rows="2"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Share logistical instructions, meal packaging guidelines, volunteer needs..."
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                ></textarea>
              </div>

              {/* Cover Image */}
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Cover Image (Optional)</label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={coverImageUrl}
                    onChange={(e) => setCoverImageUrl(e.target.value)}
                    placeholder="https://example.com/banner.jpg or upload below"
                    className="flex-1 px-3 py-2 border rounded-lg outline-none"
                  />
                  <label className="px-3 py-2 bg-slate-800 hover:bg-slate-700 text-white font-bold rounded-lg cursor-pointer flex items-center gap-1 shrink-0">
                    <Upload className="w-3.5 h-3.5" />
                    <span>{uploadingCover ? 'Uploading...' : 'Upload'}</span>
                    <input type="file" accept="image/*" className="hidden" onChange={handleCoverUpload} disabled={uploadingCover} />
                  </label>
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Event Date *</label>
                  <input
                    type="date"
                    required
                    value={eventDate}
                    onChange={(e) => setEventDate(e.target.value)}
                    className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Start Time</label>
                  <input
                    type="text"
                    value={startTime}
                    onChange={(e) => setStartTime(e.target.value)}
                    className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">End Time</label>
                  <input
                    type="text"
                    value={endTime}
                    onChange={(e) => setEndTime(e.target.value)}
                    className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                  />
                </div>
              </div>

              {/* Mandatory Completion Deadline */}
              <div className="bg-amber-50/60 p-3 rounded-xl border border-amber-200 space-y-1">
                <label className="block font-bold text-slate-800">Participation & Completion Deadline *</label>
                <p className="text-[11px] text-slate-600">The drive can only be completed by the creator after this deadline has elapsed.</p>
                <input
                  type="datetime-local"
                  required
                  value={completionDeadline}
                  onChange={(e) => setCompletionDeadline(e.target.value)}
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none bg-white text-xs mt-1"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Address Location *</label>
                <input
                  type="text"
                  required
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  placeholder="Near BBD Campus, Faizabad Road"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">City *</label>
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
                  <label className="block font-semibold text-slate-700 mb-1">State *</label>
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

              <div className="flex justify-end space-x-2 pt-3 border-t">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 border rounded-lg text-slate-600 hover:bg-slate-100 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg shadow"
                >
                  {submitting ? 'Creating...' : 'Publish Food Drive'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Join Drive Modal */}
      {joinModalDrive && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full p-6 space-y-4">
            <h3 className="font-bold text-slate-900 text-base border-b pb-3">
              Participate in: {joinModalDrive.title}
            </h3>

            <form onSubmit={handleJoinDrive} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Contributed Food Meals</label>
                <input
                  type="number"
                  min="0"
                  value={contributionQty}
                  onChange={(e) => setContributionQty(e.target.value)}
                  placeholder="e.g. 50 meals"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Contribution / Service Notes</label>
                <input
                  type="text"
                  value={contributionDesc}
                  onChange={(e) => setContributionDesc(e.target.value)}
                  placeholder="e.g. Bringing 50 meal packets + 2 volunteers"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
                />
              </div>

              <div className="flex justify-end space-x-2 pt-3 border-t">
                <button
                  type="button"
                  onClick={() => setJoinModalDrive(null)}
                  className="px-4 py-2 border rounded-lg text-slate-600 hover:bg-slate-100 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={joining}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg shadow"
                >
                  {joining ? 'Joining...' : 'Confirm Participation'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Lightbox / Larger Image View Modal */}
      {selectedGalleryImage && (
        <div
          className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4"
          onClick={() => setSelectedGalleryImage(null)}
        >
          <div className="relative max-w-4xl max-h-[90vh] bg-slate-900 rounded-2xl overflow-hidden p-2" onClick={(e) => e.stopPropagation()}>
            <button
              onClick={() => setSelectedGalleryImage(null)}
              className="absolute top-4 right-4 bg-slate-800/80 hover:bg-slate-800 text-white p-1.5 rounded-full z-10"
            >
              <X className="w-5 h-5" />
            </button>
            <img
              src={getUploadUrl(selectedGalleryImage)}
              alt="Evidence photo full view"
              className="max-w-full max-h-[80vh] object-contain rounded-xl mx-auto"
            />
          </div>
        </div>
      )}

      {/* Drives Grid */}
      {loading ? (
        <div className="text-xs text-slate-500 text-center py-12">Loading community food drives...</div>
      ) : drives.length === 0 ? (
        <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center text-slate-500 text-xs">
          No food drives found matching status "{statusFilter}" in {cityFilter || 'all cities'}.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {drives.map((fd) => {
            const isCreator = user && (user.id === fd.createdByUserId || fd.isCreator);
            const deadlinePassed = isDeadlinePassed(fd.completionDeadline, fd.eventDate, fd.endTime);
            const isOverdue = fd.status === 'EXPIRED' || deadlinePassed;
            const hasStarted = fd.status === 'IN_PROGRESS' || fd.status === 'COMPLETED' || fd.status === 'EXPIRED' || isDriveStarted(fd.eventDate, fd.startTime);
            const photoCount = fd.evidencePhotos?.length || 0;
            const isPhotosSubmitted = Boolean(fd.photosSubmitted);
            const canComplete = isCreator && fd.status !== 'COMPLETED' && fd.status !== 'CANCELLED' && !isOverdue && hasStarted && isPhotosSubmitted;

            return (
              <div key={fd.id} className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm flex flex-col justify-between">
                <div>
                  {/* Cover Banner if available */}
                  {fd.coverImageUrl && (
                    <div className="h-40 w-full overflow-hidden relative bg-slate-100">
                      <img
                        src={getUploadUrl(fd.coverImageUrl)}
                        alt={fd.title}
                        className="w-full h-full object-cover"
                      />
                      <span className={`absolute top-3 right-3 px-3 py-1 rounded text-xs font-bold border shadow-sm uppercase ${getStatusBadge(fd.status)}`}>
                        {fd.status}
                      </span>
                    </div>
                  )}

                  <div className="p-6 space-y-4">
                    {!fd.coverImageUrl && (
                      <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                        <div>
                          <span className="text-xs font-bold text-emerald-600 uppercase tracking-wider">{fd.city}, {fd.state}</span>
                          <h3 className="font-bold text-slate-900 text-lg mt-0.5">{fd.title}</h3>
                        </div>
                        <span className={`px-3 py-1 rounded text-xs font-bold border uppercase ${getStatusBadge(fd.status)}`}>
                          {fd.status}
                        </span>
                      </div>
                    )}

                    {fd.coverImageUrl && (
                      <div>
                        <span className="text-xs font-bold text-emerald-600 uppercase tracking-wider">{fd.city}, {fd.state}</span>
                        <h3 className="font-bold text-slate-900 text-lg mt-0.5">{fd.title}</h3>
                      </div>
                    )}

                    <p className="text-xs text-slate-600">{fd.purpose || fd.description}</p>

                    {/* Event Logistics Info Card */}
                    <div className="bg-slate-50 p-4 rounded-xl text-xs space-y-1.5 text-slate-700 border border-slate-100">
                      <div><strong>Location:</strong> {fd.address}, {fd.city}</div>
                      <div><strong>Event Date:</strong> {fd.eventDate} ({fd.startTime} - {fd.endTime})</div>
                      <div><strong>Host:</strong> {fd.createdByName} ({fd.createdByRole})</div>
                      
                      {/* Deadline Display */}
                      <div className="flex items-center gap-1.5 pt-1 text-slate-600">
                        <Clock className="w-3.5 h-3.5 text-amber-600 flex-shrink-0" />
                        <span>
                          <strong>Completion Deadline:</strong>{' '}
                          {fd.completionDeadline ? new Date(fd.completionDeadline).toLocaleString() : `${fd.eventDate} End of Day`}
                          {isOverdue ? (
                            <span className="ml-1.5 text-[10px] font-bold text-rose-600 bg-rose-50 px-1.5 py-0.5 rounded border border-rose-200">
                              (Expired)
                            </span>
                          ) : (
                            <span className="ml-1.5 text-[10px] font-bold text-emerald-600 bg-emerald-50 px-1.5 py-0.5 rounded border border-emerald-200">
                              (Open for Completion)
                            </span>
                          )}
                        </span>
                      </div>

                      {fd.completedAt && (
                        <div className="flex items-center gap-1.5 text-emerald-700 font-semibold pt-1">
                          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                          <span>Completed on: {new Date(fd.completedAt).toLocaleString()}</span>
                        </div>
                      )}

                      <div className="text-emerald-700 font-bold pt-1">
                        Total Contributions: {fd.totalMealsContributed} Meals • {fd.participantCount} Participants
                      </div>
                    </div>

                    {/* Public Completed Drive Photo Gallery */}
                    {fd.status === 'COMPLETED' && fd.evidencePhotos && fd.evidencePhotos.length > 0 && (
                      <div className="space-y-2 pt-2 border-t border-slate-100">
                        <div className="flex items-center justify-between">
                          <h4 className="font-bold text-slate-800 text-[11px] uppercase tracking-wider flex items-center gap-1.5">
                            <ImageIcon className="w-3.5 h-3.5 text-emerald-600" />
                            <span>Verified Evidence Photos ({fd.evidencePhotos.length})</span>
                          </h4>
                          <span className="text-[10px] text-slate-400">Click to view full image</span>
                        </div>

                        <div className="grid grid-cols-4 gap-2">
                          {fd.evidencePhotos.map((url, idx) => (
                            <div
                              key={idx}
                              onClick={() => setSelectedGalleryImage(url)}
                              className="h-16 rounded-lg overflow-hidden border border-slate-200 bg-slate-100 cursor-pointer hover:opacity-90 hover:scale-105 transition"
                            >
                              <img src={getUploadUrl(url)} alt={`Evidence ${idx + 1}`} className="w-full h-full object-cover" />
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Creator-Only Evidence Photo Management Section */}
                    {isCreator && fd.status !== 'COMPLETED' && fd.status !== 'CANCELLED' && (
                      <div className="bg-emerald-50/50 p-4 rounded-xl border border-emerald-200 space-y-3">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-1.5">
                            <Camera className="w-4 h-4 text-emerald-700" />
                            <h4 className="font-bold text-slate-900 text-xs">Creator Photo Evidence Management</h4>
                          </div>
                          <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                            photoCount >= 4 ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
                          }`}>
                            {photoCount} / 4 Required Photos
                          </span>
                        </div>

                        {!hasStarted ? (
                          <div className="p-3 bg-blue-50 border border-blue-200 rounded-xl text-xs text-blue-800 flex items-center gap-2">
                            <Clock className="w-4 h-4 text-blue-600 flex-shrink-0" />
                            <span>Event has not started yet. Photo evidence uploads and drive completion unlock on {fd.eventDate} at {fd.startTime}.</span>
                          </div>
                        ) : (
                          <>
                            <p className="text-[11px] text-slate-600">
                              Upload at least 4 distinct photos showing volunteer packaging, distribution, and beneficiaries to unlock drive completion.
                            </p>

                            {/* Photo Thumbnails */}
                            {fd.evidencePhotos && fd.evidencePhotos.length > 0 && (
                              <div className="grid grid-cols-4 gap-2">
                                {fd.evidencePhotos.map((url, idx) => (
                                  <div key={idx} className="relative h-14 rounded-lg overflow-hidden border border-slate-200 group bg-white">
                                    <img src={getUploadUrl(url)} alt={`Photo ${idx + 1}`} className="w-full h-full object-cover" />
                                    {!isOverdue && (
                                      <button
                                        type="button"
                                        onClick={() => handleRemoveEvidencePhoto(fd.id, url)}
                                        className="absolute top-1 right-1 bg-rose-600 text-white p-0.5 rounded opacity-80 hover:opacity-100"
                                        title="Remove photo"
                                      >
                                        <Trash2 className="w-3 h-3" />
                                      </button>
                                    )}
                                  </div>
                                ))}
                              </div>
                            )}

                            {/* Action Buttons Row */}
                            <div className="flex flex-wrap items-center gap-2 pt-1">
                              {!isOverdue && (
                                <label className="px-3 py-1.5 bg-slate-900 hover:bg-slate-800 text-white font-bold rounded-lg cursor-pointer inline-flex items-center gap-1.5 text-xs transition">
                                  <Upload className="w-3.5 h-3.5" />
                                  <span>{uploadingPhotoForDriveId === fd.id ? 'Uploading...' : 'Add Evidence Photo'}</span>
                                  <input
                                    type="file"
                                    accept="image/*"
                                    className="hidden"
                                    onChange={(e) => handleEvidencePhotoUpload(fd.id, e)}
                                    disabled={uploadingPhotoForDriveId === fd.id || submittingPhotosDriveId === fd.id || isOverdue}
                                  />
                                </label>
                              )}

                              {fd.photosSubmitted ? (
                                <span className="px-3 py-1.5 font-bold rounded-lg inline-flex items-center gap-1.5 text-xs bg-emerald-100 text-emerald-800 border border-emerald-300">
                                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                                  <span>Photos Submitted — Ready to Complete</span>
                                </span>
                              ) : (
                                <button
                                  type="button"
                                  onClick={() => handleSubmitPhotos(fd.id, fd.evidencePhotos)}
                                  disabled={photoCount < 4 || submittingPhotosDriveId === fd.id || isOverdue}
                                  className={`px-3 py-1.5 font-bold rounded-lg inline-flex items-center gap-1.5 text-xs transition ${
                                    photoCount >= 4 && !isOverdue
                                      ? 'bg-emerald-600 hover:bg-emerald-700 text-white shadow-sm cursor-pointer'
                                      : 'bg-slate-200 text-slate-400 cursor-not-allowed'
                                  }`}
                                  title={
                                    isOverdue
                                      ? 'Drive expired: completion deadline has passed'
                                      : photoCount < 4
                                      ? `Upload at least 4 photos to submit (current: ${photoCount})`
                                      : 'Submit Evidence Photos'
                                  }
                                >
                                  <CheckCircle2 className="w-3.5 h-3.5" />
                                  <span>
                                    {submittingPhotosDriveId === fd.id ? 'Submitting Photos...' : 'Submit Photos'}
                                  </span>
                                </button>
                              )}
                            </div>

                            {/* Submission Feedback Message */}
                            {photoSubmitSuccess[fd.id] && (
                              <div className="p-2.5 bg-emerald-50 border border-emerald-200 rounded-lg text-emerald-800 text-[11px] flex items-center gap-1.5">
                                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                                <span>{photoSubmitSuccess[fd.id]}</span>
                              </div>
                            )}
                          </>
                        )}
                      </div>
                    )}

                    {/* Participants List */}
                    {fd.participants && fd.participants.length > 0 && (
                      <div className="space-y-1.5 pt-2 border-t border-slate-100 text-xs">
                        <h4 className="font-bold text-slate-800 text-[11px] uppercase tracking-wider">Contributors & Participants:</h4>
                        {fd.participants.map((p) => (
                          <div key={p.id} className="text-slate-600 flex justify-between bg-slate-50 px-3 py-1.5 rounded">
                            <span>{p.userName} ({p.participantType})</span>
                            <strong className="text-emerald-700">{p.contributionQuantity} {p.contributionUnit}</strong>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>

                {/* Actions Footer */}
                <div className="p-6 pt-0 border-t border-slate-100 mt-4 flex items-center justify-between gap-2">
                  {/* Left: Completion Button for Creator */}
                  {isCreator && fd.status !== 'COMPLETED' && fd.status !== 'CANCELLED' && (
                    <div className="flex-1">
                      <button
                        onClick={() => handleCompleteDrive(fd.id)}
                        disabled={!canComplete || completingDriveId === fd.id}
                        className={`w-full sm:w-auto px-4 py-2 font-bold rounded-xl text-xs shadow transition flex items-center justify-center gap-1.5 ${
                          canComplete
                            ? 'bg-emerald-600 hover:bg-emerald-700 text-white'
                            : 'bg-slate-200 text-slate-400 cursor-not-allowed'
                        }`}
                        title={
                          isOverdue
                            ? 'Drive expired: completion deadline has passed'
                            : !hasStarted
                            ? 'Drive has not started yet'
                            : !isPhotosSubmitted
                            ? 'Submit at least 4 evidence photos first'
                            : 'Mark as Completed'
                        }
                      >
                        <CheckCircle2 className="w-4 h-4" />
                        <span>{completingDriveId === fd.id ? 'Completing...' : 'Mark as Completed'}</span>
                      </button>

                      {!canComplete && (
                        <p className="text-[10px] text-slate-400 mt-1">
                          {isOverdue
                            ? '• Drive expired: completion deadline has passed'
                            : !hasStarted
                            ? `• Completion unlocks after drive starts at ${fd.startTime}`
                            : !isPhotosSubmitted
                            ? `• Upload & click "Submit Photos" (${photoCount}/4 photos ready)`
                            : ''}
                        </p>
                      )}
                      {canComplete && (
                        <p className="text-[10px] text-emerald-600 font-medium mt-1">
                          • Photos submitted. You can now complete the drive before the deadline.
                        </p>
                      )}
                    </div>
                  )}

                  {/* Right: Join / Participate for Non-Completed Drives */}
                  {fd.status !== 'COMPLETED' && fd.status !== 'CANCELLED' && fd.status !== 'EXPIRED' && (
                    <button
                      onClick={() => setJoinModalDrive(fd)}
                      className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-xs shadow transition flex items-center gap-1.5 ml-auto"
                    >
                      <Users className="w-4 h-4" />
                      <span>Join / Contribute Food</span>
                    </button>
                  )}

                  {fd.status === 'COMPLETED' && (
                    <div className="flex items-center gap-1.5 text-xs font-bold text-emerald-700 ml-auto py-2">
                      <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                      <span>Drive Verified & Completed</span>
                    </div>
                  )}

                  {fd.status === 'EXPIRED' && (
                    <div className="flex items-center gap-1.5 text-xs font-semibold text-rose-600 ml-auto py-2">
                      <AlertTriangle className="w-4 h-4 text-rose-500" />
                      <span>Drive Expired (Incomplete)</span>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
