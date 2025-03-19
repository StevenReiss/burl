/********************************************************************************/
/*                                                                              */
/*              ControlWorkItem.java                                            */
/*                                                                              */
/*      Implementation of a work item from database                             */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2025 Steven P. Reiss                                          */
/*********************************************************************************
 *                                                                               *
 *  This work is licensed under Creative Commons Attribution-NonCommercial 4.0   *
 *  International.  To view a copy of this license, visit                        *      
 *      https://creativecommons.org/licenses/by-nc/4.0/                          *
 *                                                                               *
 ********************************************************************************/


package edu.brown.cs.burl.control;

import org.json.JSONObject;

import edu.brown.cs.burl.burl.BurlWorkItem;

class ControlWorkItem implements BurlWorkItem, ControlConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private JSONObject      base_data;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ControlWorkItem(JSONObject jo)
{
   base_data = jo;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public Number getItemId()
{
   return base_data.getNumber("id");
}


@Override public Number getLibraryId()
{
   return base_data.getNumber("libraryid");
}


@Override public String getItem()
{
   return base_data.getString("item");
}

@Override public BurlUpdateMode getUpdateMode()
{
   int umod = base_data.getInt("mode");
   return BurlUpdateMode.values()[umod];
}



}       // end of class ControlWorkItem




/* end of ControlWorkItem.java */

