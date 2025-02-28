/********************************************************************************/
/*                                                                              */
/*              BurlException.java                                              */
/*                                                                              */
/*      Exception for use in BURL                                               */
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


package edu.brown.cs.burl.burl;



public class BurlException extends Exception
{



/********************************************************************************/
/*                                                                              */
/*      Private storage                                                         */
/*                                                                              */
/********************************************************************************/

private static final long serialVersionUID = 1;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public BurlException(String msg)
{
   super(msg);
}


public BurlException(String msg,Throwable cause)
{
   super(msg,cause);
}



}       // end of class BurlException




/* end of BurlException.java */

