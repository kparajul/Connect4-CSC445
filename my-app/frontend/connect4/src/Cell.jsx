import React from 'react';

const Cell = ({ value, onClick }) => {
  return (
    <div className="cell" onClick={onClick}>
      {value && <div className={`disc ${value === 'R' ? 'red' : 'yellow'}`} />}
    </div>
  );
};

export default Cell;
