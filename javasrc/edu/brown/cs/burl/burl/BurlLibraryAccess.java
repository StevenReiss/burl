/********************************************************************************/
/*                                                                              */
/*              BurlLibraryAccess.java                                          */
/*                                                                              */
/*      Representation of access permission to a library for a user             */
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



public interface BurlLibraryAccess extends BurlConstants
{


/**
 *      Return the library id
 **/

Number getLibraryId();


/**
 *      Return the user email
 **/

String getUserEmail();


/**
 *      Return the access level for the user-library
 **/

BurlUserAccess getAccessLevel();


}       // end of interface BurlLibraryAccess




/* end of BurlLibraryAccess.java */

