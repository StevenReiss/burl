/********************************************************************************/
/*                                                                              */
/*              ControlSession.java                                             */
/*                                                                              */
/*      Session holder for BURL                                                 */
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

import org.json.JSONObject;

import edu.brown.cs.burl.burl.BurlUtil;
import edu.brown.cs.ivy.bower.BowerSessionBase;

class ControlSession extends BowerSessionBase implements ControlConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BowerSessionStore<ControlSession> session_store;
private ControlUser     session_user;
private Number          session_userid;
private String          session_code;
private long            last_time;
private long            create_time;
private Number          library_id;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ControlSession(BowerSessionStore<ControlSession> bss)
{
   session_store = bss;
   session_user = null;
   session_userid = null;
   library_id = null;
   session_code = BurlUtil.randomString(SESSION_CODE_LENGTH); 
   last_time = System.currentTimeMillis();
   create_time = System.currentTimeMillis();
}


ControlSession(BowerSessionStore<ControlSession> bss,JSONObject data)
{
   session_store = bss;
   session_user = null;
   session_userid = data.optNumber("userid",null);
   library_id = data.optNumber("libraryid",null);
   last_time = data.optLong("last_time");
   session_code = data.optString("code",null);
   create_time = data.optLong("creation_time");
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public BowerSessionStore<ControlSession> getSessionStore()
{
   return session_store;
}

Number getUserId()                      { return session_userid; }
ControlUser getUser()                   { return session_user; }

void setUser(ControlUser u)
{
   session_user = u;
   session_userid = (u == null ? null : u.getId());
}

void setUserId(Number uid) 
{
   session_userid = uid;
}


String getCode()                        { return session_code; }
void setCode(String code)               { session_code = code; }


Number getLibraryId()                   { return library_id; }

void setLibraryId(Number lid)
{
   library_id = lid;
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

JSONObject toJson()
{
   JSONObject rslt = BurlUtil.buildJson("session",getSessionId(), 
         "userid",session_userid,
         "libraryid",library_id,
         "code",session_code,
         "creation_time",create_time,
         "last_used",last_time);
   
   return rslt;
}




}       // end of class ControlSession




/* end of ControlSession.java */

