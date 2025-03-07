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

import java.util.HashMap;
import java.util.Iterator;
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
   
   JSONObject jobj = BurlUtil.buildJson("entityid",row.getRowId());
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
   String filterstr = BowerRouter.getParameter(he,"filter");
   String filterid = BowerRouter.getParameter(he,"filterid");
   
   BurlLibrary lib = burl_store.findLibraryById(libid);
   if (lib == null) {
      return BowerRouter.errorResponse(he,session,402,"Bad library");
    }
   BurlRepo repo = lib.getRepository();
   
   String orderby = BowerRouter.getParameter(he,"orderby");
   boolean invert = BowerRouter.getBooleanParameter(he,"invert",false);
   BurlRepoColumn sortfld = null;
   if (orderby != null && !orderby.isBlank()) {
      sortfld = repo.getColumn(orderby);
      if (sortfld == null) {
         return BowerRouter.errorResponse(he,session,400,"Bad sort field");
       }
    } 
   
   BurlRowIter iter = null; 
   if (filterid != null) {
      iter = known_filters.remove(filterid);
      if (count < 0) {
         return BowerRouter.jsonOKResponse(session);
       }
    }
   
   if (iter == null && filterstr != null) {
      JSONObject jsonfilter = null;
      if (filterstr.startsWith("{")) {
         jsonfilter = new JSONObject(filterstr);
       }
      else {
         jsonfilter = BurlUtil.buildJson("all",filterstr);
       }
      EntityFilter filter = new EntityFilter(jsonfilter,repo,sortfld,invert);
      iter = repo.getRows(filter);
    }
   else if (iter == null) {
      iter = repo.getRows(sortfld,invert);  
    }
   JSONArray results = new JSONArray();
   
   int ct = 0;
   
   if (filterid == null) {
      filterid = BurlUtil.randomString(12);
    }
   if (count >= 0 && iter != null) {
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
   
   return BowerRouter.jsonOKResponse(session,"count",itcnt,
         "data",results,"filterid",filterid);
}




/********************************************************************************/
/*                                                                              */
/*      Handle editing an entry                                                 */
/*                                                                              */
/********************************************************************************/

String handleEditEntry(HttpExchange he,ControlSession session)
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


private Boolean canEdit(BurlUserAccess acc,BurlRepoColumn brc)
{
   if (acc == BurlUserAccess.NONE || acc == BurlUserAccess.VIEWER) return false;
   
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
            if (data.contains(t)) return true;
          }
         return false;
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

