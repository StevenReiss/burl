/********************************************************************************/
/*                                                                              */
/*              BurlFieldData.java                                              */
/*                                                                              */
/*      Hold and provide information about the available fields                 */
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.xml.IvyXml;




public class BurlFieldData implements BurlConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,Element> field_map;
private List<String> field_names;
private List<String> isbn_fields;
private String count_field;
private String isbn_field;
private String lccn_field;
private String multiple_string;
private Element csv_data;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public BurlFieldData()
{
   field_map = new LinkedHashMap<>(); 
   isbn_fields = new ArrayList<>();
   field_names = new ArrayList<>();
   count_field = null;
   isbn_field = null;
   lccn_field = null;
   
   Element xmldata = null;
   try (InputStream ins = getClass().getClassLoader().getResourceAsStream("fields.xml")) {
      xmldata = IvyXml.loadXmlFromStream(ins); 
    }
   catch (Exception e) {
      IvyLog.logE("BIBENTRY","Problem reading field data",e);
      System.exit(1);
    }
   
   Element flds = IvyXml.getChild(xmldata,"FIELDS");
   for (Element fldelt : IvyXml.children(flds,"FIELD")) {
      String nm = IvyXml.getAttrString(fldelt,"NAME");
      field_names.add(nm);
      field_map.put(nm,fldelt);
      
      for (String snm : getAllNames(nm)) {
         field_map.put(snm,fldelt);
         field_map.put(snm.toLowerCase(),fldelt);
       }
      
      BurlIsbnType isbntype = IvyXml.getAttrEnum(fldelt,"ISBN",BurlIsbnType.NONE);
      if (isbntype != BurlIsbnType.NONE) {
         isbn_fields.add(nm);
         if (isbntype == BurlIsbnType.ORIGINAL) isbn_field = nm;
       }
      if (IvyXml.getAttrBool(fldelt,"COUNT")) {
         count_field = nm; 
       }
      if (IvyXml.getAttrBool(fldelt,"LCCN")) {
         lccn_field = nm; 
       }
    }
   
   csv_data = IvyXml.getChild(xmldata,"CSV");
   Element mult = IvyXml.getChild(xmldata,"MULTIPLE");
   multiple_string = IvyXml.getAttrString(mult,"TEXT"," | ");
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public Collection<String> getAllFields()
{
   return field_names;
}


public String getMarcData(String nm) 
{
   Element felt = field_map.get(nm);
   return IvyXml.getAttrString(felt,"MARC");
}


public String getOpenLibData(String nm) 
{
   Element felt = field_map.get(nm);
   return IvyXml.getAttrString(felt,"OPENLIB");
}


public String getGoogleData(String nm) 
{
   Element felt = field_map.get(nm);
   return IvyXml.getAttrString(felt,"GOOGLE");
}

public boolean isMultiple(String nm)
{
   Element felt = field_map.get(nm);
   return IvyXml.getAttrBool(felt,"MULTIPLE");
}


public BurlIsbnType getIsbnType(String nm)
{
   Element felt = field_map.get(nm);
   return IvyXml.getAttrEnum(felt,"ISBN",BurlIsbnType.NONE);  
}

public String getOtherIsbn(String nm)
{
   Element felt = field_map.get(nm);
   return IvyXml.getAttrString(felt,"ALTISBN");
}


public BurlUserAccess getAccessLevel(String nm)
{
   Element felt = field_map.get(nm);
   return IvyXml.getAttrEnum(felt,"ACCESS",BurlUserAccess.EDITOR);
}


public String getCountField()
{
   return count_field;
}

public String getOriginalIsbnField()
{
   return isbn_field;
}

public String getLccnField()
{
   return lccn_field;
}

public List<String> getIsbnFields()
{
   return isbn_fields;
}

public String getFieldName(String nm)
{
   Element felt = field_map.get(nm);
   String fnm = IvyXml.getAttrString(felt,"FIELDNAME");
   if (fnm == null) {
      fnm = nm.toLowerCase();
      fnm = fnm.replace(" ","_");
      fnm = fnm.replace(",","_");
      fnm = fnm.replace("-","_");
      fnm = fnm.replace(".","");
    }
   return fnm;
}

public String getDefault(String nm)
{
   Element felt = field_map.get(nm);
   String dflt = null;
   if (nm.equals(count_field)) dflt = "0";
   
   return IvyXml.getAttrString(felt,"DEFAULT",dflt);
}


public String getLabel(String nm)
{
   Element felt = field_map.get(nm);
   String lbl = IvyXml.getAttrString(felt,"LABEL",nm);
   return lbl;
}


public boolean isInternal(String nm)
{
   Element felt = field_map.get(nm);
   if (IvyXml.getAttrBool(felt,"INTERNAL")) return true;
   if (getLabel(nm).equals("*")) return true;
   return false;
}

public BurlFixType getFixType(String nm)
{
   Element felt = field_map.get(nm);
   return IvyXml.getAttrEnum(felt,"FIX",BurlFixType.NONE); 
}


public String getMultiple()                    { return multiple_string; }

public String getMultiplePattern()
{
   return "\\Q" + multiple_string + "\\E";
}


public String getCSVSeparator()        
{
   return IvyXml.getAttrString(csv_data,"SEPARATOR",",");
}



public String getCSVQuote()        
{
   return IvyXml.getAttrString(csv_data,"QUOTE","\"");
}


public Collection<String> getAllNames(String nm)
{
   Element felt = field_map.get(nm);
   
   Set<String> rslt = new LinkedHashSet<>();
   rslt.add(nm);
   rslt.add(getFieldName(nm));
   rslt.add(getLabel(nm));
   
   String alts = IvyXml.getAttrString(felt,"ALTNAMES","");
   for (StringTokenizer tok = new StringTokenizer(alts,","); tok.hasMoreTokens(); ) {
      String alt = tok.nextToken().trim();
      rslt.add(alt);
    }
  
   return rslt;
}

public boolean isValidField(String name)
{
   return field_map.get(name) != null;
}


public String getBaseName(String alt)
{
   Element felt = field_map.get(alt);
   if (felt == null) return null;
   return IvyXml.getAttrString(felt,"NAME");
}


}       // end of class BurlFieldData




/* end of BurlFieldData.java */

