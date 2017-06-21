const initialState = {};

const initialPerCharityState = {
	amount: 10,
	giftAid: false,
	giftAidTaxpayer: false,
	giftAidOwnMoney: false,
	giftAidNoCompensation: false,
	name: '',
	address: '',
	postcode: '',
	ready: true,
	pending: false,
	complete: false,
};

const checkDonationForm = (state, action) => {
	const { charityId, field, value } = action;

	const charityState = {
		...initialPerCharityState,
		...state[charityId],
		[field]: value,
	};

	charityState.ready = (
		// have to be donating something
		(
			charityState.amount &&
			charityState.amount > 0
		) &&
		// if gift-aiding, must have checked all confirmations & supplied name/address
		(
			!charityState.giftAid ||
			(
				charityState.giftAidTaxpayer &&
				charityState.giftAidOwnMoney &&
				charityState.giftAidNoCompensation &&
				(charityState.name.trim().length > 0) &&
				(charityState.address.trim().length > 0) &&
				(charityState.postcode.trim().length > 0)
			)
		)
	);

	const newState = {
		...state,
		[charityId]: charityState,
	};

	return newState;
};

const donationFormReducer = (state = initialState, action) => {
	switch (action.type) {
	case 'DONATION_FORM_INIT':
		return {
			...state,
			[action.charityId]: initialPerCharityState,
		};
	case 'DONATION_FORM_UPDATE':
		return checkDonationForm(state, action);
	case 'DONATION_REQUESTED':
		return {
			...state,
			[action.charityId]: {
				...(state.charityId),
				pending: true,
			},
		};
	case 'DONATION_RESPONSE':
		return {
			...state,
			[action.charityId]: {
				...(state.charityId),
				pending: false,
				complete: true,
			},
		};
	default:
		return state;
	}
};


export default donationFormReducer;
