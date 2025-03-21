/********************************************************************************/
/*                                                                              */
/*              CliMain.java                                                    */
/*                                                                              */
/*      Main program for command line interface for BURL                        */
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.json.JSONObject;

import edu.brown.cs.burl.burl.BurlUtil;
import edu.brown.cs.ivy.file.IvyLog;


public final class CliMain implements CliConstants
{


/********************************************************************************/
/*                                                                              */
/*      Main program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   CliMain main = new CliMain(args);
   
   main.process();
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String  user_email;
private String  user_pwd;
private String  library_name;
private String  host_name;
private String  host_prefix;

private String  command_name;
private List<String> command_args;
private String  url_prefix;

private Properties base_properties;
private BufferedReader line_reader;

private String  session_id;
private boolean logged_in;
private Number  library_id;
private Number  entry_id;

private HttpClient http_client;

private CliUserCommands user_commands;
private CliLibraryCommands lib_commands;
private CliEntryCommands entry_commands;
 



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private CliMain(String [] args)
{
   user_email = null;
   user_pwd = null;
   library_name = null;
   command_name = null;
   command_args = null;
   
   logged_in = false;
   library_id = null;
   entry_id = null;
   session_id = null;
   
   setupProperties();
   
   user_email = base_properties.getProperty("user");
   user_pwd = base_properties.getProperty("password");
   library_name = base_properties.getProperty("library");
   host_prefix = base_properties.getProperty("hostpfx");
   if (host_prefix == null) {
      host_name = base_properties.getProperty("host");
    }
   
   scanArgs(args);
   
   user_commands = new CliUserCommands(this);
   lib_commands = new CliLibraryCommands(this);
   entry_commands = new CliEntryCommands(this);
}



/********************************************************************************/
/*                                                                              */
/*      Argument scanning                                                       */
/*                                                                              */
/********************************************************************************/

private void scanArgs(String [] args) 
{
   for (int i = 0; i < args.length; ++i) {
      if (command_name == null) {
         if (args[i].startsWith("-")) {
            if (args[i].startsWith("-u") && i+1 < args.length) {        // -u <user email>
               user_email = args[++i];
             }
            else if (args[i].startsWith("-p") && i+1 < args.length) {   // -p <password>
               user_pwd = args[++i];
             }
            else if (args[i].startsWith("-l") && i+1 < args.length) {   // -l <library> 
               library_name = args[++i];
             }
            else if (args[i].startsWith("-h") && i+1 < args.length) {   // -h <host>
               host_name = args[++i];
             }
            else if (args[i].startsWith("-LD")) {                       // -LDebug
               IvyLog.setLogLevel(IvyLog.LogLevel.DEBUG);
             }
            else if (args[i].startsWith("-LI")) {                       // -LInfo
               IvyLog.setLogLevel(IvyLog.LogLevel.INFO);
             }
            else if (args[i].startsWith("-LW")) {                       // -LWarning
               IvyLog.setLogLevel(IvyLog.LogLevel.WARNING);
             }
            else if (args[i].startsWith("-LE")) {                       // -LError
               IvyLog.setLogLevel(IvyLog.LogLevel.ERROR);
             }
            else if (args[i].startsWith("-L") && i+1 < args.length) {   // -Log <file>
               IvyLog.setLogFile(args[++i]);
             }
            else if (args[i].startsWith("-S")) {                        // -Stderr
               IvyLog.useStdErr(true);
             }
            else badArgs();
          }
         else {
            command_name = args[i];
            command_args = new ArrayList<>();
          }
       }
      else {
         command_args.add(args[i]);
       }
    }
}


private void badArgs()
{
   System.err.println("burlcli [-u <email>][-p <password>][-l <library>] [command ...]");
   System.exit(1);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

boolean isLoggedIn()                            { return logged_in; }
void setLoggedIn(boolean fg)  
{ 
   logged_in = fg; 
   if (!fg) setLibraryId(null);
}


Number getLibraryId()                           { return library_id; }
void setLibraryId(Number id)                    
{ 
   if (id == null || !id.equals(library_id)) {
      entry_id = null;
    }
   library_id = id;
}

Number getEntryId()                             { return entry_id; }

void setEntryId(Number id)                      { entry_id = id; }

String getDefaultLibrary()                      { return library_name; }
void setDefaultLibrary(String nm)               
{ 
   library_name = nm;
   if (nm != null) updateLibrary(nm);
}

BufferedReader getLineReader()                  { return line_reader; }


List<String> getShortDisplayFields()
{
   String val = base_properties.getProperty("shortDisplay");
   if (val == null) val = "Title,Authors,LCC,Subjects";
   List<String> rslt = new ArrayList<>();
   for (StringTokenizer tok = new StringTokenizer(val,","); tok.hasMoreTokens(); ) {
      String fld = tok.nextToken().trim();
      rslt.add(fld);
    }
   return rslt;
}


/********************************************************************************/
/*                                                                              */
/*      Property methods                                                        */
/*                                                                              */
/********************************************************************************/

private void setupProperties()
{
   base_properties = new Properties();
   base_properties.setProperty("updateMode","replace");
   InputStream ins = getClass().getClassLoader().getResourceAsStream("burlcli.props");
   if (ins != null) {
      try {
	 base_properties.loadFromXML(ins);
	 ins.close();
       }
      catch (IOException e) { }
    }
   File f1 = new File(System.getProperty("user.home"));
   File f2 = new File(f1,".config");
   File f3 = new File(f2,"burl");
   File f4 = new File(f3,"burlcli.props");
   if (f4.exists()) {
      try (FileInputStream fis = new FileInputStream(f4)) {
	 base_properties.loadFromXML(fis);
       }
      catch (IOException e) { }
    }
}


void saveUserPass(String user,String pass)
{
   File f1 = new File(System.getProperty("user.home"));
   File f2 = new File(f1,".config");
   File f3 = new File(f2,"burl");
   File f4 = new File(f3,"burlcli.props");
   Properties np = new Properties();
   if (f4.exists()) {
      try (FileInputStream fis = new FileInputStream(f4)) {
	 np.loadFromXML(fis);
       }
      catch (IOException e) { }
    }
   np.setProperty("user",user);
   np.setProperty("password",pass);
   
   f3.mkdirs();
   try (OutputStream ots = new FileOutputStream(f4)) {
      np.storeToXML(ots,"Updated by BurlCLI");
    }
   catch (IOException e) { }
}


void updatePassword(String pass)
{
   File f1 = new File(System.getProperty("user.home"));
   File f2 = new File(f1,".config");
   File f3 = new File(f2,"burl");
   File f4 = new File(f3,"burlcli.props");
   if (!f4.exists()) return;
   
   Properties np = new Properties();
   try (FileInputStream fis = new FileInputStream(f4)) {
      np.loadFromXML(fis);
    }
   catch (IOException e) { }
   
   if (np.getProperty("password") == null) return;
   
   np.setProperty("password",pass);
   try (OutputStream ots = new FileOutputStream(f4)) {
      np.storeToXML(ots,"Updated by BurlCLI");
    }
   catch (IOException e) { }
}


void updateLibrary(String lib)
{
   File f1 = new File(System.getProperty("user.home"));
   File f2 = new File(f1,".config");
   File f3 = new File(f2,"burl");
   File f4 = new File(f3,"burlcli.props");
   if (!f4.exists()) return;
   
   Properties np = new Properties();
   try (FileInputStream fis = new FileInputStream(f4)) {
      np.loadFromXML(fis);
    }
   catch (IOException e) { }
   
   np.setProperty("library",lib);
   try (OutputStream ots = new FileOutputStream(f4)) {
      np.storeToXML(ots,"Updated by BurlCLI");
    }
   catch (IOException e) { }
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

private void process()
{
   setupHttp();
   
   autoLogin();
   
   autoSetLibrary();
   
   if (command_name != null) {
      processCommand(command_name,command_args);
    }
   else {
      try (BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in))) {
         line_reader = rdr;
         for ( ; ; ) {
            System.out.print("BurlCLI> ");
            String ln = rdr.readLine();
            if (ln == null) {
               System.out.println();
               break;
             }
            ln = ln.trim();
            ln = ln.replace("&quot;","\"");
            if (ln.isEmpty()) continue;
            if (ln.startsWith("#")) continue;
            List<String> cmdlist = BurlUtil.tokenize(ln); 
            if (cmdlist.size() == 0) continue;
            String cmd = cmdlist.remove(0);
            processCommand(cmd,cmdlist);
          }
       }
      catch (IOException e) { 
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Command processing                                                      */
/*                                                                              */
/********************************************************************************/

private void processCommand(String cmd,List<String> args)
{ 
   switch (cmd) {
      case "register" :
      case "reg" :
         user_commands.handleRegister(args);
         break;
      case "login" :
         user_commands.handleLogin(args);
         break;
      case "logout" :
         user_commands.handleLogout(args);  
         break;
      case "forgot" :
         user_commands.handleForgotPassword(args);
         break;
      case "password" :
         if (checkLoggedIn(cmd)) {
            user_commands.handleChangePassword(args);
          }
         break;
      case "validate" :
         user_commands.handleUserValidation(args); 
         break;
      case "removeuser" :
         if (checkLoggedIn(cmd)) {
            // user_commands.handleRemoveUser(args);
          }
         break;
         
      case "list" :
      case "listlibs" :
      case "listlibraries" :
         if (checkLoggedIn(cmd)) {
            lib_commands.handleListLibraries(args);
          }
         break;
      case "library" :
         if (checkLoggedIn(cmd)) {
            lib_commands.handleSetLibrary(args);
          }
         break;
      case "adduser" :
         if (checkLibrary(cmd)) {
            lib_commands.handleAddLibraryUser(args);
          } 
         break;
      case "users" :
         if (checkLibrary(cmd)) {
            lib_commands.handleListUsers(args);
          } 
         break;
      case "newlibrary" :
         if (checkLoggedIn(cmd)) {
            lib_commands.handleNewLibrary(args);
          }
         break;
      case "export" :
         if (checkLibrary(cmd)) {
            lib_commands.handleExportLibrary(args);
          }
         break;
      case "labels" :
      case "printlabels" :
         if (checkLibrary(cmd)) {
            lib_commands.handlePrintLabels(args); 
          }
         break;
      case "removelibrary" :
         if (checkLibrary(cmd)) {
            lib_commands.handleRemoveLibrary(args);
          }
         break;
      case "add" :
         if (checkLibrary(cmd)) {
            lib_commands.handleAddIsbns(args);
          }
         break;
      case "import" :
         if (checkLibrary(cmd)) {
            lib_commands.handleImport(args);
          }
         break;
         
      case "entry" :
         if (checkLibrary(cmd)) {
            entry_commands.handleSetEntry(args);
          }
         break;   
      case "find" :
         if (checkLibrary(cmd)) {
            entry_commands.handleFindEntries(args);
          }
         break;
      case "edit" :
         if (checkEntry(cmd)) {
            entry_commands.handleEditEntry(args); 
          }
         break;
      case "removeentry" :
         if (checkEntry(cmd)) {
            entry_commands.handleRemoveEntry(args);
          }
         break;
      case "fixfields" :
         if (checkLibrary(cmd)) {
            entry_commands.handleFixFields(args); 
          }
         break;
         
      case "exit" :
         System.exit(0);
         break;
      case "ping" :
         // handle ping
         break;
         
      case "help" :
         showValidCommands();
         break;
         
      default :
         IvyLog.logE("BURLCLI","Invalid command " + cmd);
         showValidCommands();
         break;
    }
}


private void showValidCommands()
{
   IvyLog.logI("BURLCLI","Valid commands include:");
   IvyLog.logI("BURLCLI","   register:      register a new user (reg)");
   IvyLog.logI("BURLCLI","   login:         log in to BURL"); 
   IvyLog.logI("BURLCLI","   logout:        log out"); 
   IvyLog.logI("BURLCLI","   forgot:        send forgot password email"); 
   IvyLog.logI("BURLCLI","   password:      change password"); 
// IvyLog.logI("BURLCLI","   validate:      validate user based on emailed key"); 
   IvyLog.logI("BURLCLI","   removeuser:    unregister from BURL"); 
   IvyLog.logI("BURLCLI","   list:          list available libraries"); 
   IvyLog.logI("BURLCLI","   library:       set current library"); 
   IvyLog.logI("BURLCLI","   adduser:       add a new user to current library"); 
   IvyLog.logI("BURLCLI","   users:         list all users of the current library");
   IvyLog.logI("BURLCLI","   newlibrary:    create a new library"); 
   IvyLog.logI("BURLCLI","   export:        export the current library as CSV or JSON"); 
   IvyLog.logI("BURLCLI","   labels:        print the next set of labels"); 
   IvyLog.logI("BURLCLI","   removelibrary: remove/delete the current library");  
   IvyLog.logI("BURLCLI","   add:           add entries to current library by ISBN or LCCN"); 
   IvyLog.logI("BURLCLI","   import:        import entry data from CSV or JSON"); 
   IvyLog.logI("BURLCLI","   find:          search library for entries"); 
   IvyLog.logI("BURLCLI","   entry:         set current entry");
   IvyLog.logI("BURLCLI","   edit:          edit fields of the current entry"); 
   IvyLog.logI("BURLCLI","   removeentry:   remove the current entry");
   IvyLog.logI("BURLCLI","   exit:          Exit from BURLCLI"); 
   IvyLog.logI("BURLCLI","   help:          Print this information"); 
}


private boolean checkLoggedIn(String cmd)
{
   if (logged_in) return true;
   
   IvyLog.logI("BURLCLI","Login required for command " + cmd);
   return false;
}


private boolean checkLibrary(String cmd)
{
   if (!checkLoggedIn(cmd)) return false;
   
   if (library_id != null) return true;
   
   IvyLog.logI("BURLCLI",
         "You need to use the library command to choose a library before doing " + cmd);
   return false;
}


private boolean checkEntry(String cmd)
{ 
   if (!checkLoggedIn(cmd)) return false;
   if (!checkLibrary(cmd)) return false;

   if (entry_id != null) return true;
   
   IvyLog.logI("BURLCLI",
         "You need to use the entry command to choose a library before doing " + cmd);
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Login if user gave us email                                             */
/*                                                                              */
/********************************************************************************/

private void setupHttp()
{
   HttpURLConnection.setFollowRedirects(true);
   HttpClient.Builder bldr = HttpClient.newBuilder();
   bldr.followRedirects(HttpClient.Redirect.ALWAYS);
   http_client = bldr.build();
   
   if (host_name == null && host_prefix == null) {
      host_name = "sherpa.cs.brown.edu";
    }
   
   if (host_name != null) {
      String hpfx = "https";
      if (host_name.equals("localhost")) hpfx = "http";
      host_prefix = hpfx + "://" + host_name;
    }
   
   url_prefix = host_prefix + ":" + HTTPS_PORT + "/rest/";
}




private void autoLogin()
{
   if (user_email == null) return;
   if (logged_in) return;
   
   boolean sts = user_commands.tryLogin(user_email,user_pwd,null);
   
   if (sts) {
      IvyLog.logI("BURLCLI","Login successful");
    }
}




void autoSetLibrary()
{
   if (!logged_in) return;
   
   JSONObject libobj = lib_commands.findLibrary(library_name);
   if (libobj != null) {
      String libnm = libobj.getString("name");
      IvyLog.logI("BURLCLI","Library set to " + libnm);
    }
}




/********************************************************************************/
/*                                                                              */
/*      Http methods                                                            */
/*                                                                              */
/********************************************************************************/

JSONObject createHttpGet(String urlcmd,String... query)
{
   String url = url_prefix + urlcmd;
   
   String sep = "?";
   if (url.contains("?")) sep = "&";
   if (query.length > 0) {
      for (int i = 0; i < query.length-1; i += 2) {
         String k = query[i];
         String v = query[i+1];
         v = BurlUtil.encodeURIComponent(v);
         url += sep + k + "=" + v;
         sep = "&";
       }
    }
   if (session_id != null) {
      url += sep + "session=" + session_id;
    }
   
   URI uri = null;
   try {
      uri = new URI(url);
    }
   catch (URISyntaxException e) {
      IvyLog.logE("BURLCLI","Problem with URL",e);
    }
   if (uri == null) return null;
   
   HttpRequest.Builder bldr = HttpRequest.newBuilder();
   bldr.uri(uri);
   bldr.header("Content-Type","application/json; charset=utf-8");
   bldr.header("Accept","application/json,application/xml");     
   bldr.headers("User-Agent","BURLCLI");
   bldr.GET();
   
   HttpRequest req = bldr.build();
   
   try {
      HttpResponse<String> resp = http_client.send(req,
            HttpResponse.BodyHandlers.ofString());
      JSONObject rslt = new JSONObject(resp.body());
      return rslt;
    }
   catch (Exception e) {
      IvyLog.logE("BURLCLI","Problem communicationg with server",e);
    }
   
   return null;
}


JSONObject createHttpPost(String urlcmd,JSONObject json)
{
   return createHttpPost(urlcmd,json,null);
}


JSONObject createHttpPost(String urlcmd,JSONObject json,File file)
{
   String url = url_prefix + urlcmd;
   
   URI uri = null;
   try {
      uri = new URI(url);
    }
   catch (URISyntaxException e) {
      IvyLog.logE("BURLCLI","Problem with URL",e);
    }
   if (uri == null) return null;
   
   HttpRequest.Builder bldr = HttpRequest.newBuilder();
   bldr.uri(uri);
   bldr.header("Content-Type","application/json; charset=utf-8");
   if (file == null) {
      bldr.header("Accept","application/json,application/xml");  
    }   
   bldr.headers("user-agent","BurlCLI");
   
   if (json == null) json = new JSONObject();
   if (session_id != null) json.put("session",session_id);
   
   String jsonstr = json.toString(2);
   HttpRequest.BodyPublisher pub = HttpRequest.BodyPublishers.ofString(jsonstr);
   bldr.POST(pub);
   
   HttpRequest req = bldr.build();
   
   try {
      if (file == null) {
         HttpResponse<String> resp = http_client.send(req,
               HttpResponse.BodyHandlers.ofString());
         JSONObject rslt = new JSONObject(resp.body());
         return rslt;
       }
      else {
         Path p = file.toPath();
         HttpResponse<Path> resp = http_client.send(req,
               HttpResponse.BodyHandlers.ofFile(p));
         JSONObject rslt = null;
         if (resp.statusCode() < 300) {
             rslt = BurlUtil.buildJson("status","OK","file",file);
          }
         else {
            rslt = BurlUtil.buildJson("status","ERROR");
          }
         return rslt;
       }
    }
   catch (Exception e) {
      IvyLog.logE("BURLCLI","Server is down.  Please try again later");
      System.exit(1);
      IvyLog.logE("BURLCLI","Problem communicationg with server",e);
    }
   
   
   return null;
}


boolean checkResponse(JSONObject obj,String cmd)
{
   String err = null;
   if (obj == null) {
      IvyLog.logE("BURLCLI","Server is down.  Please try again later");
      System.exit(1);
    }
   
   String sid = obj.optString("session");
   if (sid != null && !sid.isEmpty()) {
      if (!sid.equals(session_id)) {
         IvyLog.logI("BURLCLI","Changing session to " + sid);
         session_id = sid;
       }
    }
   
   String sts = obj.optString("status","ERROR");
   
   if (sts.equals("OK")) return true;
   else if (cmd == null) return false;
   
   err = obj.optString("message","Unsepecified problem");
   
   IvyLog.logE("BURLCLI","Problem with " + cmd + ": " + err);
   
   return false;
}



}       // end of class CliMain




/* end of CliMain.java */

