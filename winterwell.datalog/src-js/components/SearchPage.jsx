import React from 'react';
import _ from 'lodash';
import { assert } from 'sjtest';
import {Button, Form, FormGroup, FormControl, Glyphicon, ControlLabel, Media, MediaLeft, MediaBody, MediaHeading, Well, InputGroup, InputGroupButton} from 'react-bootstrap';
import {uid, yessy} from 'wwutils';

import ServerIO from '../plumbing/ServerIO';
import DataStore from '../plumbing/DataStore';
import Misc from './Misc.jsx';


export default class SearchPage extends React.Component {

	constructor(...params) {
		super(...params);
		this.state = {
			results: []
		};
	}

	setResults(results, total) {
		assert(_.isArray(results));
		this.setState({
			results: results,
			total: total
		});
	}

	render() {
		// query comes from the url, via MainDiv. The form uses a state not a prop, so it can change.
		const { q } = this.props;
		return (
			<div className='page SearchPage'>
				<div className='col-md-12'>
					<SearchForm query={q} setResults={this.setResults.bind(this)}/>
				</div>
				<div className='col-md-12'>
					<SearchResults results={this.state.results} query={q} />
				</div>
				<div className='col-md-10'>
					<FeaturedCharities />
				</div>
			</div>
		);
	}
}

const FeaturedCharities = () => null;
/*<div> class='featured-charities''
					<p className='featured-charities-header'>
						Featured Charities
					<FeaturedCharities results={ { TODO a render-er for top-charities or a featured charity. When a search returns results, this should convert into a sidebar, or at least become hidden, and a sidebar should be generated. } }/>
					</p> */


class SearchForm extends React.Component {
	constructor(...params) {
		super(...params);
		this.state = {
			q: this.props.query,
		};
	}

	componentDidMount() {
		if (this.state.q) {
			this.search(this.state.q);
		}
	}

	onChange(name, e) {
		e.preventDefault();
		let newValue = e.target.value;
		let newState = {};
		newState[name] = newValue;
		this.setState(newState);
	}

	onSubmit(e) {
		e.preventDefault();
		console.warn("submit",this.state);
		this.search(this.state.q || '');
	}

	search(query) {
		// Put search query in URL so it's bookmarkable / shareable
		const newHash = "#search?q="+escape(query);
		if (window.history.pushState) {
			window.history.pushState(null, null, newHash);
		} else {
			window.location.hash = newHash;
		}

		DataStore.setValue(['widget', 'Search', 'loading'], true);

		ServerIO.search(query)
		.then(function(res) {
			console.warn(res);
			let charities = res.cargo.hits;
			let total = res.cargo.total;
			DataStore.setValue(['widget', 'Search', 'loading'], false);
			// DataStore.setValue([], { TODO
			// 	charities: charities,
			// 	total: total
			// });
			this.props.setResults(charities, total);
		}.bind(this));
	}

	showAll(e) {
		e.preventDefault();
		this.setState({q: ''});
		this.search('');
	}

	render() {
		return (
			<Form onSubmit={(event) => { this.onSubmit(event); }} >
				<FormGroup className='' bsSize='lg' controlId="formq">
					<InputGroup bsSize='lg'>
						<FormControl
							className='sogive-search-box'
							type="search"
							value={this.state.q || ''}
							placeholder="Keyword search"
							onChange={(e) => this.onChange('q', e)}
						/>
						<InputGroup.Addon className='sogive-search-box' onClick={(e) => this.onSubmit(e)}>
							<Glyphicon glyph="search" />
						</InputGroup.Addon>
					</InputGroup>
				</FormGroup>
				<div className='pull-right'>
					<Button onClick={this.showAll.bind(this)} className="btn-showall" bsSize='sm'>
						Show All
					</Button>
				</div>
			</Form>
		);
	} // ./render
} //./SearchForm


const SearchResults = ({ results, query }) => {
	if ( ! results) results = [];
	const ready = _.filter(results, c => _.find(c.projects, 'ready') );
	const unready = _.filter(results, r => ready.indexOf(r) === -1);
	const hu = unready.length? <div className='unready-results col-md-10'><h3>Analysis in progress</h3>SoGive is working to collect data and model the impact of every UK charity -- all 200,000.</div> : null;
	return (
		<div className='SearchResults'>
			<SearchResultsNum results={results} query={query} />
			{ _.map(ready, item => <SearchResult key={uid()} item={item} />) }
			{hu}
			{ _.map(unready, item => <SearchResult key={uid()} item={item} />) }
		</div>);
}; //./SearchResults

const SearchResultsNum = ({results, query}) => {
	let loading = DataStore.getValue('widget', 'Search', 'loading');
	if (loading) return <div className='num-results'><Misc.Loading/></div>;
	if (results.length || query) return <div className='num-results'>{results.length} results found</div>;
	return null;
};

const SearchResult = ({ item }) => (
	<div className='SearchResult col-md-10' >
		<Media>
			<a href={`#charity?charityId=${item['@id']}`}>
				<Media.Left>
					{item.logo? <img className='charity-logo' src={item.logo} alt={`Logo for ${item.name}`} /> : null}
				</Media.Left>
				<Media.Body>
					<Media.Heading>{item.name}</Media.Heading>
					<p>{item.description}</p>
					<Misc.ImpactDesc unitImpact={item.unitRepImpact} amount={10} />
				</Media.Body>
			</a>
		</Media>
	</div>
); //./SearchResult