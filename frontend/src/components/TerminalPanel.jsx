import React, { useEffect, useRef, useState } from 'react'
import { Terminal } from 'xterm'
import { FitAddon } from 'xterm-addon-fit'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import 'xterm/css/xterm.css'
import './TerminalPanel.css'

const TerminalPanel = ({ theme = 'dark' }) => {
  const terminalRef = useRef(null)
  const xtermRef = useRef(null)
  const stompClientRef = useRef(null)
  const sessionIdRef = useRef(null)

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

      terminal.writeln('Connecting to terminal...')
      
      const socket = new SockJS('/ws')
      const client = new Client({
        webSocketFactory: () => socket,
        onConnect: () => {
          console.log('✓ WebSocket connected')
          terminal.writeln('✓ Connected, requesting terminal session...')
          
          client.subscribe('/topic/terminal/session', (message) => {
            console.log('Received session:', message.body)
            try {
              const session = JSON.parse(message.body)
              console.log('Parsed session:', session)
              sessionIdRef.current = session.sessionId
              terminal.clear()
              terminal.writeln('✓ Terminal session created: ' + session.sessionId)
              
              client.subscribe(`/topic/terminal/${session.sessionId}`, (msg) => {
                console.log('Terminal output received:', msg.body.substring(0, 50))
                terminal.write(msg.body)
              })
            } catch (e) {
              console.error('Failed to parse session:', e)
              terminal.writeln('✗ Failed to create session: ' + e.message)
            }
          })
          
          console.log('Publishing create request...')
          client.publish({
            destination: '/app/terminal/create',
            body: JSON.stringify({})
          })
          console.log('✓ Create request sent')
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame)
          terminal.writeln('✗ STOMP error: ' + frame.headers.message)
        },
        onWebSocketError: (error) => {
          console.error('WebSocket error:', error)
          terminal.writeln('✗ WebSocket error')
        }
      })
      
      client.activate()
      stompClientRef.current = client

      terminal.onData((data) => {
        if (sessionIdRef.current && stompClientRef.current) {
          stompClientRef.current.publish({
            destination: `/app/terminal/${sessionIdRef.current}/input`,
            body: data
          })
        }
      })

      xtermRef.current = terminal

      const handleResize = () => {
        fitAddon.fit()
        if (sessionIdRef.current && stompClientRef.current) {
          stompClientRef.current.publish({
            destination: `/app/terminal/${sessionIdRef.current}/resize`,
            body: JSON.stringify({
              cols: terminal.cols,
              rows: terminal.rows
            })
          })
        }
      }
      
      window.addEventListener('resize', handleResize)

      return () => {
        if (sessionIdRef.current && stompClientRef.current) {
          stompClientRef.current.publish({
            destination: `/app/terminal/${sessionIdRef.current}/close`,
            body: ''
          })
        }
        if (stompClientRef.current) {
          stompClientRef.current.deactivate()
        }
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
