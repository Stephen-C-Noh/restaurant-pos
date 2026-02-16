import { useState, useEffect, useMemo } from 'react';
import { fetchActiveMenuItems, fetchCategories } from '../services/menuService';

export default function useMenu() {
    const [menuItems, setMenuItems] = useState([]);
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedSection, setSelectedSection] = useState('ALL');

    const loadMenu = () => {
        setLoading(true);
        setError(null);

        Promise.all([fetchActiveMenuItems(), fetchCategories()])
            .then(([items, cats]) => {
                setMenuItems(items);
                setCategories(cats);
                setLoading(false);
            })
            .catch((err) => {
                setError(err.response?.data?.message || 'Failed to load menu.');
                setLoading(false);
            });
    };

    useEffect(() => {
        loadMenu();
    }, []);

    const sections = useMemo(() => {
        const sectionSet = new Set(menuItems.map((item) => item.kitchenSection));
        return Array.from(sectionSet);
    }, [menuItems]);

    const menuItemsBySection = useMemo(() => {
        const grouped = {};
        for (const item of menuItems) {
            const section = item.kitchenSection;
            if (!grouped[section]) grouped[section] = [];
            grouped[section].push(item);
        }
        return grouped;
    }, [menuItems]);

    const filteredItems = useMemo(() => {
        if (selectedSection === 'ALL') return menuItems;
        return menuItems.filter((item) => item.kitchenSection === selectedSection);
    }, [menuItems, selectedSection]);

    return {
        menuItems,
        menuItemsBySection,
        filteredItems,
        categories,
        sections,
        loading,
        error,
        selectedSection,
        setSelectedSection,
        retry: loadMenu,
    };
}
