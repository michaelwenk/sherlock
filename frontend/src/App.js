import './App.css';
import QueryPanel from './component/panels/QueryPanel';
import NMRDisplayer from 'nmr-displayer';
import { useCallback, useState } from 'react';

const preferences = {};

function App() {
  const [data, setData] = useState();

  const handleOnDataChange = useCallback((data) => {
    console.log(data);
    setData(data);
  }, []);

  return (
    <div className="App">
      <header className="App-header">
        <p>Welcome to WebCASE !!!</p>
        <NMRDisplayer
          preferences={preferences}
          onDataChange={handleOnDataChange}
        />
        <p>-----</p>
        <QueryPanel data={data} />
      </header>
    </div>
  );
}

export default App;
