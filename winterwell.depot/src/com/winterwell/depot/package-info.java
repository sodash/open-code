/**
 * An *artifact* may be a csv File, datastream, result or -- well anything we care
 * to store. Depot stores stuff across multiple servers, fetching it on demand.
 * 
 * Key Classes
 * <ul>
 * <li>Depot is a Desc-artifact map with get & put.
 * <li>Desc (artifact descriptions) can be bound to artifacts.
 * <li>RemoteStore handles the scp-ing.
 * </ul>
 */
package com.winterwell.depot;