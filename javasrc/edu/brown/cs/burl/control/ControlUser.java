/********************************************************************************/
/*										*/
/*		ControlUser.java						*/
/*										*/
/*	Representation of a user						*/
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

import edu.brown.cs.burl.burl.BurlUser;

class ControlUser implements ControlConstants, BurlUser
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private JSONObject	user_data;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ControlUser(JSONObject data)
{
   user_data = data;
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Number getId()
{
   return user_data.getNumber("id");
}


@Override public String getEmail()
{
   return user_data.getString("email");
}


@Override public String getSalt()
{
   return user_data.getString("salt");
}


@Override public String getPassword()
{
   return user_data.getString("password");
}


String getTempPassword()
{
   return user_data.optString("temp_password");
}


boolean isValid()
{
   return user_data.getBoolean("valid");
}


}	// end of class ControlUser




/* end of ControlUser.java */

