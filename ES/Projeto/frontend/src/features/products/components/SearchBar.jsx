import React from 'react';
import { Search } from 'lucide-react';
import { Input } from '../../../components/ui/input';

const SearchBar = ({ value, onChange }) => {
  return (
    <div className="relative">
      <Search
        className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground"
        size={20}
      />
      <Input
        type="text"
        placeholder="Search products..."
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="pl-10"
      />
    </div>
  );
};

export default SearchBar;