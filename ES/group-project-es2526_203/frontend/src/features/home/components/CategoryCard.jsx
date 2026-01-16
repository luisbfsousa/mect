import React from 'react';
import {
  Smartphone,
  Home,
  Dumbbell,
  BookOpen,
  Shirt,
  HeartPulse,
  Gem,
  FlaskConical,
  Gift
} from 'lucide-react';
import { Card, CardContent } from '../../../components/ui/card';

const CategoryCard = ({ category, onClick }) => {
  const getCategoryIcon = (cat) => {
    const iconClass = "h-8 w-8 mx-auto text-primary";

    const icons = {
      'Electronics': <Smartphone className={iconClass} />,
      'Home & Garden': <Home className={iconClass} />,
      'Sports': <Dumbbell className={iconClass} />,
      'Books': <BookOpen className={iconClass} />,
      'Clothing': <Shirt className={iconClass} />,
      'Health': <HeartPulse className={iconClass} />,
      'Jewelry': <Gem className={iconClass} />,
      'Science': <FlaskConical className={iconClass} />,
      'default': <Gift className={iconClass} />
    };
    return icons[cat] || icons.default;
  };

  return (
    <Card
      onClick={onClick}
      className="cursor-pointer hover:border-primary hover:shadow-lg transition-all duration-200"
    >
      <CardContent className="p-6 text-center">
        <div className="mb-3">
          {getCategoryIcon(category)}
        </div>
        <div className="font-semibold">{category}</div>
      </CardContent>
    </Card>
  );
};

export default CategoryCard;