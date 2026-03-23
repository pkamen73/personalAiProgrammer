import axios from 'axios'

const API_BASE = '/api/file'

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
