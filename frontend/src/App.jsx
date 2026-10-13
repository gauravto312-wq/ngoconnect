import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { RoleRoute } from './components/RoleRoute';
import { RoleDashboardLayout } from './components/RoleDashboardLayout';

// Public Pages
import { Home } from './pages/Home';
import { About } from './pages/About';
import { Contact } from './pages/Contact';
import { Marketplace } from './pages/Marketplace';
import { Login } from './pages/Login';
import { AdminLogin } from './pages/AdminLogin';
import { Register } from './pages/Register';

// Citizen Pages
import { CitizenDashboard } from './pages/citizen/CitizenDashboard';
import { CitizenDonations } from './pages/citizen/CitizenDonations';

// Volunteer Pages
import { VolunteerDashboard } from './pages/volunteer/VolunteerDashboard';
import { VolunteerTasks } from './pages/volunteer/VolunteerTasks';

// NGO Pages
import { NgoDashboard } from './pages/ngo/NgoDashboard';
import { NgoMarketplace } from './pages/ngo/NgoMarketplace';
import { NgoClaimedDonations } from './pages/ngo/NgoClaimedDonations';
import { NgoVolunteers } from './pages/ngo/NgoVolunteers';
import { NgoVerification } from './pages/ngo/NgoVerification';

// Restaurant Pages
import { RestaurantDashboard } from './pages/restaurant/RestaurantDashboard';
import { RestaurantBranches } from './pages/restaurant/RestaurantBranches';
import { RestaurantFoodAvailability } from './pages/restaurant/RestaurantFoodAvailability';
import { RestaurantDonations } from './pages/restaurant/RestaurantDonations';

// Admin Pages
import { AdminDashboard } from './pages/admin/AdminDashboard';
import { AdminNgoVerification } from './pages/admin/AdminNgoVerification';
import { AdminRestaurantVerification } from './pages/admin/AdminRestaurantVerification';
import { AdminUsers } from './pages/admin/AdminUsers';
import { AdminPaymentDisputes } from './pages/admin/AdminPaymentDisputes';
import { AdminContactInquiries } from './pages/admin/AdminContactInquiries';

// Shared Pages
import { FoodDrivesView } from './pages/shared/FoodDrivesView';
import { NotificationsView } from './pages/shared/NotificationsView';

import { NgoDiscovery } from './pages/NgoDiscovery';
import { RestaurantDiscovery } from './pages/RestaurantDiscovery';
import { PublicNgoProfile } from './pages/PublicNgoProfile';
import { PublicRestaurantProfile } from './pages/PublicRestaurantProfile';
import { PublicFoodDrives } from './pages/PublicFoodDrives';

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        {/* Public Routes */}
        <Route path="/" element={<Home />} />
        <Route path="/about" element={<About />} />
        <Route path="/contact" element={<Contact />} />
        <Route path="/marketplace" element={<Marketplace />} />
        <Route path="/food-drives" element={<PublicFoodDrives />} />
        <Route path="/ngos" element={<NgoDiscovery />} />
        <Route path="/restaurants" element={<RestaurantDiscovery />} />
        <Route path="/ngos/:id" element={<PublicNgoProfile />} />
        <Route path="/restaurants/:id" element={<PublicRestaurantProfile />} />
        <Route path="/login" element={<Login />} />
        <Route path="/admin/login" element={<AdminLogin />} />
        <Route path="/register" element={<Register />} />

        {/* Protected CITIZEN Routes */}
        <Route element={<RoleRoute allowedRoles={['CITIZEN']} />}>
          <Route element={<RoleDashboardLayout />}>
            <Route path="/citizen" element={<CitizenDashboard />} />
            <Route path="/citizen/profile" element={<CitizenDashboard />} />
            <Route path="/citizen/donations" element={<CitizenDonations />} />
            <Route path="/citizen/marketplace" element={<Marketplace />} />
            <Route path="/citizen/food-drives" element={<FoodDrivesView />} />
            <Route path="/citizen/notifications" element={<NotificationsView />} />
          </Route>
        </Route>

        {/* Protected VOLUNTEER Routes */}
        <Route element={<RoleRoute allowedRoles={['VOLUNTEER']} />}>
          <Route element={<RoleDashboardLayout />}>
            <Route path="/volunteer" element={<VolunteerDashboard />} />
            <Route path="/volunteer/profile" element={<VolunteerDashboard />} />
            <Route path="/volunteer/tasks" element={<VolunteerTasks />} />
            <Route path="/volunteer/food-drives" element={<FoodDrivesView />} />
            <Route path="/volunteer/notifications" element={<NotificationsView />} />
          </Route>
        </Route>

        {/* Protected NGO Routes */}
        <Route element={<RoleRoute allowedRoles={['NGO']} />}>
          <Route element={<RoleDashboardLayout />}>
            <Route path="/ngo" element={<NgoDashboard />} />
            <Route path="/ngo/profile" element={<NgoDashboard />} />
            <Route path="/ngo/verification" element={<NgoVerification />} />
            <Route path="/ngo/marketplace" element={<NgoMarketplace />} />
            <Route path="/ngo/claimed-donations" element={<NgoClaimedDonations />} />
            <Route path="/ngo/volunteers" element={<NgoVolunteers />} />
            <Route path="/ngo/food-drives" element={<FoodDrivesView />} />
            <Route path="/ngo/notifications" element={<NotificationsView />} />
          </Route>
        </Route>

        {/* Protected RESTAURANT Routes */}
        <Route element={<RoleRoute allowedRoles={['RESTAURANT']} />}>
          <Route element={<RoleDashboardLayout />}>
            <Route path="/restaurant" element={<RestaurantDashboard />} />
            <Route path="/restaurant/profile" element={<RestaurantDashboard />} />
            <Route path="/restaurant/branches" element={<RestaurantBranches />} />
            <Route path="/restaurant/availability" element={<RestaurantFoodAvailability />} />
            <Route path="/restaurant/donations" element={<RestaurantDonations />} />
            <Route path="/restaurant/marketplace" element={<Marketplace />} />
            <Route path="/restaurant/volunteers" element={<NgoVolunteers />} />
            <Route path="/restaurant/food-drives" element={<FoodDrivesView />} />
            <Route path="/restaurant/notifications" element={<NotificationsView />} />
          </Route>
        </Route>

        {/* Protected ADMIN Routes */}
        <Route element={<RoleRoute allowedRoles={['ADMIN']} />}>
          <Route element={<RoleDashboardLayout />}>
            <Route path="/admin" element={<AdminDashboard />} />
            <Route path="/admin/users" element={<AdminUsers />} />
            <Route path="/admin/ngo-verifications" element={<AdminNgoVerification />} />
            <Route path="/admin/restaurant-verifications" element={<AdminRestaurantVerification />} />
            <Route path="/admin/disputes" element={<AdminPaymentDisputes />} />
            <Route path="/admin/inquiries" element={<AdminContactInquiries />} />
            <Route path="/admin/food-drives" element={<FoodDrivesView />} />
            <Route path="/admin/notifications" element={<NotificationsView />} />
          </Route>
        </Route>

        {/* Catch-all Redirect */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
