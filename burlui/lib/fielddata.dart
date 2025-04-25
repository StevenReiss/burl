/********************************************************************************/
/*                                                                              */
/*              fielddata.dart                                                  */
/*                                                                              */
/*      Information about fields                                                */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2025 Brown University -- Steven P. Reiss                      */
/********************************************************************************/
/*********************************************************************************
 *                                                                               *
 *  This work is licensed under Creative Commons Attribution-NonCommercial 4.0   *
 *  International.  To view a copy of this license, visit                        *
 *      https://creativecommons.org/licenses/by-nc/4.0/                          *
 *                                                                               *
 ********************************************************************************/

import "util.dart" as util;
import "globals.dart" as globals;

class FieldData {
  final Map<String, Map<String, dynamic>> _fieldData = {};
  final List<String> _fieldNames = [];

  static final FieldData _instance = FieldData._();

  FieldData._();

  factory FieldData() {
    return _instance;
  }

  Future<bool> loadData() async {
    Map<String, dynamic> data = await util.getJson("fielddata");
    if (data["status"] != "OK") return false;
    globals.burlSession ??= data['session'];
    Map<String, dynamic> d0 = data['data'];
    Map<String, dynamic> d1 = d0['BURL'];
    Map<String, dynamic> fd1 = d1['FIELDS'];
    List<dynamic> flds = fd1["FIELD"];
    for (Map<String, dynamic> finfo in flds) {
      String s = finfo["NAME"];
      _fieldNames.add(s);
      _fieldData[s] = finfo;
      _fieldData[s.toLowerCase()] = finfo;
      String? s1 = finfo["ALTNAMES"];
      if (s1 != null) {
        List<String> ls = s1.split(",");
        for (String s2 in ls) {
          _fieldData[s2.trim()] = finfo;
          _fieldData[s2.trim().toLowerCase()] = finfo;
        }
      }
      String? s3 = finfo["FIELDNAME"];
      if (s3 == null) {
        String s3 = finfo["NAME"];
        s3 = s3.replaceAll(" ", "_");
        s3 = s3.replaceAll(",", "_");
        s3 = s3.replaceAll("-", "_");
        s3 = s3.replaceAll(".", "");
        s3 = s3.trim();
        _fieldData[s3] = finfo;
        _fieldData[s3.toLowerCase()] = finfo;
      }
    }
    return true;
  }

  String? getFieldName(String id) {
    Map<String, dynamic>? fd = _fieldData[id];
    return fd?["NAME"];
  }

  bool isSortable(String id) {
    Map<String, dynamic>? fd = _fieldData[id];
    String? v = fd?["SORT"];
    if (v == null) return false;
    if ("tT1yY".contains(v.substring(0, 1))) return true;
    return false;
  }

  bool isMultiple(String id) {
    Map<String, dynamic>? fd = _fieldData[id];
    String? v = fd?["MULTIPLE"];
    if (v == null) return false;
    if ("tT1yY".contains(v.substring(0, 1))) return true;
    return false;
  }

  bool isGroupEdit(String id) {
    Map<String, dynamic>? fd = _fieldData[id];
    String? v = fd?["GROUPEDIT"];
    if (v == null) return false;
    if ("tT1yY".contains(v.substring(0, 1))) return true;
    return false;
  }

  bool isViewMultiple(String id) {
    Map<String, dynamic>? fd = _fieldData[id];
    String? v = fd?["VIEWMULT"];
    if (v == null) return false;
    if ("tT1yY".contains(v.substring(0, 1))) return true;
    return false;
  }

  String getLabel(String id) {
    Map<String, dynamic>? fd = _fieldData[id];
    String? lbl = fd?["LABEL"];
    lbl ??= fd?["NAME"];
    lbl ??= "";
    return lbl;
  }

  List<String> getFieldNames() {
    return _fieldNames;
  }

  String? getDisplay(String id) {
    Map<String, dynamic>? fd = _fieldData[id];
    String? disp = fd?["DISPLAY"];
    return disp;
  }

  bool canEdit(String useracc, String fld) {
    switch (useracc) {
      case "NONE":
      case "VIEWER":
        return false;
      default:
        break;
    }
    Map<String, dynamic>? fd = _fieldData[fld];
    String? acc = fd?["ACCESS"];
    if (acc == null) return true;
    // otherwise compare user access levels here
    return true;
  }
}        // end of class FieldData


