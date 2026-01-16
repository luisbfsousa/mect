import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../../services/api';
import LandingPageForm from '../components/LandingPageForm';
import { Button } from '../../../components/ui/button';
import { Alert, AlertDescription } from '../../../components/ui/alert';

const CreateLandingPageView = () => {
    const navigate = useNavigate();
    const [error, setError] = useState(null);

    const handleSubmit = async (formData) => {
        try {
            await api.pages.create(formData);
            navigate('/landing-pages');
        } catch (err) {
            setError(err.message || 'Failed to create landing page');
        }
    };

    return (
        <div className="max-w-4xl mx-auto py-8 px-4">
            <div className="flex justify-between items-center mb-8">
                <h1 className="text-2xl font-bold">Create New Landing Page</h1>
                <Button variant="outline" onClick={() => navigate('/landing-pages')}>
                    Back to Landing Pages
                </Button>
            </div>

            {error && (
                <Alert variant="destructive" className="mb-4">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}

            <LandingPageForm onSubmit={handleSubmit} />
        </div>
    );
};

export default CreateLandingPageView;
