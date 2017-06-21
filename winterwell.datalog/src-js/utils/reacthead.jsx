/** 
 * Set head info, e.g. title or Facebook og: properties for sharing, from within the body. 
 * These tags work by modifying the html head.
*/
import React from 'react';
import $ from 'jquery';

class HeadTag extends React.Component {
	componentWillMount() {
		const {sel,tag,attr} = this.props;
		const $head = $('head');
		let $els = $(sel, $head);
		let $tag;
		if ($els.length) {
			$tag = $els;
		} else {
			$tag = $("<"+tag+"/>");
			$head.append($tag);
		}
		Object.keys(attr).forEach(k => {
			const v = attr[k];
			if ( ! v) return;
			if (k==='text') $tag.text(v);
			else $tag.attr(k, v);			
		});
		// HACK
		if (tag==='title') document.title=attr.text;
	}

	render() {
		return <div />; //{this.props.tag} {this.props.sel} {JSON.stringify(this.props.attr)}</div>;
	}
}

const Title = ({title}) => <HeadTag sel='title' tag='title' attr={{text:title}}/>;

const Meta = ({name,property,content}) => {
	let kv = (property? 'property="'+property : 'name="'+name) + '"';
	return <HeadTag sel={'meta['+kv+']'} tag='meta' attr={{name:name,property:property,content:content}}/>;
};

const Link = ({rel,href}) => <HeadTag tag='link' attr={{rel:rel,href:href}}/>;

export {Title,Meta,Link};
