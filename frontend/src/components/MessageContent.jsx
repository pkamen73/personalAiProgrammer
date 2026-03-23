import React, { useState } from 'react'
import { applyCode } from '../services/codeApi'
import './MessageContent.css'

const MessageContent = ({ content }) => {
  const [applying, setApplying] = useState(false)

  const parseContent = () => {
    const codeBlockRegex = /```(\w+)?\n([\s\S]*?)```/g
    const parts = []
    let lastIndex = 0
    let match

    while ((match = codeBlockRegex.exec(content)) !== null) {
      if (match.index > lastIndex) {
        parts.push({ type: 'text', content: content.slice(lastIndex, match.index) })
      }

      const language = match[1] || 'plaintext'
      const code = match[2]
      parts.push({ type: 'code', language, content: code })

      lastIndex = match.index + match[0].length
    }

    if (lastIndex < content.length) {
      parts.push({ type: 'text', content: content.slice(lastIndex) })
    }

    return parts.length > 0 ? parts : [{ type: 'text', content }]
  }

  const extractFilePath = (code) => {
    const pathMatch = code.match(/^\/\/\s*(?:File:|Path:)\s*(.+)$/m)
    return pathMatch ? pathMatch[1].trim() : null
  }

  const handleApplyCode = async (code, language) => {
    if (language !== 'java') {
      alert('Currently only Java files are supported')
      return
    }

    const filePath = extractFilePath(code)
    if (!filePath) {
      const path = prompt('Enter file path (e.g., src/main/java/com/example/MyClass.java):')
      if (!path) return
      
      await applyCodeToFile(path, code)
    } else {
      await applyCodeToFile(filePath, code)
    }
  }

  const applyCodeToFile = async (path, code) => {
    setApplying(true)
    try {
      console.log('Applying code to:', path)
      console.log('Code length:', code.length)
      
      const changes = [{
        path,
        oldContent: null,
        newContent: code,
        changeType: 'MODIFY'
      }]
      
      console.log('Sending changes:', changes)
      await applyCode(changes)
      console.log('✅ Apply successful')
      alert(`✅ Code applied to ${path}`)
    } catch (error) {
      console.error('Failed to apply code:', error)
      alert(`❌ Failed to apply code: ${error.message}`)
    } finally {
      setApplying(false)
    }
  }

  const parts = parseContent()

  return (
    <div className="message-content-wrapper">
      {parts.map((part, idx) => {
        if (part.type === 'text') {
          return <div key={idx} className="message-text">{part.content}</div>
        } else {
          return (
            <div key={idx} className="code-block-container">
              <div className="code-block-header">
                <span className="code-language">{part.language}</span>
                <button
                  className="apply-code-button"
                  onClick={() => handleApplyCode(part.content, part.language)}
                  disabled={applying}
                >
                  {applying ? '⏳ Applying...' : '✓ Apply Code'}
                </button>
              </div>
              <pre className="code-block">
                <code>{part.content}</code>
              </pre>
            </div>
          )
        }
      })}
    </div>
  )
}

export default MessageContent
