import React from 'react';
import { assert, assMatch } from 'sjtest';
import Login from 'you-again';
import _ from 'lodash';
import { XId } from 'wwutils';
import pivot from 'data-pivot';

import printer from '../utils/printer';
// import C from '../C';
import ServerIO from '../plumbing/ServerIO';
import DataStore from '../plumbing/DataStore';
// import ChartWidget from './ChartWidget';
import Misc from './Misc';
import ChartWidget from './ChartWidget';
import SearchQuery from '../searchquery';

class DashboardPage extends React.Component {

	render() {
		let user = Login.getUser();
		if ( ! user) {
			return (
				<div className="page DashboardPage">
					<h2>My Dashboard</h2>
					<a href='#' onClick={(e) => DataStore.setShow('LoginWidget', true)}>Login or register</a>.
				</div>
			);
		}

		const dataspace = DataStore.getUrlValue('dataspace');
		const query = DataStore.getUrlValue('q');
		let filters = {dataspace, query};
		let breakdowns = ['time', 'evt', 'host'];

		let dspec = JSON.stringify(filters)+" "+JSON.stringify(breakdowns);
		const dpath = ['widget', 'Dashboard', dspec];		
		let mydata = DataStore.getValue(dpath);
		console.log('dpath', dpath, mydata);
		if ( ! mydata) {
			// Where does ad activity data go??
			// dataspaces: trk for evt.pxl, goodloop for evt.viewable, adview, click, close, open, opened
			// see https://github.com/good-loop/doc/wiki/Canonical-Terminology-for-Logging-Good-Loop-Events
			ServerIO.getData(filters, breakdowns)
			.then((res) => {
				console.warn("yeh", dpath, res);
				let mydata2 = res.cargo;
				DataStore.setValue(dpath, mydata2);
			});
			return (
				<div className="page DashboardPage">
					<h2>My Dashboard</h2>
					<h3>Fetching your data...</h3>
					<Misc.Loading />
				</div>
			);
		}

		// pivot the data from ES output to chart.js format
		let evtdata = pivot(mydata, "'by_evt' -> 'buckets' -> bi -> {key, 'by_time' -> 'buckets' -> bi2 -> bucket}", 
						"key -> bi2 -> bucket");
		let evtdata2 = pivot(evtdata, 	"key -> bi2 -> {key_as_string, doc_count}", 
										"key -> key_as_string -> doc_count");

		// TODO pull out sub-data on myCount -> sum
		// let tdata = pivot(mydata, "'byTag' -> 'buckets' -> bi -> {key, 'byTime' -> 'buckets' -> bi2 -> bucket}", 
		// 				"key -> bucket");
		let tdata = pivot(mydata, "'by_tag' -> 'buckets' -> bi -> {key, 'by_time' -> 'buckets' -> bi2 -> bucket}", 
						"key -> bi2 -> bucket");
		let tdata2 = pivot(tdata, "key -> bi2 -> {key_as_string, 'myCount' -> 'avg' -> avg}", 'key -> key_as_string -> avg');

		// // this isn't working?!
		// let xydata = pivot(cdata, "key -> bi -> {doc_count, key_as_string}", "key -> {'x' -> key_as_string, 'y' -> doc_count}");
		// // debug
		window.pivot = pivot;
		window.mydata = mydata;
		console.warn("pivot", "cdata", evtdata2, 'from', mydata, 'tdata', tdata, 'tdata2', tdata2);

		// breakdown data
		let byDomainData = pivot(mydata, "'by_host' -> 'buckets' -> bi -> {key, doc_count}", "key -> doc_count");		

		// display...
		return (
			<div className="page DashboardPage">
				<h2>My Dashboard</h2>
				
				<p>One month of data, in per-hour segments. Near realtime: Does NOT include the most recent 15 minutes.</p>

				<FiltersWidget />

				<ChartWidget title='Tags' dataFromLabel={tdata2} />

				<ChartWidget title='Events' dataFromLabel={evtdata2} />

				<DashboardWidget title="By Host (Publisher) -- (summing all events!)">
					<BreakdownWidget data={byDomainData} param='host' />
				</DashboardWidget>
			</div>
		);
	}
} // ./DashboardPage


const DashboardWidget = ({ children, iconClass, title }) =>
	<div className="panel panel-default">
		<div className="panel-heading">
			<h3 className="panel-title"><DashTitleIcon iconClass={iconClass} /> {title || ''}</h3>
		</div>
		<div className="panel-body">
			{children}
		</div>
	</div>;
// ./DashboardWidget

const DashTitleIcon = ({ iconClass }) =>
	<i className={iconClass} aria-hidden="true" />;

const filtersEditorPath = ['widget', 'Dashboard', 'filters.editor'];
/**
 * Set e.g. publisher:thesun as a query filter
 * @param {String} param 
 * @param {*} key 
 */
const addFilter = (param, key) => {
	// get the query
	let q = DataStore.getUrlValue('q') || '';	
	// modify it	
	let sq = new SearchQuery(q);
	let sq2 = sq.setProp(param, key);	
	q = sq2.query;
	// set it
	// ...clear the editor (without an update, cos its coming next)
	DataStore.setValue(filtersEditorPath, null);
	// set	
	DataStore.setUrlValue('q', q);
};

const BreakdownWidget = ({data, param}) => {
	let keys = Object.keys(data).sort( (k1, k2) => data[k1] > data[k2] );
	let sum = Object.values(data).reduce((s, v) => s+v, 0);
	let list = keys.map(k => <BreakdownLine key={k} k={k} param={param} value={data[k]} sum={sum} />);
	return <div><ol>{list}</ol><p>Total: {sum}</p></div>;
};
const BreakdownLine = ({param, k, value, sum}) => {
	return (<li><a href='' onClick={ (e) => { e.preventDefault(); e.stopPropagation(); addFilter(param, k); } } >
				{k}: {value}
			</a></li>);
};

const FiltersWidget = () => {
	let filters = DataStore.getValue(filtersEditorPath);
	if ( ! filters) {
		// read unedited valued off the url
		filters = {
			dataspace: DataStore.getUrlValue('dataspace'),
			q: DataStore.getUrlValue('q'),
		};
		// and set in the editor (without an update)
		DataStore.setValue(filtersEditorPath, filters, false);
	}
	// click
	const setFilters = () => {
		DataStore.setUrlValue('dataspace', filters.dataspace);
		DataStore.setUrlValue('q', filters.q);
	};
		// <Misc.PropControl path={path} item={filters} prop='events' label='events' />
		// <Misc.PropControl path={path} item={filters} prop='publisher' label='Publisher' />
	return (<div className='well'>
		<Misc.PropControl path={filtersEditorPath} item={filters} prop='dataspace' label='Dataspace e.g. "gl" or "default"' />
		<Misc.PropControl path={filtersEditorPath} item={filters} prop='q' label='Query' />
		<button className='btn btn-default' onClick={setFilters}>Load Data</button>
	</div>);
};

export default DashboardPage;
