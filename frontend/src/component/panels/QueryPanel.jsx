/** @jsxImportSource @emotion/react */
// import { css } from "@emotion/react";
import axios from 'axios';
import { useCallback, useState } from 'react';
import CheckBox from '../elements/CheckBox';

const QueryPanel = ({ data }) => {
  const [queryFile, setQueryFile] = useState();
  const [dereplicate, setDereplicate] = useState(true);
  const [allowHeteroHeteroBonds, setAllowHeteroHeteroBonds] = useState(false);

  const handleOnChange = useCallback((e) => {
    e.stopPropagation();
    setQueryFile(e.target.files[0]);
  }, []);

  const handleOnClick = useCallback(
    async (e) => {
      e.stopPropagation();

      // const blob = new Blob([queryFile]);
      // const json = await blob.text().then((res) => res);
      const result = await axios({
        method: 'POST',
        url: 'http://localhost:8081/webcase-core/core',
        params: {
          dereplicate,
          allowHeteroHeteroBonds,
        },
        data, //: json,
        headers: {
          'Content-Type': 'application/json',
        },
      });
      console.log(result);
    },
    [allowHeteroHeteroBonds, data, dereplicate],
  );

  const onChangeDereplicate = useCallback((e) => {
    e.stopPropagation();
    setDereplicate(e.target.checked);
  }, []);

  const onChangeAllowHeteroHeteroBonds = useCallback((e) => {
    e.stopPropagation();
    setAllowHeteroHeteroBonds(e.target.checked);
  }, []);

  return (
    <div>
      <p>QueryPanel!!!</p>
      <form>
        <CheckBox
          isChecked={dereplicate}
          handleOnChange={onChangeDereplicate}
          title="Dereplication"
        />
        <CheckBox
          isChecked={allowHeteroHeteroBonds}
          handleOnChange={onChangeAllowHeteroHeteroBonds}
          title="Allow Hetero-Hetero Bonds"
        />
        <input type="file" onChange={handleOnChange} />
        <button type="submit" onClick={handleOnClick}>
          Submit
        </button>
      </form>
    </div>
  );
};

export default QueryPanel;
