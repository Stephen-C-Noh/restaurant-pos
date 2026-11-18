import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';

vi.mock('../../services/authService', () => ({ loginWithPin: vi.fn() }));
vi.mock('../PinPad', () => ({
    default: ({ onSubmit }) => <button onClick={() => onSubmit('7890')}>submit pin</button>,
}));

const { loginWithPin } = await import('../../services/authService');
const { default: Login } = await import('../Login');

async function loginAs(role) {
    loginWithPin.mockResolvedValue({ token: 't', userId: 'u1', username: 'u', fullName: 'U', role });
    render(
        <MemoryRouter initialEntries={['/login']}>
            <Routes>
                <Route path="/login" element={<Login />} />
                <Route path="/kds" element={<div>kitchen page</div>} />
            </Routes>
        </MemoryRouter>
    );
    fireEvent.click(screen.getByText('submit pin'));
}

describe('Login', () => {
    it('sends a kitchen user to the kitchen screen', async () => {
        await loginAs('KITCHEN');

        expect(await screen.findByText('kitchen page')).toBeInTheDocument();
    });
});
