import React, { useState, useEffect, useRef, forwardRef, useImperativeHandle } from 'react'
import { getProjectTree, openProject, getWorkspaceRoot } from '../services/projectApi'
import './FileTree.css'

const FileTree = forwardRef(({ onFileSelect, modifiedFiles = new Set() }, ref) => {
  const [tree, setTree] = useState(null)
  const [expanded, setExpanded] = useState(new Set())
  const [currentPath, setCurrentPath] = useState('')
  const [showPathInput, setShowPathInput] = useState(false)
  const [pathInput, setPathInput] = useState('')
  const [opening, setOpening] = useState(false)
  const [openError, setOpenError] = useState(null)
  const inputRef = useRef(null)

  useEffect(() => {
    loadTree()
    getWorkspaceRoot().then(setCurrentPath).catch(() => {})
  }, [])

  useEffect(() => {
    if (showPathInput && inputRef.current) {
      inputRef.current.focus()
      inputRef.current.select()
    }
  }, [showPathInput])

  const loadTree = async () => {
    try {
      const data = await getProjectTree()
      setTree(data)
    } catch (error) {
      console.error('Failed to load project tree:', error)
    }
  }

  const handleOpenProject = async () => {
    const path = pathInput.trim()
    if (!path) return
    setOpening(true)
    setOpenError(null)
    try {
      const result = await openProject(path)
      setCurrentPath(result.path)
      setShowPathInput(false)
      setPathInput('')
      await loadTree()
    } catch (err) {
      setOpenError(err.response?.data?.error || err.message)
    } finally {
      setOpening(false)
    }
  }

  useImperativeHandle(ref, () => ({
    refresh: loadTree
  }))

  const toggleExpand = (path) => {
    const newExpanded = new Set(expanded)
    if (newExpanded.has(path)) {
      newExpanded.delete(path)
    } else {
      newExpanded.add(path)
    }
    setExpanded(newExpanded)
  }

  const renderNode = (node, level = 0) => {
    if (!node) return null

    const isExpanded = expanded.has(node.path)
    const hasChildren = node.children && node.children.length > 0

    return (
      <div key={node.path}>
        <div
          className="tree-node"
          style={{ paddingLeft: `${level * 16}px` }}
          draggable={node.type === 'FILE'}
          onDragStart={(e) => {
            if (node.type === 'FILE') {
              e.dataTransfer.setData('filePath', node.path)
              e.dataTransfer.effectAllowed = 'copy'
            }
          }}
          onClick={() => {
            if (node.type === 'DIRECTORY') {
              toggleExpand(node.path)
            } else {
              onFileSelect(node.path)
            }
          }}
        >
          {node.type === 'DIRECTORY' && (
            <span className="icon">{isExpanded ? '▼' : '▶'}</span>
          )}
          {node.type === 'FILE' && <span className="icon">📄</span>}
          <span className="name">
            {node.type === 'FILE' && modifiedFiles.has(node.path) && (
              <span className="modified-indicator">● </span>
            )}
            {node.name}
          </span>
        </div>
        {node.type === 'DIRECTORY' && isExpanded && hasChildren && (
          <div className="children">
            {node.children.map(child => renderNode(child, level + 1))}
          </div>
        )}
      </div>
    )
  }

  const dirName = currentPath ? currentPath.split('/').filter(Boolean).pop() || currentPath : 'PROJECT'

  return (
    <div className="file-tree">
      <div className="file-tree-header">
        <span className="file-tree-title" title={currentPath}>{dirName}</span>
        <button
          className="file-tree-open-btn"
          onClick={() => { setShowPathInput(v => !v); setOpenError(null); setPathInput(currentPath) }}
          title="Open folder…"
        >
          📂
        </button>
      </div>
      {showPathInput && (
        <div className="file-tree-path-input-row">
          <input
            ref={inputRef}
            className="file-tree-path-input"
            type="text"
            placeholder="/path/to/project"
            value={pathInput}
            onChange={e => setPathInput(e.target.value)}
            onKeyDown={e => {
              if (e.key === 'Enter') handleOpenProject()
              if (e.key === 'Escape') { setShowPathInput(false); setOpenError(null) }
            }}
            disabled={opening}
          />
          <button
            className="file-tree-open-confirm"
            onClick={handleOpenProject}
            disabled={opening || !pathInput.trim()}
          >
            {opening ? '…' : '→'}
          </button>
          {openError && <div className="file-tree-open-error">{openError}</div>}
        </div>
      )}
      <div className="file-tree-content">
        {tree ? renderNode(tree) : <div className="loading">Loading...</div>}
      </div>
    </div>
  )
})

export default FileTree
