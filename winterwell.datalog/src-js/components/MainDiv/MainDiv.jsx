import React, { Component } from 'react';
import { connect } from 'react-redux';
import Login from 'you-again';
import { assert } from 'sjtest';
import { getUrlVars } from 'wwutils';
import _ from 'lodash';

// Plumbing
import DataStore from '../../plumbing/DataStore';

// Templates
import MessageBar from '../MessageBar';
import NavBar from '../NavBar';
import LoginWidget from '../LoginWidget/LoginWidget';
// Pages
import DashboardPage from '../DashboardPage';
import SearchPage from '../SearchPage';
import AccountPage from '../AccountPage';
import DonateToCampaignPage from '../DonateToCampaignPage';
import CharityPage from '../CharityPage';
import EditCharityPage from '../editor/EditCharityPage';
import EditorDashboardPage from '../editor/EditorDashboardPage';

// Actions


const PAGES = {
	search: SearchPage,
	dashboard: DashboardPage,
	editordashboard: EditorDashboardPage,
	account: AccountPage,
	charity: CharityPage,
	campaign: DonateToCampaignPage,
	edit: EditCharityPage
};

const DEFAULT_PAGE = 'search';


/**
		Top-level: SoGive tabs
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

		Login.app = 'sogive';
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
