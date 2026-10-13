import React from 'react';
import { CheckCircle2, AlertCircle, ShieldCheck } from 'lucide-react';

export const VerificationBadge = ({ status, className = '' }) => {
  const isVerified = status && (status.toUpperCase() === 'VERIFIED' || status.toUpperCase() === 'APPROVED');

  if (isVerified) {
    return (
      <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-200 shadow-sm ${className}`}>
        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
        <span>✓ Verified</span>
      </span>
    );
  }

  return (
    <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-50 text-amber-800 border border-amber-200 ${className}`}>
      <AlertCircle className="w-3.5 h-3.5 text-amber-600" />
      <span>Unverified</span>
    </span>
  );
};
