import React from 'react';
import { Link } from 'react-router-dom';
import { Card, CardContent } from '../../../components/ui/card';
import { Badge } from '../../../components/ui/badge';

const LandingPageCard = ({ landingPage }) => {
    const isExpired = landingPage.endDate && new Date(landingPage.endDate) < new Date();

    return (
        <Link to={`/landing-pages/${landingPage.id}`} className="block">
            <Card className="hover:shadow-lg transition-shadow">
                <CardContent className="p-6">
                    <h3 className="text-lg font-semibold mb-2">
                        {landingPage.title}
                    </h3>
                    <p className="text-muted-foreground line-clamp-2 mb-4">
                        {landingPage.description}
                    </p>
                    <div className="flex gap-2 text-sm">
                        {isExpired ? (
                            <Badge variant="destructive">Expired</Badge>
                        ) : landingPage.isPublished ? (
                            <Badge variant="default">Published</Badge>
                        ) : (
                            <Badge variant="secondary">Draft</Badge>
                        )}
                        {landingPage.startDate && (
                            <span className="text-muted-foreground">
                                {new Date(landingPage.startDate).toLocaleDateString()}
                                {landingPage.endDate ? ` - ${new Date(landingPage.endDate).toLocaleDateString()}` : ''}
                            </span>
                        )}
                    </div>
                </CardContent>
            </Card>
        </Link>
    );
};

export default LandingPageCard;