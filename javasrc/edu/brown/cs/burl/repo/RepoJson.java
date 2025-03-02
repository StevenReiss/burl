/********************************************************************************/
/*                                                                              */
/*              RepoJson.java                                                   */
/*                                                                              */
/*      Repository stored as JSON                                               */
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.brown.cs.burl.burl.BurlControl;
import edu.brown.cs.burl.burl.BurlLibrary;
import edu.brown.cs.burl.burl.BurlRepoColumn;
import edu.brown.cs.burl.burl.BurlRepoRow;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;

class RepoJson extends RepoBase
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

RepoJson(BurlControl bc,BurlLibrary lib)
{
   super(bc,lib); 
   repo_data = null;
   File f1 = bc.getDataDirectory();
   repo_file = new File(f1,lib.getNameKey() + ".json");
   max_id = 0;
}



/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
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
   exportRepository(repo_file,BurlExportFormat.CSV,null,false);
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
   
   try {
      String cnts = IvyFile.loadFile(repo_file);
      JSONObject jo = new JSONObject(cnts);
      JSONArray jarr = jo.optJSONArray("data");
      if (jarr != null) {
         for (int i = 0; i < jarr.length(); ++i) {
            JSONObject datao = jarr.getJSONObject(i);
            Number oldidx = datao.optNumber("burl_id");
            int idx = 0;
            if (oldidx != null) idx = oldidx.intValue();
            else idx = ++max_id;
            BurlRepoRow brr = newRow(idx);
            for (String fldnm : JSONObject.getNames(datao)) {
               BurlRepoColumn brc = getColumn(fldnm);
               if (brc == null) continue;
               Object v = jo.get(fldnm);
               if (v == null) continue;
               if (v instanceof JSONArray) {
                  JSONArray vals = (JSONArray) v;
                  List<String> elts = new ArrayList<>();
                  for (int j = 0; j < vals.length(); ++j) {
                     elts.add(vals.get(j).toString());
                   }
                  String vr = String.join(RepoBase.getMultiple(),elts); 
                  brr.setData(brc,vr);
                }
               else {
                  brr.setData(brc,v.toString());
                }
             }
          }
       }
    }
   catch (IOException e) {
      IvyLog.logE("REPO","Problem reading JSON input file",e);
      System.exit(1);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Row methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public BurlRepoRow newRow()
{
   return newRow(++max_id);
}


private BurlRepoRow newRow(int idx)
{
   JsonRow brr = new JsonRow(idx);
   repo_data.put(idx,brr);
   if (idx > max_id) max_id = idx;
   
   return brr;
}


@Override public Iterable<BurlRepoRow> getRows()
{
   return repo_data.values();
}


@Override public BurlRepoRow getRowForId(Number id)
{
   if (id == null) return null;
   int idx = id.intValue();
   if (idx < 0 || idx >= repo_data.size()) return null;
   
   return repo_data.get(idx);
}



/********************************************************************************/
/*                                                                              */
/*      Json row                                                                */
/*                                                                              */
/********************************************************************************/

private class JsonRow extends RepoRowBase {
   
   private Map<BurlRepoColumn,String> row_data; 
   private int row_index;
   
   JsonRow(int idx) {
      super(RepoJson.this);
      row_data = new HashMap<>();
      row_index = idx;
    }
   
   @Override public String getData(BurlRepoColumn rc) {
      return row_data.get(rc);
    }
   
   
   @Override public void setData(BurlRepoColumn rc,String v) {
      if (rc.isOriginalIsbnField()) {
         // Might want to do this for all ISBN fields
         String ov = row_data.get(rc);
         noteIsbnChange(ov,v,row_index);
       }
      else if (rc.isLccnField()) {
         String ov = row_data.get(rc);
         noteLccnChange(ov,v,row_index);
       }
      
      v = rc.fixFieldValue(v);
      if (v == null || v.isEmpty()) row_data.remove(rc);
      else row_data.put(rc,v);
    }
   
   @Override public Number getRowId() {
      return row_index;
    }
   
}       // end of inner class CsvRow






}       // end of class RepoJson




/* end of RepoJson.java */

