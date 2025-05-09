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
   exportRepository(repo_file,BurlExportFormat.CSV,null);
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
      IvyLog.logE("REPO","Problem reading CSV input file",e);
      System.exit(1);
    }
} 






/********************************************************************************/
/*                                                                              */
/*      Row methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public BurlRowIter getRows(BurlRepoColumn sort,boolean invert)
{   
   // might need to sort here
   
   return new RowIter(repo_data.values());   
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


@Override public void removeRow(Number id)
{
   if (id == null) return;
   
   repo_data.remove(id);
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
      v = rc.fixFieldValue(v); 
      
      if (rc.isOriginalIsbnField() || rc.isLccnField()) {
         // Might want to do this for all ISBN fields
         String ov = row_data.get(rc);
         updateIsbnField(ov,v);
         updateIsbnLccnMap(ov,v,row_index);
       } 
      else if (rc.isLccnField()) {
         String ov = row_data.get(rc); 
         updateIsbnLccnMap(ov,v,row_index);
       }
      
      if (v == null || v.isEmpty()) row_data.remove(rc);
      else {
         row_data.put(rc,v);
       }
      
      RepoColumn upd = getUpdateColumn(rc);  
      if (upd != null) setData(upd,v);
   }
   
   @Override public Number getRowId() {
      return row_index;
    }
   
}       // end of inner class CsvRow




}       // end of class RepoCsv




/* end of RepoCsv.java */

