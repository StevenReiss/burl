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

import org.json.JSONObject;

import edu.brown.cs.burl.burl.BurlRepoColumn;
import edu.brown.cs.burl.burl.BurlRepoRow;

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

