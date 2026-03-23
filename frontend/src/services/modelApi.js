import axios from 'axios'

const API_BASE = '/api/models'

export const getOllamaModels = async () => {
  const response = await axios.get(`${API_BASE}/ollama`)
  return response.data
}
