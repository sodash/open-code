import ServerIO from '../plumbing/ServerIO';
import NGO from '../data/charity/NGO';

export const donate = (dispatch, charity, donationForm, stripeResponse) => {
	const donationParams = {
		action: 'donate',
		charityId: charity['@id'],
		currency: 'GBP',
		giftAid: donationForm.giftAid,
		impact: donationForm.impact,
		total100: Math.floor(donationForm.amount * 100),
		name: donationForm.name,
		address: donationForm.address,
		postcode: donationForm.postcode,
		stripeToken: stripeResponse.id,
		stripeTokenType: stripeResponse.type,
		stripeEmail: stripeResponse.email,
	};

	// Add impact to submitted data
	const project = NGO.getProject(charity);
	if (project && project.impacts) {
		const unitImpact = project.impacts[0];
		const impactPerUnitMoney = unitImpact.number / unitImpact.price.value;

		donationParams.impactCount = donationForm.amount * impactPerUnitMoney;
		donationParams.impactUnit = unitImpact.name;
	}


	ServerIO.donate(donationParams)
	.then(function(response) {
		// Action dispatched on server response
		dispatch({
			type: 'DONATION_RESPONSE',
			charityId: charity['@id'],
			response,
		});
	}, function(error) {
		dispatch({
			type: 'DONATION_RESPONSE',
			charityId: charity['@id'],
			error,
		});
	});
	// Action dispatched immediately
	return {
		type: 'DONATION_REQUESTED',
		charityId: charity['@id'],
		donationParams,
	};
};

export const updateForm = (charityId, field, value) => ({
	type: 'DONATION_FORM_UPDATE',
	charityId,
	field,
	value,
});

export const initDonationForm = (charityId) => ({
	type: 'DONATION_FORM_INIT',
	charityId,
});
