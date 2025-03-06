/********************************************************************************/
/*                                                                              */
/*              itemdata. rt                                                    */
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

class ItemData {
  late Map<String, dynamic> _jsonData;

  ItemData(this._jsonData);

  ItemData.unknown() {
    _jsonData = {};
  }

  void reload(Map<String, dynamic> newdata) {
    _jsonData = newdata;
  }

  String getField(String fldname) {
    String? id = globals.fieldData.getFieldName(fldname);
    if (id == null) return "";
    dynamic v1 = _jsonData[id];
    if (v1 == null) return "";
    if (v1.runtimeType == String) {
      return v1 as String;
    } else if (v1.runtimeType == List) {
      List v1l = v1 as List;
      return v1l.join(" | ");
    }
    return "";
  }

  List<String>? getMultiple(String fldname) {
    return null;
  }

  int getId() {
    return _jsonData["burl_id"];
  }
}  // end of class ItemData