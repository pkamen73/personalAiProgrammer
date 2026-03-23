import React, { useState, useEffect } from 'react'
import { getProjectTree } from '../services/projectApi'
import './FileTree.css'

const FileTree = ({ onFileSelect, modifiedFiles = new Set() }) => {
  const [tree, setTree] = useState(null)
  const [expanded, setExpanded] = useState(new Set())

  useEffect(() => {
    loadTree()
  }, [])

  const loadTree = async () => {
    try {
      const data = await getProjectTree()
      setTree(data)
    } catch (error) {
      console.error('Failed to load project tree:', error)
    }
  }

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

  return (
    <div className="file-tree">
      <div className="file-tree-header">PROJECT</div>
      <div className="file-tree-content">
        {tree ? renderNode(tree) : <div className="loading">Loading...</div>}
      </div>
    </div>
  )
}

export default FileTree
