import React, { useState, useEffect } from 'react'
import { getAllModelConfigs, createModelConfig, updateModelConfig, deleteModelConfig, setDefaultModelConfig } from '../services/modelConfigApi'
import './ModelConfigManager.css'

const ModelConfigManager = ({ isOpen, onClose, onConfigsUpdated }) => {
  const [configs, setConfigs] = useState([])
  const [editingConfig, setEditingConfig] = useState(null)
  const [formData, setFormData] = useState({
    displayName: '',
    provider: 'openrouter',
    modelName: '',
    apiKey: '',
    type: 'CLOUD'
  })

  useEffect(() => {
    if (isOpen) {
      loadConfigs()
    }
  }, [isOpen])

  const loadConfigs = async () => {
    try {
      const data = await getAllModelConfigs()
      setConfigs(data)
    } catch (error) {
      console.error('Failed to load configs:', error)
    }
  }

  const findApiKeyForProvider = (provider) => {
    const existingConfig = configs.find(c => c.provider === provider && c.apiKey)
    return existingConfig ? existingConfig.apiKey : ''
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    console.log('Form submitted:', formData)
    try {
      if (editingConfig) {
        console.log('Updating config:', editingConfig.id)
        await updateModelConfig(editingConfig.id, formData)
      } else {
        console.log('Creating new config')
        const result = await createModelConfig(formData)
        console.log('Created:', result)
      }
      await loadConfigs()
      onConfigsUpdated && onConfigsUpdated()
      resetForm()
      alert('✅ Configuration saved')
    } catch (error) {
      console.error('Failed to save config:', error)
      alert('❌ Failed to save: ' + error.message)
    }
  }

  const handleEdit = (config) => {
    setEditingConfig(config)
    setFormData({
      displayName: config.displayName,
      provider: config.provider,
      modelName: config.modelName,
      apiKey: config.apiKey || '',
      type: config.type
    })
  }

  const handleDelete = async (id) => {
    if (confirm('Delete this model configuration?')) {
      try {
        await deleteModelConfig(id)
        await loadConfigs()
        onConfigsUpdated && onConfigsUpdated()
      } catch (error) {
        console.error('Failed to delete config:', error)
      }
    }
  }

  const handleSetDefault = async (id) => {
    try {
      await setDefaultModelConfig(id)
      await loadConfigs()
      onConfigsUpdated && onConfigsUpdated()
    } catch (error) {
      console.error('Failed to set default:', error)
    }
  }

  const resetForm = () => {
    setEditingConfig(null)
    setFormData({
      displayName: '',
      provider: 'openrouter',
      modelName: '',
      apiKey: '',
      type: 'CLOUD'
    })
  }

  if (!isOpen) return null

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Model Configurations</h2>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>

        <div className="modal-body">
          <div className="config-form">
            <h3>{editingConfig ? 'Edit Model' : 'Add Model'}</h3>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Display Name</label>
                <input
                  type="text"
                  value={formData.displayName}
                  onChange={(e) => setFormData({...formData, displayName: e.target.value})}
                  placeholder="e.g., Claude Sonnet 4.6"
                  required
                />
              </div>

              <div className="form-group">
                <label>Provider</label>
                <select
                  value={formData.provider}
                  onChange={(e) => {
                    const newProvider = e.target.value
                    const existingApiKey = findApiKeyForProvider(newProvider)
                    setFormData({
                      ...formData, 
                      provider: newProvider,
                      apiKey: existingApiKey || formData.apiKey
                    })
                  }}
                >
                  <option value="openrouter">OpenRouter.ai</option>
                  <option value="openai">OpenAI</option>
                  <option value="anthropic">Anthropic</option>
                  <option value="ollama">Ollama (Local)</option>
                </select>
              </div>

              <div className="form-group">
                <label>Model Name</label>
                <input
                  type="text"
                  value={formData.modelName}
                  onChange={(e) => setFormData({...formData, modelName: e.target.value})}
                  placeholder="e.g., anthropic/claude-sonnet-4.6"
                  required
                />
              </div>

              {formData.provider !== 'ollama' && (
                <div className="form-group">
                  <label>API Key {findApiKeyForProvider(formData.provider) && '(using shared key)'}</label>
                  <input
                    type="password"
                    value={formData.apiKey}
                    onChange={(e) => setFormData({...formData, apiKey: e.target.value})}
                    placeholder={findApiKeyForProvider(formData.provider) ? 'Leave empty to use existing key' : 'API key for cloud provider'}
                  />
                </div>
              )}

              <div className="form-actions">
                <button type="submit" className="btn-primary">
                  {editingConfig ? 'Update' : 'Add'}
                </button>
                {editingConfig && (
                  <button type="button" className="btn-secondary" onClick={resetForm}>
                    Cancel
                  </button>
                )}
              </div>
            </form>
          </div>

          <div className="config-list">
            <h3>Saved Configurations</h3>
            {configs.length === 0 ? (
              <div className="empty-state">No model configurations yet</div>
            ) : (
              <div className="config-items">
                {configs.map(config => (
                  <div key={config.id} className={`config-item ${config.isDefault ? 'is-default' : ''}`}>
                    <div className="config-info">
                      <div className="config-name">
                        {config.displayName}
                        {config.isDefault && <span className="default-badge">DEFAULT</span>}
                      </div>
                      <div className="config-details">
                        {config.provider} / {config.modelName}
                      </div>
                    </div>
                    <div className="config-actions">
                      {!config.isDefault && (
                        <button 
                          className="btn-icon" 
                          onClick={() => handleSetDefault(config.id)}
                          title="Set as default"
                        >
                          ⭐
                        </button>
                      )}
                      <button 
                        className="btn-icon" 
                        onClick={() => handleEdit(config)}
                        title="Edit"
                      >
                        ✏️
                      </button>
                      <button 
                        className="btn-icon btn-danger" 
                        onClick={() => handleDelete(config.id)}
                        title="Delete"
                      >
                        🗑️
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default ModelConfigManager
