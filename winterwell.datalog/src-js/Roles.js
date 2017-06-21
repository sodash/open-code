
import Login from 'you-again';
import DataStore from './plumbing/DataStore';
import {assMatch} from 'sjtest';

/**
 * Can the current use do this?
 */
const iCan = (capability) => {
	assMatch(capability, String);
	let roleShare = 'can:'+capability;
	let shared = DataStore.getValue('misc', 'shares', roleShare);
	if (shared===undefined) {
		let req = Login.checkShare(roleShare);
		req.then(function(res) {
			let yehorneh = res.success;
			DataStore.setValue(['misc', 'shares', roleShare], yehorneh);
		});
	}
	return shared;
};

const Roles = {
	iCan
};

export default Roles;
