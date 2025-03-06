/********************************************************************************/
/*                                                                              */
/*              BurlFilter.java                                                 */
/*                                                                              */
/*      Filter for selecting entries                                            */
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



public interface BurlFilter extends BurlConstants
{

/**
 *      Matching methods
 **/

boolean matches(BurlRepoRow row);

/**
 *      Indicate sort field (default is burl_id
 **/

BurlRepoColumn getSortField();


/**
 *      Indicate where sort is inverted
 **/

boolean invertSort();



}       // end of interface BurlFilter




/* end of BurlFilter.java */

