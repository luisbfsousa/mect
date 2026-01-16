import { useState, useEffect } from 'react';

export const useDarkMode = () => {
    const [isDark, setIsDark] = useState(() => {
        try {
            const saved = localStorage.getItem('darkMode');
            if (saved !== null) return JSON.parse(saved);
        } catch (e) {
            // ignore localStorage errors (e.g., SSR or privacy settings)
        }
        return typeof window !== 'undefined' && window.matchMedia
            ? window.matchMedia('(prefers-color-scheme: dark)').matches
            : false;
    });

    useEffect(() => {
        const html = document.documentElement;
        if (isDark) {
            html.classList.add('dark');
        } else {
            html.classList.remove('dark');
        }
        try {
            localStorage.setItem('darkMode', JSON.stringify(isDark));
            window.dispatchEvent(new CustomEvent('darkModeChange', { detail: isDark }));
        } catch (e) {
            // ignore localStorage write errors
        }
    }, [isDark]);

    useEffect(() => {
        const handleDarkModeChange = (e) => {
            setIsDark(e.detail);
        };

        window.addEventListener('darkModeChange', handleDarkModeChange);
        
        return () => {
            window.removeEventListener('darkModeChange', handleDarkModeChange);
        };
    }, []);

    const toggleDarkMode = () => setIsDark(prev => !prev);

    return { isDark, toggleDarkMode };
};

export default useDarkMode;