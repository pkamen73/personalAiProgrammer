import React, { useState, useEffect, useCallback } from 'react'
import axios from 'axios'
import './DirPicker.css'

const fetchDirs = async (path) => {
  const res = await axios.get('/api/fs/dirs', { params: path ? { path } : {} })
  return res.data
}

const DirNode = ({ entry, selectedPath, onSelect }) => {
  const [open, setOpen] = useState(false)
  const [children, setChildren] = useState(null)
  const [loading, setLoading] = useState(false)
  const isSelected = selectedPath === entry.path

  const toggle = useCallback(async (e) => {
    e.stopPropagation()
    if (!entry.hasChildren) return
    if (!open && children === null) {
      setLoading(true)
      try {
        const data = await fetchDirs(entry.path)
        setChildren(data)
      } finally {
        setLoading(false)
      }
    }
    setOpen(v => !v)
  }, [open, children, entry])

  return (
    <div className="dp-node">
      <div
        className={`dp-row ${isSelected ? 'dp-selected' : ''}`}
        onClick={() => onSelect(entry.path)}
        onDoubleClick={toggle}
      >
        <span
          className={`dp-arrow ${entry.hasChildren ? '' : 'dp-arrow-empty'}`}
          onClick={toggle}
        >
          {entry.hasChildren ? (loading ? '⏳' : open ? '▼' : '▶') : ''}
        </span>
        <span className="dp-icon">📁</span>
        <span className="dp-name">{entry.name}</span>
      </div>
      {open && children && (
        <div className="dp-children">
          {children.map(child => (
            <DirNode key={child.path} entry={child} selectedPath={selectedPath} onSelect={onSelect} />
          ))}
          {children.length === 0 && <div className="dp-empty">Empty</div>}
        </div>
      )}
    </div>
  )
}

const DirPicker = ({ initialPath, onConfirm, onCancel }) => {
  const [roots, setRoots] = useState([])
  const [selected, setSelected] = useState(initialPath || '')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchDirs(null).then(data => {
      setRoots(data)
      setLoading(false)
    })
  }, [])

  return (
    <div className="dp-overlay" onClick={onCancel}>
      <div className="dp-modal" onClick={e => e.stopPropagation()}>
        <div className="dp-header">
          <span className="dp-title">Open Folder</span>
          <button className="dp-close" onClick={onCancel}>×</button>
        </div>
        <div className="dp-selected-bar" title={selected}>
          {selected || <span className="dp-hint">Click a folder to select</span>}
        </div>
        <div className="dp-tree">
          {loading && <div className="dp-loading">Loading…</div>}
          {roots.map(entry => (
            <DirNode key={entry.path} entry={entry} selectedPath={selected} onSelect={setSelected} />
          ))}
        </div>
        <div className="dp-footer">
          <button className="dp-btn dp-btn-secondary" onClick={onCancel}>Cancel</button>
          <button
            className="dp-btn dp-btn-primary"
            onClick={() => selected && onConfirm(selected)}
            disabled={!selected}
          >
            Open
          </button>
        </div>
      </div>
    </div>
  )
}

export default DirPicker
