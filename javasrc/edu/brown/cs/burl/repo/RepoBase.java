/********************************************************************************/
/*                                                                              */
/*              RepoBase.java                                                   */
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.brown.cs.burl.burl.BurlBibEntry;
import edu.brown.cs.burl.burl.BurlControl;
import edu.brown.cs.burl.burl.BurlFieldData;
import edu.brown.cs.burl.burl.BurlFilter;
import edu.brown.cs.burl.burl.BurlLibrary;
import edu.brown.cs.burl.burl.BurlRepo;
import edu.brown.cs.burl.burl.BurlRepoColumn;
import edu.brown.cs.burl.burl.BurlRepoRow;
import edu.brown.cs.burl.burl.BurlUtil;
import edu.brown.cs.ivy.file.IvyLog;

abstract class RepoBase implements BurlRepo, RepoConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BurlControl     burl_main;
private BurlLibrary     for_library;
private Set<BurlRepoColumn> repo_columns;
private Map<String,RepoColumn> column_names;

private static BurlFieldData field_data;

private static final String [] EMPTY_STRINGS = new String [0];


      
/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

RepoBase(BurlControl bc,BurlLibrary lib)
{
   burl_main = bc;
   for_library = lib;
   if (field_data == null) {
      field_data = new BurlFieldData();
    }
   repo_columns = new TreeSet<>();
   column_names = new HashMap<>();
   
   for (String fieldname : field_data.getAllFields()) {
      addHeader(fieldname);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Abstract methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public abstract BurlRepoRow newRow(); 

@Override public abstract void openRepository();

@Override public abstract void outputRepository();

@Override public abstract void closeRepository();

@Override public abstract void deleteRepository();

@Override public abstract Iterable<BurlRepoRow> getRows();

@Override public abstract BurlRepoRow getRowForId(Number id);



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public Collection<BurlRepoColumn> getColumns()
{
   return repo_columns;
}


@Override public BurlControl getBurl()
{
   return burl_main;
}


static String getCSVSeparator()
{
   return field_data.getCSVSeparator();
}


static String getCSVQuote()
{
   return field_data.getCSVQuote();
}

@Override public String getName()        
{
   return for_library.getName();
}


@Override public String getNameKey()
{
   return for_library.getNameKey();
}

BurlLibrary getLibrary()
{
   return for_library;
}


/********************************************************************************/
/*                                                                              */
/*      Column methods                                                          */
/*                                                                              */
/********************************************************************************/

private void addHeader(String name)
{
   RepoColumn rc = new RepoColumn(name,repo_columns.size(),field_data);
   if (column_names.containsKey(name)) {
      IvyLog.logE("REPO","Duplicate column name " + name);
    }
   column_names.put(name,rc);
   String fnm = rc.getFieldName();
   column_names.put(fnm,rc);
   String lbl = rc.getLabel();
   column_names.put(lbl,rc);
   repo_columns.add(rc);
}



@Override public RepoColumn getColumn(String name)
{
   if (name == null) return null;
   
   return column_names.get(name);
}


@Override public RepoColumn getCountField()
{
   return getColumn(field_data.getCountField());
}


@Override public Collection<BurlRepoColumn> getIsbnFields()
{
   List<BurlRepoColumn> rslt = new ArrayList<>();
   for (BurlRepoColumn rc : repo_columns) {
      BurlIsbnType isbntype = rc.getIsbnType();
      if (isbntype != null && isbntype != BurlIsbnType.NONE) {
         rslt.add(rc);
       }
    }
   
   return rslt;
}


protected static String getMultiple()
{
   return field_data.getMultiple();
}


protected static String getMultiplePattern()
{
   return field_data.getMultiplePattern();
}


/********************************************************************************/
/*                                                                              */
/*      Setup a new or updated entry                                            */
/*                                                                              */
/********************************************************************************/

@Override public void computeEntry(BurlRepoRow brr,String isbn,BurlBibEntry bib)
{
   BurlUpdateMode updmode = burl_main.getUpdateMode(); 
   RepoColumn countcol = getCountField();
   
   for (BurlRepoColumn brc : repo_columns) {
      String v = brr.getData(brc);
      BurlIsbnType isbntype = field_data.getIsbnType(brc.getName());
      if (isbntype == BurlIsbnType.ORIGINAL) {
         if (v == null || v.isEmpty()) brr.setData(brc,isbn);
       }
      else if (brc == countcol) {
         if (v == null || v.isEmpty()) brr.setData(brc,"1");
         else if (updmode == BurlUpdateMode.COUNT) {
            try {
               int ct = Integer.parseInt(v);
               ct += 1;
               brr.setData(brc,String.valueOf(ct));
             }
            catch (NumberFormatException e) { }
          }
       }
      else if (bib != null) {
         switch (updmode) {
            case AUGMENT :
            case COUNT :
               if (isbntype == BurlIsbnType.NONE) {
                  if (v != null && !v.isEmpty()) continue; 
                }
               break;
          }
         String nv = bib.computeEntry(brc);   
         nv = fixIsbnField(brc,isbn,v,nv);
         
         switch (updmode) {
            case REPLACE :
            case AUGMENT :
            case COUNT :
               if (nv == null || nv.isEmpty()) continue;
               break;
          }
         brr.setData(brc,nv);
       }
    }
}


protected String fixIsbnField(BurlRepoColumn brc,String isbn,String oldv,String newv)
{
   BurlIsbnType isbntype = field_data.getIsbnType(brc.getName());
   int len = 0;
   
   switch (isbntype) {
      case NONE :
         return newv;
      case ORIGINAL :
         return oldv;
      case ISBN10 : 
         len = 10;
         break;
      case ISBN13:
         len = 13;
         break;
      default :
         break;
    }
   
   String [] isbns1 = EMPTY_STRINGS;
   if (oldv != null) {
      isbns1 = oldv.split(field_data.getMultiplePattern());  
    }
   String [] isbns2 = EMPTY_STRINGS;
   if (newv != null) {
      isbns2 = newv.split(field_data.getMultiplePattern());
    }
   
   Set<String> all = new LinkedHashSet<>();
   if (isbn != null) addIsbn(isbn,len,all);
   
   for (String s : isbns1) {
      addIsbn(s,len,all);
    }
   for (String s : isbns2) {
      addIsbn(s,len,all);
    }
   
   if (all.isEmpty()) return null;
   
   if (!field_data.isMultiple(brc.getName())) {
      for (String s : all) {
         return s;
       }
    }
   
   return String.join(field_data.getMultiple(),all);
}



private void addIsbn(String isbn,int len,Set<String> rslt)
{
   if (isbn == null) return;
   if (!BurlUtil.isValidISBN(isbn)) return; 
   if (len == 0 || isbn.length() == len) {
      rslt.add(isbn);
    }
   String isbn1 = BurlUtil.computeAlternativeISBN(isbn);
   if (isbn1 != null) {
      if (len == 0 || isbn1.length() == len) {
         rslt.add(isbn1);
       }
    }
}


@Override public void setInitialValues(BurlRepoRow brr,String isbn)
{
   if (brr == null) return;
   
   for (BurlRepoColumn brc : getColumns()) {
      BurlIsbnType isbntype = brc.getIsbnType();
      String dflt = brc.getDefault();
      if (isbntype == BurlIsbnType.ORIGINAL) {
         brr.setData(brc,isbn);
       }
      else if (brc.isCountField()) {
         brr.setData(brc,"0");
       }
      else if (dflt != null && !dflt.equals("NULL")) {
         brr.setData(brc,dflt);
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Export methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public boolean exportRepository(File otf,BurlExportFormat format,boolean external)
{
   try (PrintWriter pw = new PrintWriter(otf)) { 
      switch (format) {
         case CSV :
            pw.println(getCSVHeader(external));
            for (BurlRepoRow brr : getRows()) {
               pw.println(getCSVForRow(brr,external));
             }
            break;
         case JSON :
            JSONArray rslt = new JSONArray();
            for (BurlRepoRow brr : getRows()) {
               JSONObject jo = getJsonForRow(brr,external);
               rslt.put(jo);
             }
            JSONObject jobj = BurlUtil.buildJson("name",getName(),
                  "nameKey",getNameKey(),
                  "data",rslt);
            pw.println(jobj.toString(2));
            break;
         case LABELS :
            // TODO: need to export labels here
            break;
       }
      return true;
    }
   catch (IOException e) {
      IvyLog.logE("REPO","Problem writing export file " + otf,e);
    }
   
   return false;
}


/********************************************************************************/
/*                                                                              */
/*      Format entries                                                          */
/*                                                                              */
/********************************************************************************/

private String getCSVHeader(boolean external)
{
   String sep = field_data.getCSVSeparator();
   String quote = field_data.getCSVQuote();
   
   StringBuffer buf = new StringBuffer();
   for (BurlRepoColumn brc : getColumns()) {
      String lbl = null;
      if (external) {
         if (brc.isInternal()) continue;
         else lbl = brc.getLabel();
       }
      else {
         lbl = brc.getName();
       }
      if (!buf.isEmpty()) buf.append(field_data.getCSVSeparator()); 
      if (lbl.contains(sep)) {
         String v0 = lbl.replace(quote,quote+quote);
         lbl = quote + v0 + quote;
       }
      buf.append(lbl);  
    }
   return buf.toString();
}



String getCSVForRow(BurlRepoRow rr,boolean external)
{
   String sep = field_data.getCSVSeparator();
   String quote = field_data.getCSVQuote();
   
   StringBuffer buf = new StringBuffer();
   boolean sepneeded = false;
   for (BurlRepoColumn brc : getColumns()) {
      if (external && brc.isInternal()) continue;
      if (sepneeded) buf.append(sep);
      String v = rr.getData(brc);
      if (v == null) v = "";
      if (brc != getCountField()) {
         if (v.contains(sep) || v.contains("\n") || v.contains("\r") ||
               (!v.isEmpty() && Character.isDigit(v.charAt(0)))) {
            if (v.matches("[0-9]+")) v = "\t"+v;
            String v0 = v.replace(quote,quote+quote);
            v = quote + v0 + quote;
          }
       }
      buf.append(v);
      sepneeded = true;
    }
   
   return buf.toString();
}


JSONObject getJsonForRow(BurlRepoRow rr,boolean external)
{
   JSONObject result = new JSONObject();
   result.put("burl_id",rr.getRowId());
   for (BurlRepoColumn brc : getColumns()) {
      if (external && brc.isInternal()) continue;
       String v = rr.getData(brc);
       Object value = v;
       if (brc == getCountField()) {
          try {
             value = Integer.valueOf(v);
           }
          catch (NumberFormatException e) {
             value = Integer.valueOf(0);
           }
        }
       else if (brc.isMultiple()) {
          String [] items = v.split(field_data.getMultiplePattern());
          JSONArray arr = new JSONArray();
          arr.putAll(items);
          value = arr;
        }
       if (external) {
          value = BurlUtil.buildJson("label",brc.getLabel(),
                "value",value);
        }
       result.put(brc.getName(),value);
    }
   
   return result;
}



/********************************************************************************/
/*                                                                              */
/*      Handle filtered entries                                                 */
/*                                                                              */
/********************************************************************************/

@Override public Iterable<BurlRepoRow> getRows(BurlFilter filter)
{
   if (filter == null) return getRows();
   
   return new FilterIter(getRows().iterator(),filter);
}


private class FilterIter implements Iterable<BurlRepoRow>, Iterator<BurlRepoRow> {
   
   private BurlFilter item_filter;
   private Iterator<BurlRepoRow> base_iter;
   private BurlRepoRow next_item;
   
   FilterIter(Iterator<BurlRepoRow> base,BurlFilter filter) {
      item_filter = filter;
      base_iter = base;
      next_item = null;
    }
   
   @Override public Iterator<BurlRepoRow> iterator()            { return this; }
   
   @Override public boolean hasNext() {
      if (next_item != null) return true;
      for ( ; ; ) {
         if (!base_iter.hasNext()) {
            base_iter = null;
            return false;
          }
         BurlRepoRow brr = base_iter.next();
         if (!item_filter.matches(brr)) continue;
         next_item = brr;
         return true;
       }
    }
   
   @Override public BurlRepoRow next() {
      if (next_item == null) {
         if (!hasNext()) return null;
       }
      
      BurlRepoRow item = next_item;
      next_item = null;
      
      return item;
    }
   
}       // end of FilterIter



}       // end of class RepoBase




/* end of RepoBase.java */

