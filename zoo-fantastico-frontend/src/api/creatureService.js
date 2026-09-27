import api from './axios';

const BASE_PATH = '/creatures';

export const getAllCreatures = async () => {
  const { data } = await api.get(BASE_PATH);
  return data;
};

export const getCreatureById = async (id) => {
  const { data } = await api.get(`${BASE_PATH}/${id}`);
  return data;
};

export const createCreature = async (creatureRequest) => {
  const { data } = await api.post(BASE_PATH, creatureRequest);
  return data;
};

export const updateCreature = async (id, creatureRequest) => {
  const { data } = await api.put(`${BASE_PATH}/${id}`, creatureRequest);
  return data;
};

export const deleteCreature = async (id) => {
  await api.delete(`${BASE_PATH}/${id}`);
};

export default {
  getAllCreatures,
  getCreatureById,
  createCreature,
  updateCreature,
  deleteCreature
};
