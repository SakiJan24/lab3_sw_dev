import React, { createContext, useContext, useState, useEffect } from 'react';
import { INITIAL_ZONES, INITIAL_CREATURES } from '../mock/data';

const ZooContext = createContext();

export const ZooProvider = ({ children }) => {
  const [zones, setZones] = useState(() => {
    const saved = localStorage.getItem('zoo_zones');
    return saved ? JSON.parse(saved) : INITIAL_ZONES;
  });

  const [creatures, setCreatures] = useState(() => {
    const saved = localStorage.getItem('zoo_creatures');
    return saved ? JSON.parse(saved) : INITIAL_CREATURES;
  });

  const [notification, setNotification] = useState(null);

  useEffect(() => {
    localStorage.setItem('zoo_zones', JSON.stringify(zones));
  }, [zones]);

  useEffect(() => {
    localStorage.setItem('zoo_creatures', JSON.stringify(creatures));
  }, [creatures]);

  const showNotification = (message, type = 'info') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 4500);
  };

  // Helper to count creatures per zone
  const getCreatureCountForZone = (zoneId) => {
    return creatures.filter(c => c.zoneId === Number(zoneId)).length;
  };

  // Zone Operations
  const addZone = (zoneData) => {
    const newZone = {
      ...zoneData,
      id: Date.now(),
      capacity: Number(zoneData.capacity)
    };
    setZones(prev => [...prev, newZone]);
    showNotification(`Zona '${newZone.name}' creada exitosamente con éxito en el registro.`, 'success');
    return newZone;
  };

  const updateZone = (id, zoneData) => {
    const numericId = Number(id);
    const currentCount = getCreatureCountForZone(numericId);
    if (Number(zoneData.capacity) < currentCount) {
      const errorMsg = `No se puede reducir la capacidad a ${zoneData.capacity} porque la zona alberga actualmente ${currentCount} criatura(s).`;
      showNotification(errorMsg, 'error');
      throw new Error(errorMsg);
    }

    setZones(prev => prev.map(z => z.id === numericId ? { ...z, ...zoneData, capacity: Number(zoneData.capacity) } : z));
    showNotification(`Zona '${zoneData.name}' actualizada correctamente.`, 'success');
  };

  const deleteZone = (id) => {
    const numericId = Number(id);
    const targetZone = zones.find(z => z.id === numericId);
    const creatureCount = getCreatureCountForZone(numericId);

    if (creatureCount > 0) {
      const errorMsg = `Acción Denegada: No se puede eliminar la zona '${targetZone?.name}' porque contiene ${creatureCount} criatura(s) habitando en ella.`;
      showNotification(errorMsg, 'error');
      throw new Error(errorMsg);
    }

    setZones(prev => prev.filter(z => z.id !== numericId));
    showNotification(`Zona '${targetZone?.name}' clausurada y eliminada del catálogo.`, 'info');
  };

  // Creature Operations
  const addCreature = (creatureData) => {
    const targetZoneId = Number(creatureData.zoneId);
    const targetZone = zones.find(z => z.id === targetZoneId);

    if (targetZone) {
      const currentCount = getCreatureCountForZone(targetZoneId);
      if (currentCount >= targetZone.capacity) {
        const errorMsg = `Capacidad Superada: La zona '${targetZone.name}' ha alcanzado su límite máximo de ${targetZone.capacity} criaturas.`;
        showNotification(errorMsg, 'error');
        throw new Error(errorMsg);
      }
    }

    const newCreature = {
      ...creatureData,
      id: Date.now(),
      size: parseFloat(creatureData.size),
      dangerLevel: parseInt(creatureData.dangerLevel, 10),
      zoneId: targetZoneId
    };

    setCreatures(prev => [...prev, newCreature]);
    showNotification(`La criatura '${newCreature.name}' (${newCreature.species}) ha sido registrada en el maletín.`, 'success');
    return newCreature;
  };

  const updateCreature = (id, creatureData) => {
    const numericId = Number(id);
    const targetZoneId = Number(creatureData.zoneId);
    const existingCreature = creatures.find(c => c.id === numericId);

    // If changing zone, check capacity of new zone
    if (existingCreature && existingCreature.zoneId !== targetZoneId) {
      const newZone = zones.find(z => z.id === targetZoneId);
      if (newZone) {
        const currentCountInNewZone = getCreatureCountForZone(targetZoneId);
        if (currentCountInNewZone >= newZone.capacity) {
          const errorMsg = `No se puede reubicar a '${creatureData.name}': La zona '${newZone.name}' está llena (${newZone.capacity}/${newZone.capacity}).`;
          showNotification(errorMsg, 'error');
          throw new Error(errorMsg);
        }
      }
    }

    const updated = {
      ...creatureData,
      id: numericId,
      size: parseFloat(creatureData.size),
      dangerLevel: parseInt(creatureData.dangerLevel, 10),
      zoneId: targetZoneId
    };

    setCreatures(prev => prev.map(c => c.id === numericId ? updated : c));
    showNotification(`Ficha mágica de '${updated.name}' actualizada.`, 'success');
  };

  const deleteCreature = (id) => {
    const numericId = Number(id);
    const target = creatures.find(c => c.id === numericId);

    if (target && target.healthStatus?.toLowerCase() === 'critical') {
      const errorMsg = `Hechizo de Protección Activo: No se permite retirar del registro a '${target.name}' mientras su estado de salud sea 'CRÍTICO'.`;
      showNotification(errorMsg, 'error');
      throw new Error(errorMsg);
    }

    setCreatures(prev => prev.filter(c => c.id !== numericId));
    showNotification(`Criatura '${target?.name}' retirada del catálogo de investigación.`, 'info');
  };

  const resetToDefaultMock = () => {
    setZones(INITIAL_ZONES);
    setCreatures(INITIAL_CREATURES);
    localStorage.removeItem('zoo_zones');
    localStorage.removeItem('zoo_creatures');
    showNotification('Base de datos mágica restablecida a los valores iniciales.', 'info');
  };

  return (
    <ZooContext.Provider value={{
      zones,
      creatures,
      notification,
      showNotification,
      getCreatureCountForZone,
      addZone,
      updateZone,
      deleteZone,
      addCreature,
      updateCreature,
      deleteCreature,
      resetToDefaultMock
    }}>
      {children}
    </ZooContext.Provider>
  );
};

export const useZoo = () => useContext(ZooContext);

