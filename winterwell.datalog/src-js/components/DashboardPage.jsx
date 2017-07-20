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
		let breakdowns = ['time', 'evt', 'domain'];

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
		let byDomainData = pivot(mydata, "'by_domain' -> 'buckets' -> bi -> {key, doc_count}", "key -> doc_count");		

		// display...
		return (
			<div className="page DashboardPage">
				<h2>My Dashboard</h2>
				
				<p>One month of data, in per-hour segments. Near realtime: Does NOT include the most recent 15 minutes.</p>

				<FiltersWidget />

				<ChartWidget title='Tags' dataFromLabel={tdata2} />

				<ChartWidget title='Events' dataFromLabel={evtdata2} />

				<DashboardWidget title="By Domain (Publisher) -- (summing all events!)">
					<BreakdownWidget data={byDomainData} />
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


const BreakdownWidget = ({data}) => {
	let keys = Object.keys(data).sort( (k1, k2) => data[k1] > data[k2] );
	let list = keys.map(k => <li key={'breakdown-'+k}>{k}: {data[k]}</li>);
	return <ol>{list}</ol>;
};

const FiltersWidget = () => {
	let path = ['widget', 'Dashboard', 'filters.editor'];
	let filters = DataStore.getValue(path);
	if ( ! filters) {
		// read unedited valued off the url
		filters = {
			dataspace: DataStore.getUrlValue('dataspace'),
			q: DataStore.getUrlValue('q'),
		};
		// and set in the editor (without an update)
		DataStore.setValue(path, filters, false);
	}
	// click
	const setFilters = () => {
		DataStore.setUrlValue('dataspace', filters.dataspace);
		DataStore.setUrlValue('q', filters.q);
	};
		// <Misc.PropControl path={path} item={filters} prop='events' label='events' />
		// <Misc.PropControl path={path} item={filters} prop='publisher' label='Publisher' />
	return (<div className='well'>
		<Misc.PropControl path={path} item={filters} prop='dataspace' label='Dataspace e.g. "gl" or "default"' />
		<Misc.PropControl path={path} item={filters} prop='q' label='Query' />
		<button className='btn btn-default' onClick={setFilters}>Load Data</button>
	</div>);
};

export default DashboardPage;
