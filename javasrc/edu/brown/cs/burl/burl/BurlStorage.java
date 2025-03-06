/********************************************************************************/
/*                                                                              */
/*              BurlStorage.java                                                */
/*                                                                              */
/*      Storage for BURL (non-repository)                                       */
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

import java.util.Collection;
import java.util.List;

import org.json.JSONObject;



public interface BurlStorage extends BurlConstants
{


/**
 *      Register a user
 **/

BurlUser registerUser(String email,String pwd,String salt,String validator)
      throws BurlException;

/**
 *      Validate user (from initial email)
 **/

boolean validateUser(String email,String code);


/**
 *      Login a user
 **/

BurlUser loginUser(String email,String pwd,String localsalt);


/**
 *      Find a user by email
 **/

BurlUser findUserByEmail(String email);

/**
 *      Find a user by id
 **/

BurlUser findUserById(Number uid);


/**
 *      Change a user's password
 **/

void updatePassword(Number uid,String pwd);


/**
 *      Set temporary password for a user
 **/

void setTemporaryPassword(Number uid,String pwd);


/**
 *      Remove a user
 **/

void removeUser(Number uid);


/**
 *      Create a new library
 **/

BurlLibrary createLibrary(String name,BurlRepoType repotype);



/**
 *      Find the libraries user has access to
 **/

Collection<BurlLibrary> findLibrariesForUser(BurlUser user);

/**
 *      Find library given its id
 **/

BurlLibrary findLibraryById(Number lid);

/**
 *      Remove a library
 **/

void removeLibrary(Number lid);


/**
 *      Get the user's access to a library
 **/

BurlUserAccess getUserAccess(String email,Number lid);


/**
 *      Set user access to a library
 **/

void setUserAccess(String email,Number lid,BurlUserAccess access);


/**
 *      Get the users that can access a library
 **/
 
List<BurlLibraryAccess> getLibraryAccess(Number lid);


/**
 *      Save web session information
 **/

void startSession(String sid,String code);

/**
 *      Find an existing session:  this is local with ControlSession
 **/

// BowerSession checkSession(BowerSessionStore<?> bss,String sid);


/**
 *      Remove a web session
 **/

void removeSession(String sid);


/**
 *      Update a session
 **/

void updateSession(String sid,Number userid,Number libraryid);


/**
 *      Create or update a table to store a library if needed
 **/

boolean createDataTable(BurlRepo repo);


/**
 *      Remove the data table for a library
 **/

void removeDataTable(BurlRepo repo);


/*
 *      Look for all ids where the field has a given value
 **/

List<Number> dataFieldSearch(BurlRepo repo,String field,String value);


/**
 *      Return a result set for iterating over data rows
 **/

BurlCountIter<JSONObject> getAllDataRows(BurlRepo repo,BurlRepoColumn sort,boolean invert);


/**
 *      Return a particular row
 **/

JSONObject getDataRow(BurlRepo repo,Number id);
      
      
/**
 *      Add a new row 
 **/

Number addDataRow(BurlRepo repo);

/**
 *      Update a particular row
 **/

void updateDataRow(BurlRepo repo,Number id,String fld,String val);


/**
 *      Remove a data row
 **/

void removeDataRow(BurlRepo repo,Number id);



}       // end of interface BurlStorage




/* end of BurlStorage.java */

