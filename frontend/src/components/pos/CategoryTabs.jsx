export default function CategoryTabs({ sections, selectedSection, onSelect }) {
    const allSections = ['ALL', ...sections];

    return (
        <div className="flex gap-2 mb-4 flex-wrap">
            {allSections.map((section) => (
                <button
                    key={section}
                    onClick={() => onSelect(section)}
                    className={`px-4 py-2 rounded font-semibold transition-colors text-sm ${
                        selectedSection === section
                            ? 'bg-blue-600 text-white'
                            : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
                    }`}
                >
                    {section}
                </button>
            ))}
        </div>
    );
}
