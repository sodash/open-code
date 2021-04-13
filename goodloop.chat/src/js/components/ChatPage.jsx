import React from 'react';

import { assert } from '../base/utils/assert';
import { encURI } from '../base/utils/miscutils';
import _ from 'lodash';
import PromiseValue from 'promise-value';

import C from '../C';
import DataStore, { getDataPath, getPath } from '../base/plumbing/DataStore';
import ActionMan from '../plumbing/ActionMan';
import {getType} from '../base/data/DataClass';
import Misc from '../base/components/Misc';
import PropControl from '../base/components/PropControl';
import ListLoad from '../base/components/ListLoad';
import Roles from '../base/Roles';
import KStatus from '../base/data/KStatus';

import { setWindowTitle } from '../base/plumbing/Crud';


const ChatPage = () => {	
	const type = C.TYPES.Chat;
	const path = DataStore.getValue(['location','path']);
	const ChatId = path[1];
	setWindowTitle(type);	
	if ( ! ChatId) {
		return <ListLoad type={type} status={KStatus.ALL_BAR_TRASH} 
			// navpage,
			// q, 
			// start, end,
			// sort = 'created-desc',
			// filter, 
			hasFilter
			// , filterLocally,
			// ListItem,
			// checkboxes, 
			canDelete canCreate canFilter
			// createBase,
			// className,
			// noResults,
			// notALink, itemClassName,
			// preferStatus,
			// hideTotal,
			// pageSize,
			// unwrapped
		/>;
	}
	const pvItem = ActionMan.getDataItem({type:C.TYPES.Chat, id:ChatId, status:KStatus.DRAFT});
	if ( ! pvItem.value) {
		return (<div><h1>Chat: {ChatId}</h1><Misc.Loading /></div>);
	}
	const item = pvItem.value;
	setWindowTitle(item);	
	return (
		<div className=''>
			<h1>Chat: {item.name || ChatId}</h1>
			<EditChat item={item} />
		</div>
	);
}; // ./ChatPage


const EditChat = ({item}) => {
	assert(item.id, item);
	let path = getDataPath({status:KStatus.DRAFT, type:C.TYPES.Chat, id:item.id});
	return (
		<div className="form">
			ID: {item.id}<br />
			<Misc.SavePublishDiscard type={C.TYPES.Chat} id={item.id} />
			<PropControl label="Chat Name" item={chat} path={path} prop="name" />
			{(chat.lines || []).map(cl => <p>{cl.from}: {cl.text}</p>)}
		</div>
	);
};

export default ChatPage;
