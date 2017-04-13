
/**
 * Bayesian Graphical Models
 * 
 * This is just a place for notes for now.
 * 
 * Ideas:
 * 
 * Discounting heuristics in loopy belief propagation...
 *  - Limit the number of updates
 *  - Limit the amount of looping
 *  - Track the evidence sources of each message and discount if that source has already been seen.
 * 
 * TODO test on a problem LBP finds hard. A spin glass? See Bishop for ideas here.
 */
package com.winterwell.maths.stats.bgm;