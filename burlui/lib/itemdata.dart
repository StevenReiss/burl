/********************************************************************************/
/*                                                                              */
/*              itemdata.dart                                                   */
/*                                                                              */
/*      Data for a specific library item                                        */
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

import 'globals.dart' as globals;
import 'dart:convert';

Map<int, int> _charMap = {
  8330: 43,
  8331: 45,
  8320: 48,
  8321: 49,
  8322: 50,
  8323: 51,
  8324: 52,
  8325: 53,
  8326: 54,
  8327: 55,
  8328: 56,
  8329: 57,
};

class ItemData {
  late Map<String, dynamic> _jsonData;

  ItemData(this._jsonData);

  ItemData.unknown() {
    _jsonData = {};
  }

  ItemData.clone(ItemData old) {
    _jsonData = Map<String, dynamic>.from(old._jsonData);
  }

  void reload(Map<String, dynamic> newdata) {
    _jsonData = newdata;
  }

  String getField(String fldname, {String join = " | "}) {
    String? id = globals.fieldData.getFieldName(fldname);
    if (id == null) return "";
    dynamic v1 = _jsonData[id];
    if (v1 == null) return "";
    String v2 = "UNDEF $fldname ${v1.runtimeType}";
    if (v1.runtimeType == String) {
      v2 = v1 as String;
      if (globals.fieldData.isMultiple(fldname)) {
        if (join != " | ") {
          v2 = v2.replaceAll(" | ", join);
        }
      }
    } else if (v1.runtimeType == List) {
      List v1l = v1 as List;
      v2 = v1l.join(join);
    } else {
      String v4 = v1.toString();
      if (v4.startsWith("[")) {
        // production web application sometimes does odd things here -- need to decode
        int i = v4.lastIndexOf("]");
        v4 = v4.substring(1, i).trim();
        List<String> v4a = v4.split(",");
        v2 = v4a.join(join);
      } else {
        v2 = "UNDEF $fldname ${v1.runtimeType} $v1";
      }
    }
    List<int> runes = v2.runes.toList();
    List<int> xrunes = [];
    for (int v in runes) {
      int? v0 = _charMap[v];
      if (v0 != null) v = v0;
      xrunes.add(v);
    }
    String v3 = utf8.decode(xrunes, allowMalformed: true);
    return v3;
  }

  String getMultiField(String fldname) {
    if (globals.fieldData.isViewMultiple(fldname)) {
      return getField(fldname, join: "\n");
    } else {
      return getField(fldname);
    }
  }

  int getId() {
    return _jsonData["burl_id"];
  }
} // end of class ItemData
