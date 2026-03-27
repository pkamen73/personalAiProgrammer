import axios from 'axios'

const API_BASE = '/api/project'

export const openProject = async (path) => {
  const response = await axios.post(`${API_BASE}/open`, null, { params: { path } })
  return response.data
}

export const getProjectTree = async (path = null) => {
  const response = await axios.get(`${API_BASE}/tree`, {
    params: path ? { path } : {}
  })
  return response.data
}

export const createDirectory = async (path) => {
  await axios.post(`${API_BASE}/directory`, null, {
    params: { path }
  })
}

export const deleteItem = async (path) => {
  await axios.delete(API_BASE, {
    params: { path }
  })
}

export const getWorkspaceRoot = async () => {
  const response = await axios.get(`${API_BASE}/root`)
  return response.data.path
}

export const createFile = async (path) => {
  await axios.post(`${API_BASE}/file`, null, { params: { path } })
}

export const renameItem = async (path, newName) => {
  await axios.post(`${API_BASE}/rename`, null, { params: { path, newName } })
}

export const moveItem = async (source, dest) => {
  await axios.post(`${API_BASE}/move`, null, { params: { source, dest } })
}

export const copyItem = async (source, dest) => {
  await axios.post(`${API_BASE}/copy`, null, { params: { source, dest } })
}

export const analyzeFull = async (ollamaModel) => {
  const params = ollamaModel ? { ollamaModel } : {}
  const response = await axios.post(`${API_BASE}/analyze-full/sync`, null, { params })
  return response.data
}
