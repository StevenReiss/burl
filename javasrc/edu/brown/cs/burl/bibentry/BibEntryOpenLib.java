/********************************************************************************/
/*                                                                              */
/*              BibEntryOpenLib.java                                            */
/*                                                                              */
/*      description of class                                                    */
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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.brown.cs.burl.burl.BurlRepoColumn;
import edu.brown.cs.ivy.file.IvyLog;

class BibEntryOpenLib extends BibEntryBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private JSONObject      openlib_data;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BibEntryOpenLib(JSONObject data)
{
   openlib_data = data;
   
   IvyLog.logD("BIBENTRY",
         "Use open library information " + openlib_data.toString(2));
}



/********************************************************************************/
/*                                                                              */
/*      Code to get entry value from OPENLIB data                               */
/*                                                                              */
/********************************************************************************/


@Override 
public String computeEntry(BurlRepoColumn brc)
{
   String key = brc.getOpenLibData();
   if (key == null) return null;
   
   List<String> items = new ArrayList<>();
   StringBuffer elt = null;
   for (int idx = 0; ; ++idx) {
      StringBuffer rslt = new StringBuffer();
      boolean skip = false;
      for (int i = 0; i < key.length(); ++i) {
         char c = key.charAt(i);
         if (Character.isJavaIdentifierPart(c)) {
            if (elt == null) elt = new StringBuffer();
            elt.append(c);
            continue;
          }
         else {
            if (elt != null) {
               if (skip) skip = false;
               else {
                  String txt = getItemText(elt.toString(),idx);
                  if (txt != null) {
                     if (c == '|') skip = true;
                     rslt.append(txt);
                   }
                }
               elt = null;
             }
            if (c != '|' && !rslt.isEmpty()) rslt.append(c);
          }
       }
      if (elt != null && !skip) {
         String txt = getItemText(elt.toString(),idx);
         if (txt != null) rslt.append(txt);
         elt = null;
       }
      if (rslt.isEmpty()) break;
      items.add(rslt.toString());
    }
   
   if (items.isEmpty()) return null;
   
   if (brc.isMultiple()) {
      return String.join(brc.getMultipleSeparator(),items);
    }
   
   return items.get(0);
}



private String getItemText(String elt,int idx)
{
   Object val = openlib_data.opt(elt);
   if (val == null) return null;
   else if (val instanceof String) { 
      if (idx == 0) return val.toString();
      else return null;
    }
   else if (val instanceof Number) {
      if (idx == 0) return val.toString();
      else return null;
    }
   else if (val instanceof JSONArray) {
      JSONArray jarr = (JSONArray) val;
      if (idx < jarr.length()) {
         Object vax = jarr.get(idx);
         if (vax == null) return null;
         return vax.toString();
       }
      else return null;
    }
   
   return null;
}




}       // end of class BibEntryOpenLib




/* end of BibEntryOpenLib.java */

