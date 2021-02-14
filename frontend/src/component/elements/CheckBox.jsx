/** @jsxImportSource @emotion/react */
// import { css } from "@emotion/react";

const CheckBox = ({ isChecked, handleOnChange, title, css }) => {
  return (
    <div css={css}>
      <label>
        <input type="checkbox" checked={isChecked} onChange={handleOnChange} />
        <span>{title}</span>
      </label>
    </div>
  );
};

export default CheckBox;
