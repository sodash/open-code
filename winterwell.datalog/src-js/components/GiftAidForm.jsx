// @Flow
import React from 'react';
import { FormGroup, FormControl, Checkbox } from 'react-bootstrap';


const GiftAidForm = ({
	handleChange,
	giftAid,
	giftAidTaxpayer,
	giftAidOwnMoney,
	giftAidNoCompensation,
	name,
	address,
	postcode,
	giftAidAddressConsent,
}) => {
	// Gift Aiding? Check all these!
	const giftAidChecks = giftAid ? (
		<FormGroup>
			<p>Please tick all the following to confirm your donation is eligible for Gift Aid.</p>
			<Checkbox checked={giftAidTaxpayer} onChange={(event) => { handleChange('giftAidTaxpayer', event.target.checked); }}>
				I confirm that I am a UK taxpayer and I understand that if I pay less Income Tax and/or Capital Gains Tax in the current tax year than the amount of Gift Aid claimed on all my donations it is my responsibility to pay the difference.
			</Checkbox>
			<Checkbox checked={giftAidOwnMoney} onChange={(event) => { handleChange('giftAidOwnMoney', event.target.checked); }}>
				This is my own money. I am not paying in donations made by a third party e.g. money collected at an event, in the pub, a company donation or a donation from a friend or family member.
			</Checkbox>
			<Checkbox checked={giftAidNoCompensation} onChange={(event) => { handleChange('giftAidNoCompensation', event.target.checked); }}>
				I am not receiving anything in return for my donation e.g. book, auction prize, ticket to an event, or donating as part of a sweepstake, raffle or lottery.
			</Checkbox>
			<label htmlFor="name">Name</label>
			<FormControl
				type="text"
				name="name"
				placeholder="Enter your name"
				onChange={({ target }) => { handleChange('name', target.value); }}
				value={name}
			/>
			<label htmlFor="address">Address</label>
			<FormControl
				type="text"
				name="address"
				placeholder="Enter your address"
				onChange={({ target }) => { handleChange('address', target.value); }}
				value={address}
			/>
			<label htmlFor="postcode">Postcode</label>
			<FormControl
				type="text"
				name="postcode"
				placeholder="Enter your postcode"
				onChange={({ target }) => { handleChange('postcode', target.value); }}
				value={postcode}
			/>
			<small>I understand that my name and address may be shared with the charity for processing Gift Aid.</small>
		</FormGroup>
	) : '';

	return (
		<div className='upper-margin col-md-12 giftAid'>
			<Checkbox className="well no-padding" checked={giftAid} onChange={(event) => handleChange('giftAid', event.target.checked)}>
				Yes, add Gift Aid &nbsp;
				<small><a target='_blank' href='https://www.cafonline.org/my-personal-giving/plan-your-giving/individual-giving-account/how-does-it-work/gift-aid'>
					Find out more about Gift Aid
				</a></small>
			</Checkbox>
			{ giftAidChecks }
		</div>
	);
};

export default GiftAidForm;
