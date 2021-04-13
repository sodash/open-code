/* global navigator */
import React, { Component } from 'react';
import Login from '../base/youagain';

// Plumbing
import DataStore from '../base/plumbing/DataStore';
import Roles from '../base/Roles';
import C from '../C';
import Crud from '../base/plumbing/Crud'; // Crud is loaded here (but not used here)
import Profiler from '../base/Profiler';
import MainDivBase from '../base/components/MainDivBase';

// Templates

// Pages
import ChatPage from './ChatPage';
import ChatscriptPage from './ChatscriptPage';
import TestchatPage from './TestchatPage';

// DataStore
C.setupDataStore();

const PAGES = {
	chat: ChatPage,	
	chatscript: ChatscriptPage,
	testchat: TestchatPage
};

Login.app = C.app.service;

const MainDiv = () => {
	return <MainDivBase 
		pageForPath={PAGES}
		navbarPages={['chatscript','chat','testchat']}
		defaultPage='chat'
	/>;
}; // ./MainDiv

// if ( ! window.smartsupp) {
// 	addScript({src:"https://www.smartsuppchat.com/loader.js?key=0ad472d8daddb81252d93c1615997823038581da", async:true});
// }


export default MainDiv;
