/********************************************************************************/
/*										*/
/*		ControlConstants.java						*/
/*										*/
/*	Constants for BURL control and control program				*/
/*										*/
/********************************************************************************/
/*	Copyright 2025 Steven P. Reiss						*/
/*********************************************************************************
 *										 *
 *  This work is licensed under Creative Commons Attribution-NonCommercial 4.0	 *
 *  International.  To view a copy of this license, visit			 *
 *	https://creativecommons.org/licenses/by-nc/4.0/ 			 *
 *										 *
 ********************************************************************************/


package edu.brown.cs.burl.control;

import edu.brown.cs.burl.burl.BurlConstants;

public interface ControlConstants extends BurlConstants
{

/**
 *	NAME of the SQL data store
 **/

String BURL_DATA_STORE = "burlcontrol";



String SESSION_COOKIE = "burl-control-5567";
String SESSION_PARAMETER = "session";
long SESSION_TIMEOUT = 1000*60*60*24*3;


/**
 *	Definitions for repository management
 **/

int MAX_REPOS = 10;



}	// end of interface ControlConstants




/* end of ControlConstants.java */

