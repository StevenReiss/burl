/********************************************************************************/
/*										*/
/*		ControlLibrary.java						*/
/*										*/
/*	description of class							*/
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

import java.util.List;

import org.json.JSONObject;

import edu.brown.cs.burl.burl.BurlBibEntry;
import edu.brown.cs.burl.burl.BurlLibrary;
import edu.brown.cs.burl.burl.BurlLibraryAccess;
import edu.brown.cs.burl.burl.BurlRepo;
import edu.brown.cs.burl.burl.BurlRepoRow;
import edu.brown.cs.burl.burl.BurlUser;
import edu.brown.cs.burl.burl.BurlUtil;
import edu.brown.cs.ivy.file.IvyLog;

class ControlLibrary implements BurlLibrary, ControlConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private ControlMain     burl_control;
private JSONObject	lib_data;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ControlLibrary(ControlMain bm,JSONObject data)
{
   burl_control = bm;
   lib_data = data;
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override public Number getId()         { return lib_data.getNumber("id"); }
 
@Override public String getName()	{ return lib_data.getString("name"); }


@Override public BurlRepo getRepository()
{
   return burl_control.findRepository(this); 
}

@Override public String getNameKey()
{
   return lib_data.getString("namekey");
}


@Override public BurlUserAccess getUserAccess(String email)
{
   ControlStorage store = burl_control.getStorage();
   BurlUserAccess acc = store.getUserAccess(email,getId());
   return acc;
}


@Override public JSONObject toJson(BurlUser user)
{
   JSONObject jobj = new JSONObject(lib_data,"id","name","namekey");
   jobj.put("repo_type",getRepoType().toString());
   if (user != null) {
      BurlUserAccess acc = getUserAccess(user.getEmail());
      if (acc == BurlUserAccess.NONE) return null;
      jobj.put("access",acc.toString());
    }
   ControlStorage store = burl_control.getStorage();
   List<BurlLibraryAccess> acclist = store.getLibraryAccess(getId());
   jobj.put("owner",getUsers(BurlUserAccess.OWNER,acclist));
   jobj.put("librarian",getUsers(BurlUserAccess.LIBRARIAN,acclist));
   jobj.put("editor",getUsers(BurlUserAccess.EDITOR,acclist));
   jobj.put("viewer",getUsers(BurlUserAccess.VIEWER,acclist));
   

   return jobj;
}


private String getUsers(BurlUserAccess level,List<BurlLibraryAccess> acclst) 
{
   StringBuffer buf = new StringBuffer();
   for (BurlLibraryAccess acc : acclst) {
      if (level == null || acc.getAccessLevel() == level) {
         if (!buf.isEmpty()) buf.append(" ");
         buf.append(acc.getUserEmail());
       }
    }
   return buf.toString();
}


@Override public BurlRepoType getRepoType()
{
   int rtyp = lib_data.getInt("repo_type");
   return BurlRepoType.values()[rtyp];
}




/********************************************************************************/
/*										*/
/*	Update methods								*/
/*										*/
/********************************************************************************/

@Override public void changeUserAccess(String email,BurlUserAccess access)
{
   ControlStorage store = burl_control.getStorage();
   store.setUserAccess(email,getId(),access);
}



@Override public String addToLibrary(String isbn,BurlUpdateMode mode)
{ 
   BurlRepo repo = getRepository();
   
   BurlRepoRow row = findOldRow(repo,isbn);
   if (row != null) {
      if (mode == BurlUpdateMode.SKIP) {
         return "Item " + isbn + " already in database";
       }
    }
   else {
      if (BurlUtil.getValidISBN(isbn) == null && 
            BurlUtil.getValidLCCN(isbn) == null) {
         IvyLog.logE("CONTROL","ISBN/LCCN " + isbn + " IS INVALID -- IGNORED");
         return "Item " + isbn + " is not a valid ISBN/LCCN";
       }
    }
   
   BurlBibEntry bibentry = burl_control.findBibEntry(isbn);
   
   // possibly do a broader search if bibentry is null here
   
   if (row == null || mode == BurlUpdateMode.NEW) {
      row = repo.newRow();
      repo.setInitialValues(row,isbn);
    }
   repo.computeEntry(row,isbn,bibentry,mode); 
   
   if (bibentry != null) {
      IvyLog.logD("BURL","Computed BIB ENTRY for " + isbn);
    }
   else {
      String altisbn = BurlUtil.computeAlternativeISBN(isbn);
      IvyLog.logI("BURL",
            "Can't find any information on " + isbn + " " + altisbn);
//    return "No information available for " + isbn;
    }
   
   return null;
}



BurlRepoRow findOldRow(BurlRepo repo,String isbn)
{
   BurlRepoRow row = null;
   
   String s0 = BurlUtil.getValidISBN(isbn);
   String s1 = BurlUtil.getValidLCCN(isbn);
   if (s0 != null) { 
      row = repo.getRowForIsbn(s0);
    }
   else if (s1 != null) {
      row = repo.getRowForLccn(s1);
    }
   
   return row;
}






}	// end of class ControlLibrary




/* end of ControlLibrary.java */

