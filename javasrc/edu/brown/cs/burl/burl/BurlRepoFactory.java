/********************************************************************************/
/*                                                                              */
/*              BurlRepoFactory.java                                            */
/*                                                                              */
/*      Factory for instatiating repositories                                   */
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



public interface BurlRepoFactory extends BurlConstants
{


/**
 *      Open a repository
 **/

BurlRepo createRepository(BurlLibrary lib);


}       // end of interface BurlRepoFactory




/* end of BurlRepoFactory.java */

