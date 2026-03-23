import axios from 'axios'

const API_BASE = '/api/project'

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
