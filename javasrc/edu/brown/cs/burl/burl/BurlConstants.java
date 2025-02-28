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
   ALL             // all ISBNs for the book
}



/**
 *  Output formats for export
 **/

enum BurlExportFormat {
   CSV,                 // as a CSV file
   JSON,                // as a JSON file
   LABELS,              // as label sheets
}


/**
 *  Permission levels
 **/

enum BurlUserAccess {
   NONE,                // no access
   VIEWER,              // read-only access
   EDITOR,              // edit information fields
   SENIOR,              // etit most fields ???
   LIBRARIAN,           // edit all fields
   ADMIN,               // all access but remove
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
   REPLACE,             // replace old data with new
   REPLACE_FORCE,       // replace old data even if new is empty
   AUGMENT,             // only replace if old is empty
   COUNT,               // augment, but increase book count
}




}	// end of interface BurlConstants




/* end of BurlConstants.java */
