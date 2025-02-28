/********************************************************************************/
/*                                                                              */
/*              BurlBibEntry.java                                               */
/*                                                                              */
/*      Data obtained from a source (e.g. Lib of Congress) for an entry         */
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



public interface BurlBibEntry extends BurlConstants
{



/**
 *      Compute the column value from the report data.
 **/

String computeEntry(BurlRepoColumn column);


}       // end of interface BurlBibEntry




/* end of BurlBibEntry.java */

