import axios from 'axios';
import React, { useState, useRef, useEffect } from 'react';
import ReactMarkdown from 'react-markdown';
import "./Gemini.css";


export default function Gemini() {
    const [messages, setMessages] = useState([]); // [{sender: 'user', text: '안녕'}, {sender: 'ai', text: '안녕하세요!'}]
    const [input, setInput] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const messagesEndRef = useRef(null); // 스크롤을 맨 아래로 이동시키기 위한 Ref

    const API_BASE_URL = 'http://localhost:8080/api/chat'; // Spring Boot 백엔드 주소

    // 스크롤을 맨 아래로 이동시키는 함수
    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    // messages 상태가 업데이트될 때마다 스크롤
    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    const sendMessage = async () => {
        if (input.trim() === '') return;

        const userMessage = { sender: 'user', text: input };
        setMessages((prevMessages) => [...prevMessages, userMessage]);
        setInput('');
        setIsLoading(true);        
        try {
        const requestData = { message: userMessage.text }; 
        // 2. axios.post(URL, 데이터, 설정) 구문 사용        
        const response = await axios.post(
            API_BASE_URL, 
            requestData, // 두 번째 인자에 데이터 객체 직접 전달
            { withCredentials: true }
        );       
        console.log(response.data);
        const data = response.data;
        const aiMessage = { sender: 'ai', text: data.response };
        setMessages((prevMessages) => [...prevMessages, aiMessage]);
        } catch (error) {
        console.error('Error sending message:', error);
        setMessages((prevMessages) => [
            ...prevMessages,
            { sender: 'ai', text: '죄송합니다. 메시지를 처리하는 중 오류가 발생했습니다.' },
        ]);
        } finally {
        setIsLoading(false);
        }
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !isLoading) {
        sendMessage();
        }
    };

    // **********************************
    // JSX 렌더링 부분 문법 오류 수정
    // **********************************
    return (
        <div className="chat-container">
        <h1>Gemini 챗봇</h1>

        <div className="chat-messages">
            {messages.map((msg, index) => (
            <div key={index} className={`message ${msg.sender}`}>
                <div className="sender-label">
                {msg.sender === 'user' ? '나' : '봇'}
                </div>
                <div className="message-content">
                {/* Gemini 응답은 마크다운으로 올 수 있으므로 ReactMarkdown을 사용하여 렌더링 */}
                {/* ReactMarkdown을 사용하려면 설치 및 import 해야 합니다. */}
                {/* {msg.sender === 'ai' ? <ReactMarkdown>{msg.text}</ReactMarkdown> : msg.text} */}
                {msg.text}
                </div>
            </div>
            ))}
            {isLoading && (
            <div className="message ai loading">
                <div className="sender-label">봇</div>
                <div className="message-content loading-indicator">
                ... 생각 중 ...
                </div>
            </div>
            )}
            {/* 스크롤 위치 지정 */}
            <div ref={messagesEndRef} />
        </div>

        <div className="chat-input-area">
            <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="메시지를 입력하세요..."
            disabled={isLoading}
            />
            <button 
            onClick={sendMessage}
            disabled={isLoading || input.trim() === ''}
            >
            전송
            </button>
        </div>
        </div>
    );
}