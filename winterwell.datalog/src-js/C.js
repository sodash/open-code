
import Enum from 'easy-enums';

/**
 * app config
 */
const app = {
	name: "DataLog",
	service: "datalog",
	logo: "/img/logo.png"
};

const TYPES = new Enum("User");

/** dialogs you can show/hide.*/
const show = new Enum('LoginWidget');

const C = {
	TYPES, show,
	app
};
export default C;
