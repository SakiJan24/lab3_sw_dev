import React from 'react';
import { NavLink, Link } from 'react-router-dom';
import { Briefcase, Compass, ShieldAlert, Sparkles, PlusCircle, RotateCcw } from 'lucide-react';
import { useZoo } from '../context/ZooContext';

export const Navbar = () => {
  const { resetToDefaultMock } = useZoo();

  return (
    <header style={{
      background: 'linear-gradient(180deg, #0d3b2e 0%, #071712 100%)',
      borderBottom: '2px solid #c9a13b',
      boxShadow: '0 4px 20px rgba(0, 0, 0, 0.7)',
      position: 'sticky',
      top: 0,
      zIndex: 100
    }}>
      <div className="container" style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingTop: '0.8rem',
        paddingBottom: '0.8rem',
        flexWrap: 'wrap',
        gap: '1rem'
      }}>
        {/* Logo & Title */}
        <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', textDecoration: 'none' }}>
          <div style={{
            width: '44px',
            height: '44px',
            background: 'linear-gradient(135deg, #3b2a1a, #24180e)',
            border: '1.5px solid #c9a13b',
            borderRadius: '8px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 0 12px rgba(201, 161, 59, 0.3)'
          }}>
            <Briefcase color="#ebd180" size={24} />
          </div>
          <div>
            <h1 style={{ fontSize: '1.3rem', margin: 0, lineHeight: 1.1, color: '#ebd180' }}>
              Zoológico de Criaturas Mágicas
            </h1>
            <span style={{ fontSize: '0.75rem', color: '#b8ac97', textTransform: 'uppercase', letterSpacing: '0.15em', fontFamily: 'var(--font-heading)' }}>
              Ministerio de Magia &bull; 1926
            </span>
          </div>
        </Link>

        {/* Navigation Links */}
        <nav style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
          <NavLink 
            to="/" 
            end
            style={({ isActive }) => ({
              fontFamily: 'var(--font-heading)',
              fontSize: '0.95rem',
              color: isActive ? '#ebd180' : '#f4efe6',
              borderBottom: isActive ? '2px solid #c9a13b' : '2px solid transparent',
              paddingBottom: '0.2rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.4rem'
            })}
          >
            <Compass size={16} /> Inicio
          </NavLink>

          <NavLink 
            to="/creatures" 
            style={({ isActive }) => ({
              fontFamily: 'var(--font-heading)',
              fontSize: '0.95rem',
              color: isActive ? '#ebd180' : '#f4efe6',
              borderBottom: isActive ? '2px solid #c9a13b' : '2px solid transparent',
              paddingBottom: '0.2rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.4rem'
            })}
          >
            <Sparkles size={16} /> Criaturas
          </NavLink>

          <NavLink 
            to="/zones" 
            style={({ isActive }) => ({
              fontFamily: 'var(--font-heading)',
              fontSize: '0.95rem',
              color: isActive ? '#ebd180' : '#f4efe6',
              borderBottom: isActive ? '2px solid #c9a13b' : '2px solid transparent',
              paddingBottom: '0.2rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.4rem'
            })}
          >
            <ShieldAlert size={16} /> Zonas Hábitat
          </NavLink>
        </nav>

        {/* Action Buttons */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <Link to="/zones/new" className="btn-magical" style={{ fontSize: '0.85rem', padding: '0.45rem 0.9rem' }}>
            <PlusCircle size={15} /> + Zona
          </Link>
          <Link to="/creatures/new" className="btn-magical btn-magical-primary" style={{ fontSize: '0.85rem', padding: '0.45rem 0.9rem' }}>
            <Sparkles size={15} /> + Criatura
          </Link>
          <button 
            onClick={resetToDefaultMock}
            title="Restablecer datos simulados"
            style={{
              background: 'transparent',
              border: '1px solid rgba(201, 161, 59, 0.4)',
              color: '#b8ac97',
              borderRadius: '4px',
              padding: '0.45rem',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <RotateCcw size={15} />
          </button>
        </div>
      </div>
    </header>
  );
};

