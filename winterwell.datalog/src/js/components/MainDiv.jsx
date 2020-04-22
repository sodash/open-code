import React, { Component } from 'react';
import Login from 'you-again';
import { assert } from 'sjtest';
import { getUrlVars } from 'wwutils';
import _ from 'lodash';
import C from '../C';

// Plumbing
import DataStore from '../plumbing/DataStore';

// Templates
import MessageBar from './MessageBar';
import NavBar from './NavBar';
import LoginWidget from './LoginWidget';
// Pages
import DashboardPage from './DashboardPage';
import AccountPage from './AccountPage';
import MainDivBase from '../base/components/MainDivBase';
// Actions


const PAGES = {
	dashboard: DashboardPage,
	account: AccountPage,
};


/**
		Top-level: tabs
*/
const MainDiv = () => {
	return <MainDivBase pageForPath={PAGES} defaultPage='dashboard' />;
}

export default MainDiv;
