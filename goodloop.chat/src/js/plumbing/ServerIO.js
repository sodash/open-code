/** 
 * Wrapper for server calls.
 *
 */
import {encURI} from '../base/utils/miscutils';
import C from '../C';

import ServerIO from '../base/plumbing/ServerIOBase';
export default ServerIO;

/** The initial part of an API call. Allows for local to point at live for debugging */
ServerIO.APIBASE = '';
// Uncomment below to talk to a server other than /
// ServerIO.APIBASE = C.HTTPS+'://'+C.SERVER_TYPE+'portal.good-loop.com';
// Comment out the lines below when deploying!
// ServerIO.APIBASE = 'https://testportal.good-loop.com'; // uncomment to let local use the test server's backend
// ServerIO.APIBASE = 'https://portal.good-loop.com'; // use in testing to access live ads

ServerIO.DATALOG_ENDPOINT = `${C.HTTPS}://${C.SERVER_TYPE}lg.good-loop.com/data`;
// ServerIO.DATALOG_ENDPOINT = 'https://testlg.good-loop.com/data';
// ServerIO.DATALOG_ENDPOINT = 'https://lg.good-loop.com/data'; // live stats

ServerIO.DATALOG_DATASPACE = 'gl';

ServerIO.checkBase();

