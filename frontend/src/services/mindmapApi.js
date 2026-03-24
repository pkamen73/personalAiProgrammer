import axios from 'axios'

export const uploadImage = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  
  const response = await axios.post('/api/images/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return response.data
}

export const analyzeMindmap = async (imageId, modelType = 'local', promptId = null) => {
  const response = await axios.post('/api/mindmap/analyze', { 
    imageId, 
    modelType,
    promptId: promptId ? String(promptId) : null
  })
  return response.data
}

export const generateDiagram = async (imageId, analysisText, baseName) => {
  const response = await axios.post('/api/mindmap/generate-diagram', { 
    imageId, 
    analysisText,
    baseName
  })
  return response.data
}

export const getAnalyses = async () => {
  const response = await axios.get('/api/mindmap/analyses')
  return response.data
}

export const loadAnalysis = async (filename) => {
  const response = await axios.get(`/api/mindmap/analysis/${filename}`)
  return response.data
}

export const previewDiagram = async (analysisText) => {
  const response = await axios.post('/api/mindmap/preview-diagram', { analysisText })
  return response.data
}

export const deleteAnalysis = async (filename) => {
  await axios.delete(`/api/mindmap/analysis/${filename}`)
}
