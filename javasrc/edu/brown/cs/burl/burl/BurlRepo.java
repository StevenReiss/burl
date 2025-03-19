/********************************************************************************/
/*                                                                              */
/*              BurlRepo.java                                                   */
/*                                                                              */
/*      Repository or library                                                   */
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

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

public interface BurlRepo extends BurlConstants
{

/**
 *      Get the set of columns (ordered)
 **/

Collection<BurlRepoColumn> getColumns();





/**
 *      Return the field containing the original ISBN
 **/

BurlRepoColumn getOriginalIsbnField();


/**
 *      Return the field containing the LCCN
 **/

BurlRepoColumn getLccnField();


/**
 *      Return the field containing labeled flag
 **/

BurlRepoColumn getPrintLabelsField();


/**
 *      Return the set of ISBN fields
 **/

Collection<BurlRepoColumn> getIsbnFields();


/**
 *      Return the controller
 **/

BurlControl getBurl();


/**
 *      Get the name of the libarry
 **/

String getName();


/**
 *      Get the namekey of the library
 **/

String getNameKey();


/**
 *      Find a column by name
 **/

BurlRepoColumn getColumn(String name);



/**
 *      Compute an entry for this column to store in a row
 **/

void computeEntry(BurlRepoRow row,String isbn,BurlBibEntry entry,
      BurlUpdateMode updmode);


/**
 *      Add a new entry 
 **/

BurlRepoRow newRow();


/**
 *      Initialize an entry
 **/

void setInitialValues(BurlRepoRow row,String isbn);


/**
 *      Return the set of rows
 **/

default BurlRowIter getRows() 
{
   return getRows(null,false);
}


/**
 *      Return ordered set of rows
 **/

BurlRowIter getRows(BurlRepoColumn sort,boolean invert);


/**
 *      Return the set of rows matching a filter
 **/

BurlRowIter getRows(BurlFilter filter); 


/**
 *      Return the row associated with an id
 **/

BurlRepoRow getRowForId(Number id);


/**
 *      Return the row for the given original ISBN
 **/

BurlRepoRow getRowForIsbn(String isbn);


/**
 *      Return the row for the given LCCN
 **/

BurlRepoRow getRowForLccn(String lccn);

/**
 *      remove a row from the repository
 **/

void removeRow(Number id);


/**
 *      Setup the repository from file or database name
 **/

void openRepository();



/**
 *      Output the repository to a file or database
 **/

void outputRepository();
 

/**
 *      Close the repository
 **/

void closeRepository();


/** 
 *      Export the repository
 **/

boolean exportRepository(File otf,BurlExportFormat format,
      BurlRowIter rowiter); 


/**
 *      Print labels for a set of entries
 **/

boolean printLabels(File otf,List<Number> ids);
 
/**
 *      Import header line from CSV
 *
 *      Returns errors, fills column map
 *
 **/

String importCSVHeader(String hdr,Map<BurlRepoColumn,Integer> columnmap);


/**
 *      Import methods: add data from a CSV line
 **/

void importCSV(String csvline,BurlUpdateMode updmode,
      Map<BurlRepoColumn,Integer> columns);


/**
 *      Import data from a json line
 **/

void importJSON(JSONObject json,BurlUpdateMode updmode);



/**
 *      Delete the repository
 **/

void deleteRepository();


}       // end of interface BurlRepo




/* end of BurlRepo.java */

