import axios from 'axios'

export const getAllPrompts = async () => {
  const response = await axios.get('/api/prompts')
  return response.data
}

export const getDefaultPrompt = async () => {
  const response = await axios.get('/api/prompts/default')
  return response.data
}
