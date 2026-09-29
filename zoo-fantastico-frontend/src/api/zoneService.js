import api from './axios';

const BASE_PATH = '/zones';

export const getAllZones = async () => {
  const { data } = await api.get(BASE_PATH);
  return data;
};

export const getZoneById = async (id) => {
  const { data } = await api.get(`${BASE_PATH}/${id}`);
  return data;
};

export const createZone = async (zoneRequest) => {
  const { data } = await api.post(BASE_PATH, zoneRequest);
  return data;
};

export const updateZone = async (id, zoneRequest) => {
  const { data } = await api.put(`${BASE_PATH}/${id}`, zoneRequest);
  return data;
};

export const deleteZone = async (id) => {
  await api.delete(`${BASE_PATH}/${id}`);
};

export default {
  getAllZones,
  getZoneById,
  createZone,
  updateZone,
  deleteZone
};
