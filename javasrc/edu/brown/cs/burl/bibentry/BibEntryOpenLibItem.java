/********************************************************************************/
/*                                                                              */
/*              BibEntryOpenLibItem.java                                        */
/*                                                                              */
/*      Provide access to the data from open library search                     */
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

class BibEntryOpenLibItem implements BibEntryConstants
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

BibEntryOpenLibItem(JSONObject jo)
{
   JSONArray jarr = jo.optJSONArray("docs");
   
   results_data = jarr;
}



String getIdURL(String isbn,int ct)
{
   if (results_data == null || results_data.length() == 0) return null;
   else if (results_data.length() >= 1) {
      JSONObject r1 = results_data.getJSONObject(0);
      JSONArray r2 = r1.optJSONArray("lccn",null);
      if (r2 !=  null && r2.length() > ct) {
         String r2v = r2.getString(ct);
         return "https://lccn.loc.gov/" + r2v;
       }
      else {
         IvyLog.logD("BIBENTRY","No lccn given for openlib " + isbn);
       }
    }
   
   return null;
}



BibEntryBase getBibEntry()
{
   if (results_data == null || results_data.length() == 0) return null;
   else if (results_data.length() >= 1) {
      JSONObject r1 = results_data.getJSONObject(0);
      return new BibEntryOpenLib(r1); 
    }
   else {
//    IvyLog.logD("BOOKS","Need to check which result is correct for " + isbn);
//    IvyLog.logD("BOOKS",results_data.toString(2));
    }
   
   return null;   
}


}       // end of class BibEntryOpenLibItem




/* end of BibEntryOpenLibItem.java */

