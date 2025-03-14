/********************************************************************************/
/*                                                                              */
/*              RepoRowBase.java                                                */
/*                                                                              */
/*      Base class for row implementations                                      */
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

import java.util.LinkedHashSet;
import java.util.Set;

import org.json.JSONObject;

import edu.brown.cs.burl.burl.BurlRepoColumn;
import edu.brown.cs.burl.burl.BurlRepoRow;
import edu.brown.cs.burl.burl.BurlUtil;

abstract class RepoRowBase implements BurlRepoRow, RepoConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private RepoBase        for_repo;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

RepoRowBase(RepoBase repo)
{
   for_repo = repo;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public RepoBase getRepository()
{
   return for_repo;
}

@Override public abstract String getData(BurlRepoColumn brc);

@Override public abstract void setData(BurlRepoColumn brc,String v);

@Override public void setData(String cnm,String v)
{
   BurlRepoColumn brc = for_repo.getColumn(cnm);
   if (brc != null) {
      setData(brc,v);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Compute column fix ups                                                  */
/*                                                                              */
/********************************************************************************/

protected void updateIsbnField(String oldval,String val)
{
   if (valueMatch(oldval,val)) return;
   
   for (BurlRepoColumn brc : for_repo.getColumns()) {
      BurlIsbnType isbntype = brc.getIsbnType();
      switch (isbntype) {
         case NONE :
         case ORIGINAL :
            continue;
         case ISBN10 :
            updateSingleIsbnField(oldval,val,10,brc);
            break;
         case ISBN13 :
            updateSingleIsbnField(oldval,val,13,brc);
         case ALL :
            updateAllIsbnField(oldval,val,brc);
            break;
       }
    }
}



private void updateSingleIsbnField(String oldval,String newval,int len,BurlRepoColumn brc)
{
   String oldi = getIsbnValue(oldval,len);
   String newi = getIsbnValue(newval,len);
   String prior = getData(brc);
   if (valueMatch(prior,oldi)) {
      setData(brc,newi);
    }
}



private void updateAllIsbnField(String oldval,String newval,BurlRepoColumn brc)
{
   Set<String> vals = new LinkedHashSet<>();
   String prior = getData(brc);
   if (prior != null) {
      String [] valarr = prior.split(RepoBase.getMultiplePattern());
      for (String s : valarr) {
         if (!s.isEmpty()) vals.add(s);
       }
    }
   
   String isbna = BurlUtil.getValidISBN(oldval);
   String isbnb = BurlUtil.computeAlternativeISBN(isbna);
   String newa = BurlUtil.getValidISBN(newval);
   String newb = BurlUtil.computeAlternativeISBN(newval);
   if (isbna != null) vals.remove(isbna);
   if (isbnb != null) vals.remove(isbnb);
   if (newa != null) vals.add(newa);
   if (newb != null) vals.add(newb);
   
   String nv = String.join(RepoBase.getMultiple(),vals);
   setData(brc,nv);
}


private String getIsbnValue(String isbn,int len)
{
   if (isbn == null || isbn.isEmpty()) return null;
   if (isbn.length() == len) return isbn;
   String isbn1 = BurlUtil.computeAlternativeISBN(isbn);
   if (isbn1 != null && isbn1.length() == len) return isbn1;
   
   return null;
}


private boolean valueMatch(String v1,String v2)
{
   if (v1 == null && v2 == null) return true;
   if (v1 == null && v2.isEmpty()) return true;
   if (v2 == null && v1.isEmpty()) return true;
   if (v1 == null || v2 == null) return false;
   return v1.equals(v2);
}



/********************************************************************************/
/*                                                                              */
/*      Conversion or export methods                                            */
/*                                                                              */
/********************************************************************************/

@Override public String toCSV()
{
   return for_repo.getCSVForRow(this);
}


@Override public JSONObject toJson() 
{
   return for_repo.getJsonForRow(this); 
}





}       // end of class RepoRowBase




/* end of RepoRowBase.java */

