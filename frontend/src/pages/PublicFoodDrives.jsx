import React from 'react';
import { PublicNavbar } from '../components/PublicNavbar';
import { Footer } from '../components/Footer';
import { FoodDrivesView } from './shared/FoodDrivesView';

export const PublicFoodDrives = () => {
  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <PublicNavbar />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full flex-1">
        <FoodDrivesView />
      </div>
      <Footer />
    </div>
  );
};
