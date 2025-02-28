/********************************************************************************/
/*                                                                              */
/*              BurlRepoRow.java                                                */
/*                                                                              */
/*      Contents of a row (book or entry) in a repository (library)             */
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


package edu.brown.cs.burl.burl;

import org.json.JSONObject;

public interface BurlRepoRow extends BurlConstants
{


/**
 *      Get the associated repository
 **/

BurlRepo getRepository();


/**
 *      Gett the data for a column
 **/

String getData(BurlRepoColumn column);


/**
 *      Set the data for a column
 **/

void setData(BurlRepoColumn column,String data);



/**
 *      Set a column by name
 **/

default void setData(String name,String data)
{
   BurlRepoColumn col = getRepository().getColumn(name);
   if (col != null) {
      setData(col,data);
    }
}


/**
 *      Get the id for a column
 **/

Number getRowId();


/**
 *      Get the JSON for a row
 **/

JSONObject toJson(boolean external);


/**
 *      Get the CSV for a row
 **/

String toCSV(boolean external);



}       // end of interface BurlRepoRow




/* end of BurlRepoRow.java */

