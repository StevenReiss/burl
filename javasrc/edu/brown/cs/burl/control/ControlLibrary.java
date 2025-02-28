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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import edu.brown.cs.burl.burl.BurlBibEntry;
import edu.brown.cs.burl.burl.BurlLibrary;
import edu.brown.cs.burl.burl.BurlRepo;
import edu.brown.cs.burl.burl.BurlRepoColumn;
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
   JSONObject jobj = new JSONObject(lib_data,"id","name","namekey","repotype");
   if (user != null) {
      BurlUserAccess acc = getUserAccess(user.getEmail());
      if (acc == BurlUserAccess.NONE) return null;
      jobj.put("access",acc.toString());
    }

   return jobj;
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



@Override public void addToLibrary(Collection<String> isbns,BurlUpdateMode mode)
{
   BurlRepo repo = getRepository();
   
   Map<String,BurlRepoRow> knownisbns = new HashMap<>();
   List<BurlRepoColumn> isbncols = new ArrayList<>(repo.getIsbnFields());
   for (BurlRepoRow row : repo.getRows()) {
      for (BurlRepoColumn isbncol : isbncols) { 
         String ib = row.getData(isbncol);
         if (ib == null || ib.isEmpty()) continue;
         if (isbncol.isMultiple()) {
            String [] isb = ib.split(isbncol.getMultiplePattern());
            for (String s : isb) {
               knownisbns.put(s,row);
             }
          }
         else {
            knownisbns.put(ib,row);
          }
       }
    }
   
   for (String isbn : isbns) {
      if (!BurlUtil.isValidISBN(isbn)) {
         IvyLog.logW("BURL","Invalid ISBN: " + isbn + ", ignored");
         continue;
       }
      
      BurlRepoRow row = knownisbns.get(isbn);
      if (row != null) {
         switch (mode) {
            case AUGMENT :
               continue;
            case COUNT :
                incrementCount(row);
                break;
            case REPLACE :
            case REPLACE_FORCE :
               break;
          }
       }
      
      BurlBibEntry bibentry = burl_control.findBibEntry(isbn);
      
      // possibly do a broader search if bibentry is null here
      
      if (row == null) {
         row = repo.newRow();
         repo.setInitialValues(row,isbn);
       }
      repo.computeEntry(row,isbn,bibentry);
      
      if (bibentry != null) {
         IvyLog.logD("BURL","Computed BIB ENTRY for " + isbn);
       }
      else {
         String altisbn = BurlUtil.computeAlternativeISBN(isbn);
         IvyLog.logI("BURL",
               "Can't find any information on " + isbn + " " + altisbn);
       }
    }
}


private void incrementCount(BurlRepoRow row)
{
   BurlRepo repo = getRepository();
   BurlRepoColumn col = repo.getCountField();
   if (col == null) return;
   String v = row.getData(col);
   if (v == null || v.isEmpty()) v = "1";
   else {
      int ct = 0;
      try {
         ct = Integer.parseInt(v);
       }
      catch (NumberFormatException e) { }
      v = String.valueOf(ct+1);
    }
   row.setData(col,v);
}



}	// end of class ControlLibrary




/* end of ControlLibrary.java */

