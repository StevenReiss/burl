/********************************************************************************/
/*                                                                              */
/*              RepoCsv.java                                                    */
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import edu.brown.cs.burl.burl.BurlControl;
import edu.brown.cs.burl.burl.BurlLibrary;
import edu.brown.cs.burl.burl.BurlRepoColumn;
import edu.brown.cs.burl.burl.BurlRepoRow;
import edu.brown.cs.ivy.file.IvyLog;

class RepoCsv extends RepoBase  
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<Integer,BurlRepoRow> repo_data;
private File                    repo_file;
private int                     max_id;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

RepoCsv(BurlControl bc,BurlLibrary lib)
{
   super(bc,lib); 
   repo_data = null;
   File f1 = bc.getDataDirectory();
   repo_file = new File(f1,lib.getNameKey() + ".csv");
   max_id = 0;
}


/********************************************************************************/
/*                                                                              */
/*      Open/Close methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public void openRepository()
{
   if (repo_data == null) {
      repo_data = new TreeMap<>(); 
      inputRepoFromFile();
    }
}


@Override public void closeRepository()
{
   outputRepository();
   repo_data = null;
}


@Override public void outputRepository()
{
   // might want to handle backups
   exportRepository(repo_file,BurlExportFormat.CSV,false);
}


@Override public void deleteRepository()
{
   repo_file.delete();
   repo_data = null;
}
 


private void inputRepoFromFile()
{
   if (repo_file == null) return;
   if (!repo_file.exists()) return;
   
   List<BurlRepoColumn> cols = new ArrayList<>();
   
   boolean havehdr = false;
   try (BufferedReader fr = new BufferedReader(new FileReader(repo_file))) {
      PushbackReader pr = new PushbackReader(fr);
      for ( ; ; ) {
         List<String> cnts = splitCsv(pr); 
         if (cnts == null) break;
         if (!havehdr) {
            for (String h : cnts) {
               BurlRepoColumn brc = getColumn(h);
               cols.add(brc);
             }
            havehdr = true;
            
          }
         else {
            BurlRepoRow brr = newRow();
            for (int i = 0; i < cnts.size(); ++i) {
               BurlRepoColumn brc = cols.get(i);
               if (brc != null) {
                  String v = cnts.get(i);
                  if (v != null) v = v.trim();
                  brr.setData(brc,v);
                }
             }
          }
       }
    }
   catch (IOException e) {
      IvyLog.logE("BOOKS","Problem reading CSV input file",e);
      System.exit(1);
    }
} 


private List<String> splitCsv(PushbackReader fr)
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
      IvyLog.logE("BOOKS","Problem reading CSV input file",e);
      System.exit(1);
    }
   
   items.add(buf.toString());
   
   return items;
}



/********************************************************************************/
/*                                                                              */
/*      Row methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public Iterable<BurlRepoRow> getRows()
{   
   return repo_data.values();
}



@Override public CsvRow newRow()
{
   int idx = ++max_id;
   CsvRow brr = new CsvRow(idx);
   repo_data.put(idx,brr);
   
   return brr;
}


@Override public BurlRepoRow getRowForId(Number id)
{
   if (id == null) return null;
   
   return repo_data.get(id);
}



/********************************************************************************/
/*                                                                              */
/*      CSV row                                                                 */
/*                                                                              */
/********************************************************************************/

private class CsvRow extends RepoRowBase {

   private Map<BurlRepoColumn,String> row_data; 
   private int row_index;
    
   CsvRow(int idx) {
      super(RepoCsv.this); 
      row_data = new HashMap<>();
      row_index = idx;
    }
   
   @Override public String getData(BurlRepoColumn rc) {
      return row_data.get(rc);
    }
   
   
   @Override public void setData(BurlRepoColumn rc,String v) {
      if (v == null || v.isEmpty()) row_data.remove(rc);
      else row_data.put(rc,v);
    }
   
   @Override public Number getRowId() {
      return row_index;
    }
   
}       // end of inner class CsvRow




}       // end of class RepoCsv




/* end of RepoCsv.java */

