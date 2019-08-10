import React from 'react';
import ReactDOM from 'react-dom';

import SJTest from 'sjtest';
const assert = SJTest.assert;
import printer from '../utils/printer.js';
import C from '../C.js';



class MessageBar extends React.Component {
	componentWillMount() {
		// ViewManager.register(C.stateKey.messages, this, 'messages');
	}

	componentWillUnmount() {
		// ViewManager.unregister(C.stateKey.messages, this);
	}

	render() {
		if ( ! this.state || ! this.state.messages) return <div></div>;    
		console.log("render Messagebar", this.state.messages);
		const messageUI = _.map(this.state.messages, 
														(m, mi) => <MessageBarItem key={'mi'+mi} message={m} />);
		return (<div className='messagebar'>{messageUI}</div>);
	}
} // ./Messagebar


class MessageBarItem extends React.Component {
	closeme() {
		console.warn("MessageBarItem closeme()", this);
		this.setState({closed:true});
	}

	render() {
		if (this.state && this.state.closed) {
			return (<div></div>);
		}
		//  TODO wire up the close button
		const m = this.props.message;
		const alertType = m.type==="error"? "alert alert-danger" : "alert alert-warning";
		return (
			<div className={alertType}>{m.text}
				<button onClick={this.closeme} type="button" className="close" aria-label="Close"><span aria-hidden="true">&times;</span></button>
			</div>
		);
	}
}

export default MessageBar;
