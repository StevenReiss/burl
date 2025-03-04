/********************************************************************************/
/*                                                                              */
/*              util.dart                                                       */
/*                                                                              */
/*      Utility methods for BURL user interface                                 */
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

import 'dart:convert' as convert;
import 'package:crypto/crypto.dart' as crypto;
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'globals.dart' as globals;

String hasher(String msg) {
  final bytes = convert.utf8.encode(msg);
  crypto.Digest rslt = crypto.sha512.convert(bytes);
  String srslt = convert.base64.encode(rslt.bytes);
  return srslt;
}

bool validateEmail(String email) {
  const res =
      r'^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$';
  final regExp = RegExp(res);
  if (!regExp.hasMatch(email)) return false;
  return true;
}

bool validatePassword(String? pwd) {
  if (pwd == null || pwd == '') return false;
  // check length, contents
  return true;
}

ThemeData getTheme() {
  return ThemeData(primarySwatch: Colors.lightBlue);
}

Uri getServerUri(String path, [Map<String, dynamic>? query]) {
  String p1 = "/rest/$path";
  if (kDebugMode && globals.debugServer) {
    return Uri.http("localhost:6737", p1, query);
  }
  return Uri.https("sherpa.cs.brown.edu:6737", p1, query);
}

Future<Map<String, dynamic>> postJson(String url, {dynamic body}) async {
  Uri u = getServerUri(url);
  Map<String, String> headers = {};
  headers["accept"] = "application/json";
  if (globals.burlSession != null) {
    if (body == null) {
      body = {"session": globals.burlSession};
    } else if (body["session"] == null) {
      body["session"] = globals.burlSession;
    }
  }
  dynamic resp = await http.post(u, body: body, headers: headers);
  Map<String, dynamic> js;
  js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
  return js;
}

Future<void> postJsonOnly(String url, {dynamic body}) async {
  Uri u = getServerUri(url);
  Map<String, String> headers = {"accept": "application/json"};
  if (globals.burlSession != null) {
    if (body == null) {
      body = {"session": globals.burlSession};
    } else if (body["session"] == null) {
      body["session"] = globals.burlSession;
    }
  }
  await http.post(u, body: body, headers: headers);
}

Future<Map<String, dynamic>> getJson(String url, {dynamic body}) async {
  Map<String, String>? headers = {"accept": "application/json"};
  if (globals.burlSession != null) {
    if (body == null) {
      body = {"session": globals.burlSession};
    } else if (body["session"] == null) {
      body["session"] = globals.burlSession;
    }
  }
  Uri u = getServerUri(url, body);
  dynamic resp = await http.get(u, headers: headers);
  Map<String, dynamic> js = {};
  js = convert.jsonDecode(resp.body) as Map<String, dynamic>;
  return js;
}
