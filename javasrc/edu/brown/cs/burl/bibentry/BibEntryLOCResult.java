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
import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.xml.IvyXml;

class BibEntryLOCResult implements BibEntryConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private JSONArray       results_data;
private Element         xml_data;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BibEntryLOCResult(JSONObject jobj)
{
   JSONArray jarr = jobj.getJSONArray("results");
   results_data = jarr;
   xml_data = null;
   if (jarr.length() == 0) {
      IvyLog.logD("BIBENTRY","No results from LOC search: " +
            jobj.toString(2));
    }
}


BibEntryLOCResult(Element xml)
{
   xml_data = IvyXml.getChild(xml,"zs:records");
   results_data = null;
}




/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getIdURL(String isbn)
{
   if (xml_data != null && results_data == null) {
      return getXmlIdURL(isbn);
    }
   
   if (results_data == null || results_data.length() == 0) {
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



/********************************************************************************/
/*                                                                              */
/*      Handle XML records from lx2                                             */
/*                                                                              */
/********************************************************************************/

private String getXmlIdURL(String isbn)
{
   Element rec = findXmlBestResult(isbn);
   if (rec == null) return null;
   for (Element ident : IvyXml.children(rec,"identifier")) {
      String typ = IvyXml.getAttrString(ident,"type");
      if (typ != null && typ.equals("lccn")) {
         String lccn = IvyXml.getText(ident);
         String url = "http://lccn.loc.gov/" + lccn;
         return url;
       }
    }
   
   return null;
}



private Element findXmlBestResult(String isbn)
{
   Element best = null;
   Element bestx = null;
   
   // first check for exact match with given LCCN (if isbn given, this won't help)
   for (Element rec : IvyXml.children(xml_data,"zs:record")) {
      Element recd1 = IvyXml.getChild(rec,"zs:recordData");
      Element recd = IvyXml.getChild(recd1,"mods");
      if (recd == null) continue;
      
      boolean fnd = false;
      for (Element ident : IvyXml.children(recd,"identifier")) {
         String txt = IvyXml.getText(ident);
         if (txt != null && txt.equals(isbn)) {
            fnd = true;
            break;
          }
       }
      if (!fnd) continue;
      
      if (bestx == null) bestx = recd;
      Element lang1 = IvyXml.getChild(recd,"language");
      Element lang2 = IvyXml.getChild(lang1,"languageTerm");
      String ltxt = IvyXml.getText(lang2);
      if (ltxt != null) {
         ltxt = ltxt.trim().toLowerCase();
         if (ltxt.startsWith("eng")) {
            best = recd;
          }
       }
    }
   
   if (best == null) best = bestx;
            
   return best;
}


}       // end of class BibEntryLOCResult




/* end of BibEntryLOCResult.java */

