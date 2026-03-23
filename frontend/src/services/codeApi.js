import axios from 'axios'

const API_BASE = '/api'

export const analyzeProject = async () => {
  const response = await axios.get(`${API_BASE}/project/analyze`)
  return response.data
}

export const applyCode = async (changes) => {
  await axios.post(`${API_BASE}/code/apply`, changes)
}
