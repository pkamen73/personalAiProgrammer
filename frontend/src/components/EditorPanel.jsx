import React, { useState, useEffect } from 'react'
import Editor from '@monaco-editor/react'
import { getFile, saveFile } from '../services/fileApi'
import './EditorPanel.css'

const EditorPanel = ({ openFiles, activeFile, onFileSelect, onFileClose, modifiedFiles, setModifiedFiles, theme = 'dark' }) => {
  const [fileContents, setFileContents] = useState({})

  useEffect(() => {
    if (activeFile && !fileContents[activeFile]) {
      loadFile(activeFile)
    }
  }, [activeFile])

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
        {activeContent ? (
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
          <div className="editor-empty">No file selected</div>
        )}
      </div>
      <div className="editor-status">
        {activeFile && (
          <>
            <span>{activeContent?.language || 'plaintext'}</span>
            {modifiedFiles.has(activeFile) && (
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
