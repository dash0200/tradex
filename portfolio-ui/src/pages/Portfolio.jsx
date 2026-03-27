import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { portfolioApi, tradeUploadApi } from '../api';


  const SAMPLE_TRADE = {
    "tradeType": "BUY",
    "portfolioId": "PF1001",
    "brokerId": "BRK789",
    "symbol": "INFY",
    "quantity": 100,
    "price": 1450.75,
    "currency": "INR",
    "tradeDate": "2026-03-09",
    "exchange": "NSE",
    "clientId": "CLT456"
  };

function Portfolio() {
  const [portfolio, setPortfolio] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [uploadMsg, setUploadMsg] = useState('');
  const [uploadError, setUploadError] = useState('');
  const [jsonInput, setJsonInput] = useState(JSON.stringify(SAMPLE_TRADE, null, 2));
  const [parsedFields, setParsedFields] = useState(SAMPLE_TRADE);
  const [jsonParseError, setJsonParseError] = useState(false);
  const navigate = useNavigate();


  useEffect(() => {
    fetchPortfolio();
  }, []);

  // Sync: raw JSON textarea → parsed key-value fields
  const handleJsonChange = (text) => {
    setJsonInput(text);
    try {
      const parsed = JSON.parse(text);
      setParsedFields(parsed);
      setJsonParseError(false);
    } catch {
      setJsonParseError(true);
    }
  };

  // Sync: key-value form field change → raw JSON textarea
  const handleFieldChange = (key, value) => {
    const updated = { ...parsedFields };
    // Try to preserve number types
    if (!isNaN(value) && value.trim() !== '') {
      updated[key] = parseFloat(value);
    } else {
      updated[key] = value;
    }
    setParsedFields(updated);
    setJsonInput(JSON.stringify(updated, null, 2));
  };

  const fetchPortfolio = async () => {
    setLoading(true);
    try {
      const response = await portfolioApi.get('/api/portfolio');
      setPortfolio(response.data || []);
      setError('');
    } catch (err) {
      console.error("Portfolio fetch error:", err);
      if (err.response?.status === 401 || err.response?.status === 403) {
        handleLogout();
      } else {
        setError('Failed to fetch portfolio data.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    // navigate('/login');
    window.location.href = '/login'
  };

  const handleJsonSubmit = async () => {
    if (!jsonInput.trim()) {
      setUploadError('Please enter valid JSON data.');
      return;
    }

    let parsedData;
    try {
      parsedData = JSON.parse(jsonInput);
    } catch (err) {
      setUploadError('Invalid JSON format.');
      return;
    }

    setUploadError('');
    setUploadMsg('Uploading...');

    try {
      await tradeUploadApi.post('/api/files/upload/json', parsedData);
      setUploadMsg('Trade uploaded successfully!');
      setJsonInput('');
      setParsedFields(null);
      fetchPortfolio();
    } catch (err) {
      setUploadMsg('');
      const data = err.response?.data;
      if (data?.errors && Array.isArray(data.errors)) {
        const details = data.errors.map(e => `${e.field}: ${e.message}`).join('\n');
        setUploadError(`${data.message || 'Validation failed'}\n${details}`);
      } else {
        setUploadError(data?.message || 'Failed to upload trade data.');
      }
    }
  };

  const fieldLabelStyle = {
    fontSize: '0.75rem', color: '#94a3b8', textTransform: 'uppercase',
    letterSpacing: '0.05em', marginBottom: '0.25rem'
  };
  const fieldInputStyle = {
    width: '100%', background: 'rgba(15, 23, 42, 0.6)', border: '1px solid #334155',
    color: '#f8fafc', padding: '0.5rem 0.7rem', borderRadius: '6px', fontSize: '0.85rem',
    outline: 'none'
  };

  return (
    <div className="portfolio-container">
      <div className="portfolio-header">
        <h1>My Portfolio</h1>
        <button onClick={handleLogout} className="btn btn-danger">Logout</button>
      </div>

      <div className="dashboard-grid">
        <div className="panel">
          <h3>Holdings</h3>
          {error && <div className="error-message">{error}</div>}

          {loading ? (
            <div className="empty-state">Loading portfolio...</div>
          ) : portfolio.length === 0 ? (
            <div className="empty-state">
              <p>Your portfolio is currently empty.</p>
              <p>Upload a trade to see holdings here.</p>
            </div>
          ) : (
            <>
              <div className="portfolio-total" style={{
                textAlign: 'center', margin: '0 0 1.5rem', padding: '1.2rem',
                background: 'rgba(30, 41, 59, 0.5)', borderRadius: '12px',
                border: '1px dashed #475569'
              }}>
                <div style={{ fontSize: '0.85rem', color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.1em', marginBottom: '0.4rem' }}>
                  Total Portfolio Value
                </div>
                <div style={{ fontSize: '2rem', fontWeight: 700, color: '#10b981' }}>
                  ₹{portfolio.reduce((sum, item) => {
                    const val = item.totalValue !== undefined ? item.totalValue : ((item.quantity || 0) * (item.averagePrice || 0));
                    return sum + val;
                  }, 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </div>
              </div>

              <div className="portfolio-list">
                {portfolio.map((item, index) => {
                  const holdingValue = item.totalValue !== undefined ? item.totalValue : ((item.quantity || 0) * (item.averagePrice || 0));
                  return (
                    <div key={index} className="portfolio-item">
                      <div>
                        <div className="item-symbol">{item.symbol || 'SYM'}</div>
                        <div className="item-qty">{(item.quantity || 0).toLocaleString()} shares</div>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <div className="item-price" style={{ color: '#10b981', fontWeight: 600 }}>
                          ₹{holdingValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                        </div>
                        <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.2rem' }}>Holding Value</div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </>
          )}
        </div>

        <div className="panel">
          <h3>Upload Trade</h3>
          <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem', fontSize: '0.875rem' }}>
            Paste JSON or edit the fields below. Both views stay in sync.
          </p>

          {/* Key-Value Form View */}
          {parsedFields && typeof parsedFields === 'object' && !Array.isArray(parsedFields) && (
            <div style={{
              display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem',
              marginBottom: '1.5rem', padding: '1rem', background: 'rgba(15, 23, 42, 0.4)',
              borderRadius: '10px', border: '1px solid #334155'
            }}>
              <div style={{ gridColumn: '1 / -1', fontSize: '0.8rem', fontWeight: 600, color: '#818cf8', marginBottom: '0.25rem' }}>
                Form View
              </div>
              {Object.entries(parsedFields).map(([key, val]) => (
                <div key={key}>
                  <div style={fieldLabelStyle}>{key}</div>
                  <input
                    style={fieldInputStyle}
                    value={val}
                    onChange={(e) => handleFieldChange(key, e.target.value)}
                  />
                </div>
              ))}
            </div>
          )}

          {/* Raw JSON Textarea */}
          <div style={{ position: 'relative' }}>
            <div style={{ fontSize: '0.8rem', fontWeight: 600, color: '#818cf8', marginBottom: '0.5rem' }}>
              Raw JSON
            </div>
            <textarea
              className="form-control"
              style={{
                minHeight: '150px', marginBottom: '0.5rem', fontFamily: 'monospace',
                resize: 'vertical', borderColor: jsonParseError ? '#ef4444' : undefined
              }}
              placeholder='{"tradeType": "BUY", "symbol": "INFY", "quantity": 100, "price": 1450.75, ...}'
              value={jsonInput}
              onChange={(e) => handleJsonChange(e.target.value)}
            />
            {jsonParseError && jsonInput.trim() && (
              <div style={{ fontSize: '0.75rem', color: '#fca5a5', marginBottom: '0.5rem' }}>
                ⚠ Invalid JSON — form view will update when JSON is valid
              </div>
            )}
          </div>

          <button className="btn btn-primary" onClick={handleJsonSubmit} style={{ width: 'auto', padding: '0.5rem 1.5rem' }}>
            Submit Trade
          </button>

          {uploadError && <div className="error-message" style={{ marginTop: '1rem', whiteSpace: 'pre-line' }}>{uploadError}</div>}
          {uploadMsg && <div className="success-message">{uploadMsg}</div>}
        </div>
      </div>
    </div>
  );
}

export default Portfolio;

