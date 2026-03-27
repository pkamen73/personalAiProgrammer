import axios from 'axios'

const API_BASE = '/api/file'

const IMAGE_EXTENSIONS = new Set(['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp', 'bmp', 'ico'])

export const isImagePath = (path) => {
  if (!path) return false
  const ext = path.split('.').pop().toLowerCase()
  return IMAGE_EXTENSIONS.has(ext)
}

export const getImageUrl = (path) => `${API_BASE}/image?path=${encodeURIComponent(path)}`

export const getFile = async (path) => {
  const response = await axios.get(API_BASE, {
    params: { path }
  })
  return response.data
}

export const saveFile = async (fileContent) => {
  await axios.post(API_BASE, fileContent)
}

export const deleteFile = async (path) => {
  await axios.delete(API_BASE, {
    params: { path }
  })
}
