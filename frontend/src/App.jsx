import React, { useState, useEffect } from 'react'
import Split from 'react-split'
import FileTree from './components/FileTree'
import EditorPanel from './components/EditorPanel'
import ChatPanel from './components/ChatPanel'
import TerminalPanel from './components/TerminalPanel'
import './App.css'

function App() {
  const [openFiles, setOpenFiles] = useState([])
  const [activeFile, setActiveFile] = useState(null)
  const [modifiedFiles, setModifiedFiles] = useState(new Set())
  const [theme, setTheme] = useState(() => {
    return localStorage.getItem('theme') || 'dark'
  })

  useEffect(() => {
    localStorage.setItem('theme', theme)
    document.documentElement.setAttribute('data-theme', theme)
  }, [theme])

  const toggleTheme = () => {
    setTheme(prev => prev === 'dark' ? 'light' : 'dark')
  }

  const handleFileOpen = (filePath) => {
    if (!openFiles.includes(filePath)) {
      setOpenFiles([...openFiles, filePath])
    }
    setActiveFile(filePath)
  }

  const handleFileClose = (filePath) => {
    const newOpenFiles = openFiles.filter(f => f !== filePath)
    setOpenFiles(newOpenFiles)
    if (activeFile === filePath) {
      setActiveFile(newOpenFiles.length > 0 ? newOpenFiles[newOpenFiles.length - 1] : null)
    }
    setModifiedFiles(prev => {
      const newSet = new Set(prev)
      newSet.delete(filePath)
      return newSet
    })
  }

  return (
    <div className={`app theme-${theme}`}>
      <button className="theme-toggle" onClick={toggleTheme} title="Toggle theme">
        {theme === 'dark' ? '☀️' : '🌙'}
      </button>
      <Split
        className="split-horizontal"
        direction="vertical"
        sizes={[75, 25]}
        minSize={100}
      >
        <Split
          className="split-vertical"
          sizes={[20, 50, 30]}
          minSize={200}
        >
          <FileTree 
            onFileSelect={handleFileOpen}
            modifiedFiles={modifiedFiles}
          />
          <EditorPanel 
            openFiles={openFiles}
            activeFile={activeFile}
            onFileSelect={setActiveFile}
            onFileClose={handleFileClose}
            modifiedFiles={modifiedFiles}
            setModifiedFiles={setModifiedFiles}
            theme={theme}
          />
          <ChatPanel />
        </Split>
        <TerminalPanel theme={theme} />
      </Split>
    </div>
  )
}

export default App
