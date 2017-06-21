/** Data model functions for the NGO data-type */

import _ from 'lodash';
import {isa} from '../DataClass';
import {assert} from 'sjtest';
import Project from './Project';

const NGO = {};
export default NGO;

NGO.isa = (ngo) => isa(ngo, 'NGO');
NGO.assIsa = (ngo) => assert(NGO.isa(ngo));
NGO.name = (ngo) => isa(ngo, 'NGO') && ngo.name;
NGO.id = (ngo) => isa(ngo, 'NGO') && ngo['@id']; // thing.org id field
NGO.description = (ngo) => isa(ngo, 'NGO') && ngo.description;

NGO.getProject = (ngo) => {
	NGO.isa(ngo);
	const { projects } = ngo;

	// Representative and ready for use?
	const repProjects = _.filter(projects, (p) => (p.isRep && p.ready));

	// Get most recent, if more than one
	let repProject = repProjects.reduce((best, current) => {
		if ( ! current) return best;
		if ( ! best) return current;
		return best.year > current.year ? best : current;
	}, null);

	// ...or fall back.
	if ( ! repProject) {
		repProject = _.find(ngo.projects, p => p.name === Project.overall);
	}

	if ( ! repProject) {
		repProject = ngo.projects && ngo.projects[0];
	}

	return repProject;
};

NGO.noPublicDonations = (ngo) => NGO.isa(ngo) && ngo.noPublicDonations;

NGO.getImpacts = function(project) {

};
