/********************************************************************************/
/*                                                                              */
/*              ControlSessionStore.java                                        */
/*                                                                              */
/*      Session store for BOWER web server                                      */
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

import edu.brown.cs.ivy.bower.BowerConstants.BowerSessionStore;

class ControlSessionStore implements BowerSessionStore<ControlSession>, ControlConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ControlStorage store_db;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ControlSessionStore(ControlMain main) {
   store_db = main.getStorage();
}


/********************************************************************************/
/*                                                                              */
/*      Access methnods                                                         */
/*                                                                              */
/********************************************************************************/

@Override public String getSessionCookie()	{ return SESSION_COOKIE; } 
@Override public String getSessionKey()	{ return SESSION_PARAMETER; }
@Override public String getStatusKey()	{ return "status"; }
@Override public String getErrorMessageKey() { return "message"; }



/********************************************************************************/
/*                                                                              */
/*      Session management methods                                              */
/*                                                                              */
/********************************************************************************/

@Override public ControlSession createNewSession() {
   return new ControlSession(this);
}

@Override public void saveSession(ControlSession bs) {
   store_db.startSession(bs.getSessionId(),bs.getCode());
}

@Override public ControlSession loadSession(String sid) {
   ControlSession bs = store_db.checkSession(this,sid); 
   return bs;
}

@Override public void removeSession(ControlSession bs) {
   if (bs == null) return;
   store_db.removeSession(bs.getSessionId());
}

@Override public void updateSession(ControlSession bs) { 
   store_db.updateSession(bs.getSessionId(),bs.getUserId(),
         bs.getLibraryId());
}



}       // end of class ControlSessionStore




/* end of ControlSessionStore.java */

