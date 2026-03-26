import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Register from './pages/Register';
import Portfolio from './pages/Portfolio';

function App() {
  // We check the token dynamically on route render so that the component doesn't
  // need to be re-mounted when the user logs in from Login.jsx.
  return (
    <Router>
      <Routes>
        <Route path="/login" element={localStorage.getItem('token') ? <Navigate to="/portfolio" /> : <Login />} />
        <Route path="/register" element={localStorage.getItem('token') ? <Navigate to="/portfolio" /> : <Register />} />
        <Route
          path="/portfolio"
          element={localStorage.getItem('token') ? <Portfolio /> : <Navigate to="/login" />}
        />
        <Route path="/" element={<Navigate to="/portfolio" />} />
      </Routes>
    </Router>
  );
}

export default App;
