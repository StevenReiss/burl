/********************************************************************************/
/*                                                                              */
/*              ControlEntries.java                                             */
/*                                                                              */
/*      Handle commands dealing with enteries                                   */
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;

import edu.brown.cs.burl.burl.BurlFilter;
import edu.brown.cs.burl.burl.BurlLibrary;
import edu.brown.cs.burl.burl.BurlRepo;
import edu.brown.cs.burl.burl.BurlRepoColumn;
import edu.brown.cs.burl.burl.BurlRepoRow;
import edu.brown.cs.burl.burl.BurlUser;
import edu.brown.cs.burl.burl.BurlUtil;
import edu.brown.cs.ivy.bower.BowerRouter;

class ControlEntries implements ControlConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ControlServer   burl_server;
private ControlStorage  burl_store;
private ControlMain     burl_main;

private Map<String,BurlRowIter> known_filters;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ControlEntries(ControlMain main,ControlServer server)
{
   burl_main = main;
   burl_server = server;
   burl_store = burl_main.getStorage();
   known_filters = new HashMap<>();
}


/********************************************************************************/
/*                                                                              */
/*      Get Entry command                                                       */
/*                                                                              */
/********************************************************************************/

String handleGetEntry(HttpExchange he,ControlSession session)
{
   Number libid = burl_server.getIdParameter(he,"library");
   Number entid = burl_server.getIdParameter(he,"entry");
   
   if (libid == null || entid == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad arguments");
    }
   BurlLibrary lib = burl_store.findLibraryById(libid);
   if (lib == null) {
      return BowerRouter.errorResponse(he,session,402,"Bad library");
    }
   
   BurlUserAccess useracc = burl_server.validateLibrary(session,libid); 
   if (useracc == BurlUserAccess.NONE) {
      return BowerRouter.errorResponse(he,session,402,"Not authorized");
    }
   
   BurlRepo repo = lib.getRepository();
   BurlRepoRow brr = repo.getRowForId(entid);
   if (brr == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad entry id");
    }
   
   JSONObject jobj = brr.toJson();
   
   return BowerRouter.jsonOKResponse(session,"entry",jobj);
}



/********************************************************************************/
/*                                                                              */
/*      Handle add entry command to add an empty entry                          */
/*                                                                              */
/********************************************************************************/

String handleAddEntry(HttpExchange he,ControlSession session)
{
   Number libid = burl_server.getIdParameter(he,"library");
   BurlLibrary lib = burl_store.findLibraryById(libid);
   if (lib == null) {
      return BowerRouter.errorResponse(he,session,402,"Bad library");
    }
   
   BurlUserAccess acc = burl_server.validateLibrary(session,libid);
   switch (acc) {
      case NONE :
      case VIEWER :
      case EDITOR :
         return BowerRouter.errorResponse(he,session,400,"Not authorized");
      case OWNER :
      case LIBRARIAN :
         break;
    }
   
   BurlRepo repo = lib.getRepository();
   BurlRepoRow row = repo.newRow();
   if (row == null) {
      return BowerRouter.errorResponse(he,session,402,"Problem adding row");
    }
   
   JSONObject jobj = BurlUtil.buildJson("burl_id",row.getRowId());
   return BowerRouter.jsonOKResponse(session,"entry",jobj);
}



/********************************************************************************/
/*                                                                              */
/*      Handle duplicate entry command to create a new entry from existing one  */
/*                                                                              */
/********************************************************************************/

String handleDuplicateEntry(HttpExchange he,ControlSession session)
{
   BurlUser user = session.getUser();
   Number libid = burl_server.getIdParameter(he,"library");
   BurlLibrary lib = burl_store.findLibraryById(libid);
   if (lib == null) {
      return BowerRouter.errorResponse(he,session,402,"Bad library");
    }
   BurlRepo repo = lib.getRepository();
   Number entid = burl_server.getIdParameter(he,"entry");
   BurlRepoRow oldrow = repo.getRowForId(entid);
   if (oldrow == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad entity");
    }
   if (user == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad user");
    } 
   
   BurlUserAccess acc = burl_server.validateLibrary(session,libid);
   switch (acc) {
      case NONE :
      case VIEWER :
      case EDITOR :
         return BowerRouter.errorResponse(he,session,400,"Not authorized");
      case OWNER :
      case LIBRARIAN :
         break;
    }
   
   BurlRepoRow row = repo.newRow();
   if (row == null) {
      return BowerRouter.errorResponse(he,session,402,"Problem adding row");
    }
   for (BurlRepoColumn brc : repo.getColumns()) {
      String v = oldrow.getData(brc);
      if (v != null) {
         if (brc.getDefault() != null && brc.getDefault().equals("1")) {
            try {
               int v0 = Integer.parseInt(v);
               v = Integer.toString(v0+1);
             }
            catch (NumberFormatException e) { }
          }
         row.setData(brc,v);
       }
    }
   
   JSONObject jobj = row.toJson();
   return BowerRouter.jsonOKResponse(session,"entry",jobj);
}




/********************************************************************************/
/*                                                                              */
/*      Get entries command                                                     */
/*                                                                              */
/********************************************************************************/

String handleFindEntries(HttpExchange he,ControlSession session)
{
   Number libid = burl_server.getIdParameter(he,"library");
   int count = BowerRouter.getIntParameter(he,"count",20);
   
   BurlLibrary lib = burl_store.findLibraryById(libid);
   if (lib == null) {
      return BowerRouter.errorResponse(he,session,402,"Bad library");
    }
   BurlRepo repo = lib.getRepository();
   
   
   BurlRowIter iter = null; 
   String filterid = BowerRouter.getParameter(he,"filterid");
   if (filterid != null) {
      iter = known_filters.remove(filterid);
      if (count < 0) {
         return BowerRouter.jsonOKResponse(session);
       }
      else if (iter == null) {
         return BowerRouter.errorResponse(he,session,400,"Bad iterator");
       }
    }
   
   if (iter == null) {
      String filterstr = BowerRouter.getParameter(he,"filter");
      String orderby = BowerRouter.getParameter(he,"orderby");
      boolean invert = BowerRouter.getBooleanParameter(he,"invert",false);
      iter = getRowIterator(repo,filterstr,orderby,invert);
      if (iter == null) {
         return BowerRouter.errorResponse(he,session,400,"Bad sort field");
       }
    }
   
   JSONArray results = new JSONArray();
   int ct = 0;
   int start = 0;
   
   if (filterid == null) {
      filterid = BurlUtil.randomString(12);
    }
   if (count >= 0 && iter != null) {
      start = iter.getIndex();
      for (Iterator<BurlRepoRow> it = iter.iterator(); it.hasNext(); ) {
         BurlRepoRow br = it.next();
         results.put(br.toJson());
         ++ct;
         if (count > 0 && ct == count) {
            known_filters.put(filterid,iter); 
            break;
          }
       }
      if (!known_filters.containsKey(filterid)) filterid = null;
    }
   else {
      filterid = null;
    }
   
   int itcnt = (iter == null ? 0 : iter.getRowCount());
   
   return BowerRouter.jsonOKResponse(session,"count",itcnt,"start",start,
         "data",results,"filterid",filterid);
}



private BurlRowIter getRowIterator(BurlRepo repo,String filterstr,String orderby,boolean invert)
{
   BurlRepoColumn sortfld = null;
   if (orderby != null && !orderby.isBlank()) {
      sortfld = repo.getColumn(orderby);
      if (sortfld == null) {
         return null;
       }
    } 
   
   BurlRowIter iter = null; 
   
   if (filterstr != null) {
      JSONObject jsonfilter = null;
      if (filterstr.startsWith("{")) {
         jsonfilter = new JSONObject(filterstr);
       }
      else {
         jsonfilter = buildFilterObject(repo,filterstr);
       }
      EntityFilter filter = new EntityFilter(jsonfilter,repo,sortfld,invert);
      iter = repo.getRows(filter);
    }
   else if (iter == null) {
      iter = repo.getRows(sortfld,invert);  
    }
   
   return iter;
}



private JSONObject buildFilterObject(BurlRepo repo,String filterstr)
{
   List<String> tokens = BurlUtil.tokenize(filterstr);
   JSONObject filters = new JSONObject();
   
   for (String s : tokens) {
      String key = null;
      String value = null;
      if (s.contains(":") || s.contains("=")) {
         int idx1 = s.indexOf(":");
         int idx2 = s.indexOf("=");
         int idx;
         if (idx1 < 0) idx = idx2;
         else if (idx2 < 0) idx = idx1;
         else idx = Math.min(idx1,idx2);
         key = s.substring(0,idx).trim();
         BurlRepoColumn brc = repo.getColumn(key);
         if (brc == null) {
            key = "all";
          }
         value = s.substring(idx+1).trim();
       }
      else {
         value = s;
       }
      String okey = key;
      if (key == null) key = "all";
      Object prev = filters.opt(key);
      if (prev == null) {
         filters.put(key,value);
       }
      else if (prev instanceof String) {
         if (okey == null) {
            filters.put(key,prev + " " + value);
          }
         else {
            JSONArray nval = new JSONArray();
            nval.put(prev);
            nval.put(value);
            filters.put(key,nval);
          }
       }
      else if (prev instanceof JSONArray) {
         JSONArray nval = (JSONArray) prev;
         nval.put(value);
       }
    }
   
   return filters;
// return BurlUtil.buildJson("all",filterstr);
}


/********************************************************************************/
/*                                                                              */
/*      Handle editing an entry                                                 */
/*                                                                              */
/********************************************************************************/

String handleEditEntry(HttpExchange he,ControlSession session)
{
   BurlUser user = session.getUser();
   if (user == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad user");
    }
   Number libid = burl_server.getIdParameter(he,"library");
   BurlLibrary lib = burl_store.findLibraryById(libid);
   if (lib == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad library");
    }
   BurlRepo repo = lib.getRepository();
   Number entid = burl_server.getIdParameter(he,"entry");
   BurlRepoRow row = repo.getRowForId(entid);
   if (row == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad entity");
    }
   JSONObject edits = BowerRouter.getJson(he,"edits");
   if (edits == null) {
      return BowerRouter.errorResponse(he,session,400,"No edits");
    }
   
   BurlUserAccess acc = burl_store.getUserAccess(user.getEmail(),libid);
   Map<BurlRepoColumn,String> todo = new HashMap<>();
   for (String fld : JSONObject.getNames(edits)) {
      String newval = edits.optString(fld);
      BurlRepoColumn brc = repo.getColumn(fld);
      if (brc == null) {
         return BowerRouter.errorResponse(he,session,400,"Bad field " + fld);
       }
      String oldval = row.getData(brc);
      if (oldval == null) oldval = "";
      if (newval == null) newval = "";
      if (newval.equals(oldval)) continue;
      if (!canEdit(acc,brc)) {
         return BowerRouter.errorResponse(he,session,402,"Not authorized");
       }
      todo.put(brc,newval);
    }
  
   for (Map.Entry<BurlRepoColumn,String> ent : todo.entrySet()) {
      row.setData(ent.getKey(),ent.getValue());
    }
   
   return BowerRouter.jsonOKResponse(session,"entry",row.toJson());
}



/********************************************************************************/
/*                                                                              */
/*      Handle batch edit of a field                                            */
/*                                                                              */
/********************************************************************************/

String handleGroupEdit(HttpExchange he,ControlSession session)
{
   BurlUser user = session.getUser();
   if (user == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad user");
    }
   Number libid = burl_server.getIdParameter(he,"library");
   BurlLibrary lib = burl_store.findLibraryById(libid);
   if (lib == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad library");
    }
   BurlRepo repo = lib.getRepository();
   String fld = BowerRouter.getParameter(he,"field");
   BurlRepoColumn brc = repo.getColumn(fld);
   if (brc == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad field");
    }
   BurlUserAccess acc = burl_store.getUserAccess(user.getEmail(),libid);
   if (!canEdit(acc,brc)) {
      return BowerRouter.errorResponse(he,session,402,"Not authorized");
    }
   
   String ents0 = BowerRouter.getParameter(he,"items");
   List<String> ents = new ArrayList<>();
   StringTokenizer tok = new StringTokenizer(ents0," ,[]");
   while (tok.hasMoreTokens()) {
      String s = tok.nextToken();
      ents.add(s);
    }
   if (ents == null || ents.isEmpty()) {
      return BowerRouter.errorResponse(he,session,400,"Bad entry set");
    }
   String val = BowerRouter.getParameter(he,"value");
   if (val == null) val = "";
   
   for (String ent : ents) {
      Number n = null;
      try {
         n = Integer.parseInt(ent);
       }
      catch (NumberFormatException e) {
         continue;
       }
      BurlRepoRow row = repo.getRowForId(n);
      if (row == null) continue;
      row.setData(brc,val);
    }
   
   return BowerRouter.jsonOKResponse(session);
}


private Boolean canEdit(BurlUserAccess acc,BurlRepoColumn brc)
{
   if (acc == BurlUserAccess.NONE || acc == BurlUserAccess.VIEWER) return false;
   if (brc.isHidden()) return false;
   
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Remove entry command                                                    */
/*                                                                              */
/********************************************************************************/

String handleRemoveEntry(HttpExchange he,ControlSession session)
{
   BurlUser user = session.getUser();
   Number libid = burl_server.getIdParameter(he,"library");
   BurlLibrary lib = burl_store.findLibraryById(libid);
   if (lib == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad library");
    }
   BurlRepo repo = lib.getRepository();
   Number entid = burl_server.getIdParameter(he,"entry");
   BurlRepoRow row = repo.getRowForId(entid);
   if (row == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad entity");
    }
   if (user == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad user");
    }
   BurlUserAccess acc = burl_store.getUserAccess(user.getEmail(),libid);
   switch (acc) {
      case LIBRARIAN :
      case OWNER :
         break;
      case EDITOR :
      case NONE :
      case VIEWER :
         return BowerRouter.errorResponse(he,session,402,"Unauthorized");
    }
   
   repo.removeRow(entid);
   
   return BowerRouter.jsonOKResponse(session);
}




/********************************************************************************/
/*                                                                              */
/*      Handle export entries                                                   */
/*                                                                              */
/********************************************************************************/

String handleExportEntries(HttpExchange he,ControlSession session)
{
   Number uid = session.getUserId();
   if (uid == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad user");
    }
   Number lid = burl_server.getIdParameter(he,"library");
   if (lid == null) lid = session.getLibraryId();
   if (lid == null) {
      return BowerRouter.errorResponse(he,session,400,"No library given");
    }
   ControlLibrary lib = burl_store.findLibraryById(lid);
   if (lib == null) {
      return BowerRouter.errorResponse(he,session,400,"No library given");
    }
   BurlRepo repo = lib.getRepository();
   if (repo == null) { 
      return BowerRouter.errorResponse(he,session,400,"Bad repository");
    }  
   
   BurlExportFormat exp = BowerRouter.getEnumParameter(he,"format",BurlExportFormat.CSV);
   BurlUserAccess useracc = burl_server.validateLibrary(session,lid);
   switch (useracc) {
      case NONE :
         return BowerRouter.errorResponse(he,session,400,"Not authorized");
      case VIEWER :
      case EDITOR :
      case OWNER :
      case LIBRARIAN :
         break; 
    }
   
   String filterstr = BowerRouter.getParameter(he,"filter");
   String orderby = BowerRouter.getParameter(he,"orderby");
   boolean invert = BowerRouter.getBooleanParameter(he,"invert",false);
   BurlRowIter iter = getRowIterator(repo,filterstr,orderby,invert);
   if (iter == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad sort field");
    }
   
   String sfx = null;
   switch (exp) {
      default :
      case CSV :
         sfx = ".csv";
         break;
      case JSON :
         sfx = ".json";
         sfx = ".txt";
         break;
    }
   File f1 = null;
   try {
      f1 = File.createTempFile("Burl_" + lib.getName(),sfx);
    }
   catch (IOException e) {
      return BowerRouter.errorResponse(he,session,500,"Problem with temp file");
    }
   
  
   
   repo.exportRepository(f1,exp,iter);  
   
   String resp = BowerRouter.sendFileResponse(he,f1); 
   
   f1.delete();
   
   return resp;
}

/********************************************************************************/
/*                                                                              */
/*      Handle fix fields command                                               */
/*                                                                              */
/********************************************************************************/

String handleFixFields(HttpExchange he,ControlSession session)
{
   BurlUser user = session.getUser();
   if (user == null) {
      return BowerRouter.errorResponse(he,session,402,"Bad user");
    }
   Number libid = burl_server.getIdParameter(he,"library");
   BurlLibrary lib = burl_store.findLibraryById(libid);
   if (lib == null) {
      return BowerRouter.errorResponse(he,session,402,"Bad library");
    }
   BurlRepo repo = lib.getRepository();   
   BurlUserAccess acc = burl_store.getUserAccess(user.getEmail(),libid);
   if (acc != BurlUserAccess.OWNER) {
      return BowerRouter.errorResponse(he,session,402,"Unauthorized");
    }
   
   boolean updonly = false;
   String option = BowerRouter.getParameter(he,"option");
   if (option.equals("update")) updonly = true;
   
   for (BurlRepoRow row : repo.getRows()) {
      if (updonly) {
         for (BurlRepoColumn brc : repo.getColumns()) {
            if (brc.getUpdateFieldName() == null) continue;
            String oval = row.getData(brc);
            if (oval == null || oval.isBlank()) continue;
            row.setData(brc,oval);
          }
       }
      else {
         for (BurlRepoColumn brc : repo.getColumns()) {
            BurlFixType ftyp = brc.getFixType();
            if (ftyp == BurlFixType.NONE) continue;
            String oval = row.getData(brc);
            String nval = brc.fixFieldValue(oval);
            if ((oval == null || oval.isBlank()) && (nval == null || nval.isBlank())) continue;
            if (oval != null && oval.equals(nval)) continue;
            row.setData(brc,nval);
          }
       }
    }
   
   return BowerRouter.jsonOKResponse(session);
}



/********************************************************************************/
/*                                                                              */
/*      Filter implementation                                                   */
/*                                                                              */
/********************************************************************************/

private class EntityFilter implements BurlFilter {
   
   private JSONObject filter_data;
   private BurlRepo for_repo;
   private BurlRepoColumn sort_field;
   private boolean invert_sort;
   
   EntityFilter(JSONObject data,BurlRepo repo,BurlRepoColumn sort,boolean invert) {
      filter_data = data;
      for_repo = repo;
      sort_field = sort;
      invert_sort = invert;
    } 
    
   @Override public boolean matches(BurlRepoRow row) {
      Object all = filter_data.opt("all");
      if (all != null) {
         boolean matchany = false;
         for (BurlRepoColumn brc : for_repo.getColumns()) {
            String data = row.getData(brc);
            if (data == null || data.isEmpty()) continue;
            if (matchItem(data,all)) {
               matchany = true;
               break;
             }
          }
         if (!matchany) return false;
       }
      String [] flds = JSONObject.getNames(filter_data);
      if (flds == null || flds.length == 0) return true;
      
      for (String key : JSONObject.getNames(filter_data)) { 
         if (key.equals("all")) continue;
         BurlRepoColumn brc = for_repo.getColumn(key);
         if (brc == null) continue;
         Object match = filter_data.opt(key);
         if (match == null) continue;
         String data = row.getData(brc);
         if (!matchItem(data,match)) return false;
       }
      return true;
    } 
   
   private boolean matchItem(String data,Object keyobj) {
      data = data.toLowerCase();
      if (keyobj instanceof String) {
         String key = keyobj.toString().toLowerCase();
         for (StringTokenizer tok = new StringTokenizer(key); tok.hasMoreTokens(); ) {
            String t = tok.nextToken();
            if (!data.contains(t)) return false;
          }
         return true;
       }
      else if (keyobj instanceof JSONArray) {
         JSONArray arr = (JSONArray) keyobj;
         boolean match = false;
         for (int i = 0; i< arr.length(); ++i) {
            String s = arr.getString(i).toLowerCase();
            if (matchItem(data,s)) {
               match = true;
               break;
             }
          }
         return match;
       }
      return false;
    }
   
   @Override public BurlRepoColumn getSortField() {
      return sort_field;
    }
   
   @Override public boolean invertSort() {
      return invert_sort;
    }
   
}       // end of inner class EntityFilter




}       // end of class ControlEntries



/* end of ControlEntries.java */

