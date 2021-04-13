import React from 'react';
import ReactDOM from 'react-dom';

// Import root LESS file so webpack finds & renders it out to main.css
import '../style/main.less';
import PropControls from './base/components/PropControls';
import MainDiv from './components/MainDiv';

let dummy = PropControls; // protect the unused PropControls import


ReactDOM.render(
	<MainDiv />,
	document.getElementById('mainDiv')
	);
