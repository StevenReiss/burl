/********************************************************************************/
/*                                                                              */
/*              RepoColumn.java                                                 */
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


package edu.brown.cs.burl.repo;

import edu.brown.cs.burl.burl.BurlFieldData;
import edu.brown.cs.burl.burl.BurlRepoColumn;

class RepoColumn implements BurlRepoColumn, RepoConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String  column_name;
private int     column_number;
private BurlFieldData field_data;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

RepoColumn(String name,int no,BurlFieldData fd) 
{
   column_name = name;
   column_number = no;
   field_data = fd;
}



/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public int getNumber()                { return column_number; }

@Override public String getName()               { return column_name; }


@Override public String getGoogleData()
{  
   return field_data.getGoogleData(column_name);
}

@Override public String getMarcData()
{
   return field_data.getMarcData(column_name);
}

@Override public String getOpenLibData()
{
   return field_data.getOpenLibData(column_name);
}

@Override public BurlUserAccess getAccessLevel()
{
   return field_data.getAccessLevel(column_name);
}

@Override public String getFieldName()
{
   return field_data.getFieldName(column_name); 
}

@Override public String getDefault()
{ 
   return field_data.getDefault(column_name); 
}

@Override public boolean isCountField()
{
   return column_name.equals(field_data.getCountField());
}

@Override public BurlIsbnType getIsbnType()
{
   return field_data.getIsbnType(column_name);
}

@Override public boolean isMultiple()
{
   return field_data.isMultiple(column_name);
}

@Override public String getOtherIsbn()
{
   return field_data.getOtherIsbn(column_name);
}

@Override public String getMultipleSeparator() 
{
   return field_data.getMultiple();
}

@Override public String getMultiplePattern() 
{
   return field_data.getMultiplePattern();
}
 
@Override public String getLabel()
{
   return field_data.getLabel(column_name);
}


@Override public boolean isInternal()
{
   return field_data.isInternal(column_name);
}


}       // end of class RepoColumn




/* end of RepoColumn.java */

