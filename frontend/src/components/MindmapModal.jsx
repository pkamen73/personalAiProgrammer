import React, { useState, useEffect, useCallback, useRef } from 'react'
import { uploadImage, analyzeMindmap, generateDiagram, getAnalyses, loadAnalysis, previewDiagram, deleteAnalysis } from '../services/mindmapApi'
import { getAllPrompts } from '../services/analysisPromptApi'
import './MindmapModal.css'

const FilenameInput = ({ onConfirm, onCancel }) => {
  const [value, setValue] = useState('')
  const inputRef = useRef(null)

  useEffect(() => { inputRef.current?.focus() }, [])

  const submit = () => { if (value.trim()) onConfirm(value.trim()) }

  return (
    <div className="filename-input-row">
      <input
        ref={inputRef}
        className="search-input"
        type="text"
        placeholder="e.g. my-diagram"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={(e) => { if (e.key === 'Enter') submit(); if (e.key === 'Escape') onCancel() }}
      />
      <button className="btn-primary search-btn" onClick={submit} disabled={!value.trim()}>
        Save
      </button>
      <button className="btn-secondary search-btn" onClick={onCancel}>
        Cancel
      </button>
    </div>
  )
}

const MindmapModal = ({ isOpen, onClose }) => {
  const [imageFile, setImageFile] = useState(null)
  const [imageId, setImageId] = useState(null)
  const [modelType, setModelType] = useState('local')
  const [prompts, setPrompts] = useState([])
  const [selectedPromptId, setSelectedPromptId] = useState(null)
  const [analyzing, setAnalyzing] = useState(false)
  const [analysisResult, setAnalysisResult] = useState(null)
  const [editedAnalysis, setEditedAnalysis] = useState('')
  const [generating, setGenerating] = useState(false)
  const [diagramResult, setDiagramResult] = useState(null)
  const [savedAnalyses, setSavedAnalyses] = useState([])
  const [selectedAnalysis, setSelectedAnalysis] = useState('')
  const [previewImage, setPreviewImage] = useState(null)
  const [previewing, setPreviewing] = useState(false)
  const [showSearch, setShowSearch] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [replaceText, setReplaceText] = useState('')
  const [contextMenu, setContextMenu] = useState(null)
  const [diagramBaseName, setDiagramBaseName] = useState('')
  const [pendingSave, setPendingSave] = useState(false)

  useEffect(() => {
    if (isOpen) {
      loadSavedAnalyses()
      loadPrompts()
    }
  }, [isOpen])

  const loadSavedAnalyses = async () => {
    try {
      const data = await getAnalyses()
      setSavedAnalyses(data)
    } catch (error) {
      console.error('Failed to load analyses:', error)
    }
  }

  const loadPrompts = async () => {
    try {
      const data = await getAllPrompts()
      setPrompts(data)
      const defaultPrompt = data.find(p => p.isDefault)
      if (defaultPrompt) {
        setSelectedPromptId(defaultPrompt.id)
      }
    } catch (error) {
      console.error('Failed to load prompts:', error)
    }
  }

  const handleLoadAnalysis = async () => {
    if (!selectedAnalysis) return
    
    try {
      const data = await loadAnalysis(selectedAnalysis)
      setEditedAnalysis(data.content)
      setAnalysisResult(data.content)
      setImageId(data.associatedImage || 'loaded-from-file')
      updatePreview(data.content)
      alert('✅ Analysis loaded - Edit and regenerate diagram')
    } catch (error) {
      alert('❌ Failed to load analysis: ' + error.message)
    }
  }

  const updatePreview = useCallback(async (text) => {
    if (!text || text.trim().length === 0) return
    
    setPreviewing(true)
    try {
      const data = await previewDiagram(text)
      setPreviewImage(data.image)
    } catch (error) {
      console.error('Preview failed:', error)
      setPreviewImage(null)
    } finally {
      setPreviewing(false)
    }
  }, [])

  useEffect(() => {
    if (!editedAnalysis || !analysisResult) return
    
    const timer = setTimeout(() => {
      updatePreview(editedAnalysis)
    }, 800)
    
    return () => clearTimeout(timer)
  }, [editedAnalysis, analysisResult, updatePreview])

  const handleFileSelect = (e) => {
    const file = e.target.files[0]
    if (file) {
      setImageFile(file)
      setImageId(null)
      setAnalysisResult(null)
      setEditedAnalysis('')
      setDiagramResult(null)
    }
  }

  const handleUpload = async () => {
    if (!imageFile) return
    
    try {
      const data = await uploadImage(imageFile)
      setImageId(data.imageId)
      alert('✅ Image uploaded')
    } catch (error) {
      alert('❌ Upload failed: ' + error.message)
    }
  }

  const handleAnalyze = async () => {
    if (!imageId) return
    
    setAnalyzing(true)
    try {
      const data = await analyzeMindmap(imageId, modelType, selectedPromptId)
      setAnalysisResult(data.analysis)
      setEditedAnalysis(data.analysis)
      alert('✅ Analysis complete - Review and edit if needed')
    } catch (error) {
      alert('❌ Analysis failed: ' + error.message)
    } finally {
      setAnalyzing(false)
    }
  }

  const handleGenerateDiagram = async () => {
    if (!imageId || !editedAnalysis) return
    if (!diagramBaseName) {
      setPendingSave(true)
      return
    }
    setGenerating(true)
    try {
      const data = await generateDiagram(imageId, editedAnalysis, diagramBaseName)
      setDiagramResult(data)
      await loadSavedAnalyses()
      alert('✅ Diagram and analysis text saved')
    } catch (error) {
      alert('❌ Diagram generation failed: ' + error.message)
    } finally {
      setGenerating(false)
    }
  }

  const handleConfirmBaseName = async (name) => {
    const trimmed = name.trim()
    if (!trimmed) return
    setDiagramBaseName(trimmed)
    setPendingSave(false)
    setGenerating(true)
    try {
      const data = await generateDiagram(imageId, editedAnalysis, trimmed)
      setDiagramResult(data)
      await loadSavedAnalyses()
      alert('✅ Diagram and analysis text saved')
    } catch (error) {
      alert('❌ Diagram generation failed: ' + error.message)
    } finally {
      setGenerating(false)
    }
  }

  const matchCount = searchText
    ? (editedAnalysis.match(new RegExp(searchText.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g')) || []).length
    : 0

  const handleReplaceAll = () => {
    if (!searchText) return
    const escaped = searchText.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    setEditedAnalysis(editedAnalysis.replaceAll(new RegExp(escaped, 'g'), replaceText))
  }

  useEffect(() => {
    if (!contextMenu) return
    const dismiss = () => setContextMenu(null)
    window.addEventListener('click', dismiss)
    window.addEventListener('contextmenu', dismiss)
    return () => {
      window.removeEventListener('click', dismiss)
      window.removeEventListener('contextmenu', dismiss)
    }
  }, [contextMenu])

  const handleContextMenu = (e, analysis) => {
    e.preventDefault()
    e.stopPropagation()
    setContextMenu({ x: e.clientX, y: e.clientY, filename: analysis.filename })
  }

  const handleDeleteAnalysis = async () => {
    if (!contextMenu) return
    const { filename } = contextMenu
    setContextMenu(null)
    try {
      await deleteAnalysis(filename)
      await loadSavedAnalyses()
      if (selectedAnalysis === filename) setSelectedAnalysis('')
    } catch (error) {
      alert('❌ Delete failed: ' + error.message)
    }
  }

  const handleNewAnalysis = () => {
    setImageFile(null)
    setImageId(null)
    setAnalysisResult(null)
    setEditedAnalysis('')
    setDiagramResult(null)
    setPreviewImage(null)
    setSelectedAnalysis('')
    setDiagramBaseName('')
    setPendingSave(false)
  }

  const handleClose = () => {
    handleNewAnalysis()
    onClose()
  }

  if (!isOpen) return null

  return (
    <div className="modal-overlay" onClick={handleClose}>
      <div className="modal-content mindmap-modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>📊 Mindmap Analysis</h2>
          <button className="modal-close" onClick={handleClose}>×</button>
        </div>

        <div className="modal-body">
          {!analysisResult && (
            <>
              <div className="mindmap-upload">
            <h3>1. Upload Mindmap Image</h3>
            <input type="file" accept="image/*" onChange={handleFileSelect} />
            {imageFile && (
              <button onClick={handleUpload} className="btn-primary">
                Upload {imageFile.name}
              </button>
            )}
            {imageId && <div className="success-msg">✓ Uploaded: {imageId}</div>}
          </div>

          {savedAnalyses.length > 0 && (
            <div className="mindmap-load">
              <h3>Or Load Existing:</h3>
              <div className="analysis-gallery">
                {savedAnalyses.map((analysis, idx) => (
                  <div 
                    key={idx}
                    className={`analysis-card ${selectedAnalysis === analysis.filename ? 'selected' : ''}`}
                    onClick={() => setSelectedAnalysis(analysis.filename)}
                    onDoubleClick={handleLoadAnalysis}
                    onContextMenu={(e) => handleContextMenu(e, analysis)}
                  >
                    {analysis.diagramUrl ? (
                      <img src={analysis.diagramUrl} alt="Diagram preview" className="analysis-thumbnail" />
                    ) : (
                      <div className="analysis-no-preview">📄 No Preview</div>
                    )}
                    <div className="analysis-info">
                      <div className="analysis-filename">{analysis.filename.substring(0, 20)}...</div>
                      {analysis.modified && (
                        <div className="analysis-date">{new Date(analysis.modified).toLocaleDateString()}</div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
              <button 
                onClick={handleLoadAnalysis}
                className="btn-primary"
                disabled={!selectedAnalysis}
                style={{marginTop: '12px'}}
              >
                📥 Load Selected
              </button>
            </div>
          )}
            </>
          )}

          {imageId && !analysisResult && (
            <div className="mindmap-analyze">
              <h3>2. Select Model & Analyze</h3>
              <div className="prompt-selector-section">
                <label style={{fontWeight: 600, fontSize: '12px', color: 'var(--text-secondary)', display: 'block', marginBottom: '8px'}}>
                  Analysis Intent:
                </label>
                <select
                  value={selectedPromptId || ''}
                  onChange={(e) => setSelectedPromptId(Number(e.target.value))}
                  className="analysis-selector"
                  style={{marginBottom: '16px'}}
                >
                  {prompts.map(p => (
                    <option key={p.id} value={p.id}>
                      {p.title} - {p.description}
                    </option>
                  ))}
                </select>
              </div>

              <div className="model-selector-group">
                <label className="model-option">
                  <input
                    type="radio"
                    name="modelType"
                    value="local"
                    checked={modelType === 'local'}
                    onChange={(e) => setModelType(e.target.value)}
                  />
                  <span>🖥️ Local - Qwen3-VL:4b (Fast, Free)</span>
                </label>
                <label className="model-option">
                  <input
                    type="radio"
                    name="modelType"
                    value="cloud"
                    checked={modelType === 'cloud'}
                    onChange={(e) => setModelType(e.target.value)}
                  />
                  <span>☁️ Cloud - Claude Sonnet 4.6 (Slower, Better Quality)</span>
                </label>
              </div>
              <button 
                onClick={handleAnalyze} 
                className="btn-primary"
                disabled={analyzing}
              >
                {analyzing ? '⏳ Analyzing...' : '🔍 Analyze Mindmap'}
              </button>
            </div>
          )}

          {analysisResult && !diagramResult && (
            <div className="mindmap-review">
              <h3>3. Review & Edit Analysis</h3>
              <div className="review-split-view">
                <div className="review-editor">
                  <div className="review-editor-header">
                    <h4>PlantUML Source (editable)</h4>
                    <button
                      className={`btn-icon search-toggle ${showSearch ? 'active' : ''}`}
                      onClick={() => setShowSearch(v => !v)}
                      title="Search & Replace (Ctrl+H)"
                    >
                      🔍
                    </button>
                  </div>
                  {showSearch && (
                    <div className="search-replace-bar">
                      <div className="search-row">
                        <input
                          className="search-input"
                          type="text"
                          placeholder="Search…"
                          value={searchText}
                          onChange={(e) => setSearchText(e.target.value)}
                          autoFocus
                        />
                        {searchText && (
                          <span className="match-count">
                            {matchCount} match{matchCount !== 1 ? 'es' : ''}
                          </span>
                        )}
                      </div>
                      <div className="search-row">
                        <input
                          className="search-input"
                          type="text"
                          placeholder="Replace with…"
                          value={replaceText}
                          onChange={(e) => setReplaceText(e.target.value)}
                        />
                        <button
                          className="btn-primary search-btn"
                          onClick={handleReplaceAll}
                          disabled={!searchText || matchCount === 0}
                          title="Replace all occurrences"
                        >
                          Replace All
                        </button>
                        <button
                          className="btn-icon search-close"
                          onClick={() => { setShowSearch(false); setSearchText(''); setReplaceText('') }}
                          title="Close"
                        >
                          ×
                        </button>
                      </div>
                    </div>
                  )}
                  <textarea
                    className="analysis-editor"
                    value={editedAnalysis}
                    onChange={(e) => setEditedAnalysis(e.target.value)}
                    rows={20}
                  />
                </div>
                <div className="review-preview">
                  <h4>Live Preview</h4>
                  <div className="preview-container">
                    {previewing && <div className="preview-loading">⏳ Rendering...</div>}
                    {previewImage && !previewing && (
                      <img src={previewImage} alt="Diagram preview" className="preview-image" />
                    )}
                    {!previewImage && !previewing && (
                      <div className="preview-empty">Preview will appear here</div>
                    )}
                  </div>
                </div>
              </div>
              {pendingSave && (
                <div className="filename-prompt">
                  <label className="filename-prompt-label">Save as</label>
                  <FilenameInput onConfirm={handleConfirmBaseName} onCancel={() => setPendingSave(false)} />
                </div>
              )}
              <div className="review-actions">
                <button 
                  onClick={handleGenerateDiagram}
                  className="btn-primary"
                  disabled={generating || !editedAnalysis.trim() || pendingSave}
                >
                  {generating ? '⏳ Generating...' : diagramBaseName ? `💾 Save (${diagramBaseName})` : '✓ Generate Diagram'}
                </button>
                <button 
                  onClick={() => setEditedAnalysis(analysisResult)}
                  className="btn-secondary"
                >
                  Reset to Original
                </button>
              </div>
            </div>
          )}

          {diagramResult && (
            <div className="mindmap-results">
              <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px'}}>
                <h3 style={{margin: 0}}>4. Results</h3>
                <button onClick={handleNewAnalysis} className="btn-secondary">
                  🔄 New Analysis
                </button>
              </div>
              <div className="result-images">
                <div className="result-item">
                  <h4>Original Mindmap</h4>
                  <img src={`/api/images/${imageId}`} alt="Original mindmap" />
                  <a href={`/api/images/${imageId}`} download className="download-link">
                    ⬇️ Download Original
                  </a>
                </div>
                <div className="result-item">
                  <h4>Generated UML Diagram</h4>
                  <img src={diagramResult.diagramUrl} alt="Generated diagram" />
                  <a href={diagramResult.diagramUrl} download className="download-link">
                    ⬇️ Download Diagram
                  </a>
                </div>
              </div>
              <div className="result-analysis">
                <h4>Final Analysis</h4>
                <pre>{editedAnalysis}</pre>
                {diagramResult.analysisFileUrl && (
                  <a href={diagramResult.analysisFileUrl} download className="download-link">
                    📄 Download Analysis Text
                  </a>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {contextMenu && (
        <ul
          className="context-menu"
          style={{ top: contextMenu.y, left: contextMenu.x }}
          onClick={(e) => e.stopPropagation()}
        >
          <li className="context-menu-item context-menu-item--danger" onClick={handleDeleteAnalysis}>
            🗑 Delete
          </li>
        </ul>
      )}
    </div>
  )
}

export default MindmapModal
