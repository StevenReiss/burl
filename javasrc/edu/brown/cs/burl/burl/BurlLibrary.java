/********************************************************************************/
/*                                                                              */
/*              BurlLibrary.java                                                */
/*                                                                              */
/*      Description of an individual library                                    */
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

import org.json.JSONObject;

public interface BurlLibrary extends BurlConstants
{


/**
 *      Get the internal ID of the libary
 **/

Number getId();

/**
 *      Get the name of this library
 **/

String getName();


/**
 *      Get the repository for this library
 **/

BurlRepo getRepository();


/**
 *      Get the unique name for this library
 **/

String getNameKey();


/**
 *      Get the repository type
 **/

BurlRepoType getRepoType();


/**
 *      Get the permissions for a particular user
 **/
 
BurlUserAccess getUserAccess(String email);

/**
 *      Get Json for external use
 **/

JSONObject toJson(BurlUser user);


/**
 *      Add/Remove user or change access
 **/

void changeUserAccess(String email,BurlUserAccess access);



/**
 *      Add books (entries) to a library
 **/

void addToLibrary(Collection<String> isbns,BurlUpdateMode mode,boolean count); 



}       // end of interface BurlLibrary




/* end of BurlLibrary.java */

