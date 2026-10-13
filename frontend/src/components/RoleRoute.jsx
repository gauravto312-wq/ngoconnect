import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const RoleRoute = ({ allowedRoles }) => {
  const { user } = useAuth();

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 px-4">
        <div className="max-w-md w-full text-center bg-white p-8 rounded-xl shadow-sm border border-slate-200">
          <div className="w-12 h-12 rounded-full bg-red-100 text-red-600 flex items-center justify-center mx-auto mb-4 font-bold text-xl">
            403
          </div>
          <h2 className="text-xl font-bold text-slate-800 mb-2">Access Forbidden</h2>
          <p className="text-sm text-slate-600 mb-6">
            Your current role <span className="font-semibold text-slate-800">({user.role})</span> does not have authorization to view this dashboard page.
          </p>
          <a
            href={`/${user.role.toLowerCase()}`}
            className="inline-block px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-medium text-sm rounded-lg shadow transition"
          >
            Go to Your Dashboard
          </a>
        </div>
      </div>
    );
  }

  return <Outlet />;
};
