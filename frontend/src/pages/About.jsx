import React from 'react';
import { PublicNavbar } from '../components/PublicNavbar';
import { Footer } from '../components/Footer';
import { HeartHandshake, ShieldCheck, Truck, Users, Building2, CheckCircle2 } from 'lucide-react';

export const About = () => {
  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <PublicNavbar />
      
      <div className="max-w-4xl mx-auto px-4 py-16 flex-1">
        <div className="text-center mb-12">
          <div className="w-12 h-12 rounded-xl bg-emerald-600 text-white flex items-center justify-center mx-auto mb-4 font-bold">
            <HeartHandshake className="w-7 h-7" />
          </div>
          <h1 className="text-3xl font-extrabold text-slate-900">About NGOConnect</h1>
          <p className="text-slate-600 text-sm mt-2 max-w-xl mx-auto">
            A real-world platform designed for food recovery, item donations, active volunteer task distribution, and community food drives.
          </p>
        </div>

        <div className="space-y-8 bg-white p-8 rounded-2xl border border-slate-200 shadow-sm text-slate-700 text-sm leading-relaxed">
          <div>
            <h3 className="text-lg font-bold text-slate-900 mb-2">Our Mission</h3>
            <p>
              NGOConnect addresses urban surplus food wastage by seamlessly connecting local donors (Citizens and Restaurants) with verified Non-Governmental Organizations (NGOs) and active local volunteers.
            </p>
          </div>

          <div className="border-t border-slate-100 pt-6">
            <h3 className="text-lg font-bold text-slate-900 mb-4">Core Platform Pillars</h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
                <ShieldCheck className="w-6 h-6 text-emerald-600 mb-2" />
                <h4 className="font-bold text-slate-900">Verified Organizations</h4>
                <p className="text-xs text-slate-500 mt-1">Admin verification module ensures only legitimate NGOs claim donations.</p>
              </div>
              <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
                <Truck className="w-6 h-6 text-emerald-600 mb-2" />
                <h4 className="font-bold text-slate-900">Active Volunteer Dispatch</h4>
                <p className="text-xs text-slate-500 mt-1">Volunteers receive unique IDs (e.g. VOL-10025) and toggle availability status.</p>
              </div>
              <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
                <CheckCircle2 className="w-6 h-6 text-emerald-600 mb-2" />
                <h4 className="font-bold text-slate-900">Private Pickup Verification</h4>
                <p className="text-xs text-slate-500 mt-1">Random unique codes (e.g. NGC-847291) prevent unauthorized donation handovers.</p>
              </div>
              <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
                <Users className="w-6 h-6 text-emerald-600 mb-2" />
                <h4 className="font-bold text-slate-900">Community Food Drives</h4>
                <p className="text-xs text-slate-500 mt-1">Collective food distribution events matching local donors, NGOs, and volunteers.</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <Footer />
    </div>
  );
};
