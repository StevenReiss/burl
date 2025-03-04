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

import edu.brown.cs.burl.burl.BurlLibraryAccess;

class ControlAccess implements BurlLibraryAccess, ControlConstants
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

@Override public String getUserEmail()		{ return access_data.getString("email"); } 


@Override public Number getLibraryId()		{ return access_data.getNumber("libraryid"); }


@Override public BurlUserAccess getAccessLevel()
{
   int lvl = access_data.getInt("access_level");
   return BurlUserAccess.values()[lvl];
}


}	// end of class ControlAccess




/* end of ControlAccess.java */

