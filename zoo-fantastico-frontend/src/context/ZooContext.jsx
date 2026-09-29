import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import * as creatureService from '../api/creatureService';
import * as zoneService from '../api/zoneService';

const ZooContext = createContext();

const getBackendErrorMessage = (err, fallbackMessage) => {
  const data = err?.response?.data;
  if (data) {
    if (data.details && Object.keys(data.details).length > 0) {
      return Object.values(data.details).join(' | ');
    }
    if (data.message) return data.message;
  }
  return err?.message || fallbackMessage;
};

export const ZooProvider = ({ children }) => {
  const [zones, setZones] = useState([]);
  const [creatures, setCreatures] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notification, setNotification] = useState(null);

  const showNotification = (message, type = 'info', status = null) => {
    setNotification({ message, type, status });
    setTimeout(() => setNotification(null), 4500);
  };

  const notifyApiError = (err, fallbackMessage) => {
    const status = err?.response?.status;
    const message = getBackendErrorMessage(err, fallbackMessage);
    showNotification(message, 'error', status);
  };

  const loadZoo = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [zonesData, creaturesData] = await Promise.all([
        zoneService.getAllZones(),
        creatureService.getAllCreatures()
      ]);
      setZones(zonesData);
      setCreatures(creaturesData);
    } catch (err) {
      const message = getBackendErrorMessage(err, 'No se pudo conectar con el Ministerio de Magia (backend).');
      setError(message);
      showNotification(message, 'error');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadZoo();
  }, [loadZoo]);

  // Helper to count creatures per zone
  const getCreatureCountForZone = (zoneId) => {
    return creatures.filter(c => c.zoneId === Number(zoneId)).length;
  };

  // Zone Operations
  const addZone = async (zoneData) => {
    try {
      const payload = { ...zoneData, capacity: Number(zoneData.capacity) };
      const newZone = await zoneService.createZone(payload);
      setZones(prev => [...prev, newZone]);
      showNotification(`Zona '${newZone.name}' creada exitosamente en el registro.`, 'success');
      return newZone;
    } catch (err) {
      notifyApiError(err, 'No se pudo crear la zona.');
      return null;
    }
  };

  const updateZone = async (id, zoneData) => {
    try {
      const payload = { ...zoneData, capacity: Number(zoneData.capacity) };
      const updatedZone = await zoneService.updateZone(id, payload);
      setZones(prev => prev.map(z => z.id === updatedZone.id ? updatedZone : z));
      showNotification(`Zona '${updatedZone.name}' actualizada correctamente.`, 'success');
      return updatedZone;
    } catch (err) {
      notifyApiError(err, 'No se pudo actualizar la zona.');
      return null;
    }
  };

  const deleteZone = async (id) => {
    const numericId = Number(id);
    const targetZone = zones.find(z => z.id === numericId);
    try {
      await zoneService.deleteZone(numericId);
      setZones(prev => prev.filter(z => z.id !== numericId));
      showNotification(`Zona '${targetZone?.name ?? numericId}' clausurada y eliminada del catálogo.`, 'info');
      return true;
    } catch (err) {
      notifyApiError(err, `No se pudo eliminar la zona '${targetZone?.name ?? numericId}'.`);
      return false;
    }
  };

  // Creature Operations
  const addCreature = async (creatureData) => {
    try {
      const payload = {
        ...creatureData,
        size: parseFloat(creatureData.size),
        dangerLevel: parseInt(creatureData.dangerLevel, 10),
        zoneId: creatureData.zoneId !== '' && creatureData.zoneId != null ? Number(creatureData.zoneId) : null
      };
      const newCreature = await creatureService.createCreature(payload);
      setCreatures(prev => [...prev, newCreature]);
      showNotification(`La criatura '${newCreature.name}' (${newCreature.species}) ha sido registrada en el maletín.`, 'success');
      return newCreature;
    } catch (err) {
      notifyApiError(err, 'No se pudo registrar la criatura.');
      return null;
    }
  };

  const updateCreature = async (id, creatureData) => {
    try {
      const payload = {
        ...creatureData,
        size: parseFloat(creatureData.size),
        dangerLevel: parseInt(creatureData.dangerLevel, 10),
        zoneId: creatureData.zoneId !== '' && creatureData.zoneId != null ? Number(creatureData.zoneId) : null
      };
      const updatedCreature = await creatureService.updateCreature(id, payload);
      setCreatures(prev => prev.map(c => c.id === updatedCreature.id ? updatedCreature : c));
      showNotification(`Ficha mágica de '${updatedCreature.name}' actualizada.`, 'success');
      return updatedCreature;
    } catch (err) {
      notifyApiError(err, 'No se pudo actualizar la ficha de la criatura.');
      return null;
    }
  };

  const deleteCreature = async (id) => {
    const numericId = Number(id);
    const target = creatures.find(c => c.id === numericId);
    try {
      await creatureService.deleteCreature(numericId);
      setCreatures(prev => prev.filter(c => c.id !== numericId));
      showNotification(`Criatura '${target?.name ?? numericId}' retirada del catálogo de investigación.`, 'info');
      return true;
    } catch (err) {
      notifyApiError(err, `No se pudo retirar a '${target?.name ?? numericId}' del catálogo.`);
      return false;
    }
  };

  const resetToDefaultMock = () => {
    loadZoo();
    showNotification('Registro recargado desde el Ministerio de Magia (backend).', 'info');
  };

  return (
    <ZooContext.Provider value={{
      zones,
      creatures,
      loading,
      error,
      notification,
      showNotification,
      getCreatureCountForZone,
      addZone,
      updateZone,
      deleteZone,
      addCreature,
      updateCreature,
      deleteCreature,
      resetToDefaultMock,
      refreshZoo: loadZoo
    }}>
      {children}
    </ZooContext.Provider>
  );
};

export const useZoo = () => useContext(ZooContext);
