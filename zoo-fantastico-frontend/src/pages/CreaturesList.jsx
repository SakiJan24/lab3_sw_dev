import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useZoo } from '../context/ZooContext';
import { Sparkles, PlusCircle, Search, Trash2, Edit3, ShieldAlert, HeartPulse } from 'lucide-react';

export const CreaturesList = () => {
  const { creatures, zones, deleteCreature } = useZoo();
  const [searchTerm, setSearchTerm] = useState('');
  const [filterHealth, setFilterHealth] = useState('ALL');
  const [filterZone, setFilterZone] = useState('ALL');

  const filteredCreatures = creatures.filter(c => {
    const matchesSearch = c.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          c.species.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesHealth = filterHealth === 'ALL' || c.healthStatus?.toLowerCase() === filterHealth.toLowerCase();
    const matchesZone = filterZone === 'ALL' || c.zoneId === Number(filterZone);
    return matchesSearch && matchesHealth && matchesZone;
  });

  const handleDelete = (id, name, healthStatus) => {
    if (healthStatus?.toLowerCase() === 'critical') {
      // The context will show notification and throw 409 error
      try {
        deleteCreature(id);
      } catch (err) {
        // Handled in context toast
      }
      return;
    }

    if (window.confirm(`¿Estás seguro de que deseas retirar a '${name}' del maletín?`)) {
      try {
        deleteCreature(id);
      } catch (err) {
        // Handled in context toast
      }
    }
  };

  return (
    <div className="container" style={{ paddingTop: '2rem', paddingBottom: '4rem' }}>
      {/* Page Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', marginBottom: '2rem' }}>
        <div>
          <h1 style={{ fontSize: '2rem', color: '#ebd180', display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <Sparkles size={28} color="#c9a13b" /> Catálogo de Criaturas Mágicas
          </h1>
          <p style={{ color: '#b8ac97', fontSize: '0.95rem' }}>
            Listado e inventario completo de especímenes bajo investigación en el maletín.
          </p>
        </div>

        <Link to="/creatures/new" className="btn-magical btn-magical-primary">
          <PlusCircle size={18} /> Registrar Nueva Criatura
        </Link>
      </div>

      {/* Filter and Search Bar */}
      <div className="magic-card" style={{ marginBottom: '2rem', padding: '1.25rem' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
          {/* Search Input */}
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label" style={{ fontSize: '0.85rem' }}>Buscar por Nombre / Especie</label>
            <div style={{ position: 'relative' }}>
              <input 
                type="text" 
                className="form-input" 
                placeholder="Ej. Niffler, Pickett..."
                value={searchTerm}
                onChange={e => setSearchTerm(e.target.value)}
                style={{ paddingLeft: '2.5rem' }}
              />
              <Search size={16} color="#c9a13b" style={{ position: 'absolute', left: '0.9rem', top: '50%', transform: 'translateY(-50%)' }} />
            </div>
          </div>

          {/* Filter by Health */}
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label" style={{ fontSize: '0.85rem' }}>Estado de Salud</label>
            <select className="form-select" value={filterHealth} onChange={e => setFilterHealth(e.target.value)}>
              <option value="ALL">Todos los Estados</option>
              <option value="healthy">Healthy (Saludable)</option>
              <option value="stable">Stable (Estable)</option>
              <option value="recovering">Recovering (En recuperación)</option>
              <option value="critical">Critical (Crítico - 409 Protegido)</option>
            </select>
          </div>

          {/* Filter by Zone */}
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label" style={{ fontSize: '0.85rem' }}>Zona / Hábitat</label>
            <select className="form-select" value={filterZone} onChange={e => setFilterZone(e.target.value)}>
              <option value="ALL">Todas las Zonas</option>
              {zones.map(z => (
                <option key={z.id} value={z.id}>{z.name}</option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Creatures Cards Grid */}
      {filteredCreatures.length === 0 ? (
        <div className="magic-card" style={{ textAlign: 'center', padding: '3rem 1rem' }}>
          <ShieldAlert size={48} color="#c9a13b" style={{ marginBottom: '1rem' }} />
          <h3 style={{ color: '#ebd180', marginBottom: '0.5rem' }}>No se encontraron criaturas mágicas</h3>
          <p style={{ color: '#b8ac97' }}>Intenta ajustar los criterios de búsqueda o registra una nueva criatura.</p>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.5rem' }}>
          {filteredCreatures.map(creature => {
            const zone = zones.find(z => z.id === creature.zoneId);
            const isCritical = creature.healthStatus?.toLowerCase() === 'critical';

            return (
              <div key={creature.id} className="magic-card" style={{
                borderColor: isCritical ? 'rgba(231, 76, 60, 0.5)' : undefined
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                  <div>
                    <h3 style={{ fontSize: '1.3rem', color: '#ebd180' }}>{creature.name}</h3>
                    <span style={{ fontSize: '0.88rem', color: '#b8ac97', fontStyle: 'italic' }}>
                      {creature.species}
                    </span>
                  </div>
                  <span className={`badge-status badge-${creature.healthStatus?.toLowerCase()}`}>
                    {creature.healthStatus}
                  </span>
                </div>

                <div style={{
                  background: 'rgba(7, 26, 20, 0.7)',
                  padding: '0.85rem',
                  borderRadius: '6px',
                  margin: '1rem 0',
                  border: '1px solid rgba(201, 161, 59, 0.2)',
                  fontSize: '0.9rem'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.4rem' }}>
                    <span style={{ color: '#b8ac97' }}>Tamaño (size):</span>
                    <span style={{ color: '#f4efe6', fontWeight: 600 }}>{creature.size} m</span>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.4rem' }}>
                    <span style={{ color: '#b8ac97' }}>Nivel de Peligro:</span>
                    <span style={{
                      color: creature.dangerLevel >= 8 ? '#e74c3c' : creature.dangerLevel >= 5 ? '#f39c12' : '#2ecc71',
                      fontWeight: 700
                    }}>
                      Nivel {creature.dangerLevel} / 10
                    </span>
                  </div>

                  {/* Danger Meter Bar */}
                  <div style={{ height: '6px', background: 'rgba(0,0,0,0.5)', borderRadius: '3px', marginBottom: '0.6rem', overflow: 'hidden' }}>
                    <div style={{
                      height: '100%',
                      width: `${(creature.dangerLevel / 10) * 100}%`,
                      background: creature.dangerLevel >= 8 ? 'linear-gradient(90deg, #f39c12, #e74c3c)' : 'linear-gradient(90deg, #27ae60, #c9a13b)'
                    }} />
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', paddingTop: '0.3rem', borderTop: '1px dashed rgba(201, 161, 59, 0.2)' }}>
                    <span style={{ color: '#b8ac97' }}>Hábitat / Zona:</span>
                    <span style={{ color: '#c9a13b', fontWeight: 600 }}>
                      {zone ? zone.name : 'Sin Zona Asignada'}
                    </span>
                  </div>
                </div>

                {isCritical && (
                  <div style={{
                    fontSize: '0.8rem',
                    color: '#f1948a',
                    background: 'rgba(192, 57, 43, 0.15)',
                    padding: '0.4rem 0.6rem',
                    borderRadius: '4px',
                    marginBottom: '1rem',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.4rem',
                    border: '1px solid rgba(231, 76, 60, 0.3)'
                  }}>
                    <HeartPulse size={14} color="#e74c3c" /> Regla de Negocio: No se permite eliminar criaturas en estado crítico (409).
                  </div>
                )}

                {/* Actions */}
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.6rem' }}>
                  <Link to={`/creatures/edit/${creature.id}`} className="btn-magical" style={{ fontSize: '0.85rem', padding: '0.4rem 0.8rem' }}>
                    <Edit3 size={14} /> Editar
                  </Link>

                  <button 
                    onClick={() => handleDelete(creature.id, creature.name, creature.healthStatus)}
                    className="btn-magical btn-magical-danger"
                    style={{ fontSize: '0.85rem', padding: '0.4rem 0.8rem' }}
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

