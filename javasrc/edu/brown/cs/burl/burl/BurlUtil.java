/********************************************************************************/
/*                                                                              */
/*              BurlUtil.java                                                   */
/*                                                                              */
/*      Static Utility methods for BURL home library manager                    */
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.brown.cs.ivy.bower.BowerMailer;
import edu.brown.cs.ivy.file.IvyLog;

public abstract class BurlUtil implements BurlConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private storage                                                         */
/*                                                                              */
/********************************************************************************/

private static File base_directory = null;
private static Random rand_gen = new Random();
private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
private static Pattern LCCN_PATTERN = Pattern.compile("[a-z]{0,2}[0-9]{8,10}");



/********************************************************************************/
/*                                                                              */
/*      Base directory methods                                                  */
/*                                                                              */
/********************************************************************************/

public static File getBaseDirectory()
{
   if (base_directory == null) {
      base_directory = findBaseDirectory();
    }
   
   return base_directory;
}


private static File findBaseDirectory()
{
   File f1 = new File(System.getProperty("user.dir"));
   for (File f2 = f1; f2 != null; f2 = f2.getParentFile()) {
      if (isBaseDirectory(f2)) return f2;
    }
   File f3 = new File(System.getProperty("user.home"));
   if (isBaseDirectory(f3)) return f3;
   
   File fc = new File("/vol");
   File fd = new File(fc,"iot");
   if (isBaseDirectory(fd)) return fd;
   
   File fa = new File("/pro");
   File fb = new File(fa,"iot");
   if (isBaseDirectory(fb)) return fb;
   
   return null;
}


private static boolean isBaseDirectory(File dir)
{
   File f2 = new File(dir,"secret");
   if (!f2.exists()) return false;
   
   File f3 = new File(f2,"Database.props");
   File f4 = new File(f2,"burl.props");
   if (f3.exists() && f4.exists()) return true;
   
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Utility methods                                                         */
/*                                                                              */
/********************************************************************************/

public static String randomString(int len)
{
   StringBuffer buf = new StringBuffer();
   int cln = RANDOM_CHARS.length();
   for (int i = 0; i < len; ++i) {
      int idx = rand_gen.nextInt(cln);
      buf.append(RANDOM_CHARS.charAt(idx));
    }
   
   return buf.toString();
}


public static String encodeURIComponent(String v)
{
   try {
      return URLEncoder.encode(v,"UTF-8");
    }
   catch (UnsupportedEncodingException e) {
      IvyLog.logE("BURL","Problem with URI encoding",e);
    }
   
   return v;
}


public static String decodeURIComponent(String v)
{
   try {
      return URLDecoder.decode(v,"UTF-8");
    }
   catch (UnsupportedEncodingException e) {
      IvyLog.logE("BURL","Problem with URI encoding",e);
    }
   
   return v;
}


public static String secureHash(String s)
{
   try {
      MessageDigest md = MessageDigest.getInstance("SHA-512");
      byte [] dvl = md.digest(s.getBytes());
      String rslt = Base64.getEncoder().encodeToString(dvl);
      return rslt;
    }
   catch (Exception e) {
      throw new Error("Problem with sha-512 encoding of " + s);
    }
}


public static String getAsString(JSONObject json,String key)
{
   Object o = json.get(key);
   if (o == null) return null;
   return o.toString();
}



/********************************************************************************/
/*                                                                              */
/*      Json methods                                                            */
/*                                                                              */
/********************************************************************************/

public static JSONObject buildJson(Object... val)
{
      JSONObject rslt = new JSONObject();
      
      if (val.length > 1) {
         for (int i = 0; i+1 < val.length; i += 2) {
            String key = val[i].toString();
            Object v = val[i+1];
            rslt.put(key,v);
          }
       }
      
      return rslt;
}



public static JSONArray buildJsonArray(Object... val)
{
   JSONArray rslt = new JSONArray();
   for (Object v : val) {
      rslt.put(v);
    }
   
   return rslt;
}
   



/********************************************************************************/
/*                                                                              */
/*      ISBN methods                                                            */
/*                                                                              */
/********************************************************************************/

public static String getValidISBN(String s0)
{
   String s = s0;
   
   if (s.length() == 9) s0 = "0" + s;
   
   if (s.length() != 10 && s.length() != 13) return null;
   
   if (!s.matches("[0-9]+X?")) return null;
   
   String s1 = computeAlternativeISBN(s);
   if (s1 != null) {
      String s2 = computeAlternativeISBN(s1);
      if (s2 != null && !s2.equals(s)) {
         IvyLog.logW("ISBN check digit");
         return null;
       }
    }
   
   return s;
}


public static String computeAlternativeISBN(String isbn)
{
   if (isbn.length() == 9) isbn = "0"+isbn;
   
   if (isbn.length() == 10) {
      int sum = 9*1 + 7*3 + 8*1;
      for (int i = 0; i < 9; ++i) {
         int cv = Character.digit(isbn.charAt(i),10);
         if (i % 2 == 0) cv *= 3;
         sum += cv;
       }
      sum = sum % 10;
      if (sum != 0) sum = 10-sum;
      String rslt = "978" + isbn.substring(0,9) + sum;
      return rslt;
    }
   else if (isbn.length() == 13 && isbn.startsWith("978")) {
      String isbn1 = isbn.substring(3,12);
      int sum = 0;
      for (int i = 0; i < isbn1.length(); ++i) {
         int cv = isbn1.charAt(i) - '0';
         cv *= (10-i);
         sum += cv;
       }
      sum = sum % 11;
      char dig = '?';
      if (sum == 0) dig = '0';
      else if (sum == 1) dig = 'X';
      else {
         sum = 11 - sum;
         dig = Character.forDigit(sum,10);
       }
      return isbn1+dig;
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      LCCN methods                                                            */
/*                                                                              */
/********************************************************************************/

public static String getValidLCCN(String lccn)
{
   if (lccn == null) return null;
   
   String lccn1 = lccn.replace(" ","");
   int idx = lccn1.indexOf("-");
   if (idx >= 0) {
      String pfx = lccn1.substring(0,idx);
      String sfx = lccn1.substring(idx+1);
      while (sfx.length() < 6) {
         sfx = "0" + sfx;
       }
      lccn1 = pfx + sfx;
    }
   
   Matcher matcher = LCCN_PATTERN.matcher(lccn1);
   if (matcher.matches()) return lccn1;
   
   return null;
}


/********************************************************************************/
/*                                                                              */
/*      email methods                                                           */
/*                                                                              */
/********************************************************************************/

public static boolean sendEmail(String sendto,String subj,String body)
{
   if (sendto == null || subj == null && body == null) return false;
   
   File f2 = new File(base_directory,"secret");
   File f4 = new File(f2,"burl.props");
   Properties props = new Properties();
   try (FileInputStream fis = new FileInputStream(f4)) {
      props.loadFromXML(fis);
    }
   catch (IOException e) { }
   
   BowerMailer mi = new BowerMailer(sendto,subj,body);
   mi.setSender(props.getProperty("email.from"),
         props.getProperty("email.user"),
         props.getProperty("email.password"));
   mi.setReplyTo(props.getProperty("email.replyto"));
   boolean fg = mi.send();
   
   return fg;
}




}       // end of class BurlUtil




/* end of BurlUtil.java */

