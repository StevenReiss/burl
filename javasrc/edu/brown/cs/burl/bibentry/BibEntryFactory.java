/********************************************************************************/
/*                                                                              */
/*              BibEntryFactory.java                                            */
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


package edu.brown.cs.burl.bibentry;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONObject;
import org.w3c.dom.Element;

import edu.brown.cs.burl.burl.BurlBibEntry;
import edu.brown.cs.burl.burl.BurlControl;
import edu.brown.cs.burl.burl.BurlUtil;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.xml.IvyXml;

public class BibEntryFactory implements BibEntryConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private HttpClient      http_client;

private static String LOC_API_BASE_URL = "https://www.loc.gov/search/?all=True&st=list&fo=json";
private static String GOOGLE_API_BASE_URL = "https://www.googleapis.com/books/v1/volumes";
// private static String PAPERPILE_URL = "https://api.paperpile.com/api/public/convert";
private static String OPEN_LIBRARY_URL =
   "https://openlibrary.org/search.json?fields=*,lccn,subject,subtitle";


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public BibEntryFactory(BurlControl bc)
{
   HttpURLConnection.setFollowRedirects(true);
   HttpClient.Builder bldr = HttpClient.newBuilder();
   bldr.followRedirects(HttpClient.Redirect.ALWAYS);
   http_client = bldr.build();
}



/********************************************************************************/
/*                                                                              */
/*      Find the bib entry for an isbn                                          */
/*                                                                              */
/********************************************************************************/

public BurlBibEntry findBibEntry(String idno)
{
   String isbn = BurlUtil.getValidISBN(idno);
   String altisbn = BurlUtil.computeAlternativeISBN(isbn);
   String lccn = BurlUtil.getValidLCCN(idno);
   if (lccn != null && lccn.equals(isbn)) lccn = null;
   
   BibEntryBase bibentry = null;
   if (bibentry == null&& isbn != null) {
      bibentry = congressSearch(isbn);
    }
   if (bibentry == null && lccn != null && isbn == null) {
      bibentry = congressSearch(lccn);
    }
   if (bibentry == null && isbn != null) {
      bibentry = openLibrarySearch(isbn);
    }
   if (bibentry == null && isbn != null) {
      bibentry = googleSearch(isbn);
    }
   if (bibentry == null && altisbn != null) {
      bibentry = googleSearch(altisbn);
    }
   if (bibentry == null && altisbn != null) {
      bibentry = openLibrarySearch(altisbn);
    }
   
   return bibentry;
}


/********************************************************************************/
/*                                                                              */
/*      Library of congress search                                              */
/*                                                                              */
/********************************************************************************/

private BibEntryBase
congressSearch(String isbn)
{
   if (isbn == null) return null; 
   
   BibEntryBase marc = null;
   BibEntryLOCResult search = searchForLOCInfo(isbn);
   if (search == null)  return null;
   if (search != null) {
      String idurl = search.getIdURL(isbn);
      if (idurl != null) {
         String marcurl = idurl + "/marcxml";
         marc = searchForMarcItemXml(isbn,marcurl);
       }
    }
   return marc;
}



private BibEntryLOCResult searchForLOCInfo(String isbn)
{
   HttpClient client = http_client;
   HttpRequest.Builder builder = createHttpBuilder(LOC_API_BASE_URL,"q",isbn);
   builder.GET();
   HttpRequest req = builder.build();
   
   try {
      for (int i = 0; ; ++i) {
         try {
            HttpResponse<String> resp = client.send(req,
                  HttpResponse.BodyHandlers.ofString());
            String body = resp.body();
            int vcode = resp.statusCode();
            if (vcode == 429 || vcode == 503 || vcode == 524) {
               IvyLog.logD("BIBENTRY","Waiting for LOC server " + vcode);
               waitFor(60);
               continue;
             }
            else if (vcode >= 400) {
               IvyLog.logE("BIBENTRY","Problem doing LOC search for " + req.uri() +
                     ": " + vcode + " " + body);
               break;
             }
            JSONObject rslt0 = new JSONObject(body);
            return new BibEntryLOCResult(rslt0); 
          }
         catch (InterruptedException e) { 
            IvyLog.logE("BIBENTRY","HTTP interrupted searching Library of Congress",e);
            waitFor(10);
            continue;
          }
         catch (IOException e) {
            if (i >= 2) {
               IvyLog.logE("BIBENTRY","HTTP Error searching Library of Congress",e); 
               break;
             }
            IvyLog.logD("BIBENTRY","HTTP Error searching Library of Congress"); 
            waitFor(10);
            continue;
          }
       }
    }
   finally {
      waitFor(10);
    }
   
   return null; 
}




/********************************************************************************/
/*                                                                              */
/*      Open Library search methods                                             */
/*                                                                              */
/********************************************************************************/

private BibEntryBase openLibrarySearch(String isbn)
{
   if (isbn == null) return null;
   
   BibEntryBase bibentry = null;
   BibEntryOpenLibItem olitm = searchInOpenLibrary(isbn);
   if (olitm == null) return null;
   for (int i = 0; ; ++i) {
      String idurl = olitm.getIdURL(isbn,i); 
      if (idurl == null) break;
      String marcurl = idurl + "/marcxml";
      bibentry = searchForMarcItemXml(isbn,marcurl);
      if (bibentry != null) break;
    }
   if (bibentry == null) {
      IvyLog.logD("BIBENTRY","Using openlib entry for " + isbn);
      bibentry = olitm.getBibEntry();  
    }
   
   return bibentry;
}



private BibEntryOpenLibItem searchInOpenLibrary(String isbn)
{
   HttpClient client = http_client;
   HttpRequest.Builder builder = createHttpBuilder(OPEN_LIBRARY_URL,"q",isbn);
   builder.GET();
   HttpRequest req = builder.build();
   for (int i = 0; i < 10; ++i) {
      try {
         HttpResponse<String> resp = client.send(req,
               HttpResponse.BodyHandlers.ofString());
         String body = resp.body();
         int vcode = resp.statusCode();
         if (vcode >= 400) {
            if (vcode == 503) {
               // retry on server unavailable
               IvyLog.logD("BIBENTRY","Problem doing OpenLib search " + vcode);
               waitFor(20);
               continue;
             }
            IvyLog.logE("BIBENTRY","Problem doing OpenLib search for " + req.uri() +
                  ": " + vcode + " " + body);
            return null;
          }
//    HttpHeaders hdrs = resp.headers();
//    List<String> loc = hdrs.allValues("Location");
         JSONObject rslt0 = new JSONObject(body);
         return new BibEntryOpenLibItem(rslt0);   
       }
      catch (InterruptedException e) { 
         IvyLog.logE("BIBENTRY","HTTP interrupted searching Library of Congress",e);
       }
      catch (IOException e) {
         IvyLog.logE("BIBENTRY","HTTP Error searching Library of Congress",e); 
       }
      finally {
         waitFor(1);
       }
      
      return null; 
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Google Books search methods                                             */
/*                                                                              */
/********************************************************************************/

private BibEntryBase googleSearch(String isbn)
{
   if (isbn == null) return null;
   
   BibEntryBase bibentry = null;
   BibEntryGoogleItem google = searchForGoogleItem(isbn,true);
   if (google == null) return null;
   String idurl = google.getIdURL(isbn);
   if (idurl != null) {
      String marcurl = idurl + "/marcxml";
      bibentry = searchForMarcItemXml(isbn,marcurl);
    }
   if (bibentry == null) {
      bibentry = google.getBibEntry();  
    }
   
   return bibentry;
}


/********************************************************************************/
/*                                                                              */
/*      Handle GOOGLE BOOKS queries                                             */
/*                                                                              */
/********************************************************************************/

private BibEntryGoogleItem searchForGoogleItem(String isbn,boolean pfx)
{
   String url = GOOGLE_API_BASE_URL;
   HttpClient client = http_client;
   String q;
   if (pfx) q = "isbn:" + isbn;
   else {
//    String q1 = computeAlternativeISBN(isbn);
      q = isbn;
//    if (q1 != null) q = q1;
    }
   
   HttpRequest.Builder builder = createHttpBuilder(url,"q",q);
   builder.GET();
   HttpRequest req = builder.build();
   try {
      HttpResponse<String> resp = client.send(req,
            HttpResponse.BodyHandlers.ofString());
      JSONObject rslt0 = new JSONObject(resp.body());
      return new BibEntryGoogleItem(rslt0);
    }
   catch (InterruptedException e) { 
      IvyLog.logE("BIBENTRY","HTTP interrupted searching googlebooks",e);
    }
   catch (IOException e) {
      IvyLog.logE("BIBENTRY","HTTP Error searching googlebooks",e); 
    }
   finally {
      waitFor(1);
    }
   
   return null; 
}



/********************************************************************************/
/*                                                                              */
/*      Get MARC information from LOC database                                  */
/*                                                                              */
/********************************************************************************/

private BibEntryBase searchForMarcItemXml(String isbn,String url)
{
   for ( ; ; ) {
      HttpClient client = http_client;
      HttpRequest.Builder builder = createHttpBuilder(url);
      builder.GET();
      HttpRequest req = builder.build();
      try {
         HttpResponse<String> resp = client.send(req,
               HttpResponse.BodyHandlers.ofString());
         String body = resp.body();
         int rcode = resp.statusCode();
         if (rcode == 429 || rcode == 503 || rcode == 524) {
            IvyLog.logD("BIBENTRY","Waiting for MARC server " + rcode + " " + url);
            waitFor(60);
            continue;
          }
         if (rcode >= 400) {
            IvyLog.logE("BIBENTRY","Problem with MARC request " + rcode + " " + 
                  url + " " + body);
            return null;
          }
         if (body.contains("<!DOCTYPE html>")) {
            if (body.contains("invalid LCCN")) return null;
            IvyLog.logD("BIBENTRY","Waiting for MARC server with html page");
            if (!body.contains("No Connections Available") &&
                  !body.contains("Permalink Error")) {
               IvyLog.logD("RESULT:\n" + body);
             }
            waitFor(110);
            continue;
          }
         Element xml = IvyXml.convertStringToXml(body);
         if (IvyXml.isElement(xml,"error")) {
            IvyLog.logE("BIBENTRY","Error in MARCxml entry: " + body);
            return null;
          }
         IvyLog.logD("BIBENTRY","Found marc entry for " + isbn + " " + url);
         return new BibEntryMarc(xml);
       } 
      catch (InterruptedException e) { 
         IvyLog.logE("BIBENTRY","HTTP interrupted getting marc XML",e);
       }
      catch (IOException e) {
         IvyLog.logE("BIBENTRY","HTTP error getting marc XML",e);
       }
      finally {
         waitFor(10);
       }
      break;
    }
   
   return null; 
}



/********************************************************************************/
/*                                                                              */
/*      Generic HTTP methods                                                    */
/*                                                                              */
/********************************************************************************/

private HttpRequest.Builder createHttpBuilder(String url,String... query)
{
   String sep = "?";
   if (url.contains("?")) sep = "&";
   if (query.length > 0) {
      for (int i = 0; i < query.length-1; i += 2) {
         String k = query[i];
         String v = query[i+1];
         v = BurlUtil.encodeURIComponent(v);
         url += sep + k + "=" + v;
         sep = "&";
       }
    }
   
   URI uri = null;
   try {
      uri = new URI(url);
    }
   catch (URISyntaxException e) {
      IvyLog.logE("BIBENTRY","Problem with URL",e);
    }
   if (uri == null) return null;
   
   HttpRequest.Builder bldr = HttpRequest.newBuilder();
   bldr.uri(uri);
   bldr.header("Content-Type","application/json; charset=utf-8");
   bldr.header("Accept","application/json,application/xml");    
   bldr.headers("User-Agent","BooksLibrary");
   
   return bldr;
}



private void waitFor(int sec)
{
   try {
      Thread.sleep(sec * 1000);
    }
   catch (InterruptedException e) { }
}



}       // end of class BibEntryFactory




/* end of BibEntryFactory.java */

