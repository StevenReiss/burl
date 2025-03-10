/********************************************************************************/
/*                                                                              */
/*              BurlControl.java                                                */
/*                                                                              */
/*      Main controller to access the various components and utilities          */
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


package edu.brown.cs.burl.burl;

import java.io.File;
import java.util.Collection;

public interface BurlControl extends BurlConstants
{

/**
 *      Create a new library
 **/

BurlLibrary createLibrary(String name,BurlUser owner,BurlRepoType rtype);



/**
 *      Remove a library
 **/

void removeLibrary(BurlLibrary lib);



/**
 *      Find a user by email
 **/

BurlUser findUser(String email);



/**
 *      Return the set of libraries for a user
 **/

Collection<BurlLibrary> getLibraries(BurlUser user);

/**
 *      return the update mode to be used
 **/

BurlUpdateMode getUpdateMode();


/**
 *      return the default count mode
 **/

boolean getDoCount();


/**
 *      Return the data directory
 **/

File getDataDirectory();


/**
 *      Return the storage manager (database hook)
 **/

BurlStorage getStorage();





}       // end of interface BurlControl




/* end of BurlControl.java */

