import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

let stompClient = null

export const connectChat = (onMessageReceived) => {
  const socket = new SockJS('/ws')
  
  stompClient = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    onConnect: () => {
      stompClient.subscribe('/topic/messages', (message) => {
        const chatMessage = JSON.parse(message.body)
        onMessageReceived(chatMessage)
      })
    },
    onStompError: (frame) => {
      console.error('STOMP error:', frame)
    }
  })

  stompClient.activate()
  return stompClient
}

export const sendMessage = async (message) => {
  if (stompClient && stompClient.connected) {
    stompClient.publish({
      destination: '/app/chat',
      body: JSON.stringify(message)
    })
  } else {
    throw new Error('WebSocket not connected')
  }
}

export const disconnect = () => {
  if (stompClient) {
    stompClient.deactivate()
  }
}
