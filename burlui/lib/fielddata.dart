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

class FieldData {
  Map<String, dynamic> _jsonData = {};

  FieldData();

  Future<bool> loadData() async {
    Map<String, dynamic> data = await util.getJson("fielddata");
    if (data["status"] != "OK") return false;
    _jsonData = data;
    return true;
  }
}        // end of class FieldData

