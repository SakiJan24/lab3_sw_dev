import React from 'react';
import { Link } from 'react-router-dom';
import { useZoo } from '../context/ZooContext';
import { Shield, PlusCircle, Edit3, Trash2, ShieldAlert } from 'lucide-react';

export const ZonesList = () => {
  const { zones, getCreatureCountForZone, deleteZone } = useZoo();

  const handleDelete = async (zone, count) => {
    if (count > 0) return;
    if (window.confirm(`¿Deseas clausurar la zona '${zone.name}'?`)) {
      await deleteZone(zone.id);
    }
  };

  return (
    <div className="container" style={{ paddingTop: '2rem', paddingBottom: '4rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', marginBottom: '2rem' }}>
        <div>
          <h1 style={{ fontSize: '2rem', color: '#ebd180', display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <Shield size={28} color="#c9a13b" /> Zonas de Hábitat Mágico
          </h1>
          <p style={{ color: '#b8ac97', fontSize: '0.95rem' }}>
            Reservas dimensionales disponibles para el resguardo de criaturas fantásticas.
          </p>
        </div>

        <Link to="/zones/new" className="btn-magical btn-magical-primary">
          <PlusCircle size={18} /> Nueva Zona
        </Link>
      </div>

      {zones.length === 0 ? (
        <div className="magic-card" style={{ textAlign: 'center', padding: '3rem 1rem' }}>
          <ShieldAlert size={48} color="#c9a13b" style={{ marginBottom: '1rem' }} />
          <h3 style={{ color: '#ebd180', marginBottom: '0.5rem' }}>No hay zonas registradas</h3>
          <p style={{ color: '#b8ac97' }}>Crea la primera reserva dimensional para comenzar a alojar criaturas.</p>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.5rem' }}>
          {zones.map(zone => {
            const count = getCreatureCountForZone(zone.id);
            const isFull = count >= zone.capacity;
            const fillPct = zone.capacity > 0 ? Math.min(100, Math.round((count / zone.capacity) * 100)) : 0;
            const hasCreatures = count > 0;

            return (
              <div key={zone.id} className="magic-card">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
                  <h3 style={{ fontSize: '1.2rem', color: '#ebd180' }}>{zone.name}</h3>
                  <span className={`badge-status ${isFull ? 'badge-critical' : 'badge-healthy'}`}>
                    {count}/{zone.capacity}
                  </span>
                </div>

                <p style={{ fontSize: '0.9rem', color: '#f4efe6', marginBottom: '1.25rem', minHeight: '2.7em' }}>
                  {zone.description || 'Sin descripción registrada.'}
                </p>

                <div style={{ marginBottom: '1rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: '#b8ac97', marginBottom: '0.3rem' }}>
                    <span>Capacidad Ocupada</span>
                    <span>{fillPct}%</span>
                  </div>
                  <div style={{ height: '8px', background: 'rgba(0,0,0,0.5)', borderRadius: '4px', overflow: 'hidden', border: '1px solid rgba(201, 161, 59, 0.3)' }}>
                    <div style={{
                      height: '100%',
                      width: `${fillPct}%`,
                      background: isFull ? 'linear-gradient(90deg, #e74c3c, #c0392b)' : 'linear-gradient(90deg, #c9a13b, #ebd180)',
                      transition: 'width 0.3s ease'
                    }} />
                  </div>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.6rem' }}>
                  <Link to={`/zones/edit/${zone.id}`} className="btn-magical" style={{ fontSize: '0.85rem', padding: '0.4rem 0.8rem' }}>
                    <Edit3 size={14} /> Editar
                  </Link>

                  <button
                    onClick={() => handleDelete(zone, count)}
                    disabled={hasCreatures}
                    title={hasCreatures ? `No se puede eliminar: la zona alberga ${count} criatura(s).` : 'Eliminar zona'}
                    className="btn-magical btn-magical-danger"
                    style={{
                      fontSize: '0.85rem',
                      padding: '0.4rem 0.8rem',
                      opacity: hasCreatures ? 0.45 : 1,
                      cursor: hasCreatures ? 'not-allowed' : 'pointer'
                    }}
                  >
                    <Trash2 size={14} /> Eliminar
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
