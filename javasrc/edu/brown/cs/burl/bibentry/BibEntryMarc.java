/********************************************************************************/
/*                                                                              */
/*              BibEntryMarc.java                                               */
/*                                                                              */
/*      Bibliograhic entry based on LOC MARC data                               */
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import edu.brown.cs.burl.burl.BurlRepoColumn;
import edu.brown.cs.ivy.file.Pair;
import edu.brown.cs.ivy.xml.IvyXml;

class BibEntryMarc extends BibEntryBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Element         marc_xml;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BibEntryMarc(Element xml) 
{
   marc_xml = xml;
}


/********************************************************************************/
/*                                                                              */
/*      Code to setup a column from MARC data                                   */
/*                                                                              */
/********************************************************************************/

@Override 
public String computeEntry(BurlRepoColumn brc)
{
   String key = brc.getMarcData();
   if (key == null) return null;
   
   List<String> items = new ArrayList<>();
   StringTokenizer tok = new StringTokenizer(key,"+|",true);
   while (tok.hasMoreTokens()) {
      String code = tok.nextToken();
      if (code.equals("|") && !items.isEmpty()) break;
      if (code.equals("+")) continue;
      if (code.length() < 4) continue;
      
      String tag = code.substring(0,3);
      String addall = "";              
      Map<String,String> additems = new HashMap<>();
      
      int idx = 3;
      for ( ; idx < code.length(); ++idx) {
         char chk = code.charAt(idx);
         if (Character.isJavaIdentifierPart(chk)) break;
         if (addall == null) addall = String.valueOf(chk);
         else addall = addall + String.valueOf(chk);
       }
      
      Set<String> subs = new HashSet<>();
      String additem = null;
      for (int i = idx; i < code.length(); ++i) {
         char chk = code.charAt(i);
         if (Character.isJavaIdentifierPart(chk)) {
            String what = String.valueOf(chk);
            if (additem != null) additems.put(what,additem);
            subs.add(what);
            additem = null;
          }
         else {
            if (additem == null) additem = String.valueOf(chk);
            else additem = additem + String.valueOf(chk);
          }
       }
      List<List<Pair<String,String>>> vals = getOrderedSubfields(tag,subs);
      for (List<Pair<String,String>> ents : vals) {
         StringBuffer buf = new StringBuffer();
         for (Pair<String,String> pair : ents) {
            String val = pair.getElement1();
            String cod = pair.getElement0();
            String add = additems.get(cod);
            if (add == null) add = addall;
            if (val.isEmpty()) continue;
            if (!buf.isEmpty() && add != null) buf.append(add);
            val = val.replace("\u208a","+");
            buf.append(val);
          }
         if (!buf.isEmpty()) items.add(buf.toString());
       }
    }
   
   if (items.isEmpty()) return null;
   
   String s = brc.getMultipleSeparator().trim();
   if (s.length() == 1) {
      String rep = ",";
      if (s.equals(";")) rep = ",";
      else if (s.equals(",")) rep = ";";
      else if (s.equals("+")) rep = "-";
      else if (s.equals("-")) rep = "_";
      else if (s.equals("/")) rep = "|";
      else if (s.equals("|")) rep = "/";
      for (int i = 0; i < items.size(); ++i) {
         String s1 = items.get(i);
         s1 = s1.replace(s,rep);
         items.set(i,s1);
       }
    }
   
   if (brc.isMultiple()) {
      return String.join(brc.getMultipleSeparator(),items);
    }
   
   return items.get(0);
}




List<List<Pair<String,String>>> getOrderedSubfields(String tag,Set<String> use)
{
   if (tag == null) return null;
   
   List<List<Pair<String,String>>> rslt = new ArrayList<>();
   
   for (Element datafield : IvyXml.children(marc_xml,"datafield")) {
      if (tag.equals(IvyXml.getAttrString(datafield,"tag"))) {
         List<Pair<String,String>> tags = new ArrayList<>();
         for (Element subfld : IvyXml.children(datafield,"subfield")) {
            String code = IvyXml.getAttrString(subfld,"code");
            if (use == null || use.contains(code)) {
               String val = IvyXml.getText(subfld);
               tags.add(Pair.createPair(code,val));
             }
          }
         rslt.add(tags);
       }
    }
   
   return rslt;
}




}       // end of class BibEntryMarc




/* end of BibEntryMarc.java */

