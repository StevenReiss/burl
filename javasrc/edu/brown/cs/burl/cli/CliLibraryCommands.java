/********************************************************************************/
/*                                                                              */
/*              CliLibraryCommands.java                                         */
/*                                                                              */
/*      Library management commands                                             */
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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.brown.cs.burl.burl.BurlUtil;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;

class CliLibraryCommands implements CliConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CliMain         cli_main;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

CliLibraryCommands(CliMain main)
{
   cli_main = main;
}



/********************************************************************************/
/*                                                                              */
/*      List libraries command                                                  */
/*                                                                              */
/********************************************************************************/

void handleListLibraries(List<String> args) 
{
   for (String s : args) {
      if (s.startsWith("-")) {
         badListLibrariesArgs();
         return;
       }
      else {
         badListLibrariesArgs();
         return;
       }
    }
   
   JSONObject libs = cli_main.createHttpPost("findlibraries",null);
   
   if (!cli_main.checkResponse(libs,"list")) return;
   
   JSONArray libarr = libs.getJSONArray("libs");
   for (int i = 0; i < libarr.length(); ++i) {
      JSONObject lib = libarr.getJSONObject(i);
      System.out.println("  " + lib.getNumber("id") + "\t" +
            lib.getString("name") + "\t" + 
            lib.getString("namekey") + "\t" +
            lib.getString("access"));
           
    }
   System.out.println();
}



private void badListLibrariesArgs()
{
   IvyLog.logI("BURLCLI","list");
}

/********************************************************************************/
/*                                                                              */
/*      Set the current library                                                  */
/*                                                                              */
/********************************************************************************/

void handleSetLibrary(List<String> args)
{
   String name = null;
   for (String s : args) {
      if (s.startsWith("-")) {
         badLibraryArgs();
         return;
       }
      else if (name == null) name = s;
      else badLibraryArgs();
    }
   
   if (name == null) name = cli_main.getDefaultLibrary();
   
   String libnm = findLibrary(name);
   if (libnm != null) {
      IvyLog.logI("BURLCLI","Library set to " + libnm);
    }
   else {
      IvyLog.logI("BURLCLI","Library set failed");
    }
}



String findLibrary(String name)
{
   if (!cli_main.isLoggedIn()) return null;
   
   JSONObject libs = cli_main.createHttpPost("findlibraries",null);
   
   if (!cli_main.checkResponse(libs,null)) return null;
   
   JSONArray libarr = libs.getJSONArray("libs");
   
   if (name == null) {
      if (libarr.length() > 1) {
         // let the user choose the library from a list
         return null;
       }
    }
   Number id = null;
   if (name != null && name.matches("[0-9]+")) {
      id = Integer.parseInt(name);
    }
  
   for (int i = 0; i < libarr.length(); ++i) {
      JSONObject lib = libarr.getJSONObject(i);
      String libnm = lib.getString("name");
      Number libid = lib.getNumber("id");
      if (name == null || libnm.equals(name) || libid.equals(id)) {
         Number lid = lib.getNumber("id");
         cli_main.setLibraryId(lid);
         cli_main.setDefaultLibrary(libnm); 
         return libnm;
       }
    }
   
   return null;
}




private void badLibraryArgs()
{
   IvyLog.logI("BURLCLI","library <name>");
}


/********************************************************************************/
/*                                                                              */
/*      Create a new library                                                    */
/*                                                                              */
/********************************************************************************/

void handleNewLibrary(List<String> args)
{
   String libname = null;
   BurlRepoType repotype = BurlRepoType.DATABASE;
   
   for (String s : args) {
      if (s.startsWith("-")) {
         if (s.startsWith("-c")) {                      // -csv
            repotype = BurlRepoType.CSV;
          }
         else if (s.startsWith("-j")) {                 // -json
            repotype = BurlRepoType.JSON;
          }
         else if (s.startsWith("-d")) {                 // -database
            repotype = BurlRepoType.DATABASE;
          }
         else {
            badNewLibraryArgs();
            return;
          }
       }
      else if (libname == null) {               
         libname = s;
       }
      else {
         badNewLibraryArgs();
         return;
       }
    }
   
   if (libname == null) {
      badNewLibraryArgs();
      return;
    }
   
   JSONObject data = BurlUtil.buildJson("name",libname,
         "repotype",repotype.toString());
   JSONObject rslt = cli_main.createHttpPost("createlibrary",data);
   if (cli_main.checkResponse(rslt,"library creation")) {
      IvyLog.logI("BURLCLI","Library " + libname + " created");
      String libnm = findLibrary(libname);
      if (libnm != null) {
         IvyLog.logI("BURLCLI","Library set to " + libnm);
       }
    }
}


private void badNewLibraryArgs()
{
   IvyLog.logI("BURLCLI","newlib name [-database | -csv | -json]");
}


/********************************************************************************/
/*                                                                              */
/*      Add user to the library                                                 */
/*                                                                              */
/********************************************************************************/

void handleAddLibraryUser(List<String> args)
{
   Number libid = cli_main.getLibraryId();
   if (libid == null) return;
   
   String email = null;
   BurlUserAccess acc = BurlUserAccess.LIBRARIAN;
   
   for (String s : args) {
      if (s.startsWith("-")) {
         if (s.startsWith("-o")) {
            acc = BurlUserAccess.OWNER;
          }
         else if (s.startsWith("-l")) {
            acc = BurlUserAccess.LIBRARIAN;
          }
         else if (s.startsWith("-e")) {
            acc = BurlUserAccess.EDITOR;
          }
         else if (s.startsWith("-n")) {
            acc = BurlUserAccess.NONE;
          }
         else if (s.startsWith("-v")) {
            acc = BurlUserAccess.VIEWER;
          }
         else {
            badAddUserArgs();
            return;
          }
       }
      else if (email == null) email = s;
      else {
         badAddUserArgs();
         return;
       }
    }
   
   if (email == null) {
      badAddUserArgs();
      return;
    }
   
   JSONObject cargs = BurlUtil.buildJson("email",email,"access",acc.toString(),
         "library",libid);
   JSONObject rslt = cli_main.createHttpPost("addlibraryuser",cargs);
   if (cli_main.checkResponse(rslt,"library user add")) {
      IvyLog.logI("BURLCLI","Library user added");
    }
}



private void badAddUserArgs()
{
   IvyLog.logI("BURLCLI",
         "adduser email [-librarian|-none|-viewer|-editor|-owner|-senior|-admin]");
}


/********************************************************************************/
/*                                                                              */
/*      Add items to the library                                                */
/*                                                                              */
/********************************************************************************/

void handleAddIsbns(List<String> args)
{
   List<String> isbns = new ArrayList<>();
   BurlUpdateMode updmode = BurlUpdateMode.AUGMENT;
   boolean count = false;
   
   for (int i = 0; i < args.size(); ++i) {
      String s = args.get(i);
      if (s.startsWith("-")) {
         if (s.startsWith("-f") && i+1 < args.size()) {
            addFileIsbns(args.get(++i),args);
          }
         else if (s.startsWith("-r")) {
            updmode = BurlUpdateMode.REPLACE;
          } 
         else if (s.startsWith("-a")) {
             updmode = BurlUpdateMode.AUGMENT;
           }
         else if (s.startsWith("-R")) {
            updmode = BurlUpdateMode.REPLACE_FORCE;
          }
         else if (s.startsWith("-s")) {
            updmode = BurlUpdateMode.SKIP;
          }
         else if (s.startsWith("-c")) {
            count = true;
          }
         else if (s.startsWith("-noc")) {
            count = false;
          }
         else badAddIsbnArgs();
       }
      else isbns.add(s);
    }
   
   if (isbns.isEmpty()) {
      badAddIsbnArgs();
      return;
    }
   
   JSONArray jarr = new JSONArray(isbns);
   JSONObject data = BurlUtil.buildJson("library",cli_main.getLibraryId(),
         "mode",updmode.toString(),
         "count",count,
         "isbns",jarr);
   JSONObject rslt = cli_main.createHttpPost("addisbns",data);
   if (cli_main.checkResponse(rslt,"adding isbns")) {
      IvyLog.logI("BURLCLI","ISBN's are queued for addition");
    }
}


void addFileIsbns(String fnm,List<String> isbns)
{
   try (BufferedReader fr = new BufferedReader(new FileReader(fnm))) {
      for ( ; ; ) {
         String ln = fr.readLine();
         if (ln == null) break;
         StringTokenizer tok = new StringTokenizer(ln);
         while (tok.hasMoreTokens()) {
            String isbn = tok.nextToken();
            if (!Character.isDigit(isbn.charAt(0))) continue;
            isbn = isbn.replace("x","X");
            isbn = isbn.replace("-","");
            // possibly fix up isbn here
            isbns.add(isbn);
          }
       }
    }
   catch (IOException e) {
      IvyLog.logE("BURLCLI","Problem reading ISBN File",e);
      System.exit(1);
    }
}



private void badAddIsbnArgs()
{
   IvyLog.logI("BURLCLI",
         "add [-f <file>] [ -replace | -augment | -Replace | -skip ] [ [no]count ]  [isbn|lccn ...]");
}


/********************************************************************************/
/*                                                                              */
/*      Handle import file command                                              */
/*                                                                              */
/********************************************************************************/

void handleImport(List<String> args)
{
   BurlExportFormat format = null;
   File file = null;
   BurlUpdateMode updmode = BurlUpdateMode.REPLACE;
   boolean docounts = false;
   
   for (int i = 0; i < args.size(); ++i) {
      String s = args.get(i);
      if (s.startsWith("-")) {
         if (s.startsWith("-count")) {
            docounts = true;
          } 
         else if (s.startsWith("-c")) {                         // -csv
            format = BurlExportFormat.CSV;
          }
         else if (s.startsWith("-j")) {                         // -json
            format = BurlExportFormat.JSON;                     
          }
         else if (s.startsWith("-s")) {                         // -skip
            updmode = BurlUpdateMode.SKIP;
          }
         else if (s.startsWith("-r")) {                         // -replace
            updmode = BurlUpdateMode.REPLACE;
          }
         else if (s.startsWith("-f")) {                         // -force
            updmode = BurlUpdateMode.SKIP;
          }
         else if (s.startsWith("-a")) {                         // -augment
            updmode = BurlUpdateMode.SKIP;
          }
        
         else if (s.startsWith("-f") && i+1 < args.size()) {    // -file <file>
            if (file != null) {
               badImportArgs();
               return;
             }
            file = new File(args.get(++i));
          }
       }
      else if (file == null) {
         file = new File(s);
       }
      else badImportArgs();
    }
   
   if (file == null) {
      badImportArgs();
      return;
    }
   
   if (format == null) {
      String fnm = file.getName();
      if (fnm.endsWith(".json") || fnm.endsWith(".JSON")) {
         format = BurlExportFormat.JSON;
       }
      else if (fnm.endsWith(".csv") || fnm.endsWith(".CSV")) {
         format = BurlExportFormat.CSV;
       }
      else badImportArgs();
    }
   
   JSONObject data = BurlUtil.buildJson("library",cli_main.getLibraryId(),
         "update",updmode,"count",docounts);
   
   if (format == BurlExportFormat.CSV) {
      List<String> lines = new ArrayList<>();
      try (FileInputStream fis = new FileInputStream(file)) {
         InputStreamReader insr = new InputStreamReader(fis,"UTF-8");
         BufferedReader br = new BufferedReader(insr);
         for ( ; ; ) {
            String line = br.readLine();
            if (line == null) break;
            if (line.isBlank()) continue;
            lines.add(line);
          }
         JSONArray arr = new JSONArray(lines);
         JSONObject jdata = BurlUtil.buildJson("rows",arr);
         data.put("cvsdata",jdata);
       }
      catch (IOException e) {
         IvyLog.logI("BURLCLI","Problem reading file " + file);
         return;
       }
    }
   else if (format == BurlExportFormat.JSON) {
      try {
         String cnts = IvyFile.loadFile(file);
         JSONObject json = new JSONObject(cnts);
         data.put("jsondata",json);
       }
      catch (Exception e) {
         IvyLog.logI("BURLCLI","Problem reading file " + file);
         return;
       }
    }
   
   JSONObject rslt = cli_main.createHttpPost("import",data);
   if (cli_main.checkResponse(rslt,"import")) {
      IvyLog.logI("BURLCLI","Import successful");
    }
}




private void badImportArgs()
{
   IvyLog.logI("BURLCLI","import [ -csv | -json ] " +
         "[ -skip | -replace | -force | -augment ] <file>");
}



/********************************************************************************/
/*                                                                              */
/*      Export library command                                                  */
/*                                                                              */
/********************************************************************************/

void handleExportLibrary(List<String> args)
{
   BurlExportFormat format = null;
   File file = null;
   boolean internal = false;
   
   for (int i = 0; i < args.size(); ++i) {
      String s = args.get(i);
      if (s.startsWith("-")) {
         if (s.startsWith("-c")) {                              // -csv
            format = BurlExportFormat.CSV;
          }
         else if (s.startsWith("-j")) {                         // -json
            format = BurlExportFormat.JSON;                     
          }
         else if (s.startsWith("-i")) {                         // -internal
            internal = true;
          }
         else if (s.startsWith("-f") && i+1 < args.size()) {    // -file <file>
            if (file != null) {
               badExportArgs();
               return;
             }
            file = new File(args.get(++i));
          }
       }
      else if (file == null) {
         file = new File(s);
       }
      else badExportArgs();
    }
   
   if (file == null) {
      badExportArgs();
      return;
    }
   
   if (format == null) {
      String fnm = file.getName();
      if (fnm.endsWith(".json") || fnm.endsWith(".JSON")) {
         format = BurlExportFormat.JSON;
       }
      else {
         format = BurlExportFormat.CSV;
       }
    }
   
   JSONObject data = BurlUtil.buildJson("library",cli_main.getLibraryId(),
         "internal",internal,
         "format",format.toString());
     
   JSONObject rslt = cli_main.createHttpPost("exportlibrary",data,file);
   if (cli_main.checkResponse(rslt,"export library")) {
      IvyLog.logI("BURLCLI","Library exported to " + file);
    }
}
   

private void badExportArgs()
{
   IvyLog.logI("BURLCLI","export [-csv | -json ] [-internal] <file>");
}


/********************************************************************************/
/*                                                                              */
/*      Export library command                                                  */
/*                                                                              */
/********************************************************************************/

void handlePrintLabels(List<String> args)
{
   File file = null;
   
   for (int i = 0; i < args.size(); ++i) {
      String s = args.get(i);
      if (s.startsWith("-")) {
         if (s.startsWith("-f") && i+1 < args.size()) {    // -file <file>
            if (file != null) {
               badPrintLabelsArgs();
               return;
             }
            String fn = args.get(++i);
            if (!fn.endsWith(".rtf")) fn = fn + ".rtf";
            file = new File(fn);
          }
       }
      else if (file == null) {
         file = new File(s);
       }
      else badPrintLabelsArgs();
    }
   
   if (file == null) {
      badPrintLabelsArgs();
      return;
    }
   
   JSONObject data = BurlUtil.buildJson("library",cli_main.getLibraryId());
   
   JSONObject rslt = cli_main.createHttpPost("labels",data,file);
   if (cli_main.checkResponse(rslt,"labels")) {
      IvyLog.logI("BURLCLI","Library labels available in " + file);
      // Possibly tell if more labels can be printed
    }
}


private void badPrintLabelsArgs()
{
   IvyLog.logI("BURLCLI","labels <file[.rtf]>");
}



/********************************************************************************/
/*                                                                              */
/*      Handle remove library                                                   */
/*                                                                              */
/********************************************************************************/

void handleRemoveLibrary(List<String> args)
{
   if (args.size() > 0) {
      badRemoveLibraryArgs();
      return;
    }
   
   Number libid = cli_main.getLibraryId();
   if (libid == null) {
      badRemoveLibraryArgs();
      return;
    }
   
   JSONObject data = BurlUtil.buildJson("library",libid);
   JSONObject rslt = cli_main.createHttpPost("removelibrary",data);
   
   if (cli_main.checkResponse(rslt,"removelibrary")) {
      cli_main.setLibraryId(null);
      cli_main.setDefaultLibrary(null);
    }
}


private void badRemoveLibraryArgs()
{
   IvyLog.logI("BURLCLI","removelibrary");
}



}       // end of class CliLibraryCommands




/* end of CliLibraryCommands.java */

