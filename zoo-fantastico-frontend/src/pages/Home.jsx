import React from 'react';
import { Link } from 'react-router-dom';
import { useZoo } from '../context/ZooContext';
import { Sparkles, Shield, AlertTriangle, ArrowRight, HeartPulse, Layers, Compass } from 'lucide-react';

export const Home = () => {
  const { creatures, zones, getCreatureCountForZone } = useZoo();

  const totalCapacity = zones.reduce((sum, z) => sum + z.capacity, 0);
  const totalCreatures = creatures.length;
  const criticalCount = creatures.filter(c => c.healthStatus?.toLowerCase() === 'critical').length;
  const occupancyPercentage = totalCapacity > 0 ? Math.round((totalCreatures / totalCapacity) * 100) : 0;

  return (
    <div className="container" style={{ paddingBottom: '3rem' }}>
      {/* Hero Banner */}
      <section style={{
        marginTop: '2rem',
        padding: '3rem 2rem',
        borderRadius: '12px',
        background: 'radial-gradient(ellipse at top, rgba(13, 59, 46, 0.9) 0%, rgba(7, 23, 18, 0.95) 100%)',
        border: '1px solid rgba(201, 161, 59, 0.4)',
        boxShadow: '0 12px 40px rgba(0, 0, 0, 0.7), inset 0 0 30px rgba(201, 161, 59, 0.1)',
        textAlign: 'center',
        position: 'relative',
        overflow: 'hidden'
      }}>
        <div style={{ position: 'relative', zIndex: 2 }}>
          <span style={{
            fontFamily: 'var(--font-heading)',
            color: '#c9a13b',
            textTransform: 'uppercase',
            letterSpacing: '0.2em',
            fontSize: '0.85rem',
            background: 'rgba(59, 42, 26, 0.6)',
            padding: '0.3rem 1rem',
            borderRadius: '20px',
            border: '1px solid rgba(201, 161, 59, 0.4)',
            display: 'inline-block',
            marginBottom: '1rem'
          }}>
            <Sparkles size={14} style={{ display: 'inline', marginRight: '6px' }} />
            Expedición Newt Scamander &bull; Registro Mágico
          </span>

          <h1 style={{ fontSize: '2.5rem', marginBottom: '1rem', color: '#ebd180', lineHeight: 1.2 }}>
            Zoológico de Criaturas Mágicas & Hábitats Fantásticos
          </h1>

          <p style={{
            maxWidth: '750px',
            margin: '0 auto 2rem auto',
            fontSize: '1.1rem',
            color: '#f4efe6',
            lineHeight: 1.6
          }}>
            Bienvenido al catálogo digital de conservación de fauna mágica. Aquí podrás administrar los hábitats dimensionales, monitorear el estado de salud de especímenes raros y registrar nuevas criaturas fantásticas respetando la capacidad máxima de cada reserva.
          </p>

          <div style={{ display: 'flex', justifyContent: 'center', gap: '1rem', flexWrap: 'wrap' }}>
            <Link to="/creatures" className="btn-magical btn-magical-primary" style={{ padding: '0.75rem 1.75rem', fontSize: '1rem' }}>
              <Compass size={18} /> Explorar Criaturas ({totalCreatures})
            </Link>
            <Link to="/zones" className="btn-magical" style={{ padding: '0.75rem 1.75rem', fontSize: '1rem' }}>
              <Layers size={18} /> Ver Hábitats ({zones.length})
            </Link>
          </div>
        </div>
      </section>

      {/* Stats Counter Grid */}
      <section style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
        gap: '1.5rem',
        marginTop: '2.5rem'
      }}>
        {/* Total Creatures */}
        <div className="magic-card" style={{ textAlign: 'center' }}>
          <Sparkles size={32} color="#c9a13b" style={{ marginBottom: '0.5rem' }} />
          <h3 style={{ fontSize: '1rem', color: '#b8ac97', textTransform: 'uppercase', letterSpacing: '0.1em' }}>
            Criaturas Registradas
          </h3>
          <div style={{ fontSize: '2.8rem', fontFamily: 'var(--font-heading)', color: '#ebd180', fontWeight: 800 }}>
            {totalCreatures}
          </div>
          <p style={{ fontSize: '0.85rem', color: '#b8ac97', marginTop: '0.2rem' }}>Especímenes bajo custodia</p>
        </div>

        {/* Total Zones */}
        <div className="magic-card" style={{ textAlign: 'center' }}>
          <Shield size={32} color="#c9a13b" style={{ marginBottom: '0.5rem' }} />
          <h3 style={{ fontSize: '1rem', color: '#b8ac97', textTransform: 'uppercase', letterSpacing: '0.1em' }}>
            Zonas de Hábitat
          </h3>
          <div style={{ fontSize: '2.8rem', fontFamily: 'var(--font-heading)', color: '#ebd180', fontWeight: 800 }}>
            {zones.length}
          </div>
          <p style={{ fontSize: '0.85rem', color: '#b8ac97', marginTop: '0.2rem' }}>Reservas protegidas</p>
        </div>

        {/* Occupancy */}
        <div className="magic-card" style={{ textAlign: 'center' }}>
          <Layers size={32} color="#c9a13b" style={{ marginBottom: '0.5rem' }} />
          <h3 style={{ fontSize: '1rem', color: '#b8ac97', textTransform: 'uppercase', letterSpacing: '0.1em' }}>
            Ocupación del Maletín
          </h3>
          <div style={{ fontSize: '2.8rem', fontFamily: 'var(--font-heading)', color: occupancyPercentage > 80 ? '#f39c12' : '#ebd180', fontWeight: 800 }}>
            {occupancyPercentage}%
          </div>
          <p style={{ fontSize: '0.85rem', color: '#b8ac97', marginTop: '0.2rem' }}>
            {totalCreatures} de {totalCapacity} plazas ocupadas
          </p>
        </div>

        {/* Critical Health Alert */}
        <div className="magic-card" style={{
          textAlign: 'center',
          borderColor: criticalCount > 0 ? 'rgba(231, 76, 60, 0.6)' : undefined
        }}>
          <HeartPulse size={32} color={criticalCount > 0 ? '#e74c3c' : '#2ecc71'} style={{ marginBottom: '0.5rem' }} />
          <h3 style={{ fontSize: '1rem', color: '#b8ac97', textTransform: 'uppercase', letterSpacing: '0.1em' }}>
            Estado Crítico
          </h3>
          <div style={{ fontSize: '2.8rem', fontFamily: 'var(--font-heading)', color: criticalCount > 0 ? '#e74c3c' : '#2ecc71', fontWeight: 800 }}>
            {criticalCount}
          </div>
          <p style={{ fontSize: '0.85rem', color: criticalCount > 0 ? '#f1948a' : '#b8ac97', marginTop: '0.2rem' }}>
            {criticalCount > 0 ? 'Protegidas de eliminación (Regla 409)' : 'Todas saludables / estables'}
          </p>
        </div>
      </section>

      {/* Featured Preview Grids */}
      <div className="golden-divider" style={{ margin: '3rem 0' }} />

      {/* Zones Preview */}
      <section>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
          <div>
            <h2>Hábitats y Zonas Mágicas</h2>
            <p style={{ color: '#b8ac97', fontSize: '0.95rem' }}>Control de capacidad por zona antes de asignación</p>
          </div>
          <Link to="/zones" style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', fontSize: '0.95rem', fontWeight: 600 }}>
            Ver todas las zonas <ArrowRight size={16} />
          </Link>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.5rem' }}>
          {zones.slice(0, 3).map(zone => {
            const count = getCreatureCountForZone(zone.id);
            const isFull = count >= zone.capacity;
            const fillPct = Math.min(100, Math.round((count / zone.capacity) * 100));

            return (
              <div key={zone.id} className="magic-card">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
                  <h3 style={{ fontSize: '1.2rem', color: '#ebd180' }}>{zone.name}</h3>
                  <span className={`badge-status ${isFull ? 'badge-critical' : 'badge-healthy'}`}>
                    {count}/{zone.capacity}
                  </span>
                </div>
                <p style={{ fontSize: '0.9rem', color: '#f4efe6', marginBottom: '1.25rem', minHeight: '2.7em' }}>
                  {zone.description}
                </p>

                {/* Progress bar */}
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

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                  <Link to={`/zones/edit/${zone.id}`} className="btn-magical" style={{ fontSize: '0.8rem', padding: '0.35rem 0.75rem' }}>
                    Editar
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* Creatures Preview */}
      <section style={{ marginTop: '3.5rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
          <div>
            <h2>Ejemplares Fantásticos Destacados</h2>
            <p style={{ color: '#b8ac97', fontSize: '0.95rem' }}>Clasificación por nivel de peligro y estado de salud</p>
          </div>
          <Link to="/creatures" style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', fontSize: '0.95rem', fontWeight: 600 }}>
            Ver catálogo completo <ArrowRight size={16} />
          </Link>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.5rem' }}>
          {creatures.slice(0, 3).map(creature => {
            return (
              <div key={creature.id} className="magic-card">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                  <div>
                    <h3 style={{ fontSize: '1.25rem', color: '#ebd180' }}>{creature.name}</h3>
                    <span style={{ fontSize: '0.85rem', color: '#b8ac97', fontStyle: 'italic' }}>
                      {creature.species}
                    </span>
                  </div>
                  <span className={`badge-status badge-${creature.healthStatus?.toLowerCase()}`}>
                    {creature.healthStatus}
                  </span>
                </div>

                <div style={{
                  background: 'rgba(7, 26, 20, 0.6)',
                  padding: '0.75rem',
                  borderRadius: '6px',
                  margin: '1rem 0',
                  border: '1px solid rgba(201, 161, 59, 0.2)',
                  fontSize: '0.85rem'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.3rem' }}>
                    <span style={{ color: '#b8ac97' }}>Peligrosidad:</span>
                    <span style={{ color: '#ebd180', fontWeight: 700 }}>
                      Nivel {creature.dangerLevel} / 10
                    </span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.3rem' }}>
                    <span style={{ color: '#b8ac97' }}>Tamaño:</span>
                    <span style={{ color: '#f4efe6' }}>{creature.size} m</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: '#b8ac97' }}>Hábitat Asignado:</span>
                    <span style={{ color: '#c9a13b', fontWeight: 600 }}>
                      {creature.zoneName || 'Sin Asignar'}
                    </span>
                  </div>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                  <Link to={`/creatures/edit/${creature.id}`} className="btn-magical" style={{ fontSize: '0.8rem', padding: '0.35rem 0.75rem' }}>
                    Editar Ficha
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
};

