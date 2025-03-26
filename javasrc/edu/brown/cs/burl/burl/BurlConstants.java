/********************************************************************************/
/*										*/
/*		BurlConstants.java						*/
/*										*/
/*	General constants for the BURL home library system			*/
/*										*/
/********************************************************************************/
/*	Copyright 2025 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *										 *
 *  This work is licensed under Creative Commons Attribution-NonCommercial 4.0	 *
 *  International.  To view a copy of this license, visit			 *		
 *	https://creativecommons.org/licenses/by-nc/4.0/ 			 *
 *										 *
 ********************************************************************************/



package edu.brown.cs.burl.burl;

import java.util.Iterator;

public interface BurlConstants {



/********************************************************************************/
/*                                                                              */
/*      Random string lengths                                                   */
/*                                                                              */
/********************************************************************************/

int     EMAIIL_CODE_LENGTH = 48;
int     SALT_LENGTH = 16;
int     PASSWORD_LENGTH = 12;
int     SESSION_CODE_LENGTH = 32;
int     NAME_KEY_LENGTH = 12;
int     NEW_ROW_MARKER_LENGTH = 24;



/********************************************************************************/
/*                                                                              */
/*      Connection constants                                                    */
/*                                                                              */
/********************************************************************************/

/**
 *	Definitions for web server
 **/

int HTTPS_PORT = 6737;



/********************************************************************************/
/*                                                                              */
/*      Enumerations                                                            */
/*                                                                              */
/********************************************************************************/

/**
 *  How to interpret and set ISBN fields
 **/

enum BurlIsbnType {
   NONE,           // no checking
   ISBN10,         // 10 digit ISBN
   ISBN13,         // 13 digit ISBN
   ORIGINAL,       // given ISBN for the book
   ALL,            // all ISBNs for the book
}



/**
 *  Output formats for export
 **/

enum BurlExportFormat {
   CSV,                 // as a CSV file
   JSON,                // as a JSON file
}


/**
 *  Permission levels
 **/

enum BurlUserAccess {
   NONE,                // no access
   VIEWER,              // read-only access
   EDITOR,              // edit information fields
   LIBRARIAN,           // edit all fields
   OWNER,               // all access plus remove
}


/**
 *  Type of repository
 **/

enum BurlRepoType {
   DATABASE,            // Use the system database
   CSV,                 // local CSV file
   JSON,                // local JSON file
}


/**
 *  Update mode
 **/

enum BurlUpdateMode {
   NEW,                 // add new entry to database, no merge
   REPLACE,             // replace old data with new
   REPLACE_FORCE,       // replace old data even if new is empty
   AUGMENT,             // only replace if old is empty
   SKIP,                // keep old entry
}


/**
 *      Possible fixes for various fields
 **/

enum BurlFixType {
   NONE,
   LAST_FIRST,          // first last => last, first
   LCC_CODE,            // remove excess zeros from LC classifcation
   LCCN,                // normalize the LCCN
   ISBN,                // adjust iand normalize isbn
   YES_NO,              // yes or no are the only valid values
   DATE,                // date containing only a year
   DEFAULT,             // set to default value
}
   


/**
 *      Type of sort desired based on field
 **/

enum BurlSortType {
   NORMAL,              // normal sorting, nothing fancy                              
   NOCASE,              // ignore case while sorting
   TITLE,               // ignore leading articles (the, a, an, ...)
}



/**
 *      Generic iterator that knows its count
 **/

interface BurlCountIter<T> extends Iterator<T> {

   int getRowCount();
   int getIndex();

}       // end of interface BurlCountIter



/**
 *      Count iterator for Repository rows
 **/

interface BurlRowIter extends BurlCountIter<BurlRepoRow>, Iterable<BurlRepoRow> {
   
   int getRowCount(); 
   int getIndex();
   
}       // end of interface BurlRowIter


}	// end of interface BurlConstants




/* end of BurlConstants.java */
