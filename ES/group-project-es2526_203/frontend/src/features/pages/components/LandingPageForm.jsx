import React, { useState, useEffect } from 'react';
import { Button } from '../../../components/ui/button';
import { Input } from '../../../components/ui/input';
import { Label } from '../../../components/ui/label';
import { Textarea } from '../../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../../components/ui/select';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import api from '../../../services/api';

const LandingPageForm = ({ onSubmit, initialData }) => {
    // Try to extract image_url and button_category from metadata if present
    let initialImageUrl = '';
    let initialCategory = '';
    try {
        if (initialData?.metadata) {
            const parsed = JSON.parse(initialData.metadata);
            initialImageUrl = parsed?.image_url || '';
            initialCategory = parsed?.button_category || '';
        }
    } catch (e) {
        initialImageUrl = '';
    }

    const [formData, setFormData] = useState({
        title: initialData?.title || '',
        description: initialData?.description || '',
        metadata: initialData?.metadata || '{}',
        imageUrl: initialImageUrl,
        category: initialCategory,
        startDate: initialData?.startDate || '',
        endDate: initialData?.endDate || ''
    });

    const [categories, setCategories] = useState([]);

    useEffect(() => {
        let mounted = true;
        const fetchCategories = async () => {
            try {
                const data = await api.categories.getAll();
                if (mounted && Array.isArray(data)) {
                    setCategories(data);

                    // If the page already has a category stored (maybe an id), try to normalize it to the category name
                    if (initialCategory) {
                        // find a matching category object by id or name
                        const found = data.find(c => {
                            if (typeof c === 'string') return c === initialCategory;
                            return (c.categoryId && String(c.categoryId) === String(initialCategory)) || c.name === initialCategory || c.name === String(initialCategory);
                        });
                        if (found) {
                            const normalized = typeof found === 'string' ? found : (found.name || String(found.categoryId));
                            setFormData(prev => ({ ...prev, category: normalized }));
                        }
                    }
                }
            } catch (e) {
                // ignore
            }
        };
        fetchCategories();
        return () => { mounted = false; };
    }, []);

    const handleSubmit = (e) => {
        e.preventDefault();
        // Merge imageUrl into metadata JSON before submitting
        let metadataObj = {};
        try {
            if (formData.metadata) {
                metadataObj = JSON.parse(formData.metadata);
            }
        } catch (err) {
            // if metadata is invalid JSON, keep as empty object
            metadataObj = {};
        }

        if (formData.imageUrl) {
            metadataObj.image_url = formData.imageUrl;
        }
        if (formData.category && formData.category !== 'none') {
            metadataObj.button_category = formData.category;
        } else {
            // remove if empty
            if (metadataObj.button_category) delete metadataObj.button_category;
        }

        const payload = {
            title: formData.title,
            description: formData.description,
            metadata: JSON.stringify(metadataObj),
            startDate: formData.startDate || null,
            endDate: formData.endDate || null
        };

        onSubmit(payload);
    };

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        const newValue = type === 'checkbox' ? checked : value;
        setFormData(prev => ({
            ...prev,
            [name]: newValue
        }));
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-6">
            <div className="space-y-4">
                <div className="space-y-2">
                    <Label htmlFor="title">Title</Label>
                    <Input
                        id="title"
                        name="title"
                        value={formData.title}
                        onChange={handleChange}
                        required
                    />
                </div>

                <div className="space-y-2">
                    <Label htmlFor="description">Description</Label>
                    <Textarea
                        id="description"
                        name="description"
                        value={formData.description}
                        onChange={handleChange}
                        required
                        rows={4}
                    />
                </div>

                <div className="space-y-2">
                    <Label htmlFor="metadata">Metadata (JSON)</Label>
                    <Textarea
                        id="metadata"
                        name="metadata"
                        value={formData.metadata}
                        onChange={handleChange}
                        rows={4}
                    />
                </div>

                <div className="space-y-2">
                    <Label htmlFor="imageUrl">Image URL</Label>
                    <Input
                        id="imageUrl"
                        type="text"
                        name="imageUrl"
                        value={formData.imageUrl}
                        onChange={handleChange}
                        placeholder="https://example.com/image.jpg"
                    />
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-2">
                        <Label htmlFor="startDate">Banner Start Date</Label>
                        <Input
                            id="startDate"
                            type="datetime-local"
                            name="startDate"
                            value={formData.startDate}
                            onChange={handleChange}
                        />
                    </div>
                    <div className="space-y-2">
                        <Label htmlFor="endDate">Banner End Date</Label>
                        <Input
                            id="endDate"
                            type="datetime-local"
                            name="endDate"
                            value={formData.endDate}
                            onChange={handleChange}
                        />
                    </div>
                </div>

                <div className="space-y-2">
                    <Label htmlFor="category">Button target category</Label>
                    <Select
                        value={formData.category}
                        onValueChange={(value) => handleChange({ target: { name: 'category', value } })}
                    >
                        <SelectTrigger id="category">
                            <SelectValue placeholder="-- none --" />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="none">-- none --</SelectItem>
                            {categories.map((c) => {
                                const isString = typeof c === 'string';
                                const value = isString ? c : (c.name || c.categoryId || JSON.stringify(c));
                                const key = isString ? c : (c.categoryId || c.name || JSON.stringify(c));
                                return <SelectItem key={key} value={value}>{value}</SelectItem>;
                            })}
                        </SelectContent>
                    </Select>
                    <p className="text-xs text-muted-foreground">When set, the banner button will navigate users to this category.</p>
                </div>

                <Button type="submit">
                    {initialData ? 'Update Landing Page' : 'Create Landing Page'}
                </Button>
            </div>

            <Card className="mt-8">
                <CardHeader>
                    <CardTitle>Preview</CardTitle>
                </CardHeader>
                <CardContent>
                    {formData.imageUrl ? (
                        <img src={formData.imageUrl} alt="Landing page" className="w-full max-h-64 object-cover rounded-lg mb-4" />
                    ) : (
                        <div className="w-full h-40 bg-muted flex items-center justify-center text-muted-foreground rounded-lg mb-4">No image</div>
                    )}
                    <h4 className="text-xl font-bold mb-2">{formData.title || 'Title preview'}</h4>
                    <p className="text-muted-foreground mb-4">{formData.description || 'Description preview'}</p>
                    <Button size="lg">Shop Now</Button>
                </CardContent>
            </Card>
        </form>
    );
};

export default LandingPageForm;
