import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../../../services/api';
import { Button } from '../../../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Alert, AlertDescription } from '../../../components/ui/alert';
import { Loader2 } from 'lucide-react';

const LandingPageDetailView = () => {
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

    const handlePublish = async () => {
        try {
            const data = await api.pages.publish(id);
            setLandingPage(data);
        } catch (err) {
            setError(err.message || 'Failed to publish landing page');
        }
    };

    const handleDelete = async () => {
        if (window.confirm('Are you sure you want to delete this landing page?')) {
            try {
                await api.pages.delete(id);
                navigate('/landing-pages');
            } catch (err) {
                setError(err.message || 'Failed to delete landing page');
            }
        }
    };

    const handleUnpublish = async () => {
        try {
            const data = await api.pages.unpublish(id);
            setLandingPage(data);
        } catch (err) {
            setError(err.message || 'Failed to unpublish landing page');
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
                <h1 className="text-2xl font-bold">{landingPage.title}</h1>
                <div className="space-x-2">
                    {!landingPage.isPublished && (
                        <Button onClick={handlePublish}>
                            Publish
                        </Button>
                    )}
                    {landingPage.isPublished && (
                        <Button variant="outline" onClick={handleUnpublish}>
                            Unpublish
                        </Button>
                    )}
                    <Button variant="outline" onClick={() => navigate(`/landing-pages/${id}/edit`)}>
                        Edit
                    </Button>
                    <Button variant="destructive" onClick={handleDelete}>
                        Delete
                    </Button>
                    <Button variant="outline" onClick={() => navigate('/landing-pages')}>
                        Back
                    </Button>
                </div>
            </div>

            <Card>
                <CardContent className="p-6">
                    <div className="prose max-w-none dark:prose-invert">
                        <p className="text-lg mb-4">{landingPage.description}</p>

                        {landingPage.metadata && (
                            <div className="mt-8">
                                <h2 className="text-xl font-semibold mb-4">Additional Information</h2>
                                <div className="bg-muted p-4 rounded text-sm overflow-x-auto">
                                    <pre className="whitespace-pre-wrap break-words max-w-full">
                                        {JSON.stringify(JSON.parse(landingPage.metadata), null, 2).split('\n').map((line, idx) => {
                                            if (line.includes('url') && line.length > 80) {
                                                return line.substring(0, 77) + '...' + '\n';
                                            }
                                            return line;
                                        }).join('\n')}
                                    </pre>
                                </div>
                            </div>
                        )}
                    </div>
                </CardContent>
            </Card>
        </div>
    );
};

export default LandingPageDetailView;
