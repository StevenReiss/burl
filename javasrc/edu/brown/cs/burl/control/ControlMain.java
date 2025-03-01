/********************************************************************************/
/*										*/
/*		ControlMain.java						*/
/*										*/
/*	Control program for BURL home library manager				*/
/*										*/
/********************************************************************************/
/*	Copyright 2025 Steven P. Reiss						*/
/*********************************************************************************
 *										 *
 *  This work is licensed under Creative Commons Attribution-NonCommercial 4.0	 *
 *  International.  To view a copy of this license, visit			 *
 *	https://creativecommons.org/licenses/by-nc/4.0/ 			 *
 *										 *
 ********************************************************************************/


package edu.brown.cs.burl.control;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import edu.brown.cs.burl.bibentry.BibEntryFactory;
import edu.brown.cs.burl.burl.BurlBibEntry;
import edu.brown.cs.burl.burl.BurlControl;
import edu.brown.cs.burl.burl.BurlException;
import edu.brown.cs.burl.burl.BurlLibrary;
import edu.brown.cs.burl.burl.BurlRepo;
import edu.brown.cs.burl.burl.BurlRepoFactory;
import edu.brown.cs.burl.burl.BurlUser;
import edu.brown.cs.burl.repo.RepoFactory;
import edu.brown.cs.ivy.exec.IvyExecQuery;
import edu.brown.cs.ivy.file.IvyLog;

public final class ControlMain implements ControlConstants, BurlControl
{



/********************************************************************************/
/*										*/
/*	Control program for BURL server 					*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   ControlMain mc = new ControlMain(args);

   mc.process();
}

/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private Properties	base_properties;
private ControlStorage	control_storage;
private File            base_directory;
private ControlRepoManager repo_manager;
private BurlRepoFactory repo_factory;
private BurlUpdateMode  update_mode;
private boolean         do_counts;
private BibEntryFactory bibentry_factory;
private Map<Number,BurlRepo> repo_map;
private String          url_prefix;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

private ControlMain(String [] args)
{
   setupProperties();

   scanArgs(args);

   control_storage = new ControlStorage(this);
   repo_manager = new ControlRepoManager(this); 
   repo_factory = new RepoFactory(this);   
   bibentry_factory = new BibEntryFactory(this);
   
   repo_map = new HashMap<>();
}



/********************************************************************************/
/*										*/
/*	Server argument scanning						*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
	 if (args[i].startsWith("-LD")) {                               // -LDebug
	    IvyLog.setLogLevel(IvyLog.LogLevel.DEBUG);
	  }
	 else if (args[i].startsWith("-LI")) {                          // -LInfo
	    IvyLog.setLogLevel(IvyLog.LogLevel.INFO);
	  }
	 else if (args[i].startsWith("-LW")) {                          // -LWarning
	    IvyLog.setLogLevel(IvyLog.LogLevel.WARNING);
	  }
	 else if (args[i].startsWith("-LE")) {                          // -LError
	    IvyLog.setLogLevel(IvyLog.LogLevel.ERROR);
	  }
	 else if (args[i].startsWith("-L") && i+1 < args.length) {      // -Log <file>
	    IvyLog.setLogFile(args[++i]);
	  }
	 else if (args[i].startsWith("-S")) {                           // -Stderr
	    IvyLog.useStdErr(true);
	  }
	 else badArgs();
       }
      else {
	 badArgs();
       }
    }
}


private void badArgs()
{
   System.err.println("BURLSERVER: <no arguments> ");
   System.exit(1);
}


/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

Properties getProperties()			{ return base_properties; }

@Override public ControlStorage getStorage()	
{ 
   return control_storage; 
}

ControlRepoManager getRepoManager()             { return repo_manager; }

BurlRepoFactory getRepoFactory()                { return repo_factory; }

BibEntryFactory getBibEntryFactory()            { return bibentry_factory; }

File getBaseDirectory()                         { return base_directory; }

String getUrlPrefix()                           { return url_prefix; }


@Override public BurlUpdateMode getUpdateMode() 
{
   return update_mode; 
}

@Override public boolean getDoCount()           { return do_counts; }
 
@Override public File getDataDirectory() 
{
   String dd = base_properties.getProperty("dataDirectory");
   File fd = null;
   if (dd != null && !dd.isEmpty()) {
      fd = new File(dd);
    }
   else {
      fd = new File(base_directory,"data");
    }
   if (!fd.exists()) fd.mkdirs();
   
   return fd;
}



/********************************************************************************/
/*										*/
/*	Property mehtods							*/
/*										*/
/********************************************************************************/

private void setupProperties()
{
   base_properties = new Properties();
   base_properties.setProperty("updateMode","replace");
   base_properties.setProperty("doCounts","F");
   InputStream ins = getClass().getClassLoader().getResourceAsStream("burl.props");
   if (ins != null) {
      try {
	 base_properties.loadFromXML(ins);
	 ins.close();
       }
      catch (IOException e) {
         IvyLog.logE("BURL","Problem loading properties",e);
       }
    }
   
   String bd = base_properties.getProperty("baseDirectory");
   if (bd != null) base_directory = new File(bd);
   else {
      base_directory = findBaseDirectory();
      File f5 = new File(base_directory,"secret");
      File f6 = new File(f5,"burl.props");
      try (FileInputStream fis = new FileInputStream(f6)) {
	 base_properties.loadFromXML(fis);
       }
      catch (IOException e) { } 
    }
   
   File f1 = new File(System.getProperty("user.home"));
   File f2 = new File(f1,".config");
   File f3 = new File(f2,"burl");
   File f4 = new File(f3,"burl.props");
   if (f4.exists()) {
      try (FileInputStream fis = new FileInputStream(f4)) {
	 base_properties.loadFromXML(fis);
       }
      catch (IOException e) { }
    }
   
   if (base_properties.getProperty("dataDirectory") == null) {
      File fd = new File(base_directory,"data");
      base_properties.setProperty("dataDirectory",fd.getPath());
    }
   
   setUpdateMode();
   
   String hostpfx = base_properties.getProperty("hostpfx");
   String host = base_properties.getProperty("host");
   if (host == null && hostpfx == null) host = IvyExecQuery.getHostName();
   if (host != null) {
      String pfx = "http";
      if (base_properties.getProperty("jkspwd") != null) pfx = "https";
      hostpfx = pfx + "://" + host;
      base_properties.setProperty("hostpfx",hostpfx);
    }
   url_prefix = hostpfx + ":" + HTTPS_PORT;
}



private boolean setUpdateMode()
{
   boolean rslt = true;
   String md = base_properties.getProperty("updateMode");
   if (md == null || md.isEmpty()) md = "r";
   char c = Character.toLowerCase(md.charAt(0));
   switch (c) {
      case 'r' :                       
         update_mode = BurlUpdateMode.REPLACE;
         break;
      case 'f' :
         update_mode = BurlUpdateMode.REPLACE_FORCE;
         break;                     
      case 'a' :
         update_mode = BurlUpdateMode.AUGMENT;
         break;
      case 's' :
         update_mode = BurlUpdateMode.SKIP;
         break;
      default :
         IvyLog.logW("BURL","Bad update mode");
         rslt = false;
         break;
    }
   
   String cnt = base_properties.getProperty("doCounts");
   if (cnt == null || cnt.isEmpty()) cnt = "Fburl";
   if ("nN0fF".indexOf(cnt.charAt(0)) >= 0) do_counts = false;
   else do_counts = true;
   
   return rslt;
}


/********************************************************************************/
/*										*/
/*	Find base directory							*/
/*										*/

/********************************************************************************/

private File findBaseDirectory()
{
   File f1 = new File(System.getProperty("user.dir"));
   for (File f2 = f1; f2 != null; f2 = f2.getParentFile()) {
      if (isBaseDirectory(f2)) return f2;
    }
   File f3 = new File(System.getProperty("user.home"));
   if (isBaseDirectory(f3)) return f3;
   
   File fc = new File("/vol");
   File fd = new File(fc,"burl");
   if (isBaseDirectory(fd)) return fd;
   
   File fa = new File("/pro");
   File fb = new File(fa,"burl");
   if (isBaseDirectory(fb)) return fb;
   
   return null;
}


private static boolean isBaseDirectory(File dir)
{
   File f2 = new File(dir,"secret");
   if (!f2.exists()) return false;
   File f2a = new File(dir,"resources");
   File f3 = new File(f2a,"burl.props");
   File f4 = new File(f2,"burl.props");
   File f5 = new File(f2a,"fields.xml");
   if (f3.exists() && f4.exists() && f5.exists()) return true;
   
   return false;
}


/********************************************************************************/
/*										*/
/*	Processing code 							*/
/*										*/
/********************************************************************************/

private void process()
{
   String keypass = base_properties.getProperty("keystorePassword");
   ControlServer server = new ControlServer(this,keypass);
   try {
      server.start();
    }
   catch (BurlException e) {
      IvyLog.logE("BURL","Problem starting BURL server",e);
    }
}



/********************************************************************************/
/*										*/
/*	Abstract Method Implementations 					*/
/*										*/
/********************************************************************************/

@Override public Collection<BurlLibrary> getLibraries(BurlUser user)
{
   Collection<BurlLibrary> libs = control_storage.findLibrariesForUser(user);

   return libs;
}



@Override public BurlLibrary createLibrary(String name,BurlUser user,BurlRepoType rtyp)
{
   if (name == null || user == null) return null;
   
   BurlLibrary lib = control_storage.createLibrary(name,rtyp);
   if (lib == null) return null;
   
   control_storage.setUserAccess(user.getEmail(),lib.getId(),
         BurlUserAccess.OWNER);
   
   BurlRepo repo = repo_factory.createRepository(lib);
   if (repo != null) {
      repo_map.put(lib.getId(),repo);
    }
 
   return lib;
}



@Override public void removeLibrary(BurlLibrary lib) 
{
   if (lib == null) return; 
   
   control_storage.removeLibrary(lib.getId());
   BurlRepo repo = repo_map.get(lib.getId());
   if (repo != null) {
      repo.deleteRepository();
    }
}



@Override public BurlUser findUser(String email)
{
   if (email == null) return null;
   
   BurlUser user = control_storage.findUserByEmail(email);

   return user;
}


BurlRepo findRepository(BurlLibrary lib)
{
   if (lib == null) return null;
   
   BurlRepo repo = repo_map.get(lib.getId());
   
   if (repo == null) {
      repo = repo_factory.createRepository(lib);
      repo_map.put(lib.getId(),repo);
    }
   
   return repo;
}


BurlBibEntry findBibEntry(String isbn)
{
   if (isbn == null || isbn.isEmpty()) return null;
   
   return bibentry_factory.findBibEntry(isbn);
}



}	// end of class ControlMain




/* end of ControlMain.java */

