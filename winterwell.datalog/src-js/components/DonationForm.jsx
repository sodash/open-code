// @Flow
import React from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
import { assert } from 'sjtest';
import Login from 'you-again';
import StripeCheckout from 'react-stripe-checkout';
import { uid, XId } from 'wwutils';
import { Button, FormControl, InputGroup } from 'react-bootstrap';
import printer from '../utils/printer';
import NGO from '../data/charity/NGO';
import Misc from './Misc';
import GiftAidForm from './GiftAidForm';

import { donate, updateForm, initDonationForm } from './DonationForm-actions';


class DonationForm extends React.Component {

	render() {
		const {charity, donationForm, handleChange, initDonation, sendDonation } = this.props;
		let user = Login.getUser();

		// some charities dont accept donations
		if (charity.noPublicDonations) {
			return (<div className="DonationForm noPublicDonations">Sorry: This charity does not accept public donations.</div>);
		}

		if ( ! donationForm) {
			initDonation();
			return <div />;
		}

		assert(NGO.isa(charity), charity);

		// FIXME props.project set but bogus (empty bar year)!!
		let project = this.props.project || NGO.getProject(charity);
		assert(project, charity);

		// donated?
		if (donationForm.complete) {
			return (<ThankYouAndShare thanks user={user} charity={charity} donationForm={donationForm} project={project} />);
		}

		const donateButton = donationForm.ready ? (
			<DonationFormButton
				amount={Math.floor(donationForm.amount * 100)}
				onToken={(stripeResponse) => { sendDonation(charity, donationForm, stripeResponse); }}
			/>
		) : (
			<Button disabled title='Something is wrong with your donation'>Donate</Button>
		);

		const giftAidForm = charity.uk_giftaid ? (
			<GiftAidForm {...donationForm} handleChange={handleChange} />
		) : <small>This charity is not eligible for Gift-Aid.</small>;

		return (
			<div>
				<div className='DonationForm'>
					<DonationAmounts
							options={[5, 10, 20]}
							impacts={project.impacts}
							charity={charity}
							project={project}
							amount={donationForm.amount}
							handleChange={handleChange}
						/>
				</div>
				<div className='col-md-12 donate-button'>
					{ donateButton }
				</div>
				{ giftAidForm }
				<ThankYouAndShare thanks={false} charity={charity} />
			</div>
		);
	}
}


class ThankYouAndShare extends React.Component {

	constructor(...params) {
		super(...params);
		const { thanks, user, charity, donationForm, project} = this.props;

		let impact;
		/*
		// Commented out because it's spitting out "NaN people helped"
		if (project && project.impacts) {
			const unitImpact = project.impacts[0];
			const impactPerUnitMoney = unitImpact.number / unitImpact.price.value;
			impact = printer.prettyNumber(impactPerUnitMoney * donationForm.amount, 2) + ' ' + unitImpact.name;
		}
		*/

		let shareText;
		if (user && user.name) {
			if (impact) {
				shareText = `${charity.name} and SoGive thank ${user.name} for helping to fund ${impact} - why not join in?`;
			} else {
				shareText = `${charity.name} and SoGive thank ${user.name} for their donation - why not join in?`;
			}
		} else {
			shareText = `Help to fund ${charity.name} and see the impact of your donations on SoGive:`;
		}

		this.state = {
			shareText,
		};
	}

	shareOnFacebook() {
		let url = ""+window.location;
		FB.ui({
			quote: this.state.shareText,
			method: 'share',
			href: url,
			},
			// callback
			function(response) {
				console.log("FB", response);
				if (response && response.error_message) {
					console.error('Error while posting.');
					return;
				}
				// foo
			}
		);
	}

	onChangeShareText(event) {
		event.preventDefault();
		console.log("event", event);
		this.setState({shareText: event.target.value});
	}

	render() {
		const { thanks } = this.props;
		const { shareText } = this.state;
	/*
	<div className="fb-share-button"
		data-href={url}
		data-layout="button_count"
		data-size="large" data-mobile-iframe="true"><a className="fb-xfbml-parse-ignore" target="_blank"
		href={"https://www.facebook.com/sharer/sharer.php?u="+escape(url)}>Share</a>
	</div>
	*/

		let url = `${window.location}`;

		const header = thanks ? <h3>Thank you for donating!</h3> : '';

		return (
			<div className='col-md-12'>
				<div className='ThankYouAndShare panel-success'>
					{ header }

					<p>Share this on social media? We expect this will lead to 2-3 times more donations on average.</p>

					<textarea
						className='form-control'
						onChange={() => { this.onChangeShareText(); }}
						defaultValue={shareText}
					/>
				</div>
				<div className='col-md-12 social-media-buttons'>
					<center>
						<a className='btn twitter-btn' href={'https://twitter.com/intent/tweet?text='+escape(this.state.shareText)+'&url='+escape(url)} data-show-count="none">
							<Misc.Logo service='twitter' />
						</a>

						<a className='btn facebook-btn' onClick={() => { this.shareOnFacebook(); }}>
							<Misc.Logo service='facebook' />
						</a>
					</center>
				</div>
			</div>
		);
	}
} // ./ThankYouAndShare

/**
 * one-click donate, or Stripe form?
 */
const DonationFormButton = ({onToken, amount}) => {
	if (false) {
		return <button>Donate</button>;
	}
	let email = Login.getId('Email');
	if (email) email = XId.id(email);
	return (
		<div>
			<StripeCheckout name="SoGive" description="Donate with impact tracking"
				image="http://local.sogive.org/img/SoGive-Light-64px.png"
				email={email}
				panelLabel="Donate"
				amount={amount}
				currency="GBP"
				stripeKey="pk_live_InKkluBNjhUO4XN1QAkCPEGY"
				bitcoin
				allowRememberMe
				token={onToken}
			>
				<center>
					<Button bsStyle="primary" className='sogive-donate-btn'>Donate</Button>
				</center>
			</StripeCheckout>
		</div>
	);
};


const DonationAmounts = ({options, impacts, amount, handleChange}) => {
	let unitImpact = impacts && impacts[0];
	let damounts = _.map(options, price => (
		<span key={'donate_'+price}>
			<DonationAmount
				price={price}
				selected={price === amount}
				unitImpact={unitImpact}
				handleChange={handleChange}
			/>
			&nbsp;
		</span>
	));

	let fgcol = (options.indexOf(amount) === -1) ? 'white' : null;
	let bgcol = (options.indexOf(amount) === -1) ? '#337ab7' : null;

	return(
		<div className='full-width'>
			<form>
				<div className="form-group col-md-1 col-xs-2">
					{damounts}
				</div>
				<div className="form-group col-md-8 col-xs-10">
					<InputGroup>
						<InputGroup.Addon style={{color: fgcol, backgroundColor: bgcol}}>£</InputGroup.Addon>
						<FormControl
							type="number"
							min="1"
							max="100000"
							step="1"
							placeholder="Enter donation amount"
							onChange={({ target }) => { handleChange('amount', target.value); }}
							value={amount}
						/>
					</InputGroup>
				</div>
				<div className="form-group col-md-2">
					<Misc.ImpactDesc unitImpact={unitImpact} amount={amount} />
				</div>
			</form>
		</div>
	);
};

const DonationAmount = function({selected, price, handleChange}) {
	return (
			<div className=''>
				<Button
					bsStyle={selected? 'primary' : null}
					bsSize="sm"
					className='amount-btn'
					onClick={() => handleChange('amount', price)}
				>
					£ {price}
				</Button>
			</div>
	);
};


const DonationList = ({donations}) => {
	let ddivs = _.map(donations, d => <li key={uid()}>{d}</li>);
	return <ul>{ddivs}</ul>;
};

const mapStateToProps = (state, ownProps) => ({
	...ownProps,
	donationForm: state.donationForm[ownProps.charity['@id']],
});

const mapDispatchToProps = (dispatch, ownProps) => ({
	handleChange: (field, value) => dispatch(updateForm(ownProps.charity['@id'], field, value)),
	sendDonation: (charity, donationForm, stripeResponse) => dispatch(donate(dispatch, charity, donationForm, stripeResponse)),
	initDonation: () => dispatch(initDonationForm(ownProps.charity['@id'])),
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DonationForm);
