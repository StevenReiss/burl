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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

private static final Pattern PRE_ZERO = Pattern.compile("[A-Za-z](0+)[1-9]");
private static final Pattern POST_ZERO = Pattern.compile("\\.[0-9]*[1-9](0+)[ .,A-Z]");
private static final Pattern POST_ZERO_A = Pattern.compile("(\\.0+)[ .,A-Z]");
private static final Pattern POST_ZERO_B = Pattern.compile("\\.[0-9]*[1-9](0+)$");
private static final Pattern POST_ZERO_C = Pattern.compile("(\\.0+)$");

private static final Pattern LCC_NUMBER = Pattern.compile("[0-9]+(\\.[0-9]+)?");
private static final Pattern LCC_SPACE = Pattern.compile("(\\s*\\.)[A-Z]");

private static final int LCC_SORT_COUNT = 6;
private static final String LCC_SORT_PREFIX = "0000000000";





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


@Override public boolean isHidden()
{
   return field_data.isHidden(column_name);
}


@Override public boolean isGroupEdit()
{ 
   return field_data.isGroupEdit(column_name);
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


@Override public BurlSortType getSortType()
{
   return field_data.getSortType(column_name); 
}


@Override public String getUpdateFieldName()
{
   return field_data.getUpdateField(column_name);
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
      case NONE :
         break;
      case DEFAULT :
         if (val == null || val.isEmpty()) {
            String dflt = getDefault();
            if (dflt != null && !dflt.isEmpty() && !dflt.equals("NULL")) nval = dflt;
          }
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
      case LCC_SORT :
         nval = fixLccSort(val);
         break;
      case LAST_FIRST :
         nval = BurlUtil.fixFirstLast(val); 
         break;
      case YES_NO :
         nval = fixYesNo(val);
         break; 
      case DATE :
         nval = fixDate(val); 
         break;
    }
   
   if (nval != null) return nval;
   
   return val;
}



static String fixLccCode(String code0)
{
   if (code0 == null) return null;
   String code = code0.trim();
   
   if (!code.contains("00")) return code;
   
   code = code.replace("-","");

   for ( ; ; ) {
      Matcher m = PRE_ZERO.matcher(code);
      if (!m.find()) break;
      int start = m.start(1);
      int end = m.end(1);
      code = code.substring(0,start) + code.substring(end);
    }
   for ( ; ; ) {
      Matcher m = POST_ZERO.matcher(code);
      if (!m.find()) break;
      int start = m.start(1);
      int end = m.end(1);
      code = code.substring(0,start) + code.substring(end);
    }
   
   for ( ; ; ) {
      Matcher m = POST_ZERO_A.matcher(code);
      if (!m.find()) break;
      int start = m.start(1);
      int end = m.end(1);
      code = code.substring(0,start) + code.substring(end);
    }
   
   for ( ; ; ) {
      Matcher m = POST_ZERO_B.matcher(code);
      if (!m.find()) break;
      int start = m.start(1);
      code = code.substring(0,start);
    }
   
   for ( ; ; ) {
      Matcher m = POST_ZERO_C.matcher(code);
      if (!m.find()) break;
      int start = m.start(1);
      code = code.substring(0,start);
    }
   
   return code;
}


static String fixLccSort(String code0)
{
   String code = code0;
   if (code == null) {
      return null;
    }
   code = fixLccCode(code);
   int start = 0;
   boolean havelc = false;
   for ( ; ; ) {
      Matcher m = LCC_NUMBER.matcher(code);
      if (!m.find(start)) break;
      int p0 = m.start();
      int p1 = m.end();
      String n = m.group();
      int idx = n.indexOf(".");
      if (idx < 0) {
         idx = n.length();
       }
      int ct = LCC_SORT_COUNT - idx;
      String pfx = LCC_SORT_PREFIX.substring(0,ct);
      n = pfx + n;
      String t = "";
      if (!havelc) {
         idx = n.indexOf(".");
         int fln = 6;
         String t1 = ".";
         if (idx > 0) {
            fln = LCC_SORT_COUNT - (n.length() - idx - 1);
            t1 = "";
          }
         t = t1 + LCC_SORT_PREFIX.substring(0,fln);
         ct += t.length();
         havelc = true;
       }
      code = code.substring(0,p0) + n + t + code.substring(p1);
      start = p1 + ct;
    }
   
   for ( ; ; ) {
      Matcher m = LCC_SPACE.matcher(code);
      if (!m.find()) break;
      int p0 = m.start(1);
      int p1 = m.end(1);
      code = code.substring(0,p0) + " " + code.substring(p1);
    }
   
   System.err.println("CONVERT " + code0 + " => " + code);
   
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


private String fixDate(String val)
{
   if (val == null || val.isBlank()) return "";
   
   Pattern pat = Pattern.compile("[12][0-9]{3}");
   Matcher m = pat.matcher(val);
   if (!m.find()) return "";
   
   return m.group(0);
}


/********************************************************************************/
/*                                                                              */
/*      Ouptut Methods                                                          */
/*                                                                              */
/********************************************************************************/


@Override public String toString()
{
   return column_name;
}


}       // end of class RepoColumn




/* end of RepoColumn.java */

