/********************************************************************************/
/*                                                                              */
/*              BurlRepoColumn.java                                             */
/*                                                                              */
/*      Column of informationn for a library element                            */
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



public interface BurlRepoColumn extends BurlConstants, Comparable<BurlRepoColumn>
{

/**
 *      Get the column number
 **/

int getNumber();


/**
 *      Get the column name or header
 **/

String getName();


/**
 *      Return access information for MARC elements
 **/

String getMarcData();


/**
 *      Return access information for OPENLIB elements
 **/

String getOpenLibData();


/**
 *      Return access information for GOOGLE BOOKS elements
 **/

String getGoogleData();



/**
 *      Get name for use as a database field
 **/

String getFieldName();

/**
 *      Get the default value
 **/

String getDefault();


/**
 *      Specify whether can hold multiple values
 **/

boolean isMultiple();


/**
 *      Get ISBN type of the field (how to interpret/set up for ISBN)
 **/

BurlIsbnType getIsbnType(); 


/**
 *      Get alternative ISBN field name (for 10/13 differentiation)
 **/

String getOtherIsbn();


/**
 *      Specify whether this is the count (# copies) field
 **/

boolean isCountField();



/**
 *      Specify wheter this is the original ISBN field
 **/

boolean isOriginalIsbnField();


/**
 *      Specify whether this is the LCCN field
 **/

boolean isLccnField();


/**
 *      Get access level needed for field
 **/

BurlUserAccess getAccessLevel();

/**
 *      Get the multiple item separator
 **/

String getMultipleSeparator();


/**
 *      Get the multiple item separator pattern
 **/

String getMultiplePattern();

/**
 *      Get the label for external viewing
 **/

String getLabel();





/**
 *      Get the type of field value checking and fixing for this field.
 **/

BurlFixType getFixType();

/**
 *      Handle corrections based on field type
 **/

String fixFieldValue(String val);


/**
 *      Comparison method that is needed
 **/

@Override default int compareTo(BurlRepoColumn brc)
{
   return Integer.compare(getNumber(),brc.getNumber());
}





}       // end of interface BurlRepoColumn




/* end of BurlRepoColumn.java */

