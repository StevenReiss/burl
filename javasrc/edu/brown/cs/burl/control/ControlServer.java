/********************************************************************************/
/*                                                                              */
/*              ControlServer.java                                              */
/*                                                                              */
/*      Http Server for BURL                                                    */
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import com.sun.net.httpserver.HttpExchange;

import edu.brown.cs.ivy.bower.BowerServer;
import edu.brown.cs.ivy.bower.BowerUtil;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.burl.burl.BurlException;
import edu.brown.cs.burl.burl.BurlLibrary;
import edu.brown.cs.burl.burl.BurlRepo;
import edu.brown.cs.burl.burl.BurlRepoColumn;
import edu.brown.cs.burl.burl.BurlUser;
import edu.brown.cs.burl.burl.BurlUtil;
import edu.brown.cs.ivy.bower.BowerCORS;
import edu.brown.cs.ivy.bower.BowerRouter;

class ControlServer implements ControlConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BowerServer<ControlSession> http_server;
private ControlMain burl_main;
private ControlSessionStore session_store;
private ControlAuthentication burl_auth;
private ControlEntries entry_manager;
private ControlStorage burl_store;
private ControlWorkThread work_thread;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ControlServer(ControlMain main,String keystorepwd)
{
   session_store = new ControlSessionStore(main); 
   
   File f1 = main.getBaseDirectory(); 
   File f2 = new File(f1,"secret");
   File f3 = new File(f2,"catre.jks");
   
   burl_main = main;
   burl_auth = new ControlAuthentication(main,session_store); 
   entry_manager = new ControlEntries(main,this);  
   burl_store = main.getStorage();
   work_thread = new ControlWorkThread(main);
   work_thread.start();
   
   BowerRouter<ControlSession> br = setupRouter();
   
   http_server = new BowerServer<>(HTTPS_PORT,session_store);
   http_server.setRouter(br);
   
   if (keystorepwd != null) {
      http_server.setupHttps(f3,keystorepwd);
    }
}



/********************************************************************************/
/*										*/
/*	Start the servers							*/
/*										*/
/********************************************************************************/

void start() throws BurlException
{
   if (!http_server.start()) {
      throw new BurlException("Can't start web service");
    }
   IvyLog.logD("BURL","BURL server set up on port " + HTTPS_PORT);
}



/********************************************************************************/
/*										*/
/*	Setup the router							*/
/*										*/
/********************************************************************************/

BowerRouter<ControlSession> setupRouter()
{
   BowerRouter<ControlSession> br = new BowerRouter<>(session_store);
   br.addRoute("ALL",BowerRouter::handleParameters);
   br.addRoute("ALL",BowerRouter::handleLogging);
   br.addRoute("ALL",br::handleSessions); 
   br.addRoute("ALL",new BowerCORS("*"));
   
   br.addRoute("ALL","/rest/ping",this::handlePing);
 
   br.addRoute("GET","/rest/login",burl_auth::handlePreLogin);
   br.addRoute("GET","/rest/register",burl_auth::handlePreRegister); 
   br.addRoute("POST","/rest/login",burl_auth::handleLogin);
   br.addRoute("POST","/rest/register",burl_auth::handleRegister);
   br.addRoute("ALL","/rest/logout",burl_auth::handleLogout);
   
   br.addRoute("GET","/validate",burl_auth::handleValidationRequest);
   br.addRoute("GET","/rest/validate",burl_auth::handleValidationRequest);
   br.addRoute("ALL","/rest/forgotpassword",burl_auth::handleForgotPassword);
   
   br.addRoute("ALL","/rest/fielddata",this::handleFieldData);
   br.addRoute("ALL","/rest/log",this::handleLog);
   
   br.addRoute("USE",burl_auth::handleAuthentication);
   
   br.addRoute("POST","/rest/addlibraryuser",this::handleAddLibraryUser);
   br.addRoute("POST","/rest/createlibrary",this::handleCreateLibrary);
   br.addRoute("POST","/rest/findlibraries",this::handleFindAllLibraries);
   br.addRoute("POST","/rest/removelibrary",this::handleRemoveLibrary);
   br.addRoute("POST","/rest/labels",this::handlePrintLabels);
   
   br.addRoute("POST","/rest/addisbns",this::handleAddIsbns);
   br.addRoute("POST","/rest/import",this::handleImport);
   
   br.addRoute("POST","/rest/getentry",entry_manager::handleGetEntry);
   br.addRoute("POST","/rest/entries",entry_manager::handleFindEntries); 
   br.addRoute("POST","/rest/editentry",entry_manager::handleEditEntry); 
   br.addRoute("POST","/rest/groupedit",entry_manager::handleGroupEdit); 
   br.addRoute("POST","/rest/removeentry",entry_manager::handleRemoveEntry); 
   br.addRoute("POST","/rest/duplicateentry",entry_manager::handleDuplicateEntry); 
   br.addRoute("POST","/rest/addentry",entry_manager::handleAddEntry); 
   br.addRoute("POST","/rest/exportlibrary",entry_manager::handleExportEntries); 
   br.addRoute("POST","/rest/fixfields",entry_manager::handleFixFields); 
   
// br.addRoute("POST","/rest/removeentry",this::handleRemoveEntry);
   
   br.addRoute("GET","/rest/changepassword",burl_auth::handlePreChangePassword); 
   br.addRoute("POST","/rest/changepassword",burl_auth::handleChangePassword);
   br.addRoute("POST","/rest/removeuser",burl_auth::handleRemoveUser); 
   
   br.addRoute("ALL","/rest/about",this::handleAbout);
   
   br.addRoute("ALL",this::handle404);
   br.addErrorHandler(this::handleError);
 
   return br;
}




/********************************************************************************/
/*                                                                              */
/*      Basic responses                                                         */
/*                                                                              */
/********************************************************************************/

String handlePing(HttpExchange he,ControlSession session)
{
   return BowerRouter.jsonOKResponse(session,"PONG",true);
}


String handle404(HttpExchange he,ControlSession session) 
{
   return BowerRouter.errorResponse(he,session,404,"Invalid URL");
}


String handleError(HttpExchange he,ControlSession session)
{
   Object t = he.getAttribute(BowerRouter.BOWER_EXCEPTION);
   String msg = "Internal error";
   if (t != null) {
      msg += ": " + t;
    }
   
   return BowerRouter.errorResponse(he,session,500,msg);
}


String handleLog(HttpExchange he,ControlSession session) 
{
   // enough logging is probably done using the Bower logger.
   // otherwise can provide additional information here
   
   return BowerRouter.jsonOKResponse(session);
}



/********************************************************************************/
/*                                                                              */
/*      Library methods                                                         */
/*                                                                              */
/********************************************************************************/

String handleAddLibraryUser(HttpExchange he,ControlSession session)
{
   String email = BowerRouter.getParameter(he,"email");
   BurlUser user = session.getUser();
   if (email != null && email.equals("*")) {
      email = user.getEmail();
    }
   if (!BowerUtil.validateEmail(email)) {
      return BowerRouter.errorResponse(he,session,400,"Bad email");
    }
   email = email.toLowerCase();
   
   BurlUserAccess acc = BowerRouter.getEnumParameter(he,"access",BurlUserAccess.NONE);
   Number libid = getIdParameter(he,"library");
   if (libid == null) {
      libid = session.getLibraryId();
    }
   BurlUserAccess useracc = validateLibrary(session,libid);
   
   if (user.getEmail() == email && acc == BurlUserAccess.NONE) {
      // always allow user to remove themselves
      useracc = BurlUserAccess.LIBRARIAN;
    }
   
   switch (useracc) {
      case NONE :
      case VIEWER :
      case EDITOR :
         return BowerRouter.errorResponse(he,session,402,"Not authorized");
      case OWNER :
         // might want to not remove the only owner of a library here
      case LIBRARIAN :
         break; 
    }
   
   ControlLibrary lib = burl_store.findLibraryById(libid);
   if (lib == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad library");
    }
   
   BurlUserAccess oldacc = burl_store.getUserAccess(email,libid);
   
   burl_store.setUserAccess(email,libid,acc);
   
   if (oldacc == BurlUserAccess.NONE && acc != BurlUserAccess.NONE) {
      Properties props = burl_main.getProperties();
      String host = props.getProperty("webaddress","https://sherpa.cs.brown.edu/burl");
      String an = "a ";
      if (acc == BurlUserAccess.OWNER) an = "an ";
      String body = "You have been invited to access the library " + lib.getName() +
         " by " + user.getEmail() + " as " + an + acc + ".\n" +
         "If you haven't already, please register with BURL (" + 
         host + ").\n";
      boolean fg = BurlUtil.sendEmail(email,"BURL Library Invitation",body);
      if (!fg) {
         
         // try again -- gmail is fussy
         BurlUtil.sendEmail(email,"BURL Library Invitation",body);
       }
    }
      
   return BowerRouter.jsonOKResponse(session);
}


String handleCreateLibrary(HttpExchange he,ControlSession session)
{
   String name = BowerRouter.getParameter(he,"name");
   if (name == null || name.isEmpty()) {
      return BowerRouter.errorResponse(he,session,400,"Bad library name");
    }
   BurlRepoType repotype = BowerRouter.getEnumParameter(he,"repotype",BurlRepoType.DATABASE);
   BurlLibrary lib = burl_main.createLibrary(name,session.getUser(),repotype);
   
   if (lib == null) {
      return BowerRouter.errorResponse(he,session,400,"Unable to create library");
    }
   
   JSONObject jo = lib.toJson(session.getUser());
   
   return BowerRouter.jsonOKResponse(session,jo);
}



String handleFindAllLibraries(HttpExchange he,ControlSession session)
{
   ControlUser user = session.getUser();
   if (user == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad user");
    }
   Collection<BurlLibrary> libs = burl_store.findLibrariesForUser(user); 
   
   JSONArray li = new JSONArray();
   for (BurlLibrary lib : libs) {
      JSONObject jobj = lib.toJson(user);
      li.put(jobj);
    }
   return BowerRouter.jsonOKResponse(session,"libs",li);
}



String handleRemoveLibrary(HttpExchange he,ControlSession session)
{
   Number uid = session.getUserId();
   if (uid == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad user");
    }
   Number lid = getIdParameter(he,"library");
   if (lid == null) lid = session.getLibraryId();
   if (lid == null) {
      return BowerRouter.errorResponse(he,session,400,"No library given");
    }
   ControlLibrary lib = burl_store.findLibraryById(lid);
   if (lib == null) {
      return BowerRouter.errorResponse(he,session,400,"No library given");
    }
   BurlUserAccess useracc = validateLibrary(session,lid);
   switch (useracc) {
      case NONE :
      case VIEWER :
      case EDITOR :
      case LIBRARIAN :
         return BowerRouter.errorResponse(he,session,400,"Not authorized");
      case OWNER :
         break; 
    }
   
   burl_main.removeLibrary(lib);
   // remove from ControlRepoManager
   
   return BowerRouter.jsonOKResponse(session);
}


String handleExportLibrary(HttpExchange he,ControlSession session)
{
   Number uid = session.getUserId();
   if (uid == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad user");
    }
   Number lid = getIdParameter(he,"library");
   if (lid == null) lid = session.getLibraryId();
   if (lid == null) {
      return BowerRouter.errorResponse(he,session,400,"No library given");
    }
   ControlLibrary lib = burl_store.findLibraryById(lid);
   if (lib == null) {
      return BowerRouter.errorResponse(he,session,400,"No library given");
    }
   BurlExportFormat exp = BowerRouter.getEnumParameter(he,"format",BurlExportFormat.CSV);
   BurlUserAccess useracc = validateLibrary(session,lid);
   switch (useracc) {
      case NONE :
         return BowerRouter.errorResponse(he,session,400,"Not authorized");
      case VIEWER :
      case EDITOR :
      case OWNER :
      case LIBRARIAN :
         break; 
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
   
   BurlRepo repo = lib.getRepository();
   if (repo == null) { 
      return BowerRouter.errorResponse(he,session,400,"Bad repository");
    }  
   
   repo.exportRepository(f1,exp,null);  
   
   String resp = BowerRouter.sendFileResponse(he,f1); 
   
   f1.delete();
   
   return resp;
}



String handlePrintLabels(HttpExchange he,ControlSession session)
{
   Number uid = session.getUserId();
   if (uid == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad user");
    }
   Number lid = getIdParameter(he,"library");
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
   BurlRepoColumn brc = repo.getPrintLabelsField();
   if (brc == null) {
      return BowerRouter.errorResponse(he,session,400,"No labeled field");
    }
   boolean reset = BowerRouter.getBooleanParameter(he,"reset",false);
   
   BurlUserAccess useracc = validateLibrary(session,lid);
   switch (useracc) {
      case NONE :
      case VIEWER :
      case EDITOR :
         return BowerRouter.errorResponse(he,session,402,"Not authorized");
      default :
         break;
    }
   
   List<Number> todo = burl_store.dataFieldSearch(repo,brc.getFieldName(),"yes");
   
   File f1 = null;
   try {
      f1 = File.createTempFile("Burl_" + lib.getName(),".rtf");
    }
   catch (IOException e) {
      return BowerRouter.errorResponse(he,session,500,"Problem with temp file");
    }
   
   repo.printLabels(f1,todo,reset);  
   
   String resp = BowerRouter.sendFileResponse(he,f1); 
   
   f1.delete();
   
   return resp;
}





   
/********************************************************************************/
/*                                                                              */
/*      Add entries to library                                                  */
/*                                                                              */
/********************************************************************************/

String handleAddIsbns(HttpExchange he,ControlSession session)
{
   Number lid = getIdParameter(he,"library");
   if (lid == null) lid = session.getLibraryId();
   if (lid == null) {
      return BowerRouter.errorResponse(he,session,400,"No library given");
    }
   List<String> isbns = BowerRouter.getParameterList(he,"isbns");
   BurlUpdateMode upd = BowerRouter.getEnumParameter(he,"mode",BurlUpdateMode.AUGMENT);
   boolean count = BowerRouter.getBooleanParameter(he,"count",true);
   String isbnstr = BowerRouter.getParameter(he,"isbnstr");
   if (isbnstr != null && !isbnstr.isBlank()) {
      if (isbns == null) isbns = new ArrayList<>();
      for (StringTokenizer tok = new StringTokenizer(isbnstr); tok.hasMoreTokens(); ) {
         String s = tok.nextToken();
         if (s.startsWith("@")) {
            StringBuffer buf = new StringBuffer();
            buf.append(s);
            String s1 = tok.nextToken("\r\n");
            buf.append(" ");
            buf.append(s1);
            isbns.add(s1);
          }
         else {
            String s1 = BurlUtil.getValidISBN(s);
            if (s1 == null) s1 = BurlUtil.getValidLCCN(s);
            if (s1 != null) isbns.add(s1);
          }
       }
    }
   
   BurlUserAccess acc = validateLibrary(session,lid);
   switch (acc) {
      case NONE :
      case VIEWER :
      case EDITOR :
         return BowerRouter.errorResponse(he,session,400,"Not authorized");
      case OWNER :
      case LIBRARIAN :
         break;
    }
   ControlLibrary lib = burl_store.findLibraryById(lid);
   if (lib == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad library");
    }
   
   work_thread.addTask(lid,session.getUserId(),isbns,upd,count); 
   
   return BowerRouter.jsonOKResponse(session);
}


String handleImport(HttpExchange he,ControlSession session) 
{
   Number lid = getIdParameter(he,"library");
   if (lid == null) lid = session.getLibraryId();
   if (lid == null) {
      return BowerRouter.errorResponse(he,session,400,"No library given");
    }
   ControlLibrary lib = burl_store.findLibraryById(lid);
   if (lib == null) {
      return BowerRouter.errorResponse(he,session,400,"Bad library id");
    }
   BurlUpdateMode updmode = BowerRouter.getEnumParameter(he,"update",burl_main.getUpdateMode());
   
   BurlRepo repo =  lib.getRepository();
   
   JSONObject cindata = BowerRouter.getJson(he,"csvdata");
   if (cindata != null) {
      JSONArray dataarr = cindata.getJSONArray("rows");
      Map<BurlRepoColumn,Integer> colmap = null;
      for (int i = 0; i < dataarr.length(); ++i) {
         String row = dataarr.getString(i);
         if (colmap == null) {
            colmap = new HashMap<>();
            String err = repo.importCSVHeader(row,colmap);
            if (err != null) {
               return BowerRouter.errorResponse(he,session,400,err); 
             }
            continue;
          }
         repo.importCSV(row,updmode,colmap);
       }
      
      return BowerRouter.jsonOKResponse(session);
    }
   
   JSONObject jindata = BowerRouter.getJson(he,"jsondata");
   if (jindata != null) {
      JSONArray dataarr = jindata.getJSONArray("rows");
      for (int i = 0; i < dataarr.length(); ++i) {
         JSONObject row = dataarr.getJSONObject(i);
         repo.importJSON(row,updmode);  
       }
      return BowerRouter.jsonOKResponse(session);
    }
   
   return BowerRouter.errorResponse(he,session,500,"Not implemented");
}



/********************************************************************************/
/*                                                                              */
/*      Info requests                                                           */
/*                                                                              */
/********************************************************************************/

String handleFieldData(HttpExchange he,ControlSession session)
{
   JSONObject data = null;
   try (InputStream ins = getClass().getClassLoader().getResourceAsStream("fields.xml")) {
      Reader rdr = new InputStreamReader(ins,"UTF-8");
      data = XML.toJSONObject(rdr,true);
    }
   catch (Exception e) {
      IvyLog.logE("BURLCLI","Problem reading field data",e);
      System.exit(1);
    }
   
   return BowerRouter.jsonOKResponse(session,"data",data);
}



/********************************************************************************/
/*                                                                              */
/*      HTML requests                                                           */
/*                                                                              */
/********************************************************************************/

String handleAbout(HttpExchange he,ControlSession session)
{
   String rslt = loadResource("burlabout.html");
   if (rslt == null) rslt = "About Page";
   return BowerRouter.jsonOKResponse(session,"html",rslt);
}



/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

Number getIdParameter(HttpExchange he,String name)
{
   int v = BowerRouter.getIntParameter(he,name);
   if (v == 0) return null;
   return v;
}


String loadResource(String name)
{
   String cnts = null;
   InputStream ins = getClass().getClassLoader().getResourceAsStream("name");
   if (ins != null) {
      try {
         cnts = IvyFile.loadFile(ins);
       }
      catch (IOException e) {
         IvyLog.logE("BURL","Problem loading resource " + name,e);
       }
    }
   if (cnts == null) {
      File f1 = burl_main.getBaseDirectory();
      File f2 = new File(f1,"resources");
      File f3 = new File(f2,name);
      if (f3.exists()) {
         try {
            cnts = IvyFile.loadFile(f3);
          }
         catch (IOException e) {
            IvyLog.logE("BURL","Problem loading resource file " + name,e);
          }
       }
    }
   
   return cnts;
}



BurlUserAccess validateLibrary(ControlSession session,Number lid)
{
   ControlUser user = session.getUser();
   if (user == null) return BurlUserAccess.NONE;
   if (lid == null) return BurlUserAccess.NONE;
   
   BurlUserAccess acc = burl_store.getUserAccess(user.getEmail(),lid);
   
   if (acc != BurlUserAccess.NONE) {
      session.setLibraryId(lid);
    }
   
   return acc;
}




}       // end of class ControlServer




/* end of ControlServer.java */

