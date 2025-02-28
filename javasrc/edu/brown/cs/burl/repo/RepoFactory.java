/********************************************************************************/
/*                                                                              */
/*              RepoFactory.java                                                */
/*                                                                              */
/*      description of class                                                    */
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


package edu.brown.cs.burl.repo;

import edu.brown.cs.burl.burl.BurlControl;
import edu.brown.cs.burl.burl.BurlLibrary;
import edu.brown.cs.burl.burl.BurlRepo;
import edu.brown.cs.burl.burl.BurlRepoFactory;

public class RepoFactory implements RepoConstants, BurlRepoFactory
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BurlControl     burl_main;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public RepoFactory(BurlControl ctrl)
{
   burl_main = ctrl;
}



/********************************************************************************/
/*                                                                              */
/*      Create a repository instance                                            */
/*                                                                              */
/********************************************************************************/

@Override public BurlRepo createRepository(BurlLibrary lib)
{
   if (lib == null) return null;
   
   BurlRepoType rtyp = lib.getRepoType();
   BurlRepo repo = null;
   
   switch (rtyp) {
      case CSV :
         repo = new RepoCsv(burl_main,lib);  
         break;
      case JSON :
         repo = new RepoJson(burl_main,lib); 
         break;
      case DATABASE :
         repo = new RepoDatabase(burl_main,lib);
         break;
    }
   
   if (repo != null) {
      repo.openRepository(); 
    }

   return repo;
}






}       // end of class RepoFactory




/* end of RepoFactory.java */

