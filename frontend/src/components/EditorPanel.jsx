import React, { useState, useEffect } from 'react'
import Editor from '@monaco-editor/react'
import { getFile, saveFile, isImagePath, getImageUrl } from '../services/fileApi'
import PumlEditor from './PumlEditor'
import './EditorPanel.css'

const isPumlPath = (path) => path && path.toLowerCase().endsWith('.puml')

const EditorPanel = ({ openFiles, activeFile, onFileSelect, onFileClose, modifiedFiles, setModifiedFiles, theme = 'dark' }) => {
  const [fileContents, setFileContents] = useState({})

  useEffect(() => {
    if (activeFile && !isImagePath(activeFile) && !fileContents[activeFile]) {
      loadFile(activeFile)
    }
  }, [activeFile])

  const handlePumlChange = (value) => {
    if (activeFile) {
      setFileContents(prev => ({ ...prev, [activeFile]: { ...prev[activeFile], content: value } }))
      setModifiedFiles(prev => new Set(prev).add(activeFile))
    }
  }

  const handlePumlSaved = () => {
    setModifiedFiles(prev => { const s = new Set(prev); s.delete(activeFile); return s })
  }

  useEffect(() => {
    const handleKeyDown = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 's') {
        e.preventDefault()
        if (activeFile && modifiedFiles.has(activeFile)) {
          handleSave()
        }
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [activeFile, modifiedFiles, fileContents])

  const loadFile = async (path) => {
    try {
      const data = await getFile(path)
      setFileContents(prev => ({ ...prev, [path]: data }))
    } catch (error) {
      console.error('Failed to load file:', error)
    }
  }

  const handleEditorChange = (value) => {
    if (activeFile) {
      setFileContents(prev => ({
        ...prev,
        [activeFile]: { ...prev[activeFile], content: value }
      }))
      setModifiedFiles(prev => new Set(prev).add(activeFile))
    }
  }

  const handleSave = async () => {
    if (activeFile && fileContents[activeFile]) {
      try {
        await saveFile(fileContents[activeFile])
        setModifiedFiles(prev => {
          const newSet = new Set(prev)
          newSet.delete(activeFile)
          return newSet
        })
      } catch (error) {
        console.error('Failed to save file:', error)
      }
    }
  }

  const activeContent = activeFile ? fileContents[activeFile] : null

  return (
    <div className="editor-panel">
      <div className="tab-bar">
        {openFiles.map(file => (
          <div
            key={file}
            className={`tab ${file === activeFile ? 'active' : ''} ${modifiedFiles.has(file) ? 'modified' : ''}`}
            onClick={() => onFileSelect(file)}
          >
            <span className="tab-label">
              {modifiedFiles.has(file) && <span className="modified-dot">● </span>}
              {file.split('/').pop()}
            </span>
            <button
              className="tab-close"
              onClick={(e) => {
                e.stopPropagation()
                onFileClose(file)
              }}
            >
              ×
            </button>
          </div>
        ))}
      </div>
      <div className="editor-container">
        {!activeFile ? (
          <div className="editor-empty">No file selected</div>
        ) : isImagePath(activeFile) ? (
          <div className="image-viewer">
            <img
              src={getImageUrl(activeFile)}
              alt={activeFile.split('/').pop()}
              className="image-viewer-img"
            />
            <div className="image-viewer-name">{activeFile.split('/').pop()}</div>
          </div>
        ) : isPumlPath(activeFile) && activeContent ? (
          <PumlEditor
            filePath={activeFile}
            content={activeContent.content}
            modified={modifiedFiles.has(activeFile)}
            onChange={handlePumlChange}
            onSaved={handlePumlSaved}
          />
        ) : activeContent ? (
          <Editor
            height="100%"
            language={activeContent.language}
            value={activeContent.content}
            onChange={handleEditorChange}
            theme={theme === 'dark' ? 'vs-dark' : 'light'}
            options={{
              minimap: { enabled: true },
              fontSize: 14,
              wordWrap: 'on',
              automaticLayout: true
            }}
          />
        ) : (
          <div className="editor-empty">Loading…</div>
        )}
      </div>
      <div className="editor-status">
        {activeFile && (
          <>
            <span>{isImagePath(activeFile) ? 'image' : isPumlPath(activeFile) ? 'plantuml' : (activeContent?.language || 'plaintext')}</span>
            {!isImagePath(activeFile) && !isPumlPath(activeFile) && modifiedFiles.has(activeFile) && (
              <button className="save-button" onClick={handleSave}>
                Save (⌘S)
              </button>
            )}
          </>
        )}
      </div>
    </div>
  )
}

export default EditorPanel
