import axios from 'axios'

const API_BASE = '/api/models/configs'

export const getAllModelConfigs = async () => {
  const response = await axios.get(API_BASE)
  return response.data
}

export const getModelConfig = async (id) => {
  const response = await axios.get(`${API_BASE}/${id}`)
  return response.data
}

export const getDefaultModelConfig = async () => {
  const response = await axios.get(`${API_BASE}/default`)
  return response.data
}

export const createModelConfig = async (config) => {
  const response = await axios.post(API_BASE, config)
  return response.data
}

export const updateModelConfig = async (id, config) => {
  const response = await axios.put(`${API_BASE}/${id}`, config)
  return response.data
}

export const deleteModelConfig = async (id) => {
  await axios.delete(`${API_BASE}/${id}`)
}

export const setDefaultModelConfig = async (id) => {
  const response = await axios.post(`${API_BASE}/${id}/set-default`)
  return response.data
}
