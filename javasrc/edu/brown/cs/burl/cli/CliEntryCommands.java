/********************************************************************************/
/*                                                                              */
/*              CliEntryCommands.java                                           */
/*                                                                              */
/*      Command processing for individual entries                               */
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


package edu.brown.cs.burl.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.brown.cs.burl.burl.BurlFieldData;
import edu.brown.cs.burl.burl.BurlUtil;
import edu.brown.cs.ivy.file.IvyLog;

class CliEntryCommands implements CliConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CliMain         cli_main;
private BurlFieldData   field_data;
private JSONObject      cur_entry;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CliEntryCommands(CliMain main)
{
   cli_main = main;
   field_data = new BurlFieldData();
   cur_entry = null;
}


/********************************************************************************/
/*                                                                              */
/*      Command to list (filtered) entries in current library                   */
/*                                                                              */
/********************************************************************************/

void handleFindEntries(List<String> args)
{
   JSONObject filters = new JSONObject();
   
   for (String s : args) {
      String key = null;
      String value = null;
      
      if (s.startsWith("-")) {
         badFindEntriesArgs();
         return;
       }
      else if (s.contains(":") || s.contains("=")) {
         int idx1 = s.indexOf(":");
         int idx2 = s.indexOf("=");
         int idx;
         if (idx1 < 0) idx = idx2;
         else if (idx2 < 0) idx = idx1;
         else idx = Math.min(idx1,idx2);
               
         key = s.substring(0,idx).trim();
         if (!field_data.isValidField(key)) { 
            badFindEntriesArgs();
            return;
          }
         value = s.substring(idx+1).trim();
       }
      else {
         value = s;
         key = "all";
       }
      
      Object prev = filters.opt(key);
      if (prev == null) {
         filters.put(key,value);
       }
      else if (prev instanceof String) {
         JSONArray nval = new JSONArray();
         nval.put(prev);
         nval.put(value);
         filters.put(key,nval);
       }
      else if (prev instanceof JSONArray) {
         JSONArray nval = (JSONArray) prev;
         nval.put(value);
       }
    }
   
   Number libid = cli_main.getLibraryId();
   JSONObject data = BurlUtil.buildJson("library",libid,
         "count",4,"filter",filters);
   JSONObject rslt = cli_main.createHttpPost("entries",data);
   
   if (!cli_main.checkResponse(rslt,"find")) {
      return;
    }
   
   Number useelt = null;
   String filterid = null;
   for ( ; ; ) {
      filterid = rslt.optString("filterid",null);
      if (filterid != null && filterid.isEmpty()) filterid = null;
      JSONArray items = rslt.getJSONArray("data");
      if (items.length() == 0) break;
      
      JSONObject item = null;
      for (int i = 0; i < items.length(); ++i) {
         item = items.getJSONObject(i);
         shortEntryDisplay(item);
         System.out.println();
       }
      int next = readStatus(item.getNumber("burl_id"));
      if (next < 0 || filterid == null) break;
      if (next > 0) {
         useelt = next;
         break;
       }
      
      data = BurlUtil.buildJson("library",libid,"filterid",filterid,
            "count",4);
      rslt = cli_main.createHttpPost("entries",data);
      if (!cli_main.checkResponse(rslt,"find")) {
         useelt = null;
         break;
       }    
    }
   
   if (filterid != null) {
      data = BurlUtil.buildJson("library",libid,"filterid",filterid,"count",-1);
      rslt = cli_main.createHttpPost("entries",data);
    }
   setEntry(useelt);
      
}


private int readStatus(Number dflt)
{
   System.out.print(
         "More (\\n for next, e or x to quit, <number> to choose entry, = for last entry: ");
   try {
      String ln = cli_main.getLineReader().readLine();
      if (ln == null) return -1;
      ln = ln.trim();
      if (ln.isEmpty()) return 0;
      if (ln.startsWith("e") || ln.startsWith("x")) return -1;
      if (ln.matches("[0-9]+")) {
         int vl = Integer.parseInt(ln);
         return vl;
       }
      return 0;
    }
   catch (IOException e) { }
   
   return -1;
}



private void badFindEntriesArgs()
{
   IvyLog.logI("BURLCLI","find [<filter> ...]");
   IvyLog.logI("BURLCLI",
         "   filter : text or key=text or key:text where key is a field identifier");
}



/********************************************************************************/
/*                                                                              */
/*      Set entry command                                                       */
/*                                                                              */
/********************************************************************************/

void handleSetEntry(List<String> args) 
{
   Number entid = null;
   for (String s : args) {
      if (s.startsWith("-")) {
         badSetEntryArgs();
         return;
       }
      else if (s.matches("[0-9]+") && entid == null) {
         entid = Integer.parseInt(s);
       }
      else {
         badSetEntryArgs();
         return;
       }
    }
   
   if (entid == null) {
      entid = cli_main.getEntryId();
      if (entid == null) {
         badSetEntryArgs();
         return;
       }
    }
   if (entid.intValue() == 0) entid = null;
   
   if (!setEntry(entid) && entid != null) {
      IvyLog.logI("BURLCLI","Bad entry number");
      return;
    }
   
   if (entid == null) return;
   
   fullEntryDisplay(cur_entry);
   System.out.println();
}


private void badSetEntryArgs()
{
   IvyLog.logI("BURLCLI","entry [<id>]");
}


boolean setEntry(Number entid)
{
   Number oldid = cli_main.getEntryId();
   Number libid = cli_main.getLibraryId();
   if (libid == null) return false;
   if (entid != null) {
      JSONObject data = BurlUtil.buildJson("library",libid,"entry",entid);
      JSONObject rslt = cli_main.createHttpPost("getentry",data);
      if (!cli_main.checkResponse(rslt,null)) {
         entid = null;
       }
      else {
         cur_entry = rslt.getJSONObject("entry");
       }
    }
   cli_main.setEntryId(entid);
   if (entid != null && !entid.equals(oldid)) {
      IvyLog.logI("BURCLI","Entry set to " + entid);
    }
   
   return (entid != null);
}




/********************************************************************************/
/*                                                                              */
/*      Edit command                                                            */
/*                                                                              */
/********************************************************************************/

void handleEditEntry(List<String> args)
{
   JSONObject edits = new JSONObject();
   for (String s : args) {
      String key = null;
      String value = null;
      
      if (s.startsWith("-")) {
         badEditEntryArgs();
         return;
       }
      else if (s.contains(":") || s.contains("=")) {
         int idx1 = s.indexOf(":");
         int idx2 = s.indexOf("=");
         int idx;
         if (idx1 < 0) idx = idx2;
         else if (idx2 < 0) idx = idx1;
         else idx = Math.min(idx1,idx2);
         
         key = s.substring(0,idx).trim();
         if (!field_data.isValidField(key)) { 
            badEditEntryArgs();
            return;
          }
         value = s.substring(idx+1).trim();
       }
      else {
         badEditEntryArgs();
       }
      
      String prev = edits.optString(key,null);
      if (prev != null) {
         value = prev + field_data.getMultiple() + value;
       }
      edits.put(key,value);
    }
   
   if (edits.isEmpty()) {
      badEditEntryArgs();
      return;
    }
   
   Number libid = cli_main.getLibraryId();
   Number entid = cli_main.getEntryId();
   JSONObject data = BurlUtil.buildJson("library",libid,"entry",entid,
         "edits",edits);
   JSONObject rslt = cli_main.createHttpPost("editentry",data);
   if (cli_main.checkResponse(rslt,"edit")) {
      cur_entry = rslt.getJSONObject("entry");
      fullEntryDisplay(cur_entry);
      System.out.println();
    }
}


private void badEditEntryArgs()
{
   IvyLog.logI("BURLCLI","edit <set> [<set> ...]");
   IvyLog.logI("BURLCLI",
         "   set : field=text or field:text");
}





/********************************************************************************/
/*                                                                              */
/*      Entry displays                                                          */
/*                                                                              */
/********************************************************************************/

private void shortEntryDisplay(JSONObject entry)
{
   Number entid = entry.getNumber("burl_id");
   
   entryDisplay(entry,cli_main.getShortDisplayFields(),entid,90,4,false);
}


private void fullEntryDisplay(JSONObject entry)
{
   entryDisplay(entry,field_data.getAllFields(),null,90,4,true);
}



private void entryDisplay(JSONObject entry,Collection<String> fields,Number prefix,
      int linelen,int indent,boolean blanks)
{
   int pfxlen = (prefix == null ? 0 : 8);
   int hdrlen = 0;
   for (String s : fields) {
      hdrlen = Math.max(hdrlen,s.length() + 2);
    }
   
   int lno = 0;
   for (String fld : fields) { 
      int pos = 0;
      String val = getEntryValue(entry,fld,field_data.getMultiple());
      if (val == null && !blanks) continue;
      if (lno++ == 0 && prefix != null) {
         String e = "[" + prefix + "]";
         System.out.print(e);
         pos += e.length();
       }
      if (fld.isEmpty()) {                      // empty field yields a blank line
         System.out.println();
         continue;
       }
      for ( ; pos < pfxlen; ++pos) {
         System.out.print(" ");
       }
      if (fld != null) {
         String e = fld + ": ";
         System.out.print(e);
         pos += e.length();
       }
      for ( ; pos < pfxlen+hdrlen; ++pos) {
         System.out.print(" ");
       }
      
      List<String> lines = splitString(val,linelen-pos,4);
      if (lines.isEmpty()) System.out.println();
      for (int i = 0; i < lines.size(); ++i) {
         String ln = lines.get(i);
         if (i != 0) {
            for (int p = 0; p < pfxlen+hdrlen+indent; ++p) {
               System.out.print(" ");
             }
          }
         System.out.println(ln);
       }
    }
}



private String getEntryValue(JSONObject entry,String fld,String split)
{
   JSONObject valobj = entry.optJSONObject(fld);
   if (valobj == null) {
      valobj = entry.optJSONObject(field_data.getBaseName(fld));
    }
   if (valobj == null) {
      valobj = entry.optJSONObject(field_data.getLabel(fld));
    }
   String val = null;
   if (valobj != null) {
      Object valset = valobj.opt("value");
      if (valset instanceof String) {
         val = valset.toString();
       }
      else if (valset instanceof JSONArray) {
         JSONArray jarr = (JSONArray) valset;
         List<String> items = new ArrayList<>();
         for (Object obj : jarr) {
            items.add(obj.toString());
          }
         val = String.join(split,items);
       }
    }
   
   return val;
}


List<String> splitString(String val,int len,int indent)
{
   List<String> rslt = new ArrayList<>();
   String [] lines = val.split("\n");
   for (String s : lines) {
      List<String> ln1 = splitOneString(s,len,indent);
      rslt.addAll(ln1);
    }
   
   return rslt;
}



List<String> splitOneString(String val,int len,int indent)
{
   List<String> rslt = new ArrayList<>();
   
   if (val == null) return rslt;
   int nlen = len-indent;
   int olen = len;
   
   while (val.length() > olen) {
      String nval = null;
      int sp0 = findBreak(val," /",olen);
      int sp1 = findBreak(val," | ",olen);
      int sp2 = findBreak(val," ",olen);
      if (sp0 > olen / 2 || sp0 > sp1) sp1 = sp0;
      if (sp1 < olen / 2 && sp2 > sp1) sp1 = sp2;
      if (sp1 <= 0) sp1 = olen;
      
      nval = val.substring(sp1+1).trim();
      val = val.substring(0,sp1).trim();
      rslt.add(val);
      if (nval == null || nval.isEmpty()) break;
      val = nval;
      olen = nlen;
    }
   
   if (val != null && !val.isEmpty()) {
      rslt.add(val);
    }
   
   return rslt;
}


int findBreak(String val,String sep,int len)
{
   int sp = 0;
   for ( ; ; ) {
      int nsp = val.indexOf(sep,sp+1);
      if (nsp > len || nsp < 0) break;
      sp = nsp;
    }
   return sp;
}




}       // end of class CliEntryCommands




/* end of CliEntryCommands.java */

