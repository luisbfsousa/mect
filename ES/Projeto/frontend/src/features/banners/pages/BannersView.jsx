import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../../services/api';
import { Button } from '../../../components/ui/button';
import { Card, CardContent } from '../../../components/ui/card';
import { Alert, AlertDescription } from '../../../components/ui/alert';

const BannersView = () => {
    const navigate = useNavigate();
    const [banners, setBanners] = useState([]);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetchBanners();
    }, []);

    const fetchBanners = async () => {
        try {
            const data = await api.banners.getAll();
            setBanners(data || []);
        } catch (err) {
            setError(err.message || 'Failed to fetch banners');
        }
    };

    return (
        <div className="max-w-7xl mx-auto py-8 px-4">
            <div className="flex justify-between items-center mb-8">
                <h1 className="text-2xl font-bold">Promotional Banners</h1>
                <Button onClick={() => navigate('/banners/create')}>Create Banner</Button>
            </div>

            {error && (
                <Alert variant="destructive" className="mb-4">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}

            {banners.length === 0 ? (
                <div className="text-center py-12">
                    <p className="text-muted-foreground">No banners found.</p>
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {banners.map(b => (
                        <Card key={b.id}>
                            <CardContent className="p-4">
                                <h3 className="font-semibold text-lg">{b.title}</h3>
                                <p className="text-sm text-muted-foreground">{b.description}</p>
                                <div className="mt-4 space-x-2">
                                    <Button variant="outline" size="sm" onClick={() => navigate(`/banners/${b.id}`)}>
                                        View
                                    </Button>
                                    <Button variant="outline" size="sm" onClick={() => navigate(`/banners/${b.id}/edit`)}>
                                        Edit
                                    </Button>
                                </div>
                            </CardContent>
                        </Card>
                    ))}
                </div>
            )}
        </div>
    );
};

export default BannersView;
