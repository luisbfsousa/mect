import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../../../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Separator } from '../../../components/ui/separator';
import ProfileField from '../components/ProfileField';

const ProfileView = ({ user, orderHistory, handleLogout }) => {
  const navigate = useNavigate();

  return (
    <div className="max-w-2xl mx-auto">
      <Card>
        <CardHeader>
          <CardTitle>My Profile</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <ProfileField label="Name" value={user?.name} />
            <ProfileField label="Email" value={user?.email} />
            <ProfileField label="Total Orders" value={orderHistory.length} />

            <Separator className="my-4" />

            <div className="space-y-3">
              <Button
                onClick={() => navigate('/profile/shipping-billing')}
                className="w-full"
                size="lg"
              >
                Update Shipping & Billing Information
              </Button>

              <Button
                onClick={handleLogout}
                variant="destructive"
                className="w-full"
                size="lg"
              >
                Sign Out
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default ProfileView;