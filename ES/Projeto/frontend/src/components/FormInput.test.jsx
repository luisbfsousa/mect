import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import FormInput from './FormInput';

describe('FormInput Component', () => {
  test('renders label and input field', () => {
    render(
      <FormInput
        label="Email"
        name="email"
        value=""
        onChange={() => {}}
      />
    );

    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByRole('textbox')).toBeInTheDocument();
  });

  test('handles user input', () => {
    const handleChange = jest.fn();

    render(
      <FormInput
        label="Username"
        name="username"
        value=""
        onChange={handleChange}
      />
    );

    const input = screen.getByRole('textbox');
    userEvent.type(input, 'testuser');

    expect(handleChange).toHaveBeenCalled();
  });

  test('displays value prop correctly', () => {
    render(
      <FormInput
        label="Name"
        name="name"
        value="John Doe"
        onChange={() => {}}
      />
    );

    expect(screen.getByRole('textbox')).toHaveValue('John Doe');
  });

  test('renders with placeholder', () => {
    render(
      <FormInput
        label="Phone"
        name="phone"
        value=""
        placeholder="Enter phone number"
        onChange={() => {}}
      />
    );

    expect(screen.getByPlaceholderText(/enter phone number/i)).toBeInTheDocument();
  });

  test('renders required field', () => {
    render(
      <FormInput
        label="Password"
        name="password"
        type="password"
        value=""
        required
        onChange={() => {}}
      />
    );

    const input = screen.getByLabelText(/password/i);
    expect(input).toBeRequired();
  });
});
