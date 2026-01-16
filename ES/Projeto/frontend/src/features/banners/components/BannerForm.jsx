import React, { useState } from 'react';
import { Button } from '../../../components/ui/button';
import { Input } from '../../../components/ui/input';
import { Label } from '../../../components/ui/label';
import { Textarea } from '../../../components/ui/textarea';
import { Checkbox } from '../../../components/ui/checkbox';

const BannerForm = ({ onSubmit, initialData }) => {
    const [formData, setFormData] = useState({
        title: initialData?.title || '',
        description: initialData?.description || '',
        imageUrl: initialData?.imageUrl || '',
        metadata: initialData?.metadata || '{}',
        startAt: initialData?.startAt || '',
        endAt: initialData?.endAt || '',
        priority: initialData?.priority || 0,
        landingPageId: initialData?.landingPageId || '' ,
        isPublished: initialData?.isPublished || false
    });

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        const newValue = type === 'checkbox' ? checked : value;
        setFormData(prev => ({ ...prev, [name]: newValue }));
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        onSubmit(formData);
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
                <Label htmlFor="title">Title</Label>
                <Input id="title" name="title" value={formData.title} onChange={handleChange} required />
            </div>

            <div className="space-y-2">
                <Label htmlFor="description">Description</Label>
                <Textarea id="description" name="description" value={formData.description} onChange={handleChange} rows={3} />
            </div>

            <div className="space-y-2">
                <Label htmlFor="imageUrl">Image URL</Label>
                <Input id="imageUrl" name="imageUrl" value={formData.imageUrl} onChange={handleChange} />
            </div>

            <div className="space-y-2">
                <Label htmlFor="metadata">Metadata (JSON)</Label>
                <Textarea id="metadata" name="metadata" value={formData.metadata} onChange={handleChange} rows={3} />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                    <Label htmlFor="startAt">Start At (ISO)</Label>
                    <Input id="startAt" name="startAt" value={formData.startAt} onChange={handleChange} placeholder="2025-11-09T12:00:00" />
                </div>
                <div className="space-y-2">
                    <Label htmlFor="endAt">End At (ISO)</Label>
                    <Input id="endAt" name="endAt" value={formData.endAt} onChange={handleChange} placeholder="2025-11-30T23:59:59" />
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                    <Label htmlFor="priority">Priority</Label>
                    <Input id="priority" name="priority" type="number" value={formData.priority} onChange={handleChange} />
                </div>
                <div className="space-y-2">
                    <Label htmlFor="landingPageId">Landing Page ID (optional)</Label>
                    <Input id="landingPageId" name="landingPageId" value={formData.landingPageId} onChange={handleChange} />
                </div>
            </div>

            <div className="flex items-center space-x-2">
                <Checkbox id="isPublished" name="isPublished" checked={formData.isPublished} onCheckedChange={(checked) => handleChange({ target: { name: 'isPublished', type: 'checkbox', checked } })} />
                <Label htmlFor="isPublished">Published</Label>
            </div>

            <Button type="submit">{initialData ? 'Update Banner' : 'Create Banner'}</Button>
        </form>
    );
};

export default BannerForm;
