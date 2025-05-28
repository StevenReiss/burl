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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

private static final Pattern LCC_ELEMENT = Pattern.compile(
      "(([A-Za-z]+)([0-9]+)(\\.[0-9]+)?)|([0-9]{4})");

private static final Pattern VOL_PATTERN = Pattern.compile(
      "v(ol)?(\\.)?\\s([-a-z0-9A-Z]+)"); 

private static Set<String> VOL_PREFIX = Set.of(
      "v","v.","vol","vol.","volume"
);

private static Pattern VOLNUM_PATTERN = Pattern.compile(
      "[0-9ivxl]+");

private static BurlRepoColumn burlid_column;


      
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
      burlid_column = new RepoColumn("burl_id",-1,field_data);
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

@Override
public abstract BurlRowIter getRows(BurlRepoColumn sort,boolean invert);

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


protected void updateIsbnLccnMap(String oldval,String val,Number idx)
{
   if (isbn_lccn_map == null) isbn_lccn_map = new HashMap<>();
   
   if (oldval != null && !oldval.equals(val)) {
      isbn_lccn_map.remove(oldval);
    }
   if (val != null) {
      isbn_lccn_map.put(val,idx);
    }
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
   
   RepoColumn rc = column_names.get(name);
   if (rc == null) {
      String n1 = field_data.getBaseName(name);
      if (n1 != null) rc = column_names.get(n1);
    }
   
   return rc;
}




@Override public RepoColumn getOriginalIsbnField()
{
   return getColumn(field_data.getOriginalIsbnField());
}

@Override public RepoColumn getLccnField()
{
   return getColumn(field_data.getLccnField());
}


@Override public RepoColumn getPrintLabelsField() 
{
   return getColumn(field_data.getPrintLabelsField());
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


protected RepoColumn getUpdateColumn(BurlRepoColumn rc)
{
   String fld = rc.getUpdateFieldName();
   if (fld == null) return null;
   return getColumn(fld);
}


/********************************************************************************/
/*                                                                              */
/*      Setup a new or updated entry                                            */
/*                                                                              */
/********************************************************************************/

@Override public void computeEntry(BurlRepoRow brr,String isbn, 
      BurlBibEntry bib,BurlUpdateMode updmode)
{
   String visbn = BurlUtil.getValidISBN(isbn);
   
   for (BurlRepoColumn brc : repo_columns) {
      if (brc.isHidden()) continue;
      String v = brr.getData(brc);
      BurlIsbnType isbntype = brc.getIsbnType();
      boolean islccn = brc.isLccnField();
      if (isbntype == BurlIsbnType.ORIGINAL) {
         if (visbn != null && (v == null || v.isEmpty())) {
            brr.setData(brc,visbn);
          }
         else visbn = v;
       }
      else if (bib != null) {
         switch (updmode) {
            case AUGMENT :
               if (isbntype == BurlIsbnType.NONE && !islccn) {
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
   if (isbn != null) lccn = null;
   
   for (BurlRepoColumn brc : getColumns()) {
      String dflt = brc.getDefault();
      if (brc.isOriginalIsbnField() && isbn != null) {
         brr.setData(brc,isbn);
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

@Override public boolean exportRepository(File otf,BurlExportFormat format,BurlRowIter rowiter)
{ 
   if (rowiter == null) rowiter = getRows();

   try (PrintWriter pw = new PrintWriter(otf)) { 
      switch (format) {
         case CSV :
            pw.println(getCSVHeader());
            for (BurlRepoRow brr : rowiter) { 
               pw.println(getCSVForRow(brr));
             }
            break;
         case JSON : 
            JSONArray rslt = new JSONArray();
            for (BurlRepoRow brr : rowiter) {
               JSONObject jo = getJsonForRow(brr);
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

private String getCSVHeader()
{
   String sep = field_data.getCSVSeparator();
   String quote = field_data.getCSVQuote();
   
   StringBuffer buf = new StringBuffer();
   for (BurlRepoColumn brc : getColumns()) {
      String lbl = brc.getName();
      if (!buf.isEmpty()) buf.append(field_data.getCSVSeparator()); 
      if (lbl.contains(sep)) {
         String v0 = lbl.replace(quote,quote+quote);
         lbl = quote + v0 + quote;
       }
      buf.append(lbl);  
    }
   if (!buf.isEmpty()) buf.append(field_data.getCSVSeparator()); 
   buf.append("burl_id");
   return buf.toString();
}



String getCSVForRow(BurlRepoRow rr)
{
   String sep = field_data.getCSVSeparator();
   String quote = field_data.getCSVQuote();
   
   StringBuffer buf = new StringBuffer();
   boolean sepneeded = false;
   for (BurlRepoColumn brc : getColumns()) {
      if (sepneeded) buf.append(sep);
      String v = rr.getData(brc);
      if (v == null) v = "";
      if (v.contains(sep) || v.contains("\n") || v.contains("\r") ||
            (!v.isEmpty() && Character.isDigit(v.charAt(0)))) {
         if (v.matches("[0-9]+")) v = "\t"+v;
         String v0 = v.replace(quote,quote+quote);
         if (!field_data.getMultiple().contains("\n")) {
            v0 = v0.replace("\n"," ");
          }
         v = quote + v0 + quote;
       }
      buf.append(v);
      sepneeded = true;
    }
   if (sepneeded) buf.append(sep);
   buf.append(rr.getRowId().toString());
   
   return buf.toString();
}


JSONObject getJsonForRow(BurlRepoRow rr)
{
   JSONObject result = new JSONObject();
   result.put("burl_id",rr.getRowId());
   for (BurlRepoColumn brc : getColumns()) {
      String v = rr.getData(brc);
      Object value = v;
      // possibly edit value -- make array (makes front end break)
      //     or set to number or boolean if relevant to field
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
         if (fld.equals("burl_id")) {
            columns.put(burlid_column,i);
            continue;
          }
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



@Override public void importCSV(String row,BurlUpdateMode updmode,
      Map<BurlRepoColumn,Integer> colmap)
{
   try (PushbackReader pr = new PushbackReader(new StringReader(row))) {
      List<String> items = splitCsv(pr);
      if (items == null) return; 
      
      BurlRepoColumn brc1 = getOriginalIsbnField();
      String isbn = getCSVEntry(brc1,items,colmap);
      BurlRepoColumn brc2 = getLccnField();
      String lccn = getCSVEntry(brc2,items,colmap);
      String idno = isbn;
      if (idno == null) idno = lccn; 
      
      BurlRepoRow dbrow = null;
      
      if (updmode != BurlUpdateMode.NEW) {
         String burlidstr = getCSVEntry(burlid_column,items,colmap);
         if (burlidstr != null && !burlidstr.isEmpty()) {
            try {
               Number burlid = Integer.getInteger(burlidstr);
               dbrow = getRowForId(burlid);
             }
            catch (NumberFormatException e) { }
          }
         if (dbrow != null) {
            String ib1 = dbrow.getData(brc1);
            String ib2 = dbrow.getData(brc2);
            int ct = 0;
            if (isbn != null && ib1 != null && !isbn.equals(ib1)) ++ct;
            if (lccn != null && ib2 != null && !lccn.equals(ib2)) ++ct;
            if (ct == 2) dbrow = null;
          }
         else if (isbn != null) { 
            if (dbrow == null) {
               dbrow = getRowForIsbn(isbn);
             }
          }
         else if (lccn != null) {
            dbrow = getRowForLccn(lccn);
          }
       }
      if (dbrow == null) {
         dbrow = newRow();
         setInitialValues(dbrow,idno);
       }
      else if (updmode == BurlUpdateMode.SKIP) {
         dbrow = null;
       }
      
      if (dbrow != null) {
         ImportCsvEntry cent = new ImportCsvEntry(items,colmap);
         computeEntry(dbrow,idno,cent,updmode);
       }
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
            if (quoted && buf.isEmpty() && ch == '\t') continue;
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

@Override public void importJSON(JSONObject row,BurlUpdateMode updmode)
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
   computeEntry(dbrow,idno,jent,updmode);
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

@Override public boolean printLabels(File otf,List<Number> ids,boolean reset)
{
   Element lbldata = field_data.getLabelData();
   if (lbldata == null) {
      IvyLog.logT("REPO","Missing label data");
      return false;
    }
   
   String cntstr = null;
   try (InputStream ins = getClass().getClassLoader().getResourceAsStream("labelout.rtf")) {
      cntstr = IvyFile.loadFile(ins);
    } 
   catch (IOException e) {
      IvyLog.logE("REPO","Problem reading label template",e);
      return false;
    }
   
   if (cntstr == null) {
      IvyLog.logE("REPO","No template read " + System.getProperty("java.class.path"));
      return false;
    }
   
   BurlRepoColumn print = getPrintLabelsField();
   
   StringBuffer cnts = new StringBuffer(cntstr);
   int cntidx = 0;
   boolean more = false;
   List<Number> done = new ArrayList<>();
   
   for (Number id : ids) {
      BurlRepoRow brr = getRowForId(id);
      if (brr == null) continue;
      String flg = brr.getData(print);
      if (flg != null && flg.equals("no")) continue;
       
      String code = findLabelData(lbldata,"CODE",brr);
      if (code == null) {
         IvyLog.logI("REPO","Missing code field: no label printed");
         continue;
       } 
      code = RepoColumn.fixLccCode(code); 
      String author = findLabelData(lbldata,"AUTHOR",brr);
      if (author == null) author = "";
      int idx = author.indexOf(field_data.getMultiple());
      if (idx >= 0) {
         author = author.substring(0,idx);
       }
      idx = author.indexOf(",");
      if (idx > 0) author = author.substring(0,idx);
      if (author.length() > 12) {
         author = author.substring(0,12);
       }
      author = author.replace("{","(");
      author = author.replace("}",")");
      author = author.replace("\\","/");
      author = fixUnicode(author);
      
      String year = findLabelData(lbldata,"YEAR",brr);
      if (year != null) {
         int idx1 = year.indexOf(" ");
         if (idx1 > 0) year = year.substring(0,idx1);
       }
      String volume = findLabelData(lbldata,"VOLUME",brr);
      String copy = findLabelData(lbldata,"COPY",brr);
      
      List<String> items = getLabelElements(code,year,volume,copy);
      
      cntidx = insertLabelText(cnts,cntidx,"nnnnnnnnnnnn",items.get(0));
      if (cntidx < 0) {
         more = false;
         break;
       }
      cntidx = insertLabelText(cnts,cntidx,"cccccccccccc",items.get(1));
      cntidx = insertLabelText(cnts,cntidx,"yyyy",items.get(2));
      cntidx = insertLabelText(cnts,cntidx,"vvvvvvvvvddd",items.get(3));
      cntidx = insertLabelText(cnts,cntidx,"aaaaaaaaaaaa",author);

      done.add(id);
    }
   
   
   if (!more) {
      for ( ; ; ) {
         cntidx = insertLabelText(cnts,cntidx,"nnnnnnnnnnnn","");
         if (cntidx < 0) break;
         cntidx = insertLabelText(cnts,cntidx,"cccccccccccc","");
         cntidx = insertLabelText(cnts,cntidx,"yyyy","");
         cntidx = insertLabelText(cnts,cntidx,"vvvvvvvvvddd","");
         cntidx = insertLabelText(cnts,cntidx,"aaaaaaaaaaaa","");
       }
    }
   
   try (FileWriter fw = new FileWriter(otf)) {
      fw.write(cnts.toString());
    }
   catch (IOException e) {
      IvyLog.logE("REPO","Problem writing temp label file",e);
      return false;
    }
   
   IvyLog.logD("REPO","Labels updated");
  
   if (reset) {
      for (Number id : done) {
         BurlRepoRow rr = getRowForId(id);
         rr.setData(print,"no");
       }
    }

   return more;
}



private String fixUnicode(String s) 
{
   if (s == null || s.isEmpty()) return s;
   StringBuffer buf = new StringBuffer();
   s.codePoints().forEach((ch) -> {
      if (ch > 127) {
         buf.append("\\u" + ch + "?");
       }
      else buf.append((char) ch);
    });
   
   if (buf.length() == s.length()) return s;
   
   return buf.toString();
}


List<String> getLabelElements(String lcc,String date,String vol,String copy)
{
   List<String> lcclets = getLccElements(lcc);
   List<String> rslt = new ArrayList<>();
   if (lcclets.isEmpty()) {
      rslt.add("");
    }
   else {
      rslt.add(lcclets.remove(0));
    }
   
   String year = "";
   String add = "";
   while (!lcclets.isEmpty()) {
      String l0 = lcclets.remove(0);
      if (Character.isDigit(l0.charAt(0))) {
         year = l0;
         break;
       }
      if (add.isEmpty()) add = l0;
      else {
         if (add.length() + 1 + l0.length() > 12) continue;
         else add = add + " " + l0;
       }
    }
   
   if (date != null && !date.isEmpty()) year = date;
   
   
   if (vol == null || vol.isEmpty()) {
      Matcher m = VOL_PATTERN.matcher(lcc);
      if (m.find()){
         vol = m.group(0);
       }
    }
   vol = fixVolume(vol);
   copy = fixCopy(copy);
   
   String x = "";
   if (vol == null && copy == null) x = "";
   else if (vol == null) x = "   " + copy;
   else if (copy == null) {
      x = " " + vol;
    }
   else if (vol.length() + 1 + copy.length() + 2 <= 12) {
      x = " " + vol + "  " + copy;
    }
   else {
      if (copy.length() > 2) copy = "c#";
      int len = 12 - copy.length() - 2 - 1;
      if (vol.length() > len) {
         vol = vol.substring(0,len);
       }
      x = " " + vol + "  " + copy;
    }
   
   rslt.add(add);
   rslt.add(year);
   rslt.add(x);
   
   return rslt;
}



private String fixVolume(String vol0)
{
   if (vol0 == null || vol0.isBlank()) return null;
   
   String vol = vol0.trim();
   
   // first remove any prefix (vol. ...)
   int idx1 = vol.indexOf(" ");
   if (idx1 > 0) {
      String pfx = vol.substring(0,idx1).toLowerCase();
      if (VOL_PREFIX.contains(pfx)) {
         vol = vol.substring(idx1+1).trim();
       }
    }
   
   // next determine if a number and add v. in front if so
   // if not, enclose in braces
   String main = vol.toLowerCase();
   int idx2 = main.indexOf(" ");
   if (idx2 > 0) {
      main = main.substring(0,idx2);
    }
   Matcher m = VOLNUM_PATTERN.matcher(main);
   if (m.find() && m.start(0) == 0) {
      vol = "v. " + vol;
    }
   else vol = "[" + vol + "]";
   
   return vol;
}


private String fixCopy(String copy0)
{
   if (copy0 == null|| copy0.isBlank()) return null;
   
   String copy = copy0;
   if (copy.matches("[0-9]+")) {
      Integer iv = Integer.parseInt(copy);
      if (iv <= 1) return null;
      else if (iv > 1 && iv < 10) {
         return "c" + iv;
       }
      else {
         return "c" + iv;
       }
    }
   
   return null;
}

List<String> getLccElements(String lcc)
{
   List<String> rslt = new ArrayList<>();
   if (lcc == null) return rslt;
   lcc = lcc.trim();
   
   Matcher m = LCC_ELEMENT.matcher(lcc);
   
   while (m.find()) {
      String x = m.group(0);
      String x5 = m.group(5);
      if (x5 != null && x5.isEmpty()) x5 = null;
      if (x.length() >= 12) x = x.substring(0,12);
      rslt.add(x);
      if (x5 != null) break; 
    }
   
   return rslt;
}


private String findLabelData(Element lbldata,String key,BurlRepoRow brr)
{
   String flds = IvyXml.getAttrString(lbldata,key);
   if (flds == null) return null;
   String [] alts = flds.split("\\,");
   for (String fldnm : alts) {
      BurlRepoColumn brc = getColumn(fldnm);
      if (brc == null) continue;
      String data = brr.getData(brc); 
      if (data != null && !data.isEmpty()) return data.trim();
    }
   
   return null;
}


private int insertLabelText(StringBuffer buf,int start,String pat,String rep)
{
   int idx = buf.indexOf(pat,start);
   if (idx < 0) return -1;
   
   int len = pat.length();
   buf.replace(idx,idx+len,rep);
   
   return idx + rep.length() - 1;
}



/********************************************************************************/
/*                                                                              */
/*      Handle filtered entries                                                 */
/*                                                                              */
/********************************************************************************/

@Override public BurlRowIter getRows(BurlFilter filter)
{
   if (filter == null) return getRows();
   
   return new FilterIter(getRows(filter.getSortField(),
         filter.invertSort()),filter);
}


protected static class RowIter implements BurlRowIter {
   
   private Iterator<BurlRepoRow> row_iter;
   private int row_count;
   private int row_index;
   
   protected RowIter(Collection<BurlRepoRow> data) {
      row_iter = data.iterator();
      row_count = data.size();
      row_index = 0;
    }
   
   @Override public Iterator<BurlRepoRow> iterator()    { return this; }
   
   @Override public boolean hasNext()            { return row_iter.hasNext(); }
   @Override public BurlRepoRow next() { 
      ++row_index;
      return row_iter.next(); 
    }
   
   @Override public int getRowCount()           { return row_count; }
   @Override public int getIndex()              { return row_index; }
   
}       // end of inner class BurlRowIter



  
@SuppressWarnings("unused")
private class FilterIterOld implements Iterable<BurlRepoRow>, BurlRowIter {
   
   private BurlFilter item_filter;
   private BurlRowIter base_iter;
   private BurlRepoRow next_item;
   private int  item_count;
   private int item_index;
   
   FilterIterOld(BurlRowIter base,BurlFilter filter) {
      item_filter = filter;
      base_iter = base;
      next_item = null;
      item_count = base.getRowCount();
      item_index = 0;
    }
   
   @Override public Iterator<BurlRepoRow> iterator()            { return this; }
   
   @Override public int getIndex() {
      return item_index;
    }
   
   @Override public int getRowCount() {    
      return item_count;
    }
   
   @Override public boolean hasNext() {
      if (next_item != null) return true;
      if (base_iter == null) return false;
      for ( ; ; ) {
         if (!base_iter.hasNext()) {
            return false;
          }
         BurlRepoRow brr = base_iter.next();
         if (!item_filter.matches(brr)) {
            --item_count;
            continue;
          }
         next_item = brr;
         return true;
       }
    }
   
   @Override public BurlRepoRow next() {
      if (base_iter == null) return null;
      if (next_item == null) {
         if (!hasNext()) return null;
       }
      
      BurlRepoRow item = next_item;
      next_item = null;
      ++item_index;
      
      return item;
    }
   
}       // end of FilterIter


private class FilterIter implements Iterable<BurlRepoRow>, BurlRowIter {
   
   private List<BurlRepoRow> filtered_rows;
   private Iterator<BurlRepoRow> row_iter;
   private int row_index;
   
   FilterIter(BurlRowIter base,BurlFilter filter) {
      filtered_rows = new ArrayList<>();
      while (base.hasNext()) {
         BurlRepoRow brr = base.next();
         if (filter.matches(brr)) {
            filtered_rows.add(brr);
          }
       }
      row_iter = filtered_rows.iterator();
      row_index = 0;
    }
   
   @Override public Iterator<BurlRepoRow> iterator() {
      return this;
    }
   
   @Override public int getRowCount() {
      return filtered_rows.size();
    }
   
   @Override public int getIndex() {
      return row_index;
    }
   
   @Override public boolean hasNext() {
      ++row_index;
      return row_iter.hasNext();
    }
   
   @Override public BurlRepoRow next() {
      return row_iter.next();
    }
   
}       // end of FilerIter





}       // end of class RepoBase




/* end of RepoBase.java */

