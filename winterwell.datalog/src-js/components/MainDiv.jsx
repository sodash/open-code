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
		// _.debounce( this debounce made things worse ?!
		const updateReact = (mystate) => this.setState({}); //, 1000);
		DataStore.addListener(updateReact);

		Login.app = 'datalog';
		// Set up login watcher here, at the highest level		
		Login.change(() => {
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
		const page = hash.split('?')[0] || DEFAULT_PAGE;
		const pageProps = getUrlVars(hash);
		return { page, pageProps };
	}

	render() {
		const { page, pageProps } = this.state;
		assert(page, this.props);
		const Page = PAGES[page];
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

// /* connect() with no second argument (normally mapDispatchToProps)
//  * makes dispatch itself available as a prop of MainDiv
//  */
// export default connect()(MainDiv);
export default MainDiv;
