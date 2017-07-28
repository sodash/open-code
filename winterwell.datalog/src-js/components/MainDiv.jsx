import React, { Component } from 'react';
import Login from 'you-again';
import { assert } from 'sjtest';
import { getUrlVars } from 'wwutils';
import _ from 'lodash';

// Plumbing
import DataStore from '../plumbing/DataStore';

// Templates
import MessageBar from './MessageBar';
import NavBar from './NavBar';
import LoginWidget from './LoginWidget';
// Pages
import DashboardPage from './DashboardPage';
import AccountPage from './AccountPage';

// Actions


const PAGES = {
	dashboard: DashboardPage,
	account: AccountPage,
};

const DEFAULT_PAGE = 'dashboard';


/**
		Top-level: tabs
*/
class MainDiv extends Component {
	constructor(props) {
		super(props);
		this.state = this.decodeHash(window.location.href);
	}

	componentWillMount() {
		// redraw on change
		// _.debounce( this debounce made things worse ?! Also: debounce state updates cause horrible bugs with text inputs
		const updateReact = (mystate) => this.setState({}); //, 1000);
		DataStore.addListener(updateReact);

		Login.app = 'datalog';
		// Set up login watcher here, at the highest level		
		Login.change(() => {
			// ?? should we store and check for "Login was attempted" to guard this??
			if (Login.isLoggedIn()) {
				// close the login dialog on success
				DataStore.setShow('LoginWidget', false);
			} else {
				// poke React via DataStore (e.g. for Login.error)
				DataStore.update({});
			}
			this.setState({});
		});
	}

	componentDidMount() {
		window.addEventListener('hashchange', ({newURL}) => { this.hashChanged(newURL); });
	}

	componentWillUnmount() {
		window.removeEventListener('hashchange', ({newURL}) => { this.hashChanged(newURL); });
	}

	hashChanged(newURL) {
		this.setState(
			this.decodeHash(newURL)
		);
	}

	decodeHash(url) {
		const hashIndex = url.indexOf('#');
		const hash = (hashIndex >= 0) ? url.slice(hashIndex + 1) : '';
		let page = hash.split('?')[0] || DEFAULT_PAGE;
		const pageProps = getUrlVars(hash);
		// peel off eg publisher/myblog
		const pageBits = page.split('/');
		page = pageBits[0];
		if (pageBits.length > 1) {
			// store in DataStore focus
			const ptype = toTitleCase(page); // hack publisher -> Publisher
			DataStore.setValue(['focus', ptype], pageBits[1]);
		}		
		return { page, pageProps };
	}

	render() {
		const { page, pageProps } = this.state;
		assert(page, this.props);
		let Page = PAGES[page];		
		assert(Page, (page, PAGES));

		return (
			<div>
				<NavBar page={page} />
				<div className="container avoid-navbar">
					<MessageBar />
					<div id={page}>
						<Page {...pageProps} />
					</div>
				</div>
				<LoginWidget logo='sogive' title='Welcome to SoGive' />
			</div>
		);
	}
}

export default MainDiv;
