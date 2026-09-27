import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { App } from './App';
import { ZooProvider } from './context/ZooContext';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <ZooProvider>
        <App />
      </ZooProvider>
    </BrowserRouter>
  </React.StrictMode>
);
