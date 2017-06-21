import React from 'react';
import ReactDOM from 'react-dom';
import { createStore } from 'redux';
import { Provider } from 'react-redux';

import SJTest from 'sjtest';

import reducer from './reducers';
import MainDiv from './components/MainDiv/MainDiv';

const store = createStore(reducer);

ReactDOM.render(
	<Provider store={store}>
		<MainDiv />
	</Provider>,
	document.getElementById('mainDiv'));
