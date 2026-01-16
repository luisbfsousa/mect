import React from 'react';
import { Input } from './ui/input';
import { Label } from './ui/label';

const FormInput = ({ label, name, type = 'text', value, onChange, placeholder, required }) => (
  <div className="space-y-2">
    <Label htmlFor={name}>{label}</Label>
    <Input
      id={name}
      name={name}
      type={type}
      required={required}
      value={value}
      onChange={onChange}
      placeholder={placeholder}
    />
  </div>
);

export default FormInput;