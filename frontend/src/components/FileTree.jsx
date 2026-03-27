import React, { useState, useEffect, useRef, forwardRef, useImperativeHandle } from 'react'
import { getProjectTree, openProject, getWorkspaceRoot, createDirectory, createFile, deleteItem, renameItem, copyItem, moveItem } from '../services/projectApi'
import DirPicker from './DirPicker'
import './FileTree.css'

const FileTree = forwardRef(({ onFileSelect, modifiedFiles = new Set() }, ref) => {
  const [tree, setTree] = useState(null)
  const [expanded, setExpanded] = useState(new Set())
  const [currentPath, setCurrentPath] = useState('')
  const [showPicker, setShowPicker] = useState(false)
  const [opening, setOpening] = useState(false)
  const [contextMenu, setContextMenu] = useState(null)
  const [renaming, setRenaming] = useState(null)
  const [renameValue, setRenameValue] = useState('')
  const [creating, setCreating] = useState(null)
  const [createValue, setCreateValue] = useState('')
  const [destPicker, setDestPicker] = useState(null)
  const renameInputRef = useRef(null)
  const createInputRef = useRef(null)

  useEffect(() => {
    loadTree()
    getWorkspaceRoot().then(setCurrentPath).catch(() => {})
  }, [])

  useEffect(() => {
    if (renaming && renameInputRef.current) renameInputRef.current.select()
  }, [renaming])

  useEffect(() => {
    if (creating && createInputRef.current) createInputRef.current.focus()
  }, [creating])

  useEffect(() => {
    if (!contextMenu) return
    const dismiss = () => setContextMenu(null)
    window.addEventListener('click', dismiss)
    window.addEventListener('contextmenu', dismiss)
    return () => { window.removeEventListener('click', dismiss); window.removeEventListener('contextmenu', dismiss) }
  }, [contextMenu])

  const loadTree = async () => {
    try { setTree(await getProjectTree()) } catch (e) { console.error(e) }
  }

  useImperativeHandle(ref, () => ({ refresh: loadTree }))

  const toggleExpand = (path) => {
    const s = new Set(expanded)
    s.has(path) ? s.delete(path) : s.add(path)
    setExpanded(s)
  }

  const handleOpenProject = async (path) => {
    setShowPicker(false)
    setOpening(true)
    try {
      const result = await openProject(path)
      setCurrentPath(result.path)
      setExpanded(new Set())
      await loadTree()
    } catch (err) {
      alert('❌ ' + (err.response?.data?.error || err.message))
    } finally { setOpening(false) }
  }

  const handleContextMenu = (e, node) => {
    e.preventDefault()
    e.stopPropagation()
    setContextMenu({ x: e.clientX, y: e.clientY, node })
  }

  const handleRename = (node) => {
    setContextMenu(null)
    setRenaming(node.path)
    setRenameValue(node.name)
  }

  const commitRename = async () => {
    const name = renameValue.trim()
    if (!name || !renaming) return
    try {
      await renameItem(renaming, name)
      await loadTree()
    } catch (e) { alert('❌ Rename failed: ' + e.message) }
    setRenaming(null)
  }

  const handleDelete = async (node) => {
    setContextMenu(null)
    if (!confirm(`Delete "${node.name}"? This cannot be undone.`)) return
    try { await deleteItem(node.path); await loadTree() }
    catch (e) { alert('❌ Delete failed: ' + e.message) }
  }

  const handleCreate = (node, type) => {
    setContextMenu(null)
    const dirPath = node.type === 'DIRECTORY' ? node.path : node.path.substring(0, node.path.lastIndexOf('/')) || '/'
    if (!expanded.has(dirPath)) toggleExpand(dirPath)
    setCreating({ path: dirPath, type })
    setCreateValue('')
  }

  const commitCreate = async () => {
    const name = createValue.trim()
    if (!name || !creating) return
    const full = creating.path === '/' ? '/' + name : creating.path + '/' + name
    try {
      if (creating.type === 'file') await createFile(full)
      else await createDirectory(full)
      await loadTree()
    } catch (e) { alert('❌ Create failed: ' + e.message) }
    setCreating(null)
  }

  const handleCopy = (node) => {
    setContextMenu(null)
    setDestPicker({ node, op: 'copy' })
  }

  const handleMove = (node) => {
    setContextMenu(null)
    setDestPicker({ node, op: 'move' })
  }

  const commitDestPicker = async (destPath) => {
    const { node, op } = destPicker
    setDestPicker(null)
    try {
      if (op === 'copy') await copyItem(node.path, destPath)
      else await moveItem(node.path, destPath)
      await loadTree()
    } catch (e) { alert(`❌ ${op} failed: ` + e.message) }
  }

  const renderNode = (node, level = 0) => {
    if (!node) return null
    const isExpanded = expanded.has(node.path)
    const isRenaming = renaming === node.path
    const isDir = node.type === 'DIRECTORY'

    return (
      <div key={node.path}>
        <div
          className={`tree-node ${isDir ? 'tree-node-dir' : ''}`}
          style={{ paddingLeft: `${level * 16 + 8}px` }}
          draggable={!isDir}
          onDragStart={(e) => { if (!isDir) { e.dataTransfer.setData('filePath', node.path); e.dataTransfer.effectAllowed = 'copy' } }}
          onClick={() => { if (!isRenaming) { isDir ? toggleExpand(node.path) : onFileSelect(node.path) } }}
          onContextMenu={(e) => handleContextMenu(e, node)}
        >
          <span className="icon">
            {isDir ? (isExpanded ? '▼' : '▶') : '📄'}
          </span>
          {isRenaming ? (
            <input
              ref={renameInputRef}
              className="tree-rename-input"
              value={renameValue}
              onChange={e => setRenameValue(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter') commitRename(); if (e.key === 'Escape') setRenaming(null) }}
              onBlur={commitRename}
              onClick={e => e.stopPropagation()}
            />
          ) : (
            <span className="name">
              {!isDir && modifiedFiles.has(node.path) && <span className="modified-indicator">● </span>}
              {node.name}
            </span>
          )}
        </div>

        {isDir && isExpanded && (
          <div className="children">
            {creating?.path === node.path && (
              <div className="tree-create-row" style={{ paddingLeft: `${(level + 1) * 16 + 8}px` }}>
                <span className="icon">{creating.type === 'file' ? '📄' : '📁'}</span>
                <input
                  ref={createInputRef}
                  className="tree-rename-input"
                  placeholder={creating.type === 'file' ? 'filename.txt' : 'folder-name'}
                  value={createValue}
                  onChange={e => setCreateValue(e.target.value)}
                  onKeyDown={e => { if (e.key === 'Enter') commitCreate(); if (e.key === 'Escape') setCreating(null) }}
                  onBlur={commitCreate}
                />
              </div>
            )}
            {node.children?.map(child => renderNode(child, level + 1))}
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
        <button className="file-tree-open-btn" onClick={() => setShowPicker(true)} disabled={opening} title="Open folder…">
          {opening ? '⏳' : '📂'}
        </button>
      </div>
      <div className="file-tree-content">
        {tree ? renderNode(tree) : <div className="loading">Loading...</div>}
      </div>

      {contextMenu && (
        <ul className="ft-context-menu" style={{ top: contextMenu.y, left: contextMenu.x }} onClick={e => e.stopPropagation()}>
          {contextMenu.node.type === 'DIRECTORY' && <>
            <li className="ft-cm-item" onClick={() => handleCreate(contextMenu.node, 'file')}>📄 New File</li>
            <li className="ft-cm-item" onClick={() => handleCreate(contextMenu.node, 'dir')}>📁 New Folder</li>
            <li className="ft-cm-divider" />
          </>}
          <li className="ft-cm-item" onClick={() => handleRename(contextMenu.node)}>✏️ Rename</li>
          <li className="ft-cm-item" onClick={() => handleCopy(contextMenu.node)}>📋 Copy to…</li>
          <li className="ft-cm-item" onClick={() => handleMove(contextMenu.node)}>✂️ Move to…</li>
          <li className="ft-cm-divider" />
          <li className="ft-cm-item ft-cm-danger" onClick={() => handleDelete(contextMenu.node)}>🗑 Delete</li>
        </ul>
      )}

      {showPicker && <DirPicker initialPath={currentPath} onConfirm={handleOpenProject} onCancel={() => setShowPicker(false)} />}

      {destPicker && (
        <DirPicker
          initialPath={currentPath}
          onConfirm={commitDestPicker}
          onCancel={() => setDestPicker(null)}
        />
      )}
    </div>
  )
})

export default FileTree
