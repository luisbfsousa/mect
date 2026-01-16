import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../../../services/api';
import { Button } from '../../../components/ui/button';
import { Card, CardContent } from '../../../components/ui/card';
import { Alert, AlertDescription } from '../../../components/ui/alert';
import BannerForm from '../components/BannerForm';

const BannerDetailView = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [banner, setBanner] = useState(null);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetchBanner();
    }, [id]);

    const fetchBanner = async () => {
        try {
            const data = await api.banners.getById(id);
            setBanner(data);
        } catch (err) {
            setError(err.message || 'Failed to fetch banner');
        }
    };

    const handlePublish = async () => {
        try {
            const data = await api.banners.publish(id);
            setBanner(data);
        } catch (err) {
            setError(err.message || 'Failed to publish banner');
        }
    };

    const handleUpdate = async (formData) => {
        try {
            await api.banners.update(id, formData);
            navigate('/banners');
        } catch (err) {
            setError(err.message || 'Failed to update banner');
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

    if (!banner) {
        return (
            <div className="max-w-4xl mx-auto py-8 px-4">
                <div className="text-center text-muted-foreground">Loading...</div>
            </div>
        );
    }

    return (
        <div className="max-w-4xl mx-auto py-8 px-4">
            <div className="flex justify-between items-center mb-8">
                <h1 className="text-2xl font-bold">{banner.title}</h1>
                <div className="space-x-2">
                    {!banner.isPublished && <Button onClick={handlePublish}>Publish</Button>}
                    <Button variant="outline" onClick={() => navigate('/banners')}>Back</Button>
                </div>
            </div>

            <Card>
                <CardContent className="p-6">
                    <BannerForm onSubmit={handleUpdate} initialData={banner} />
                </CardContent>
            </Card>
        </div>
    );
};

export default BannerDetailView;
