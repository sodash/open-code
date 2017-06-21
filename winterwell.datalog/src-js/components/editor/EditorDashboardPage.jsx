import React from 'react';
import { assert, assMatch } from 'sjtest';
import Login from 'you-again';
import _ from 'lodash';
import { XId } from 'wwutils';
import {Panel} from 'react-bootstrap';

import printer from '../../utils/printer';
// import C from '../C';
import ServerIO from '../../plumbing/ServerIO';
import DataStore from '../../plumbing/DataStore';
import ActionMan from '../../plumbing/ActionMan';
// import ChartWidget from './ChartWidget';
import Misc from '../Misc';


class EditorDashboardPage extends React.Component {
	render() {
		// display...
		return (
			<div className="page EditorDashboardPage">
				<h2>Editorial Dashboard</h2>
				<h3>In development...</h3>
				<AddCharityWidget />
				<AddEditorWidget />
			</div>
		);
	}
} // ./EditorDashboardPage


const AddCharityWidget = () => {
	let id = DataStore.getValue(['widget','AddCharityWidget','result','id']);
	if (id) {
		return <a href={'/#edit?charityId='+escape(id)}>Edit {DataStore.getValue(['widget','AddCharityWidget','form','name'])}</a>;
	}
	return (<div>
		<div className='alert alert-warning'>
			ALWAYS <a href='#search'>search</a> first to check the charity isn't already in the database. 
			Otherwise we will have ugly merge problems.</div>
		<Misc.PropControl prop='name' label='Name' path={['widget','AddCharityWidget', 'form']} />
		<button className='btn btn-warning' onClick={() => ActionMan.addCharity()}>Add</button>		
	</div>);
};

const doAddEditor = function() {
	let email = DataStore.appstate.widget.AddEditorWidget.form.email;
	if ( ! email) return;
	Login.shareThing('can:publish-edits', email);
	DataStore.setValue(['widget', 'AddEditorWidget', 'form'], {});
};

const AddEditorWidget = () => {
	Login.shareThing();
	return (<Panel header='<h2>Add a new Editor</h2>'>
		<p>Use this form to add someone to the editors team. Anyone can make edits, but only approved editors can publish them.</p>
		<Misc.PropControl prop='email' label='Email' path={['widget','AddEditorWidget', 'form']} />
		<button className='btn btn-warning' onClick={doAddEditor}>Add Them</button>
	</Panel>);
};

export default EditorDashboardPage;
