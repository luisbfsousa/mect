import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../../services/api';
import LandingPageCard from '../components/LandingPageCard';
import { Button } from '../../../components/ui/button';
import { Card } from '../../../components/ui/card';
import { Alert, AlertDescription } from '../../../components/ui/alert';
import { Loader2 } from 'lucide-react';

const LandingPagesView = () => {
    const navigate = useNavigate();
    const [landingPages, setLandingPages] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetchLandingPages();
    }, []);

    const fetchLandingPages = async () => {
        try {
            setLoading(true);
            setError(null);

            // Use authenticated API helper
            const data = await api.pages.getAll();

            // Accept multiple possible response shapes — prefer array direct response
            if (Array.isArray(data)) {
                setLandingPages(data);
            } else if (data && Array.isArray(data.data)) {
                setLandingPages(data.data);
            } else if (data && Array.isArray(data.content)) {
                setLandingPages(data.content);
            } else {
                // Not an array — avoid calling .map on non-array
                console.warn('Unexpected landing pages response shape:', data);
                setLandingPages([]);
            }
        } catch (err) {
            // api.pages.getAll throws with a user-friendly message when auth fails
            setError(err.message || 'Failed to fetch landing pages');
            setLandingPages([]); // Clear on error
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="max-w-7xl mx-auto p-6">
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">Landing Pages</h1>
                <Button onClick={() => navigate('/landing-pages/create')}>
                    Create Landing Page
                </Button>
            </div>

            {loading ? (
                <div className="text-center py-12">
                    <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4" />
                    <p className="text-muted-foreground">Loading landing pages...</p>
                </div>
            ) : error ? (
                <Alert variant="destructive" className="mb-4">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            ) : landingPages.length === 0 ? (
                <Card className="p-12 text-center">
                    <p className="text-muted-foreground mb-4">No landing pages yet</p>
                    <Button onClick={() => navigate('/landing-pages/create')}>
                        Create your first landing page
                    </Button>
                </Card>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {landingPages.map((page) => (
                        <LandingPageCard key={page.id} landingPage={page} />
                    ))}
                </div>
            )}
        </div>
    );
};

export default LandingPagesView;
