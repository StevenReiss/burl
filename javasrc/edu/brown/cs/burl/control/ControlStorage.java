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
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
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
private Connection	sql_database;
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

   boolean propsfound = false;

   Properties props = burl_control.getProperties();
   if (props.containsKey("edu.brown.cs.ivy.file.dbmstype")) {
      try {
	 IvyDatabase.setProperties(props);
	 propsfound = true;
       }
      catch (SQLException e) { }
    }

   File f1 = new File(System.getProperty("user.home"));
   File f2 = new File(f1,".config");
   File f3 = new File(f2,"burl");
   File f4 = new File(f3,"database.props");

   if (!propsfound && f4.exists()) {
      try {
	 IvyDatabase.setProperties(f4);
	 propsfound = true;
       }
      catch (SQLException e) { }
    }
   File f5 = new File(f1,"database.props");
   if (!propsfound && f5.exists()) {
      try {
	 IvyDatabase.setProperties(f5);
	 propsfound = true;
       }
      catch (SQLException e) { }
    }
   if (!propsfound) {
      try (InputStream ins = getClass().getClassLoader().getResourceAsStream("database.props")) {
	 IvyDatabase.setProperties(ins);
	 propsfound = true;
       }
      catch (Exception e) {
	 IvyLog.logE("BURL","Database properties not found or bad");
	 System.exit(1);
       }
    }

   checkDatabase();
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

   List<JSONObject> userset = sqlQueryN(q1,email);
   if (!userset.isEmpty()) throw new BurlException("User already registered");

   if (salt == null) {
      salt = BurlUtil.randomString(SALT_LENGTH);
    }
   boolean valid = (validator == null || validator.isEmpty());
   sqlUpdate(q2,email,pwd,salt,valid);
   
   JSONObject user = sqlQuery1(q1,email);

   if (user == null) return null;
   
   ControlUser cuser = new ControlUser(user);
   
   if (!valid) {
      sqlUpdate(q3,cuser.getId(),validator);
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
   
   JSONObject rslt = sqlQuery1(q1,email,code);
   if (rslt == null) return false;
   
   Number uid = rslt.getNumber("userid");
   sqlUpdate(q2,uid,code);
   sqlUpdate(q3,uid);
   
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
         sqlUpdate(q1,user.getId()); 
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
   
   JSONObject juser = sqlQuery1(q1,email);
   if (juser == null) return null;
   
   return new ControlUser(juser);
}


@Override public ControlUser findUserById(Number uid) 
{
   if (uid == null) return null;
   
   String q1 = "SELECT * FROM BurlUsers WHERE id = $1";
   
   JSONObject juser = sqlQuery1(q1,uid);
   if (juser == null) return null;
   
   return new ControlUser(juser);
}



@Override public void removeUser(Number uid)
{
   if (uid == null) return; 
   
   String q1 = "DELETE FROM BurlUserAccess WHERE userid = $1";
   String q2 = "DELETE FROM BurlUsers WHERE id = $1";
   // third query to delete any libraries that have no associated users
   
   sqlUpdate(q1,uid);
   sqlUpdate(q2,uid); 
}



@Override public void updatePassword(Number uid,String pwd)
{ 
   String q1 = "UPDATE iQsignUsers SET password = $1, " +
      " temppassword = NULL " +
      "WHERE id = $2";
   sqlUpdate(q1,pwd,uid);
}



@Override public void setTemporaryPassword(Number uid,String pwd)
{
   String q1 = "UPDATE BurlUsers SET temp_password = $1 WHERE id = $2";
   sqlUpdate(q1,pwd,uid);
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
      int ct = sqlUpdate(q1,name,namekey,repotype.ordinal());
      if (ct == 1) break;
      // retry in case of duplicate namekeys      
    }
   
   JSONObject libj = sqlQuery1(q2,namekey);
   if (libj == null) return null;
   
   return new ControlLibrary(burl_control,libj);
}



@Override public Collection<BurlLibrary> findLibrariesForUser(BurlUser user)
{
   String q1 = "SELECT * FROM BurlUserAccess WHERE email = $1";
   
   List<JSONObject> accs = sqlQueryN(q1,user.getEmail());
   
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
   JSONObject libj = sqlQuery1(q1,lid);
   if (libj == null) return null;
   
   return new ControlLibrary(burl_control,libj);
} 


@Override public void removeLibrary(Number lid)
{
   String q1 = "DELETE FROM BurlUserAccess WHERE libraryid = $1";
   String q2 = "DELETE FROM BurlLibraries WHERE id = $1";
   
   sqlUpdate(q1);
   sqlUpdate(q2);
}



/********************************************************************************/
/*                                                                              */
/*      Library access methods                                                  */
/*                                                                              */
/********************************************************************************/

@Override public BurlUserAccess getUserAccess(String email,Number lid) 
{
   String q1 = "SELECT * FROM BurlUserAccess WHERE email = $1 AND libraryid = $2";
   
   JSONObject jobj = sqlQuery1(q1,email,lid);
   if (jobj == null) return BurlUserAccess.NONE;
   ControlAccess acc = new ControlAccess(jobj);
   return acc.getAccessLevel();
}


@Override public void setUserAccess(String email,Number lid,BurlUserAccess acc)
{
   String q1 = "DELETE FROM BurlUserAccess WHERE email = $1 and libraryid = $2";
   String q2 = "INSERT INTO BurlUserAccess ( email, libraryid, access_level ) " +
         "VALUES ( $1, $2, $3 )";
   
   sqlUpdate(q1,email,lid);
   
   if (acc != BurlUserAccess.NONE) {
      sqlUpdate(q2,email,lid,acc);
    }
}



@Override public List<BurlLibraryAccess> getLibraryAccess(Number lid)
{
   String q1 = "SELECT * FROM BurlUserAccess WHERE libraryid = $1 AND access_level = $2";
   List<JSONObject> accs = sqlQueryN(q1,lid,BurlUserAccess.OWNER.ordinal());
   
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
   sqlUpdate(q,sid,code);
}


@Override public void updateSession(String sid,Number uid,Number lid) 
{
   String q = "UPDATE BurlSession SET userid = $1, libraryid = $2, last_used = CURRENT_TIMESTAMP " +
      "WHERE session = $3";
   sqlUpdate(q,uid,lid,sid);
}



@Override public void removeSession(String sid)
{
   String q = "DELETE FROM BurlSession WHERE session = $1";
   sqlUpdate(q,sid);
}


ControlSession checkSession(BowerSessionStore<ControlSession> bss,String sid) 
{
   if (sid == null || sid.isEmpty()) return null;
   
   String q = "SELECT * FROM BurlSession WHERE session = $1";
   
   JSONObject json = sqlQuery1(q,sid);
   if (json != null) {
      long now = System.currentTimeMillis();
      long lupt = json.getLong("last_used");
      if (now - lupt <= SESSION_TIMEOUT) { 
	 ControlSession bs = new ControlSession(bss,json);
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
   
   JSONObject tbl = sqlQuery1(q1,kname);
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
      int ct = sqlUpdate(q2); 
      if (ct < 0) return false;
      sqlUpdate(q3,kname,flds.toString());
      BurlRepoColumn isbnfld = repo.getOriginalIsbnField();
      if (isbnfld != null) {
         String q4 = "CREATE INDEX " + rname + "Isbn ON " + rname + " ( " + isbnfld.getFieldName() + " )";
         sqlUpdate(q4);
       }
      BurlRepoColumn lccnfld = repo.getLccnField();
      if (lccnfld != null) {
         String q5 = "CREATE INDEX " + rname + "Lccn ON " + rname + " ( " + lccnfld.getFieldName() + " )";
         sqlUpdate(q5);
       }
    }
         
   return true;
}


@Override public List<Number> dataFieldSearch(BurlRepo repo,String fld,String val)
{
   List<Number> ids = new ArrayList<>();
   String kname = repo.getNameKey(); 
   String rname = "BurlRepo_" + kname;
   String q1 = "SELECT burl_id FROM " + rname + " WHERE " + fld + " = $1";
   
   List<JSONObject> rslts = sqlQueryN(q1,val);
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
   
   sqlUpdate(q1);
   sqlUpdate(q2,kname);
}



@Override public BurlCountIter<JSONObject> getAllDataRows(BurlRepo repo,
      BurlRepoColumn sort,boolean invert)
{
   String orderby = (sort == null ? "burl_id" : sort.getFieldName());
   String desc = " DESC";
   String kname = repo.getNameKey();
   String rname = "BurlRepo_" + kname;
   String q1 = "SELECT * FROM " + rname + " ORDER BY " + orderby + desc;
   String q2 = "SELECT COUNT(burl_id) FROM " + rname;

   try {
      ResultSet rs = executeQueryStatement(q1);
      JSONObject cntj = sqlQuery1(q2);
      int cnt = cntj.getInt("count");
      return new ResultSetIterator(rs,cnt); 
    }
   catch (SQLException e) {
      IvyLog.logE("BURL","SQL problem",e);
    }
   return null;
}



@Override public JSONObject getDataRow(BurlRepo repo,Number rid)
{
   if (rid == null) return null;
   
   String kname = repo.getNameKey();
   String rname = "BurlRepo_" + kname;
   String q1 = "SELECT * FROM " + rname + " WHERE burl_id = $1";
   
   return sqlQuery1(q1,rid);
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
   sqlUpdate(q1,marker);
   JSONObject jo = sqlQuery1(q2,marker);
   Number id = jo.getNumber("burl_id");
   sqlUpdate(q3,id);
   return id;
}

 
@Override public void updateDataRow(BurlRepo repo,Number rid,String fld,String val)
{
   String kname = repo.getNameKey();
   String rname = "BurlRepo_" + kname;
   String q1 = "UPDATE " + rname + " SET " + fld + " = $1 WHERE burl_id = $2";
   sqlUpdate(q1,val,rid);
}


@Override public void removeDataRow(BurlRepo repo,Number rid)
{
   String kname = repo.getNameKey(); 
   String rname = "BurlRepo_" + kname;
   String q1 = "DELETE FROM " + rname + " WHERE burl_id = $1";
   sqlUpdate(q1,rid);
}


/********************************************************************************/
/*										*/
/*	Utility methods 							*/
/*										*/
/********************************************************************************/

private int sqlUpdate(String query,Object... data)
{
   IvyLog.logD("BURL","SQL: " + query + " " + getDataString(data));

   try {
      return executeUpdateStatement(query,data);
    }
   catch (SQLException e) {
      IvyLog.logE("BURL","SQL problem",e);
    }

   return -1;
}


private JSONObject sqlQuery1(String query,Object... data)
{
   IvyLog.logD("BURL","SQL: " + query + " " + getDataString(data));

   JSONObject rslt = null;

   try {
      ResultSet rs = executeQueryStatement(query,data);
      if (rs.next()) {
	 rslt = getJsonFromResultSet(rs);
       }
      if (rs.next()) rslt = null;
    }
   catch (SQLException e) {
      IvyLog.logE("BURL","SQL problem",e);
    }

   return rslt;
}



private List<JSONObject> sqlQueryN(String query,Object... data)
{
   IvyLog.logD("BURL","SQL: " + query + " " + getDataString(data));

   List<JSONObject> rslt = new ArrayList<>();;

   try {
      ResultSet rs = executeQueryStatement(query,data);
      while (rs.next()) {
	 JSONObject json = getJsonFromResultSet(rs);
	 rslt.add(json);
       }
    }
   catch (SQLException e) {
      IvyLog.logE("BURL","SQL problem",e);
    }

   return rslt;
}



private ResultSet executeQueryStatement(String q,Object... data) throws SQLException
{
   for ( ; ; ) {
      waitForDatabase();

      PreparedStatement pst = setupStatement(q,data);

      try {
	 ResultSet rslt = pst.executeQuery();
	 return rslt;
       }
      catch (SQLException e) {
	 if (checkDatabaseError(e)) throw e;
       }
    }
}


private int executeUpdateStatement(String q,Object... data) throws SQLException
{
   for ( ; ; ) {
      waitForDatabase();

      PreparedStatement pst = setupStatement(q,data);

      try {
	 int rslt = pst.executeUpdate();
	 return rslt;
       }
      catch (SQLException e) {
	 if (checkDatabaseError(e)) throw e;
       }
    }
}



private PreparedStatement setupStatement(String query,Object... data) throws SQLException
{
   query = query.replaceAll("\\$[0-9]+","?");
   PreparedStatement pst = sql_database.prepareStatement(query);
   for (int i = 0; i < data.length; ++i) {
      Object v = data[i];
      if (v instanceof String) {
	 pst.setString(i+1,(String) v);
       }
      else if (v instanceof Integer) {
	 pst.setInt(i+1,(Integer) v);
       }
      else if (v instanceof Long) {
	 pst.setLong(i+1,(Long) v);
       }
      else if (v instanceof Date) {
	 pst.setDate(i+1,(Date) v);
       }
      else if (v instanceof Timestamp) {
	 pst.setTimestamp(i+1,(Timestamp) v);
       }
      else if (v instanceof Boolean) {
	 pst.setBoolean(i+1,(Boolean) v);
       }
      else if (v instanceof Enum) {
         pst.setInt(i+1,((Enum<?>) v).ordinal());
       }
      else {
	 pst.setObject(i+1,v);
       }
    }
   return pst;
}


private String getDataString(Object... data)
{
   if (data.length == 0) return "";

   StringBuffer buf = new StringBuffer();
   for (int i = 0; i < data.length; ++i) {
      if (i == 0) buf.append("[");
      else buf.append(",");
      buf.append(String.valueOf(data[i]));
    }
   buf.append("]");

   return buf.toString();
}


private JSONObject getJsonFromResultSet(ResultSet rs)
{
   JSONObject rslt = new JSONObject();
   try {
      ResultSetMetaData meta = rs.getMetaData();
      for (int i = 1; i <= meta.getColumnCount(); ++i) {
	 String nm = meta.getColumnName(i);
	 Object v = rs.getObject(i);
	 if (v instanceof Date) {
	    Date d = (Date) v;
	    v = d.getTime();
	  }
	 else if (v instanceof Timestamp) {
	    Timestamp ts = (Timestamp) v;
	    v = ts.getTime();
	  }
	 if (v != null) rslt.put(nm,v);
       }
    }
   catch (SQLException e) {
      IvyLog.logE("BURL","Database problem decoding result set ",e);
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
      
      return getJsonFromResultSet(result_set);
    }
   
   
   @Override public int getRowCount()           { return row_count; }
   
}       // end of inner class ResulSetIterator


/********************************************************************************/
/*										*/
/*	Establish database connection						*/
/*										*/
/********************************************************************************/

private boolean checkDatabase()
{
   if (sql_database == null && database_name != null) {
      try {
	 sql_database = IvyDatabase.openDatabase(database_name);
       }
      catch (Throwable t) {
	 IvyLog.logE("BURL","Database connection problem",t);
       }
    }

   return sql_database != null;
}



private void waitForDatabase()
{
   while (sql_database == null) {
      try {
	 Thread.sleep(1000);
       }
      catch (InterruptedException e) { }
      checkDatabase();
    }
}


private boolean checkDatabaseError(SQLException e)
{
   String msg = e.getMessage();
   if (msg.contains("FATAL")) sql_database = null;
   Throwable ex = e.getCause();
   if (ex instanceof IOException) sql_database = null;
   if (sql_database == null) {
      IvyLog.logE("BURL","Database lost connection",e);
    }
   return sql_database != null;
}




}	// end of class ControlStorage




/* end of ControlStorage.java */

