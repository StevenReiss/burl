/********************************************************************************/
/*                                                                              */
/*              BibEntryBase.java                                               */
/*                                                                              */
/*      Parent class for the various bibligraphic entry data classes            */
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


package edu.brown.cs.burl.bibentry;

import edu.brown.cs.burl.burl.BurlBibEntry;
import edu.brown.cs.burl.burl.BurlRepoColumn;

abstract class BibEntryBase implements BurlBibEntry, BibEntryConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public abstract String computeEntry(BurlRepoColumn brc);



}       // end of class BibEntryBase




/* end of BibEntryBase.java */

