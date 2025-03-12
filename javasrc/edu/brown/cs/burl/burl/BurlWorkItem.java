/********************************************************************************/
/*                                                                              */
/*              BurlWorkItem.java                                               */
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


package edu.brown.cs.burl.burl;



public interface BurlWorkItem extends BurlConstants
{

/**
 *      Return unique id of this item (to remove)
 **/

Number getItemId();


/**
 *      Return library id
 **/

Number getLibraryId();


/**
 *      Get the item to work on
 **/

String getItem();


/**
 *      Return update mode
 **/

BurlUpdateMode getUpdateMode();


/**
 *      Tell if count is enabled
 **/

boolean doCount();


}       // end of interface BurlWorkItem




/* end of BurlWorkItem.java */

