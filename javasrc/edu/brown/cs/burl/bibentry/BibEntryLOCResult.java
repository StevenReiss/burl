/********************************************************************************/
/*                                                                              */
/*              BibEntryLOCResult.java                                          */
/*                                                                              */
/*      Access data from a library of congress LCCN search                      */
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

import org.json.JSONArray;
import org.json.JSONObject;

import edu.brown.cs.ivy.file.IvyLog;

class BibEntryLOCResult implements BibEntryConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private JSONArray       results_data;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BibEntryLOCResult(JSONObject jobj)
{
   JSONArray jarr = jobj.getJSONArray("results");
   results_data = jarr;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getIdURL(String isbn)
{
   if (results_data.length() == 0) return null;
   if (results_data.length() > 1) {
      IvyLog.logD("BIBENTRY","Need to check which result is correct for LOC " + isbn);
      IvyLog.logD("BIBENTRY",results_data.toString(2));
    }
   
   JSONObject r1 = results_data.getJSONObject(0);
   return r1.getString("id");
}


}       // end of class BibEntryLOCResult




/* end of BibEntryLOCResult.java */

