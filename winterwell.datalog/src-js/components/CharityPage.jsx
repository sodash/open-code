// @Flow
import React from 'react';
import _ from 'lodash';
import {assert} from 'sjtest';
import {yessy} from 'wwutils';
import { Panel, Image, Well, Label } from 'react-bootstrap';

import ServerIO from '../plumbing/ServerIO';
import printer from '../utils/printer';
import C from '../C';
import NGO from '../data/charity/NGO';
import Project from '../data/charity/Project';
import Misc from './Misc';
import Login from 'you-again';
import DonationForm from './DonationForm';

class CharityPage extends React.Component {

	constructor(...params) {
		super(...params);
		this.state = {
		};
	}

	componentWillMount() {
		// fetch
		let cid = this.props.charityId;
		ServerIO.getCharity(cid)
		.then(function(result) {
			let charity = result.cargo;
			assert(NGO.isa(charity), charity);
			this.setState({charity: charity});
		}.bind(this));
	}

	render() {
		const charity = this.state.charity;
		if ( ! charity) {
			return <Misc.Loading />;
		}
		let allprojects = charity.projects;
		// split out overall vs projects
		const overalls = _.filter(allprojects, p => Project.name(p) === 'overall');
		let projectProjects = _.filter(allprojects, p => Project.name(p) !== 'overall');
		// latest only
		const overall = Project.getLatest(overalls);	
		const year = overall? overall.year : 0;
		let oldProjects = _.filter(projectProjects, p => p.year !== overall.year);
		let currentProjects = _.filter(projectProjects, p => p.year === overall.year);
		// sort by cost, biggest first
		currentProjects = _.sortBy(currentProjects, p => {
			let annualCost = _.find(p.inputs, pi => pi.name==='annualCosts');
			return annualCost? -annualCost.value : 0;
		});

		// TODO not if there's only overall		
		const projectsDiv = yessy(currentProjects)? <div><h2>Projects</h2><ProjectList projects={currentProjects} charity={charity} /></div> : null;
		const oldProjectsDiv = yessy(oldProjects)? <div><h2>Old Projects</h2><ProjectList projects={oldProjects} charity={charity} /></div> : null;
		const overallDiv = <ProjectPanel project={overall} charity={charity} />;		
		const project = NGO.getProject(charity);
		// put it together
		return (
			<div className='page CharityPage'>
				<CharityProfile charity={charity} />
				<div className='upper-padding col-md-12 charity-donation-div'>
					<p className='donateto'>Donate to { charity.name }</p>
					<div className='col-md-12 charity-donation-form'>
						<DonationForm charity={charity} project={project} />
					</div>
				</div>
				<div className='charity-statistics-div'>
					{overallDiv}
					{projectsDiv}
					{oldProjectsDiv}
				</div>
			</div>
		);
	}
} // ./CharityPage


const CharityProfile = ({charity}) => {
	const tags = charity.tags && (
		<div>
			<h4>Tags</h4>
			{ charity.tags.split('&').map((tag) => (
				<span key={tag}><Label>{tag.trim()}</Label> </span>
			)) }
		</div>
	);
	const turnover = charity.turnover && (
		<p>
			Turnover: { charity.turnover }
		</p>
	);
	const employees = charity.employees && (
		<p>
			Employees: { charity.employees }
		</p>
	);
	const website = charity.url && (
		<p>
			Website: <a href={charity.url} target='_blank' rel="noopener noreferrer">{charity.url}</a>
		</p>
	);
	return (<div className='CharityProfile-div'>
				<EditLink charity={charity} />
				<h4 className='CharityProfile'>Charity Profile</h4>
				<div className='col-md-12'>
					<div className='col-md-2 charity-logo-div'>
						<Image src={charity.logo} responsive thumbnail className="charity-logo" />
					</div>
					<div className='col-md-7 charity-name-div'>
						<h2>{charity.name}</h2>
						<br />
						<a href={'/#charity/'+charity['@id']}>{charity.id}</a>
						<p dangerouslySetInnerHTML={{ __html: printer.textToHtml(charity.description) }} />
					</div>
					<div className='col-md-3'>
						<ProjectImage images={charity.images} />
					</div>
					<div className='col-md-12 charity-data-div'>
						{ tags }
						{ turnover }
						{ employees }
						{ website }
					</div>
				</div>
	</div>);
};


// TODO only for registered editors!!!
const EditLink = ({charity}) => Login.isLoggedIn()? <div className='pull-right'><a href={'#edit?charityId='+charity['@id']}>edit</a></div> : null;

const ProjectList = ({projects, charity}) => {
	if ( ! projects) return <div />;

	const renderedProjects = projects
		.map(p => <ProjectPanel key={p.name+'-'+p.year} project={p} charity={charity} />);

	if (renderedProjects.length === 0) return <div />;

	return (
		<div>
			{ renderedProjects }
		</div>
	);
};

const COSTNAMES = {
	annualCosts: "Annual costs",
	fundraisingCosts: "Fundraising costs",
	tradingCosts: "Trading costs",
	incomeFromBeneficiaries: "Income from beneficiaries"
};

const ProjectPanel = ({project}) => {
	const outputs = project.outputs || [];
	const inputs = project.inputs || [];
	return (
		<div className='col-md-12 ProjectPanel'>
			<div className='charity-project-title-div'>
				<p className='project-name'>{project.name}: {project.year}</p>
			</div>
			<div className='charity-project-div'>
				<div className='image-and-story-div'>
					<div className='col-md-2 project-image'>
						<ProjectImage images={project.images} />
					</div>
					<div className='col-md-offset-1 col-md-7 project-story'>
						<p className='project-story-text' dangerouslySetInnerHTML={{ __html: printer.textToHtml(project.stories) }} />
					</div>
				</div>
				<div className='upper-margin col-md-offset-2 col-md-8 inputs-outputs'>
					<div className='col-md-6 inputs'><h4>Inputs</h4>
						{inputs.map(output => <div key={"in_"+output.name}>{COSTNAMES[output.name] || output.name}: <Misc.Money precision={false} amount={output} /></div>)}
					</div>
					<div className='col-md-6 outputs'><h4>Outputs</h4>
						{outputs.map(output => <div key={"out_"+output.name}>{output.name}: {printer.prettyNumber(output.number)}</div>)}
					</div>
				</div>
				<div className='upper-padding'>
					<div className='col-md-offset-2 col-md-8 comments'>
						{project.adjustmentComment}
						{project.analysisComment}
					</div>
				</div>
				<Citations thing={project} />
			</div>
		</div>
	);
};

const ProjectImage = ({images}) => {
	if ( ! yessy(images)) return null;
	let image = _.isArray(images)? images[0] : images;
	return <div><center><img src={image} className='project-image'/></center></div>;
};

const Citations = ({thing}) => {
	let dsrc = thing['data-src'];
	if ( ! dsrc) return null;
	if (_.isArray(dsrc)) {
		if (dsrc.length > 1) {
			return <div>Sources:<ul>{dsrc.map(ds => <Citation citation={ds} />)}</ul></div>;
		}
		dsrc = dsrc[0];
	}
	return <div className='upper-padding col-md-offset-2 col-md-8'>Source: <Citation citation={dsrc} /></div>;	
};
const Citation = ({citation}) => {
	if (_.isString(citation)) return <p>{citation}</p>;
	return <a className='citation-url' href={citation.url}>{citation.name || citation.url}</a>;
};

export default CharityPage;
