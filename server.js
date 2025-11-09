const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const cors = require('cors');
const { scanTop100, scanSymbol } = require('./scanner/analyzer');

const app = express();
app.use(cors());
app.use(express.json());

const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

// Store connected clients
const clients = new Set();

wss.on('connection', (ws) => {
    console.log('Client connected');
    clients.add(ws);

    ws.on('close', () => {
        console.log('Client disconnected');
        clients.delete(ws);
    });
    
    ws.send(JSON.stringify({ type: 'connection', message: 'Successfully connected to SMC Bot WebSocket' }));
});

// Function to broadcast messages to all connected clients
const broadcast = (data) => {
    const serializedData = JSON.stringify(data);
    clients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(serializedData);
        }
    });
};

// --- API Endpoints ---

// Endpoint to trigger a scan of the top 100 coins
app.post('/scan/all', async (req, res) => {
    console.log('Received request to scan all top 100 symbols.');
    res.status(202).json({ message: 'Full scan initiated. Signals will be broadcasted via WebSocket.' });
    
    // Non-blocking call to the scanner
    scanTop100((signal) => {
        broadcast({ type: 'new_signal', payload: signal });
    });
});

// Endpoint to scan a specific coin symbol
app.post('/scan/symbol', async (req, res) => {
    const { symbol } = req.body;
    if (!symbol) {
        return res.status(400).json({ error: 'Symbol is required' });
    }
    console.log(`Received request to scan symbol: ${symbol}`);
    res.status(202).json({ message: `Scan initiated for ${symbol}. Signals will be broadcasted.` });

    // Non-blocking call to the scanner
    scanSymbol(symbol.toUpperCase(), (signal) => {
        broadcast({ type: 'new_signal', payload: signal });
    });
});


// --- Server Initialization ---
const PORT = process.env.PORT || 8080;
server.listen(PORT, () => {
    console.log(`Server is running on http://localhost:${PORT}`);
    console.log(`WebSocket server is running on ws://localhost:${PORT}`);
});

// Export the broadcast function for use in other modules if needed
module.exports = { broadcast };
