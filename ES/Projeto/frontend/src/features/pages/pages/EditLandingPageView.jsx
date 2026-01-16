import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../../../services/api';
import LandingPageForm from '../components/LandingPageForm';
import { Button } from '../../../components/ui/button';
import { Alert, AlertDescription } from '../../../components/ui/alert';
import { Loader2 } from 'lucide-react';

const EditLandingPageView = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [landingPage, setLandingPage] = useState(null);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetchLandingPage();
    }, [id]);

    const fetchLandingPage = async () => {
        try {
            const data = await api.pages.getById(id);
            setLandingPage(data);
        } catch (err) {
            setError(err.message || 'Failed to fetch landing page');
        }
    };

    const handleSubmit = async (formData) => {
        try {
            await api.pages.update(id, formData);
            navigate(`/landing-pages/${id}`);
        } catch (err) {
            setError(err.message || 'Failed to update landing page');
        }
    };

    if (error) {
        return (
            <div className="max-w-4xl mx-auto py-8 px-4">
                <Alert variant="destructive">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            </div>
        );
    }

    if (!landingPage) {
        return (
            <div className="max-w-4xl mx-auto py-8 px-4 text-center">
                <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4" />
                <p className="text-muted-foreground">Loading...</p>
            </div>
        );
    }

    return (
        <div className="max-w-4xl mx-auto py-8 px-4">
            <div className="flex justify-between items-center mb-8">
                <h1 className="text-2xl font-bold">Edit Landing Page</h1>
                <Button variant="outline" onClick={() => navigate(`/landing-pages/${id}`)}>
                    Back
                </Button>
            </div>

            <LandingPageForm onSubmit={handleSubmit} initialData={landingPage} />
        </div>
    );
};

export default EditLandingPageView;
