/********************************************************************************/
/*										*/
/*		ControlStorage.java						*/
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

import java.io.File;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.json.JSONObject;

import edu.brown.cs.burl.burl.BurlException;
import edu.brown.cs.burl.burl.BurlLibrary;
import edu.brown.cs.burl.burl.BurlLibraryAccess;
import edu.brown.cs.burl.burl.BurlRepo;
import edu.brown.cs.burl.burl.BurlRepoColumn;
import edu.brown.cs.burl.burl.BurlStorage;
import edu.brown.cs.burl.burl.BurlUser;
import edu.brown.cs.burl.burl.BurlUtil;
import edu.brown.cs.burl.burl.BurlWorkItem;
import edu.brown.cs.ivy.bower.BowerDatabasePool;
import edu.brown.cs.ivy.bower.BowerConstants.BowerSessionStore;
import edu.brown.cs.ivy.file.IvyDatabase;
import edu.brown.cs.ivy.file.IvyLog;

class ControlStorage implements ControlConstants, BurlStorage
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private ControlMain	burl_control;
private BowerDatabasePool sql_database;
private String		database_name;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ControlStorage(ControlMain ctrl)
{
   burl_control = ctrl;
   sql_database = null;
   database_name = BURL_DATA_STORE;

   Properties props = burl_control.getProperties();
   if (props.containsKey("edu.brown.cs.ivy.file.dbmstype")) {
      try {
	 sql_database = new BowerDatabasePool(props,database_name);
       }
      catch (SQLException e) { }
    }

   File f1 = new File(System.getProperty("user.home"));
   File f2 = new File(f1,".config");
   File f3 = new File(f2,"burl");
   File f4 = new File(f3,"database.props");

   if (sql_database == null && f4.exists()) {
      try {
	 sql_database = new BowerDatabasePool(f4,database_name);
       }
      catch (SQLException e) { }
    }
   File f5 = new File(f1,"database.props");
   if (sql_database == null && f5.exists()) {
      try {
	 sql_database = new BowerDatabasePool(f5,database_name);
       }
      catch (SQLException e) { }
    }
   if (sql_database == null) {
      try (InputStream ins = getClass().getClassLoader().getResourceAsStream("database.props")) {
	 sql_database = new BowerDatabasePool(ins,database_name);
       } 
      catch (Exception e) {
	 IvyLog.logE("BURL","Database properties not found or bad");
	 System.exit(1);
       }
    }
}



/********************************************************************************/
/*										*/
/*	User registration and login methods     				*/
/*										*/
/********************************************************************************/

@Override public ControlUser registerUser(String email,String pwd, 
      String salt,String validator) 
   throws BurlException
{
   String q1 = "SELECT * FROM BurlUsers WHERE email = $1";
   String q2 = "INSERT INTO BurlUsers ( id, email, password, salt, valid ) " +
	 "VALUES ( DEFAULT, $1, $2, $3, $4 )";
   String q3 = "INSERT INTO BurlValidator ( id, userid, validator, timeout ) " +
               "VALUES ( DEFAULT, $1, $2, (CURRENT_TIMESTAMP + INTERVAL '2 DAYS' ) )";

   
   if (email == null || pwd == null) return null;
   email = email.toLowerCase();

   List<JSONObject> userset = sql_database.sqlQueryN(q1,email);
   if (!userset.isEmpty()) throw new BurlException("User already registered");

   if (salt == null) {
      salt = BurlUtil.randomString(SALT_LENGTH);
    }
   boolean valid = (validator == null || validator.isEmpty());
   sql_database.sqlUpdate(q2,email,pwd,salt,valid);
   
   JSONObject user = sql_database.sqlQuery1(q1,email);

   if (user == null) return null;
   
   ControlUser cuser = new ControlUser(user);
   
   if (!valid) {
      sql_database.sqlUpdate(q3,cuser.getId(),validator);
    }

   return cuser;
}


@Override public boolean validateUser(String email,String code)
{
   String q1 = "SELECT U.id as userid " +
      "FROM BurlValidator V, BurlUsers U " +
      "WHERE V.userid = U.id AND U.email = $1 AND V.validator = $2 AND " +
      "V.timeout > CURRENT_TIMESTAMP";
   String q2 = "DELETE FROM BurlValidator WHERE userid = $1 AND validator = $2";
   String q3 = "UPDATE BurlUsers SET valid = TRUE WHERE id = $1";
   
   JSONObject rslt = sql_database.sqlQuery1(q1,email,code);
   if (rslt == null) return false;
   
   Number uid = rslt.getNumber("userid");
   sql_database.sqlUpdate(q2,uid,code);
   sql_database.sqlUpdate(q3,uid);
   
   return true;
}



@Override public ControlUser loginUser(String email,String pwd,String localsalt)
{
   String q1 = "UPDATE BurlUsers SET temp_password = NULL WHERE id = $1";
   
   if (email == null) return null;
   email = email.toLowerCase();
   
   ControlUser user = findUserByEmail(email);
   if (user == null) return null;
   if (!user.isValid()) return null; 
   
   String encoded = user.getPassword();
   String encoded1 = BurlUtil.secureHash(encoded + localsalt);
   if (encoded1.equals(pwd)) return user;
   String tencoded = user.getTempPassword();
   if (tencoded != null && !tencoded.isEmpty()) {
      tencoded = BurlUtil.secureHash(encoded + localsalt);
      if (tencoded.equals(pwd)) {
         sql_database.sqlUpdate(q1,user.getId()); 
         return user;
       }
    }

   return null; 
}





@Override public ControlUser findUserByEmail(String email)
{
   if (email == null) return null;
   email = email.toLowerCase();
   
   String q1 = "SELECT * FROM BurlUsers WHERE email = $1";
   
   JSONObject juser = sql_database.sqlQuery1(q1,email);
   if (juser == null) return null;
   
   return new ControlUser(juser);
}


@Override public ControlUser findUserById(Number uid) 
{
   if (uid == null) return null;
   
   String q1 = "SELECT * FROM BurlUsers WHERE id = $1";
   
   JSONObject juser = sql_database.sqlQuery1(q1,uid);
   if (juser == null) return null;
   
   return new ControlUser(juser);
}



@Override public void removeUser(Number uid)
{
   if (uid == null) return; 
   
   String q1 = "DELETE FROM BurlUserAccess WHERE userid = $1";
   String q2 = "DELETE FROM BurlUsers WHERE id = $1";
   // third query to delete any libraries that have no associated users
   
   sql_database.sqlUpdate(q1,uid);
   sql_database.sqlUpdate(q2,uid); 
}



@Override public void updatePassword(Number uid,String pwd)
{ 
   String q1 = "UPDATE BurlUsers SET password = $1, " +
      " temp_password = NULL " +
      "WHERE id = $2";
   sql_database.sqlUpdate(q1,pwd,uid);
}



@Override public void setTemporaryPassword(Number uid,String pwd)
{
   String q1 = "UPDATE BurlUsers SET temp_password = $1 WHERE id = $2";
   sql_database.sqlUpdate(q1,pwd,uid);
}
 


/********************************************************************************/
/*                                                                              */
/*      Library methods                                                         */
/*                                                                              */
/********************************************************************************/

@Override public BurlLibrary createLibrary(String name,BurlRepoType repotype)
{ 
   String q1 = "INSERT INTO BurlLibraries ( id, name, namekey, repo_type ) " +
      "VALUES ( DEFAULT, $1, $2, $3 )";
   String q2 = "SELECT * from BurlLibraries WHERE namekey = $1";
   
   String namekey = null;
   for (int i = 0; i < 3; ++i) {
      namekey = BurlUtil.randomString(NAME_KEY_LENGTH);
      namekey = namekey.toLowerCase();
      int ct = sql_database.sqlUpdate(q1,name,namekey,repotype.ordinal());
      if (ct == 1) break;
      // retry in case of duplicate namekeys      
    }
   
   JSONObject libj = sql_database.sqlQuery1(q2,namekey);
   if (libj == null) return null;
   
   return new ControlLibrary(burl_control,libj);
}



@Override public Collection<BurlLibrary> findLibrariesForUser(BurlUser user)
{
   String q1 = "SELECT * FROM BurlUserAccess WHERE email = $1";
   
   List<JSONObject> accs = sql_database.sqlQueryN(q1,user.getEmail());
   
   List<BurlLibrary> rslt = new ArrayList<>();
   for (JSONObject obj : accs) {
      ControlAccess acc = new ControlAccess(obj);
      BurlLibrary bll = findLibraryById(acc.getLibraryId());
      if (bll != null) rslt.add(bll);
    }
   
   return rslt;
}


@Override public ControlLibrary findLibraryById(Number lid)
{
   if (lid == null) return null;
   
   String q1 = "SELECT * FROM BurlLibraries WHERE id = $1";
   JSONObject libj = sql_database.sqlQuery1(q1,lid);
   if (libj == null) return null;
   
   return new ControlLibrary(burl_control,libj);
} 


@Override public void removeLibrary(Number lid)
{
   String q1 = "DELETE FROM BurlUserAccess WHERE libraryid = $1";
   String q1a = "DELETE FROM BurlWorkQueue WHERE libraryid = $1";
   String q2 = "DELETE FROM BurlLibraries WHERE id = $1";
   
   sql_database.sqlUpdate(q1,lid);
   sql_database.sqlUpdate(q1a,lid);
   sql_database.sqlUpdate(q2,lid);
}



/********************************************************************************/
/*                                                                              */
/*      Library access methods                                                  */
/*                                                                              */
/********************************************************************************/

@Override public BurlUserAccess getUserAccess(String email,Number lid) 
{
   String q1 = "SELECT * FROM BurlUserAccess WHERE email = $1 AND libraryid = $2";
   
   JSONObject jobj = sql_database.sqlQuery1(q1,email,lid);
   if (jobj == null) return BurlUserAccess.NONE;
   ControlAccess acc = new ControlAccess(jobj);
   return acc.getAccessLevel();
}


@Override public void setUserAccess(String email,Number lid,BurlUserAccess acc)
{
   String q1 = "DELETE FROM BurlUserAccess WHERE email = $1 and libraryid = $2";
   String q2 = "INSERT INTO BurlUserAccess ( email, libraryid, access_level ) " +
         "VALUES ( $1, $2, $3 )";
   
   sql_database.sqlUpdate(q1,email,lid);
   
   if (acc != BurlUserAccess.NONE) {
      sql_database.sqlUpdate(q2,email,lid,acc);
    }
}



@Override public List<BurlLibraryAccess> getLibraryAccess(Number lid)
{
   String q1 = "SELECT * FROM BurlUserAccess WHERE libraryid = $1";
   List<JSONObject> accs = sql_database.sqlQueryN(q1,lid);
   
   List<BurlLibraryAccess> rslt = new ArrayList<>();
   for (JSONObject obj : accs) {
      ControlAccess acc = new ControlAccess(obj);
      if (acc.getAccessLevel() != BurlUserAccess.NONE) {
         rslt.add(acc);
       }
    }
   
   return rslt;
}




/********************************************************************************/
/*                                                                              */
/*      Session Management                                                      */
/*                                                                              */
/********************************************************************************/

@Override public void startSession(String sid,String code)
{
   String q = "INSERT INTO BurlSession (session, code) VALUES ( $1, $2 )";
   sql_database.sqlUpdate(q,sid,code);
}


@Override public void updateSession(String sid,Number uid,Number lid) 
{
   String q = "UPDATE BurlSession SET userid = $1, libraryid = $2, last_used = CURRENT_TIMESTAMP " +
      "WHERE session = $3";
   sql_database.sqlUpdate(q,uid,lid,sid);
}



@Override public void removeSession(String sid)
{
   String q = "DELETE FROM BurlSession WHERE session = $1";
   sql_database.sqlUpdate(q,sid);
}


ControlSession checkSession(BowerSessionStore<ControlSession> bss,String sid) 
{
   if (sid == null || sid.isEmpty()) return null;
   
   String q = "SELECT * FROM BurlSession WHERE session = $1";
   
   JSONObject json = sql_database.sqlQuery1(q,sid);
   if (json != null) {
      long now = System.currentTimeMillis();
      long lupt = json.getLong("last_used");
      if (now - lupt <= SESSION_TIMEOUT) { 
	 ControlSession bs = new ControlSession(bss,sid,json); 
	 return bs;
       }
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Repository methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public boolean createDataTable(BurlRepo repo)
{
   String kname = repo.getNameKey();
   String rname = "BurlRepo_" + repo.getNameKey();
   String q1 = "SELECT * FROM BurlRepoStores WHERE name = $1";
   
   JSONObject tbl = sql_database.sqlQuery1(q1,kname);
   if (tbl != null) {
      // check if we need to add a new field to the table and then update the fields
      // by doing ALTER TABLE ADD (or REMOVE) for each field
      // then update RepoStores with new field set
    }
   else {
      StringBuffer flds = new StringBuffer();
      String q2 = "CREATE TABLE " + rname + "( ";
      q2 += "burl_id " + IvyDatabase.getIdDefType() + " NOT NULL PRIMARY KEY";
      for (BurlRepoColumn brc : repo.getColumns()) {
         String dflt = brc.getDefault();
         if (dflt == null) dflt = "NULL";
         else dflt = "'" + dflt + "'";
         q2 += ", " + brc.getFieldName() + " text DEFAULT " + dflt;
         if (!flds.isEmpty()) flds.append(",");
         flds.append(brc.getFieldName());
       }
      q2 += ")";
      String q3 = "INSERT INTO BurlRepoStores ( name, fields ) VALUES ( $1, $2 )";
      int ct = sql_database.sqlUpdate(q2); 
      if (ct < 0) return false;
      sql_database.sqlUpdate(q3,kname,flds.toString());
      BurlRepoColumn isbnfld = repo.getOriginalIsbnField();
      if (isbnfld != null) {
         String q4 = "CREATE INDEX " + rname + "Isbn ON " + rname + " ( " + isbnfld.getFieldName() + " )";
         sql_database.sqlUpdate(q4);
       }
      BurlRepoColumn lccnfld = repo.getLccnField();
      if (lccnfld != null) {
         String q5 = "CREATE INDEX " + rname + "Lccn ON " + rname + " ( " + lccnfld.getFieldName() + " )";
         sql_database.sqlUpdate(q5);
       }
    }
         
   return true;
}


@Override public List<Number> dataFieldSearch(BurlRepo repo,String fld,Object val) 
{
   List<Number> ids = new ArrayList<>();
   String kname = repo.getNameKey(); 
   String rname = "BurlRepo_" + kname;
   String q1 = "SELECT burl_id FROM " + rname + " WHERE " + fld + " = $1 ORDER BY burl_id";
   
   List<JSONObject> rslts = sql_database.sqlQueryN(q1,val);
   for (JSONObject rslt : rslts) {
      ids.add(rslt.getNumber("burl_id"));
    }
   return ids;
}



@Override public void removeDataTable(BurlRepo repo)
{
   String kname = repo.getNameKey();
   String rname = "BurlRepo_" + repo.getNameKey();
   String q1 = "DROP TABLE IF EXISTS " + rname + " CASCADE";
   String q2 = "DELETE FROM BurlRepoStores WHERE name = $1";
   
   sql_database.sqlUpdate(q1);
   sql_database.sqlUpdate(q2,kname);
}



@Override public BurlCountIter<JSONObject> getAllDataRows(BurlRepo repo,
      BurlRepoColumn sort,boolean invert)
{
// String orderby = (sort == null ? "burl_id" : sort.getFieldName());
// String desc = (invert ? " DESC" : "");
   String orderby = getOrderBy(sort,invert);
   String kname = repo.getNameKey();
   String rname = "BurlRepo_" + kname;
   String q1 = "SELECT * FROM " + rname + orderby;
   String q2 = "SELECT COUNT(burl_id) FROM " + rname;

   try {
      ResultSet rs = sql_database.executeQueryStatement(q1);
      JSONObject cntj = sql_database.sqlQuery1(q2);
      int cnt = cntj.getInt("count");
      return new ResultSetIterator(rs,cnt); 
    }
   catch (SQLException e) {
      IvyLog.logE("BURL","SQL problem",e);
    }
   return null;
}


private String getOrderBy(BurlRepoColumn sort,boolean invert)
{
   String pfx = " ORDER BY ";
   String sfx = (invert ? " DESC" : "");
   String body = "burl_id";
   if (sort != null) {
      BurlSortType sorttype = sort.getSortType();
      switch (sorttype) {
         case NORMAL :
            body = sort.getFieldName();
            break;
         case NOCASE :
            body = "LOWER(" + sort.getFieldName() +")";
            break;
         case TITLE :
            body = "TRIM(LEADING 'a ' FROM TRIM(LEADING 'an ' FROM " +
               "TRIM(LEADING 'the ' FROM LOWER(" +
               sort.getFieldName() + "))))";
            break;
       }
    }
   
   return pfx + body + sfx;
}



@Override public JSONObject getDataRow(BurlRepo repo,Number rid)
{
   if (rid == null) return null;
   
   String kname = repo.getNameKey();
   String rname = "BurlRepo_" + kname;
   String q1 = "SELECT * FROM " + rname + " WHERE burl_id = $1";
   
   return sql_database.sqlQuery1(q1,rid);
}


@Override public Number addDataRow(BurlRepo repo)
{
   String fld = null;
   for (BurlRepoColumn brc : repo.getColumns()) {
      if (brc.getDefault() == null || brc.getDefault().equals("NULL")) {
         fld = brc.getFieldName();
         break;
       }
    }
   String kname = repo.getNameKey();
   String rname = "BurlRepo_" + kname;
   String q1 = "INSERT INTO " + rname + " ( burl_id, " + fld + ") VALUES ( DEFAULT, $1 )";
   String q2 = "SELECT burl_id FROM " + rname + " WHERE " + fld + " = $1";
   String q3 = "UPDATE " + rname + " SET " + fld + " = NULL WHERE burl_id = $1";
   String marker = BurlUtil.randomString(NEW_ROW_MARKER_LENGTH);
   sql_database.sqlUpdate(q1,marker);
   JSONObject jo = sql_database.sqlQuery1(q2,marker);
   Number id = jo.getNumber("burl_id");
   sql_database.sqlUpdate(q3,id);
   return id;
}

 
@Override public void updateDataRow(BurlRepo repo,Number rid,String fld,String val)
{
   String kname = repo.getNameKey();
   String rname = "BurlRepo_" + kname;
   String q1 = "UPDATE " + rname + " SET " + fld + " = $1 WHERE burl_id = $2";
   sql_database.sqlUpdate(q1,val,rid);
}


@Override public void removeDataRow(BurlRepo repo,Number rid)
{
   String kname = repo.getNameKey(); 
   String rname = "BurlRepo_" + kname;
   String q1 = "DELETE FROM " + rname + " WHERE burl_id = $1";
   sql_database.sqlUpdate(q1,rid);
}



/********************************************************************************/
/*                                                                              */
/*      Work queue methods                                                      */
/*                                                                              */ 
/********************************************************************************/

@Override public void addToWorkQueue(Number libid,String isbn,BurlUpdateMode upd,boolean count)
{
   String q1 = "INSERT INTO BurlWorkQueue ( id, libraryid, item, count, mode ) " +
      "VALUES ( DEFAULT, $1, $2, $3, $4 )";
   
   sql_database.sqlUpdate(q1,libid,isbn,count,upd.ordinal());
}



@Override public void removeFromWorkQueue(Number wqid)
{
   String q1 = "DELETE FROM BurlWorkQueue WHERE id = $1";
   
   sql_database.sqlUpdate(q1,wqid);
}


@Override public List<BurlWorkItem> getWorkQueue()
{
   String q1 = "SELECT * FROM BurlWorkQueue ORDER BY id";
   
   List<JSONObject> data = sql_database.sqlQueryN(q1);
   
   List<BurlWorkItem> rslt = new ArrayList<>();
   for (JSONObject jo : data) {
      ControlWorkItem witm = new ControlWorkItem(jo);
      rslt.add(witm);
    }
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Iterating over a result set                                             */
/*                                                                              */
/********************************************************************************/

private class ResultSetIterator implements BurlCountIter<JSONObject> {
   
   private ResultSet result_set;
   private boolean next_done;
   private int row_count;
   
   ResultSetIterator(ResultSet rs,int ct) {
      result_set = rs;
      next_done = false;
      row_count = ct;
    }
   
   @Override public boolean hasNext() {
      if (next_done) return true;
      try {
         boolean fg = result_set.next();
         if (fg) next_done = true;
         return fg;
       }
      catch (SQLException e) {
         IvyLog.logE("REPO","SQL problem doing interation",e);
         return false;
       }
    }
   
   @Override public JSONObject next() {
      if (!next_done) {
         if (!hasNext()) return null;
       }
      next_done = false;
      
      return sql_database.getJsonFromResultSet(result_set); 
    }
   
   
   @Override public int getRowCount()           { return row_count; }
   
}       // end of inner class ResulSetIterator




}	// end of class ControlStorage




/* end of ControlStorage.java */

