/********************************************************************************/
/*                                                                              */
/*              BurlUser.java                                                   */
/*                                                                              */
/*      Information about a user                                                */
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



public interface BurlUser extends BurlConstants
{

/**
 *      Get the user's email
 **/

String getEmail();



/**
 *      Get the encoded password
 **/

String getPassword();


/**
 *      Get the salt string for this user
 **/

String getSalt();


/**
 *      Get the id of the user
 **/

Number getId();



}       // end of interface BurlUser




/* end of BurlUser.java */

