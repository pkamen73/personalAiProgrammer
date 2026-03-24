import React, { useState, useEffect, useCallback } from 'react'
import { uploadImage, analyzeMindmap, generateDiagram, getAnalyses, loadAnalysis, previewDiagram } from '../services/mindmapApi'
import './MindmapModal.css'

const MindmapModal = ({ isOpen, onClose }) => {
  const [imageFile, setImageFile] = useState(null)
  const [imageId, setImageId] = useState(null)
  const [modelType, setModelType] = useState('local')
  const [analyzing, setAnalyzing] = useState(false)
  const [analysisResult, setAnalysisResult] = useState(null)
  const [editedAnalysis, setEditedAnalysis] = useState('')
  const [generating, setGenerating] = useState(false)
  const [diagramResult, setDiagramResult] = useState(null)
  const [savedAnalyses, setSavedAnalyses] = useState([])
  const [selectedAnalysis, setSelectedAnalysis] = useState('')
  const [previewImage, setPreviewImage] = useState(null)
  const [previewing, setPreviewing] = useState(false)

  useEffect(() => {
    if (isOpen) {
      loadSavedAnalyses()
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
      const data = await analyzeMindmap(imageId, modelType)
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
    
    setGenerating(true)
    try {
      const data = await generateDiagram(imageId, editedAnalysis)
      setDiagramResult(data)
      await loadSavedAnalyses()
      alert('✅ Diagram and analysis text saved')
    } catch (error) {
      alert('❌ Diagram generation failed: ' + error.message)
    } finally {
      setGenerating(false)
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
  }

  if (!isOpen) return null

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content mindmap-modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>📊 Mindmap Analysis</h2>
          <button className="modal-close" onClick={onClose}>×</button>
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
              <div style={{display: 'flex', gap: '8px'}}>
                <select 
                  value={selectedAnalysis}
                  onChange={(e) => setSelectedAnalysis(e.target.value)}
                  className="analysis-selector"
                  style={{flex: 1}}
                >
                  <option value="">-- Select saved analysis --</option>
                  {savedAnalyses.map((analysis, idx) => (
                    <option key={idx} value={analysis.filename}>
                      {analysis.filename}
                    </option>
                  ))}
                </select>
                <button 
                  onClick={handleLoadAnalysis}
                  className="btn-primary"
                  disabled={!selectedAnalysis}
                >
                  📥 Load
                </button>
              </div>
            </div>
          )}
            </>
          )}

          {imageId && !analysisResult && (
            <div className="mindmap-analyze">
              <h3>2. Select Model & Analyze</h3>
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
                  <h4>PlantUML Source (editable)</h4>
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
              <div className="review-actions">
                <button 
                  onClick={handleGenerateDiagram}
                  className="btn-primary"
                  disabled={generating || !editedAnalysis.trim()}
                >
                  {generating ? '⏳ Generating...' : '✓ Generate Diagram'}
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
    </div>
  )
}

export default MindmapModal
