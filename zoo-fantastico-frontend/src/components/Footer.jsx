import React from 'react';
import { Feather, Shield, BookOpen } from 'lucide-react';

export const Footer = () => {
  return (
    <footer style={{
      marginTop: '4rem',
      borderTop: '1px solid rgba(201, 161, 59, 0.3)',
      background: 'linear-gradient(180deg, #071712 0%, #040e0b 100%)',
      padding: '2.5rem 0 1.5rem 0',
      color: '#b8ac97',
      fontSize: '0.9rem'
    }}>
      <div className="container" style={{ textAlign: 'center' }}>
        <div style={{ display: 'flex', justifyContent: 'center', gap: '2rem', marginBottom: '1rem', flexWrap: 'wrap' }}>
          <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <Feather size={16} color="#c9a13b" /> Registro Oficial de Scamander
          </span>
          <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <Shield size={16} color="#c9a13b" /> Departamento de Regulación y Control de Criaturas Mágicas
          </span>
          <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <BookOpen size={16} color="#c9a13b" /> MACUSA / Ministerio de Magia 1926
          </span>
        </div>
        <p style={{ fontFamily: 'var(--font-heading)', color: '#ebd180', fontSize: '0.85rem', letterSpacing: '0.1em' }}>
          &ldquo;MI REGLA ES SENCILLA: SI ESTÁ EN PELIGRO, PROTÉGELO&rdquo; &mdash; NEWT SCAMANDER
        </p>
        <p style={{ marginTop: '0.5rem', fontSize: '0.8rem', opacity: 0.7 }}>
          Laboratorio 3 &bull; Universidad Javeriana &bull; Sistema Zoo Fantástico (Spring Boot 3 + React Vite)
        </p>
      </div>
    </footer>
  );
};

