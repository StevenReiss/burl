/********************************************************************************/
/*                                                                              */
/*              RepoDatabase.java                                               */
/*                                                                              */
/*      Repository implemented as part of the burl database                     */
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

import java.util.Iterator;

import org.json.JSONObject;

import edu.brown.cs.burl.burl.BurlControl;
import edu.brown.cs.burl.burl.BurlLibrary;
import edu.brown.cs.burl.burl.BurlRepoColumn;
import edu.brown.cs.burl.burl.BurlRepoRow;
import edu.brown.cs.burl.burl.BurlStorage;

class RepoDatabase extends RepoBase implements RepoConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BurlStorage     burl_store;

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

RepoDatabase(BurlControl bc,BurlLibrary lib)
{
   super(bc,lib);
   
   burl_store = bc.getStorage();
}



/********************************************************************************/
/*                                                                              */
/*      Open/Close methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public void openRepository()
{
   burl_store.createDataTable(this);  
   // method body goes here
}




@Override public void closeRepository()
{
   // nothing needed
}




@Override public void outputRepository()
{
   // nothing needed
}



@Override public void deleteRepository()
{
   burl_store.removeDataTable(this);
}


/********************************************************************************/
/*                                                                              */
/*      Row methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override public BurlRepoRow newRow()
{
   Number id = burl_store.addDataRow(this);
   
   BurlRepoRow brr = new DatabaseRow(id);
   
   return brr;
}


@Override public BurlRepoRow getRowForId(Number id)
{
   if (id == null) return null;
   
   return new DatabaseRow(id);
}


@Override public Iterable<BurlRepoRow> getRows()
{
   Iterator<JSONObject> objiter = burl_store.getAllDataRows(this);
   
   return new RowIterator(objiter);
}


private class RowIterator implements Iterable<BurlRepoRow>, Iterator<BurlRepoRow> {
   
   private Iterator<JSONObject> object_iter;
   
   RowIterator(Iterator<JSONObject> oiter) {
      object_iter = oiter;
    }
   
   @Override public Iterator<BurlRepoRow> iterator()            { return this; }
   
   @Override public boolean hasNext() {
      if (object_iter == null) return false;
      return object_iter.hasNext();
    }
   
   @Override public BurlRepoRow next() {
      if (object_iter == null) return null;
      JSONObject jo = object_iter.next();
      if (jo == null) return null;
      return new DatabaseRow(jo);
    }
   
}       // end of inner class RowIterator



/********************************************************************************/
/*                                                                              */
/*      Reprenstation of a row                                                  */
/*                                                                              */
/********************************************************************************/

private class DatabaseRow extends RepoRowBase {
   
   private JSONObject row_data; 
   private Number row_index;
   
   DatabaseRow(Number index) {
      super(RepoDatabase.this);
      row_data = null;
      row_index = index;
    }
   
   DatabaseRow(JSONObject data) {
      super(RepoDatabase.this);
      row_data = data;
      row_index = data.getNumber("burl_id");
    }
   
   
   @Override public String getData(BurlRepoColumn rc) {
      if (row_data == null) {
         row_data = burl_store.getDataRow(RepoDatabase.this,row_index);
       }
      if (row_data == null) return null;
      return row_data.optString(rc.getFieldName());
    }
   
   @Override public void setData(BurlRepoColumn rc,String v) {
      burl_store.updateDataRow(RepoDatabase.this,row_index,rc.getFieldName(),v);
      if (row_data != null) {
         row_data.put(rc.getFieldName(),v);
       }
    }
   
   @Override public Number getRowId() {
      return row_index;
    }
   
}



}       // end of class RepoDatabase




/* end of RepoDatabase.java */

