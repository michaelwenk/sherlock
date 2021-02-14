import './App.css';
import QueryPanel from './component/panels/QueryPanel';
import NMRDisplayer from 'nmr-displayer';

function App() {
  return (
    <div className="App">
      <header className="App-header">
        <p>Welcome to WebCASE !!!</p>
        <NMRDisplayer />
        <p>-----</p>
        <QueryPanel />
      </header>
    </div>
  );
}

export default App;
