/********************************************************************************/
/*                                                                              */
/*              BibEntryGoogleItem.java                                         */
/*                                                                              */
/*      Provide access to data returned from google books search                */
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

class BibEntryGoogleItem
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

BibEntryGoogleItem(JSONObject jo)
{
   JSONArray jarr = jo.optJSONArray("items");
   
   results_data = jarr;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getIdURL(String isbn)
{
   if (results_data == null || results_data.length() == 0) return null;
   if (results_data.length() > 1) {
      IvyLog.logD("BIBENTRY","Need to check which result is correct for " + isbn);
    }
   
   JSONObject r1 = results_data.getJSONObject(0);
   String r2 = r1.optString("lccn",null);
   if (r2 !=  null && !r2.isEmpty()) {
      return "https://lccn.loc.gov/" + r2;
    }
   else {
      IvyLog.logD("BIBENTRY","No lccn given for google " + isbn);
    }
   
   return null;
}



BibEntryBase getBibEntry()
{
   if (results_data == null || results_data.length() == 0) return null;
   if (results_data.length() > 1) {
      IvyLog.logE("BIBENTRY","Recieved " + results_data.length() + " results");
    }
   
   JSONObject r1 = results_data.getJSONObject(0);
   return new BibEntryGoogle(r1); 
}


}       // end of class BibEntryGoogleItem




/* end of BibEntryGoogleItem.java */

