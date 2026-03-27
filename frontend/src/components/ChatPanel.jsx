import React, { useState, useEffect, useRef } from 'react'
import { connectChat, sendMessage, setProgressCallback } from '../services/chatApi'
import { getOllamaModels } from '../services/modelApi'
import { getAllModelConfigs } from '../services/modelConfigApi'
import { getFile } from '../services/fileApi'
import { analyzeFull } from '../services/projectApi'
import ModelConfigManager from './ModelConfigManager'
import MessageContent from './MessageContent'
import MindmapModal from './MindmapModal'
import './ChatPanel.css'

const ChatPanel = ({ onFileOpen, onRefreshTree }) => {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [connected, setConnected] = useState(false)
  const [selectedConfigId, setSelectedConfigId] = useState(null)
  const [ollamaModels, setOllamaModels] = useState([])
  const [cloudConfigs, setCloudConfigs] = useState([])
  const [attachedFiles, setAttachedFiles] = useState([])
  const [showConfigManager, setShowConfigManager] = useState(false)
  const [showMindmapModal, setShowMindmapModal] = useState(false)
  const [analyzing, setAnalyzing] = useState(false)
  const [analysisResult, setAnalysisResult] = useState(null)
  const [analysisProgress, setAnalysisProgress] = useState(null)
  const messagesEndRef = useRef(null)

  useEffect(() => {
    loadOllamaModels()
    loadCloudConfigs()

    
    const client = connectChat((message) => {
      if (message.role === 'ASSISTANT') {
        setMessages(prev => {
          const lastMsg = prev[prev.length - 1]
          if (lastMsg && lastMsg.role === 'ASSISTANT' && lastMsg.id === message.id) {
            return [...prev.slice(0, -1), {
              ...lastMsg,
              content: lastMsg.content + message.content
            }]
          }
          return [...prev, message]
        })
      } else {
        setMessages(prev => [...prev, message])
      }
      setConnected(true)
    })

    setProgressCallback((p) => {
      setAnalysisProgress(p)
      if (p.stage === 'COMPLETE') {
        setAnalyzing(false)
        if (onRefreshTree) onRefreshTree()
      }
      if (p.stage === 'ERROR') {
        setAnalyzing(false)
        setAnalysisResult({ ok: false, error: p.message })
        setAnalysisProgress(null)
      }
    })

    return () => {
      if (client) client.deactivate()
      setProgressCallback(null)
    }
  }, [])
  
  const loadOllamaModels = async () => {
    try {
      const models = await getOllamaModels()
      setOllamaModels(models)
      
      if (!selectedConfigId && models.length > 0) {
        setSelectedConfigId(`ollama-${models[0]}`)
      }
    } catch (error) {
      console.error('Failed to load Ollama models:', error)
    }
  }

  const loadCloudConfigs = async () => {
    try {
      const configs = await getAllModelConfigs()
      setCloudConfigs(configs)
      
      const defaultConfig = configs.find(c => c.isDefault)
      if (defaultConfig && !selectedConfigId) {
        setSelectedConfigId(defaultConfig.id)
      } else if (configs.length > 0 && !selectedConfigId && ollamaModels.length === 0) {
        setSelectedConfigId(configs[0].id)
      }
    } catch (error) {
      console.error('Failed to load cloud configs:', error)
    }
  }

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const [isDraggingOver, setIsDraggingOver] = useState(false)

  const handleDrop = async (e) => {
    e.preventDefault()
    setIsDraggingOver(false)
    const filePath = e.dataTransfer.getData('filePath')
    
    if (filePath) {
      try {
        const fileData = await getFile(filePath)
        setAttachedFiles(prev => [...prev, fileData])
      } catch (error) {
        console.error('Failed to load file:', error)
      }
    }
  }

  const handleDragOver = (e) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'copy'
  }

  const handleDragEnter = (e) => {
    e.preventDefault()
    setIsDraggingOver(true)
  }

  const handleDragLeave = (e) => {
    e.preventDefault()
    if (e.currentTarget === e.target) {
      setIsDraggingOver(false)
    }
  }

  const removeAttachedFile = (path) => {
    setAttachedFiles(prev => prev.filter(f => f.path !== path))
  }

  const handleSend = async () => {
    if (!input.trim()) return

    const userMessage = {
      role: 'USER',
      content: input,
      timestamp: new Date().toISOString(),
      fileContext: attachedFiles.map(f => ({
        path: f.path,
        content: f.content,
        language: f.language
      }))
    }

    setMessages(prev => [...prev, userMessage])
    setInput('')
    setAttachedFiles([])

    try {
      const isOllama = typeof selectedConfigId === 'string' && selectedConfigId.startsWith('ollama-')

      const history = messages
        .filter(m => m.role === 'USER' || m.role === 'ASSISTANT')
        .map(m => ({ role: m.role.toLowerCase(), content: m.content }))

      await sendMessage({
        message: userMessage,
        modelConfigId: isOllama ? null : selectedConfigId,
        ollamaModel: isOllama ? selectedConfigId.replace('ollama-', '') : null,
        history
      })
    } catch (error) {
      console.error('Failed to send message:', error)
    }
  }

  const handleAnalyze = async () => {
    const ollamaModel = typeof selectedConfigId === 'string' && selectedConfigId.startsWith('ollama-')
      ? selectedConfigId.replace('ollama-', '')
      : null
    setAnalyzing(true)
    setAnalysisResult(null)
    setAnalysisProgress({ stage: 'STARTING', message: 'Starting analysis…', totalFiles: 0, doneFiles: 0 })
    try {
      const result = await analyzeFull(ollamaModel)
      setAnalysisResult({ ok: true, result })
    } catch (err) {
      setAnalysisResult({ ok: false, error: err.message })
      setAnalyzing(false)
    } finally {
      setAnalysisProgress(null)
    }
  }

  const stageLabel = (stage) => ({
    STARTING: '🔬 Starting',
    SCANNING: '📂 Scanning',
    SUMMARIZING: '📝 Summarizing',
    SYNTHESIZING: '🧠 Synthesizing',
    WRITING: '📄 Writing docs',
    COMPLETE: '✅ Complete',
    ERROR: '❌ Error',
  }[stage] || stage)

  const activeModelLabel = (() => {
    if (!selectedConfigId) return null
    if (typeof selectedConfigId === 'string' && selectedConfigId.startsWith('ollama-')) {
      return selectedConfigId.replace('ollama-', '')
    }
    const cfg = cloudConfigs.find(c => c.id === selectedConfigId)
    return cfg ? cfg.displayName : null
  })()

  return (
    <div className="chat-panel">
      <div className="chat-header">
        <span className="chat-header-label">AI CHAT</span>
        <div className="header-controls">
          <select 
            className="model-selector"
            value={selectedConfigId || ''}
            onChange={(e) => {
              const val = e.target.value
              setSelectedConfigId(val.startsWith('ollama-') ? val : Number(val))
            }}
          >
            {cloudConfigs.length === 0 && ollamaModels.length === 0 && (
              <option value="">No models available</option>
            )}
            {cloudConfigs.length > 0 && (
              <optgroup label="Cloud Models">
                {cloudConfigs.map(config => (
                  <option key={config.id} value={config.id}>
                    {config.displayName} {config.isDefault ? '⭐' : ''}
                  </option>
                ))}
              </optgroup>
            )}
            {ollamaModels.length > 0 && (
              <optgroup label="Local (Ollama)">
                {ollamaModels.map(model => (
                  <option key={model} value={`ollama-${model}`}>
                    {model}
                  </option>
                ))}
              </optgroup>
            )}
          </select>
          <button 
            className="settings-button"
            onClick={() => setShowConfigManager(true)}
            title="Settings — manage model configurations"
          >
            ⚙️
          </button>
          <button 
            className="settings-button"
            onClick={() => setShowMindmapModal(true)}
            title="Mindmap — analyze image"
          >
            📊
          </button>
          <button
            className="settings-button"
            onClick={handleAnalyze}
            disabled={analyzing}
            title="Analyze project — generates docs in .ai-ide/docs/"
          >
            {analyzing ? '⏳' : '🔬'}
          </button>
        </div>
      </div>
      {analysisProgress && !analysisResult && (
        <div className="analysis-result-banner analysis-progress-banner">
          <span className="progress-spinner">⏳</span>
          <span className="progress-stage">{stageLabel(analysisProgress.stage)}</span>
          <span className="progress-message">{analysisProgress.message}</span>
          {analysisProgress.totalFiles > 0 && analysisProgress.doneFiles > 0 && (
            <span className="progress-count">{analysisProgress.doneFiles}/{analysisProgress.totalFiles}</span>
          )}
        </div>
      )}
      {analysisResult && (
        <div className={`analysis-result-banner ${analysisResult.ok ? 'analysis-result-ok' : 'analysis-result-err'}`}>
          {analysisResult.ok ? (
            <>
              {analysisResult.result.status === 'NO_FILES' ? (
                <>⚠️ No Java files found in: <code>{analysisResult.result.docsPath}</code></>
              ) : (
                <>
                  ✅ Analysis complete — {analysisResult.result.summarizedFiles}/{analysisResult.result.totalFiles} files
                  {analysisResult.result.docsPath && <> · docs: <code>{analysisResult.result.docsPath}</code></>}
                  {analysisResult.result.errors?.length > 0 && <> · {analysisResult.result.errors.length} warning(s)</>}
                </>
              )}
            </>
          ) : (
            <>❌ Analysis failed: {analysisResult.error}</>
          )}
          <button className="analysis-result-close" onClick={() => setAnalysisResult(null)}>×</button>
        </div>
      )}
      
      <ModelConfigManager 
        isOpen={showConfigManager}
        onClose={() => setShowConfigManager(false)}
        onConfigsUpdated={loadCloudConfigs}
      />
      <MindmapModal 
        isOpen={showMindmapModal}
        onClose={() => setShowMindmapModal(false)}
      />
      <div 
        className={`context-drop-zone ${isDraggingOver ? 'dragging-over' : ''} ${attachedFiles.length > 0 ? 'has-files' : ''}`}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragEnter={handleDragEnter}
        onDragLeave={handleDragLeave}
      >
        <div className="context-header">FILE CONTEXT</div>
        {attachedFiles.length === 0 ? (
          <div className="context-empty">
            {isDraggingOver ? (
              <div className="drop-indicator">📎 Drop files here</div>
            ) : (
              <div className="drop-hint">Drag files here to add context</div>
            )}
          </div>
        ) : (
          <div className="context-files">
            {attachedFiles.map((file, idx) => (
              <div key={idx} className="context-file-item">
                <span className="context-file-icon">📄</span>
                <span className="context-file-name">{file.path.split('/').pop()}</span>
                <span className="context-file-path">{file.path}</span>
                <button 
                  className="context-remove"
                  onClick={() => removeAttachedFile(file.path)}
                  title="Remove file"
                >
                  ×
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
      <div className="chat-messages">
        {messages.map((msg, idx) => (
          <div 
            key={msg.id || `msg-${idx}`}
            className={`message ${msg.role.toLowerCase()}`}
          >
            {msg.fileContext && msg.fileContext.length > 0 && (
              <div className="message-files">
                {msg.fileContext.map((file, i) => (
                  <span key={i} className="file-tag">📎 {file.path.split('/').pop()}</span>
                ))}
              </div>
            )}
            <MessageContent 
              content={msg.content}
              onFileOpen={onFileOpen}
              onRefreshTree={onRefreshTree}
            />
            <div className="message-time">
              {new Date(msg.timestamp).toLocaleTimeString()}
            </div>
          </div>
        ))}
        <div ref={messagesEndRef} />
      </div>
      <div className="chat-input-container">
        <textarea
          className="chat-input"
          placeholder="Ask AI for help..."
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault()
              handleSend()
            }
          }}
        />
        <button 
          className="chat-send"
          onClick={handleSend}
          disabled={!input.trim()}
        >
          Send
        </button>
      </div>
      <div className="chat-status">
        <span className={`status-indicator ${connected ? 'connected' : 'disconnected'}`} />
        {connected ? 'Connected' : 'Disconnected'}
        {activeModelLabel && (
          <span className="active-model-badge" title="Active model">
            ⬡ {activeModelLabel}
          </span>
        )}
      </div>
    </div>
  )
}

export default ChatPanel
