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
   if (jarr.length() == 0) {
      IvyLog.logD("BIBENTRY","No results from LOC search: " +
            jobj.toString(2));
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getIdURL(String isbn)
{
   if (results_data.length() == 0) {
      return null;
    }
   int which = 0;
   if (results_data.length() > 1) {
      IvyLog.logD("BIBENTRY","Need to check which result is correct for LOC " + isbn);
      IvyLog.logD("BIBENTRY",results_data.toString(2));
      which = findBestResult(results_data,isbn);
    }
   
   JSONObject r1 = results_data.getJSONObject(which);
   return r1.getString("id");
}


private int findBestResult(JSONArray rslts,String isbn)
{
   int best = -1;
   
   // first check for exact match with given LCCN (if isbn given, this won't help)
   for (int i = 0; i < rslts.length(); ++i) {
      JSONObject rslt = rslts.getJSONObject(i);
      if (fieldContains(rslt,"number",isbn)) return i;
    }
   
   // next check for book and prefer Engligh language
   for (int i = 0; i < rslts.length(); ++i) {
      JSONObject rslt = rslts.getJSONObject(i);
      if (fieldContains(rslt,"original_format","book") ||
            fieldContains(rslt,"type","text")) {
         best = i;
         if (fieldContains(rslt,"language","english")) {
            return i;
          }
       }
    }
   
   if (best < 0) best = 0;
   
   return best;
}


private boolean fieldContains(JSONObject obj,String fld,String what)
{
   JSONArray itms = obj.optJSONArray(fld);
   if (itms == null) return false;
   for (int i = 0; i < itms.length(); ++i) {
      String v = itms.getString(i);
      if (v.equalsIgnoreCase(what)) return true;
    }
   return false;
}


}       // end of class BibEntryLOCResult




/* end of BibEntryLOCResult.java */

