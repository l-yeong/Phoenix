// import { StrictMode } from 'react'
// import { createRoot } from 'react-dom/client'
// import './index.css'
// import App from './App.jsx'
//
// createRoot(document.getElementById('root')).render(
//   <StrictMode>
//     <App />
//   </StrictMode>,
// )
import { createRoot } from 'react-dom/client'

//2. index.html(SPA) 에서 root 마크업 가져오기
const root = document.querySelector('#root');

//3. 가져온 root 마크업을  createRoot 함수의 매개변수로 전달한다
const create = createRoot(root);

import TicketQR from './features/tickets/TicketQR';
create.render(
      <TicketQR />
);