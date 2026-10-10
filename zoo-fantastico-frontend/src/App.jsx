import React from 'react';
import { Routes, Route } from 'react-router-dom';
import { Navbar } from './components/Navbar';
import { Footer } from './components/Footer';
import { NotificationBanner } from './components/NotificationBanner';
import { useZoo } from './context/ZooContext';
import { Home } from './pages/Home';
import { CreaturesList } from './pages/CreaturesList';
import { CreatureForm } from './pages/CreatureForm';
import { ZonesList } from './pages/ZonesList';
import { ZoneForm } from './pages/ZoneForm';

export const App = () => {
  const { loading, error, zones, creatures } = useZoo();
  const initialLoad = loading && zones.length === 0 && creatures.length === 0;

  return (
    <>
      <Navbar />
      <main>
        {initialLoad ? (
          <div className="container" style={{ padding: '4rem 0', textAlign: 'center' }}>
            <p style={{ fontFamily: 'var(--font-heading)', color: '#ebd180' }}>
              Invocando el registro mágico desde el Ministerio...
            </p>
          </div>
        ) : (
          <>
            {error && (
              <div className="container" style={{ marginTop: '1.5rem' }}>
                <div className="magic-card" style={{ borderColor: 'rgba(231, 76, 60, 0.6)' }}>
                  <p style={{ color: '#f1948a' }}>
                    No se pudo conectar completamente con el backend: {error}
                  </p>
                </div>
              </div>
            )}

            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/creatures" element={<CreaturesList />} />
              <Route path="/creatures/new" element={<CreatureForm />} />
              <Route path="/creatures/edit/:id" element={<CreatureForm />} />
              <Route path="/zones" element={<ZonesList />} />
              <Route path="/zones/new" element={<ZoneForm />} />
              <Route path="/zones/edit/:id" element={<ZoneForm />} />
            </Routes>
          </>
        )}
      </main>
      <Footer />
      <NotificationBanner />
    </>
  );
};
