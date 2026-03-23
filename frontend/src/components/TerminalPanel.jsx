import React, { useEffect, useRef } from 'react'
import { Terminal } from 'xterm'
import { FitAddon } from 'xterm-addon-fit'
import 'xterm/css/xterm.css'
import './TerminalPanel.css'

const TerminalPanel = ({ theme = 'dark' }) => {
  const terminalRef = useRef(null)
  const xtermRef = useRef(null)

  useEffect(() => {
    if (terminalRef.current && !xtermRef.current) {
      const darkTheme = {
        background: '#1e1e1e',
        foreground: '#cccccc',
        cursor: '#ffffff',
        selection: '#264f78'
      }
      
      const lightTheme = {
        background: '#ffffff',
        foreground: '#333333',
        cursor: '#000000',
        selection: '#add6ff'
      }
      
      const terminal = new Terminal({
        cursorBlink: true,
        fontSize: 14,
        fontFamily: 'Menlo, Monaco, "Courier New", monospace',
        theme: theme === 'dark' ? darkTheme : lightTheme
      })

      const fitAddon = new FitAddon()
      terminal.loadAddon(fitAddon)
      
      terminal.open(terminalRef.current)
      fitAddon.fit()

      terminal.writeln('Terminal ready.')
      terminal.writeln('Note: This is a placeholder. Backend terminal integration pending.')
      terminal.write('$ ')

      terminal.onData((data) => {
        terminal.write(data)
      })

      xtermRef.current = terminal

      const handleResize = () => fitAddon.fit()
      window.addEventListener('resize', handleResize)

      return () => {
        window.removeEventListener('resize', handleResize)
        terminal.dispose()
      }
    }
  }, [])

  useEffect(() => {
    if (xtermRef.current) {
      const darkTheme = {
        background: '#1e1e1e',
        foreground: '#cccccc',
        cursor: '#ffffff',
        selection: '#264f78'
      }
      
      const lightTheme = {
        background: '#ffffff',
        foreground: '#333333',
        cursor: '#000000',
        selection: '#add6ff'
      }
      
      xtermRef.current.options.theme = theme === 'dark' ? darkTheme : lightTheme
    }
  }, [theme])

  return (
    <div className="terminal-panel">
      <div className="terminal-header">TERMINAL</div>
      <div ref={terminalRef} className="terminal-container" />
    </div>
  )
}

export default TerminalPanel
