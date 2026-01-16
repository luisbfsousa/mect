import React from 'react';
import { Button as ShadcnButton } from './ui/button';
import { cn } from '../lib/utils';

const Button = ({
  children,
  type = 'button',
  onClick,
  disabled = false,
  variant = 'primary',
  size = 'default',
  fullWidth = false,
  className = '',
  ...props
}) => {
  // Map old variant names to shadcn variants
  const variantMap = {
    primary: 'default',
    secondary: 'secondary',
    link: 'link'
  };

  const mappedVariant = variantMap[variant] || variant;

  // Map old size names to shadcn sizes
  const sizeMap = {
    sm: 'sm',
    md: 'default',
    lg: 'lg'
  };

  const mappedSize = sizeMap[size] || size;

  return (
    <ShadcnButton
      type={type}
      onClick={onClick}
      disabled={disabled}
      variant={mappedVariant}
      size={mappedSize}
      className={cn(fullWidth && 'w-full', className)}
      {...props}
    >
      {children}
    </ShadcnButton>
  );
};

export default Button;