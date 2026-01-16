import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../../services/api';
import BannerForm from '../components/BannerForm';
import { Button } from '../../../components/ui/button';
import { Alert, AlertDescription } from '../../../components/ui/alert';

const CreateBannerView = () => {
    const navigate = useNavigate();
    const [error, setError] = useState(null);

    const handleSubmit = async (formData) => {
        try {
            await api.banners.create(formData);
            navigate('/banners');
        } catch (err) {
            setError(err.message || 'Failed to create banner');
        }
    };

    return (
        <div className="max-w-4xl mx-auto py-8 px-4">
            <div className="flex justify-between items-center mb-8">
                <h1 className="text-2xl font-bold">Create New Banner</h1>
                <Button variant="outline" onClick={() => navigate('/banners')}>Back to Banners</Button>
            </div>

            {error && (
                <Alert variant="destructive" className="mb-4">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}

            <BannerForm onSubmit={handleSubmit} />
        </div>
    );
};

export default CreateBannerView;
