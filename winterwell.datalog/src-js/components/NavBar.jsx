import React from 'react';

import AccountMenu from './AccountMenu';

const NavBar = ({page}) => {
	console.log('NavBar', page);

	return (
		<nav className="navbar navbar-fixed-top navbar-inverse">
			<div className="container">
				<div className="navbar-header" title="Dashbrd">
					<button
						type="button"
						className="navbar-toggle collapsed"
						data-toggle="collapse"
						data-target="#navbar"
						aria-expanded="false"
						aria-controls="navbar"
					>
						<span className="sr-only">Toggle navigation</span>
						<span className="icon-bar" />
						<span className="icon-bar" />
						<span className="icon-bar" />
					</button>
					<a className="" href="#dashboard">
						<img alt="SoGive logo" style={{maxWidth:'100px',maxHeight:'50px'}} src="img/logo-white-sm.png" />
					</a>
				</div>
				<div id="navbar" className="navbar-collapse collapse">
					<ul className="nav navbar-nav">
						<li className={page === 'dashboard'? 'active' : ''}>
							<a className="nav-item nav-link" href="#dashboard">
								My Profile
							</a></li>
						<li className={page === 'search'? 'active' : ''}>
							<a className="nav-item nav-link" href="#search">
								Search
							</a></li>
					</ul>
					<div>
						<AccountMenu active={page === 'account'} />
					</div>
				</div>
			</div>
		</nav>
	);
};
// ./NavBar

export default NavBar;
