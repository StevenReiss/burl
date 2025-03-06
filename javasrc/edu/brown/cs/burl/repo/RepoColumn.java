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
import edu.brown.cs.burl.burl.BurlUtil;

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

@Override public boolean isOriginalIsbnField() 
{
   return column_name.equals(field_data.getOriginalIsbnField());
}

@Override public boolean isLccnField() 
{
   return column_name.equals(field_data.getLccnField());
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




@Override public BurlFixType getFixType()
{
   return field_data.getFixType(column_name);
}


/********************************************************************************/
/*                                                                              */
/*      Field value fix methods                                                 */
/*                                                                              */
/********************************************************************************/

@Override public String fixFieldValue(String val) 
{
   BurlFixType fixtype = getFixType();
   String nval = val;
   
   switch (fixtype) {
      default :
      case NONE :
         break;
      case LCCN :
         nval = BurlUtil.getValidLCCN(val);
         break;
      case ISBN : 
         nval = BurlUtil.getValidISBN(val);
         break;
      case LCC_CODE :
         nval = fixLccCode(val);
         break;
      case LAST_FIRST :
         nval = BurlUtil.fixFirstLast(val); 
         break;
      case YES_NO :
         nval = fixYesNo(val);
         break; 
    }
   
   if (nval != null) return nval;
   
   return val;
}



private String fixLccCode(String code)
{
   if (code == null) return null;
   
   if (!code.endsWith("0")) return code;
   code = code.replace("-","");
   int idx2 = -1;
   for (int i = 0; i < code.length(); ++i) {
      if (code.charAt(i) == '0') {
         if (idx2 < 0) idx2 = i;
       }
      else {
         if (idx2 >= 0) {
            String ncode = code.substring(0,idx2);
            ncode += code.substring(i);
            code = ncode;
            int len = (i-idx2);
            i -= len;
          }
         idx2 = -1;
       }
    }
   
   return code;
}


private String fixYesNo(String val)
{
   if (val == null || val.isBlank()) return "no";
   
   val = val.trim();
   char c = val.charAt(0);
   if ("yY1tT".indexOf(c) >= 0) return "yes";
   
   return "no";
}



}       // end of class RepoColumn




/* end of RepoColumn.java */

