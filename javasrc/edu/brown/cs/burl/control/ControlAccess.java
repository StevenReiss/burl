/********************************************************************************/
/*										*/
/*		ControlAccess.java						*/
/*										*/
/*	Access control information for user-library				*/
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

import org.json.JSONObject;

class ControlAccess implements ControlConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JSONObject	access_data;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ControlAccess(JSONObject data)
{
   access_data = data;
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

Number getUserId()		{ return access_data.getNumber("userid"); }


Number getLibraryId()		{ return access_data.getNumber("libraryid"); }


BurlUserAccess getAccessLevel()
{
   int lvl = access_data.getInt("access_level");
   return BurlUserAccess.values()[lvl];
}


}	// end of class ControlAccess




/* end of ControlAccess.java */

