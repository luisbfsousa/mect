import React from 'react';
import { Card } from '../../../components/ui/card';

const ProfileField = ({ label, value }) => {
  return (
    <div>
      <label className="block font-semibold mb-2">{label}</label>
      <Card className="px-4 py-2 bg-muted">
        {value}
      </Card>
    </div>
  );
};

export default ProfileField;