import React, { useState, useEffect, useCallback, useRef } from 'react'
import { previewDiagram } from '../services/mindmapApi'
import { saveFile } from '../services/fileApi'
import '../components/MindmapModal.css'

const LineNumberedEditor = ({ value, onChange }) => {
  const taRef = useRef(null)
  const gutterRef = useRef(null)
  const lineCount = Math.max(1, value.split('\n').length)

  const syncScroll = () => {
    if (gutterRef.current && taRef.current) {
      gutterRef.current.scrollTop = taRef.current.scrollTop
    }
  }

  return (
    <div className="plantuml-editor">
      <div ref={gutterRef} className="ln-gutter" aria-hidden="true">
        {Array.from({ length: lineCount }, (_, i) => (
          <div key={i} className="ln-number">{i + 1}</div>
        ))}
      </div>
      <textarea
        ref={taRef}
        className="analysis-editor"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onScroll={syncScroll}
        spellCheck={false}
      />
    </div>
  )
}

const PumlEditor = ({ filePath, content, modified, onChange, onSaved }) => {
  const [previewImage, setPreviewImage] = useState(null)
  const [previewing, setPreviewing] = useState(false)
  const [saving, setSaving] = useState(false)

  const updatePreview = useCallback(async (text) => {
    if (!text || !text.trim()) return
    setPreviewing(true)
    try {
      const data = await previewDiagram(text)
      setPreviewImage(data.image)
    } catch {
      setPreviewImage(null)
    } finally {
      setPreviewing(false)
    }
  }, [])

  useEffect(() => {
    if (!content) return
    const timer = setTimeout(() => updatePreview(content), 800)
    return () => clearTimeout(timer)
  }, [content, updatePreview])

  const handleSave = async () => {
    setSaving(true)
    try {
      await saveFile({ path: filePath, content, language: 'plaintext' })
      if (onSaved) onSaved()
    } catch (err) {
      console.error('Save failed:', err)
    } finally {
      setSaving(false)
    }
  }

  useEffect(() => {
    const onKeyDown = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 's') {
        e.preventDefault()
        if (modified) handleSave()
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [modified, content, filePath])

  return (
    <div className="puml-editor-root">
      <div className="puml-editor-toolbar">
        <span className="puml-editor-filename">{filePath.split('/').pop()}</span>
        <button
          className="save-button"
          onClick={handleSave}
          disabled={saving || !modified}
          title="Save (⌘S)"
        >
          {saving ? 'Saving…' : modified ? '● Save (⌘S)' : 'Saved'}
        </button>
        <button
          className="save-button"
          onClick={() => updatePreview(content)}
          disabled={previewing}
          title="Force re-render preview"
          style={{ marginLeft: 4 }}
        >
          {previewing ? '⏳' : '↺ Preview'}
        </button>
      </div>
      <div className="review-split-view puml-split-fill">
        <div className="review-editor">
          <div className="review-editor-header">
            <h4>PlantUML Source</h4>
          </div>
          <LineNumberedEditor value={content || ''} onChange={onChange} />
        </div>
        <div className="review-preview">
          <h4>Live Preview</h4>
          <div className="preview-container">
            {previewing && <div className="preview-loading">⏳ Rendering…</div>}
            {previewImage && !previewing && (
              <img src={previewImage} alt="Diagram preview" className="preview-image" />
            )}
            {!previewImage && !previewing && (
              <div className="preview-empty">Preview will appear here</div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default PumlEditor
