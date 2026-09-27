import React from 'react';
import { useZoo } from '../context/ZooContext';
import { AlertTriangle, CheckCircle, Info } from 'lucide-react';

export const NotificationBanner = () => {
  const { notification } = useZoo();

  if (!notification) return null;

  const getStyle = () => {
    switch (notification.type) {
      case 'error':
        return {
          bg: 'linear-gradient(135deg, rgba(192, 57, 43, 0.95), rgba(120, 40, 31, 0.95))',
          border: '#e74c3c',
          icon: <AlertTriangle size={20} color="#fadbd8" />
        };
      case 'success':
        return {
          bg: 'linear-gradient(135deg, rgba(39, 174, 96, 0.95), rgba(20, 90, 50, 0.95))',
          border: '#2ecc71',
          icon: <CheckCircle size={20} color="#d4efdf" />
        };
      default:
        return {
          bg: 'linear-gradient(135deg, rgba(41, 128, 185, 0.95), rgba(21, 67, 96, 0.95))',
          border: '#3498db',
          icon: <Info size={20} color="#d4e6f1" />
        };
    }
  };

  const style = getStyle();

  return (
    <div style={{
      position: 'fixed',
      bottom: '1.5rem',
      right: '1.5rem',
      zIndex: 1000,
      maxWidth: '420px',
      background: style.bg,
      border: `1.5px solid ${style.border}`,
      borderRadius: '8px',
      padding: '1rem 1.25rem',
      boxShadow: '0 8px 30px rgba(0, 0, 0, 0.6), 0 0 15px rgba(201, 161, 59, 0.2)',
      color: '#ffffff',
      display: 'flex',
      alignItems: 'flex-start',
      gap: '0.75rem',
      animation: 'slideIn 0.3s ease-out'
    }}>
      <div style={{ marginTop: '2px' }}>{style.icon}</div>
      <div>
        <h4 style={{
          fontSize: '0.95rem',
          margin: 0,
          color: '#ffffff',
          fontFamily: 'var(--font-heading)',
          letterSpacing: '0.03em'
        }}>
          {notification.type === 'error' ? 'Regla Mágica / Error 409' : 'Registro de Fauna'}
        </h4>
        <p style={{ margin: '0.2rem 0 0 0', fontSize: '0.9rem', lineHeight: '1.4' }}>
          {notification.message}
        </p>
      </div>
    </div>
  );
};

