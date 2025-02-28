/********************************************************************************/
/*                                                                              */
/*              BibEntryGoogle.java                                             */
/*                                                                              */
/*      Bibliographic data from google books                                    */
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


package edu.brown.cs.burl.bibentry;

import org.json.JSONObject;

import edu.brown.cs.burl.burl.BurlRepoColumn;
import edu.brown.cs.ivy.file.IvyLog;

class BibEntryGoogle extends BibEntryBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private JSONObject      google_data;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BibEntryGoogle(JSONObject google)
{
   google_data = google;
   
   IvyLog.logD("BOOKS",
         "Use google information " + google_data.toString(2));
}




/********************************************************************************/
/*                                                                              */
/*      Code to get entry value from GOOGLE BOOKS data                          */
/*                                                                              */
/********************************************************************************/

@Override 
public String computeEntry(BurlRepoColumn brc)
{
   return null;
}




}       // end of class BibEntryGoogle




/* end of BibEntryGoogle.java */

