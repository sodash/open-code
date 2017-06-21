import React from 'react';
import ReactDOM from 'react-dom';

import SJTest from 'sjtest'
const assert = SJTest.assert;
import printer from '../utils/printer.js';
import C from '../C.js';


class AccountPage extends React.Component {

    render() {
        return (
            <div className=''>
                <h2>My Account</h2>
				<h3>In development...</h3>
				<p>Thank you for joining SoGive at this early stage.
					This is our first release, and there's still lots of work to do.
					By the way, we release all our code as open-source. If you would
					like to contribute to building SoGive, please get in touch.
				</p>
            </div>
        );
    }

}

export default AccountPage;
