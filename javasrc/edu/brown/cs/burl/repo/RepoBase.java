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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.PushbackReader;
import java.io.StringReader;
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
import org.w3c.dom.Element;

import edu.brown.cs.burl.burl.BurlBibEntry;
import edu.brown.cs.burl.burl.BurlControl;
import edu.brown.cs.burl.burl.BurlFieldData;
import edu.brown.cs.burl.burl.BurlFilter;
import edu.brown.cs.burl.burl.BurlLibrary;
import edu.brown.cs.burl.burl.BurlRepo;
import edu.brown.cs.burl.burl.BurlRepoColumn;
import edu.brown.cs.burl.burl.BurlRepoRow;
import edu.brown.cs.burl.burl.BurlUtil;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.xml.IvyXml;

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
private Map<String,Number> isbn_lccn_map;

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
   
   isbn_lccn_map = null;
   
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

@Override public abstract void removeRow(Number id);




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
/*      Row lookup methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public BurlRepoRow getRowForIsbn(String isbn)
{
   if (isbn_lccn_map == null) return null;
   Number id = isbn_lccn_map.get(isbn);
   if (id == null) return null;
   return getRowForId(id);
}


@Override public BurlRepoRow getRowForLccn(String lccn)
{
   if (isbn_lccn_map == null) return null;
   Number id = isbn_lccn_map.get(lccn);
   if (id == null) return null;
   return getRowForId(id);
}


protected void noteIsbnChange(String oldval,String val,Number idx)
{
   if (isbn_lccn_map == null) isbn_lccn_map = new HashMap<>();
   
   if (oldval != null && !oldval.equals(val)) {
      isbn_lccn_map.remove(oldval);
    }
   if (val != null) {
      isbn_lccn_map.put(val,idx);
    }
}

protected void noteLccnChange(String oldval,String val,Number idx)
{
   noteIsbnChange(oldval,val,idx);
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

@Override public RepoColumn getOriginalIsbnField()
{
   return getColumn(field_data.getOriginalIsbnField());
}

@Override public RepoColumn getLccnField()
{
   return getColumn(field_data.getLccnField());
}


@Override public RepoColumn getLabeledField()
{
   return getColumn(field_data.getLabeledField());
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

@Override public void computeEntry(BurlRepoRow brr,String isbn, 
      BurlBibEntry bib,BurlUpdateMode updmode,boolean count)
{
   if (updmode == BurlUpdateMode.SKIP) return;
   
   String visbn = BurlUtil.getValidISBN(isbn);
   
   for (BurlRepoColumn brc : repo_columns) {
      String v = brr.getData(brc);
      BurlIsbnType isbntype = brc.getIsbnType();
      if (isbntype == BurlIsbnType.ORIGINAL) {
         if (visbn != null && (v == null || v.isEmpty())) {
            brr.setData(brc,visbn);
          }
         else visbn = v;
       }
      else if (brc.isCountField()) {
         if (v == null || v.isEmpty()) brr.setData(brc,"1");
         else if (count) {
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
               if (nv == null || nv.isEmpty()) continue;
               break;
          }
         brr.setData(brc,nv);
       }
    }
   
   if (visbn == null) {
      BurlRepoColumn orig = getOriginalIsbnField();
      if (orig != null) {
         visbn = findIsbn(brr);
         brr.setData(orig,visbn);
       }
    }
}


private String findIsbn(BurlRepoRow brr)
{
   for (BurlRepoColumn brc : repo_columns) {
      BurlIsbnType isbntype = brc.getIsbnType();
      switch (isbntype) {
         case ALL :
         case ISBN10 :
         case ISBN13 :
             break;
         case ORIGINAL :
         case NONE :
            continue;
       }
      
      String v = brr.getData(brc);
      String [] split = v.split(field_data.getMultiplePattern());
      if (split.length > 0) {
         return split[0].trim();
       }
    }
   
   return null;
}


protected String fixIsbnField(BurlRepoColumn brc,String isbn,String oldv,String newv)
{
   BurlIsbnType isbntype = brc.getIsbnType();
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



private static void addIsbn(String isbn,int len,Set<String> rslt)
{
   if (isbn == null) return;
   if (BurlUtil.getValidISBN(isbn) == null) return; 
 
   if ((len == 0 || len == 10) && isbn.length() == 9) {
      isbn = "0" + isbn;
    }
   
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


@Override public void setInitialValues(BurlRepoRow brr,String idno)
{
   if (brr == null) return;
   
   String isbn = BurlUtil.getValidISBN(idno);
   String lccn = BurlUtil.getValidLCCN(idno);
   
   for (BurlRepoColumn brc : getColumns()) {
      String dflt = brc.getDefault();
      if (brc.isOriginalIsbnField() && isbn != null) {
         brr.setData(brc,isbn);
       }
      else if (brc.isCountField()) {
         brr.setData(brc,"0");
       }
      else if (brc.isLccnField() && lccn != null) {
         brr.setData(brc,lccn);
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
/*      Import methods : CSV                                                    */
/*                                                                              */
/********************************************************************************/
 
@Override public String importCSVHeader(String hdr,Map<BurlRepoColumn,Integer> columns)
{
   if (columns == null) return "No column map"; 
   
   String error = null;
   
   try (PushbackReader pr = new PushbackReader(new StringReader(hdr))) {
      List<String> flds = splitCsv(pr);
      for (int i = 0; i < flds.size(); ++i) {
         String fld = flds.get(i);
         BurlRepoColumn brc = getColumn(fld);
         if (brc == null) {
            if (error == null) error = fld;
            else error += ", " + fld;
          }
         else {
            columns.put(brc,i);
          }
       }
    }
   catch (IOException e) {      
      IvyLog.logE("REPO","Problem reading CSV header row");
    }
   
   if (error == null) return null;
   
   return "Unknown columns: " + error; 

}



@Override public void importCSV(String row,BurlUpdateMode updmode,boolean count,
      Map<BurlRepoColumn,Integer> colmap)
{
   try (PushbackReader pr = new PushbackReader(new StringReader(row))) {
      List<String> items = splitCsv(pr);
      if (items == null) return;
      String isbn = null;
      String lccn = null;
      BurlRepoColumn brc1 = getOriginalIsbnField();
      isbn = getCSVEntry(brc1,items,colmap);
      BurlRepoColumn brc2 = getLccnField();
      lccn = getCSVEntry(brc2,items,colmap);
      String idno = isbn;
      if (idno == null) idno = lccn;
      if (idno == null) return;
      BurlRepoRow dbrow = null;
      if (isbn != null) { 
         dbrow = getRowForIsbn(isbn);
       }
      else if (lccn != null) {
         dbrow = getRowForLccn(lccn);
       }
      if (dbrow == null) {
         dbrow = newRow();
         setInitialValues(dbrow,idno);
       }
      ImportCsvEntry cent = new ImportCsvEntry(items,colmap);
      computeEntry(dbrow,idno,cent,updmode,count);
      // if isbn is known then get the original row given id
      // else create a new row
      // then call compute entry with an import bib entry that just returns the map result
    }
   catch (IOException e) {   
      IvyLog.logE("REPO","Problem reading CSV row");
    }
}


private String getCSVEntry(BurlRepoColumn brc,List<String> data,Map<BurlRepoColumn,Integer> colmap)
{
   if (brc == null) return null;
   
   Integer idx = colmap.get(brc);
   if (idx == null) return null;
   int id = idx.intValue();
   if (id < 0 || id >= data.size()) return null;
   
   return data.get(id);
}


private class ImportCsvEntry implements BurlBibEntry {
   
   private Map<BurlRepoColumn,Integer> column_map;
   private List<String> data_values;
   
   ImportCsvEntry(List<String> data,Map<BurlRepoColumn,Integer> colmap) {
      column_map = colmap;
      data_values = data;
    }
   
   @Override public String computeEntry(BurlRepoColumn brc) {
      return getCSVEntry(brc,data_values,column_map);  
    }
   
}       // end of inner class ImportCsvEntry



String parseCsvHeader(String hdr,Map<Integer,BurlRepoColumn> columns)
{
   return null;
}



protected List<String> splitCsv(PushbackReader fr)
{
   List<String> items = new ArrayList<>();
   
   boolean quoted = false;
   StringBuffer buf = new StringBuffer();
   char quote = getCSVQuote().charAt(0);
   char sep = getCSVSeparator().charAt(0);
   
   try {
      for ( ; ; ) {
         int ch = fr.read();
         if (ch < 0) return null;
         if (ch != '\r' && ch != '\n') {
            fr.unread(ch);
            break;
          }
       }
      
      for ( ; ; ) {
         int ch = fr.read();
         if (ch < 0) break;
         if (ch == quote) {
            if (quoted) {
               int nextch = fr.read();
               if (nextch == quote) {
                  buf.append((char) ch);
                  continue;
                }
               else if (nextch == sep || nextch == '\n' || nextch == '\r') {
                  quoted = false;
                }
               fr.unread(nextch);
             }
            else if (buf.length() == 0) {
               quoted = true;
             }
            else {
               buf.append((char) ch);
             }
          }
         else if (ch == sep && !quoted) {
            items.add(buf.toString());
            buf.setLength(0);
          }
         else if (ch == '\r' || ch == '\n') {
            break;
          }
         else {
            buf.append((char) ch); 
          }
       }
    }
   catch (IOException e) {
      IvyLog.logE("REPO","Problem reading CSV input file",e);
      System.exit(1);
    }
   
   items.add(buf.toString());
   
   return items;
}




/********************************************************************************/
/*                                                                              */
/*      Import methods: JSON                                                    */
/*                                                                              */
/********************************************************************************/

@Override public void importJSON(JSONObject row,BurlUpdateMode updmode,boolean count)
{
   String isbn = null;
   String lccn = null;
   BurlRepoColumn brc1 = getOriginalIsbnField();
   isbn = getJsonEntry(brc1,row);
   BurlRepoColumn brc2 = getLccnField();
   lccn = getJsonEntry(brc2,row);
   String idno = isbn;
   if (idno == null) idno = lccn;
   if (idno == null) return;
   BurlRepoRow dbrow = null;
   if (isbn != null) { 
      dbrow = getRowForIsbn(isbn);
    }
   else if (lccn != null) {
      dbrow = getRowForLccn(lccn);
    }
   if (dbrow == null) {
      dbrow = newRow();
      setInitialValues(dbrow,idno);
    }
   ImportJsonEntry jent = new ImportJsonEntry(row);
   computeEntry(dbrow,idno,jent,updmode,count);
}


private String getJsonEntry(BurlRepoColumn brc,JSONObject data)
{
   if (brc == null) return null;
   return data.optString(brc.getName(),null);
}


private class ImportJsonEntry implements BurlBibEntry {
   
   private JSONObject row_data;
   
   ImportJsonEntry(JSONObject row) {
      row_data = row;
    }
   
   @Override public String computeEntry(BurlRepoColumn brc) {
      return getJsonEntry(brc,row_data);
    }
   
}       // end of inner class ImportJsonEntry



/********************************************************************************/
/*                                                                              */
/*      Print labels                                                            */
/*                                                                              */
/********************************************************************************/

@Override public boolean printLabels(File otf,List<Number> ids)
{
   Element lbldata = field_data.getLabelData();
   if (lbldata == null) {
      IvyLog.logE("Missing label data");
      return false;
    }
   
   String cntstr = null;
   try (InputStream ins = getClass().getClassLoader().getResourceAsStream("labelout.rtf")) {
      cntstr = IvyFile.loadFile(ins);
    } 
   catch (IOException e) {
      IvyLog.logE("Problem reading label template",e);
      return false;
    }
   
   BurlRepoColumn lbld = getLabeledField();
   
   StringBuffer cnts = new StringBuffer(cntstr);
   int cntidx = 0;
   boolean more = false;
   List<Number> done = new ArrayList<>();
   
   for (Number id : ids) {
      BurlRepoRow brr = getRowForId(id);
      if (brr == null) continue;
      String flg = brr.getData(lbld);
      if (flg != null && flg.equals("yes")) continue;
       
      String code = findLabelData(lbldata,"CODE",brr);
      String author = findLabelData(lbldata,"AUTHOR",brr);
      if (code == null) {
         IvyLog.logI("REPO","Missing code field: no label printed");
         continue;
       }
      
      if (author == null) author = "";
      int idx = author.indexOf(field_data.getMultiple());
      if (idx >= 0) {
         author = author.substring(0,idx);
       }
      idx = author.indexOf(",");
      if (idx > 0) author = author.substring(0,idx);
      author = author.trim();
      idx = author.indexOf(" ");
      if (idx > 0) author = author.substring(idx+1).trim();
      if (author.length() > 3) author = author.substring(0,3);
      
      String code1 = code;
      String code2 = "";
      int len = code.length();
      if (len > 12) {
         int idx1 = code.lastIndexOf(" ",12);
         int idx2 = code.lastIndexOf(".",12);
         int idx3 = code.lastIndexOf("-",12);
         int sidx = 0;
         if (idx1 > 0 && idx1 > sidx) sidx = idx1;
         if (idx2 > 0 && idx2 > sidx) sidx = idx2;
         if (idx3 > 0 && idx3 > sidx) sidx = idx3;
         code1 = code.substring(0,sidx);
         code2 = code.substring(sidx+1).trim();
         if (code2.length() > 12) code2 = code2.substring(0,12);
       }
      
      cntidx = insertLabelText(cnts,cntidx,"aaaaaaaaaaaa",code1);
      if (cntidx < 0) {
         more = true;
         break;
       }
      cntidx = insertLabelText(cnts,cntidx,"bbbbbbbbbbbb",code2);
      cntidx = insertLabelText(cnts,cntidx,"CCC",author);
      done.add(id);
    }
   
   if (!more) {
      for ( ; ; ) {
         cntidx = insertLabelText(cnts,cntidx,"aaaaaaaaaaaa","");
         if (cntidx < 0) break;
         cntidx = insertLabelText(cnts,cntidx,"bbbbbbbbbbbb","");
         cntidx = insertLabelText(cnts,cntidx,"CCC","");
       }
    }
   
   try (FileWriter fw = new FileWriter(otf)) {
      fw.write(cnts.toString());
    }
   catch (IOException e) {
      IvyLog.logE("Problem writing temp label file",e);
      return false;
    }
   
  
   for (Number id : done) {
      BurlRepoRow rr = getRowForId(id);
      rr.setData(lbld,"yes");
    }

   return more;
}


private String findLabelData(Element lbldata,String key,BurlRepoRow brr)
{
   String flds = IvyXml.getAttrString(lbldata,key);
   String [] alts = flds.split("\\,");
   for (String fldnm : alts) {
      BurlRepoColumn brc = getColumn(fldnm);
      if (brc == null) continue;
      String data = brr.getData(brc); 
      if (data != null && !data.isEmpty()) return data;
    }
   
   return null;
}


private int insertLabelText(StringBuffer buf,int start,String pat,String rep)
{
   int idx = buf.indexOf(pat,start);
   if (idx < 0) return -1;
   
   int len = pat.length();
   buf.replace(start,start+len,rep);
   
   return start + rep.length() - 1;
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

