import React from 'react';
import { AlertCircle } from 'lucide-react';

const ErrorAlert = ({ message }) => {
  if (!message) return null;

  return (
    <div className="bg-destructive/10 border border-destructive/20 text-destructive px-4 py-3 rounded-lg mb-4 flex items-start gap-2">
      <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
      <span>{message}</span>
    </div>
  );
};

export default ErrorAlert;