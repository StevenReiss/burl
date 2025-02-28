/********************************************************************************/
/*                                                                              */
/*              ControlRepoManager.java                                         */
/*                                                                              */
/*      Provide access to active repositories                                   */
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

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import edu.brown.cs.burl.burl.BurlLibrary;
import edu.brown.cs.burl.burl.BurlRepo;

class ControlRepoManager implements ControlConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ControlMain burl_main;
private Map<String,ActiveRepo> repo_map;
private PriorityQueue<ActiveRepo> active_repos;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ControlRepoManager(ControlMain main)
{
   burl_main = main;
   repo_map = new HashMap<>();
   active_repos = new PriorityQueue<>();
}



/********************************************************************************/
/*                                                                              */
/*      Find a repo                                                             */
/*                                                                              */
/********************************************************************************/

BurlRepo findRepository(String email,ControlLibrary lib)
{
   if (lib == null) return null;
   
   ActiveRepo ar = null;
   
   if (email != null) {
      BurlUserAccess acc = lib.getUserAccess(email);
      if (acc == BurlUserAccess.NONE) return null;
    }
   
   synchronized (this) {
      ar = repo_map.get(lib.getNameKey());
      if (ar == null) {
         ar = new ActiveRepo(lib);
       }
      ar.noteUsed();
      active_repos.remove(ar);
      active_repos.add(ar);
    }
   
   if (active_repos.size() > MAX_REPOS) {
      ActiveRepo ar1 = null;
      synchronized (this) {
         ar1 = active_repos.remove();
       }
      ar1.closeRepository();
    }
   
   return ar.getRepository();
}



/********************************************************************************/
/*                                                                              */
/*      Track active repositories                                               */
/*                                                                              */
/********************************************************************************/

private class ActiveRepo implements Comparable<ActiveRepo> {
   
   private BurlLibrary for_library;
   private BurlRepo active_repo;
   private long last_used;
   
   ActiveRepo(BurlLibrary lib) {
      for_library = lib;
      active_repo = null;
      last_used = System.currentTimeMillis();
    }
   
   BurlRepo getRepository() {
      if (active_repo != null) return active_repo;
      active_repo = burl_main.getRepoFactory().createRepository(for_library);
      return active_repo;
    }
   
   void closeRepository() {
      BurlRepo br = active_repo;
      active_repo = null;
      br.closeRepository();
    }
   
   void noteUsed() {
      last_used = System.currentTimeMillis();
    }
   
   @Override public int compareTo(ActiveRepo ar) {
      return Long.compare(last_used,ar.last_used);
    }
   
}       // end of inner class ActiveRepo


}       // end of class ControlRepoManager




/* end of ControlRepoManager.java */

