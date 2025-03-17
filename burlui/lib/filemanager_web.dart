/********************************************************************************/
/*                                                                              */
/*              filemanager_web.dart                                            */
/*                                                                              */
/*      Handle file upload/download for non-web implementations                 */
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

import 'filemanager.dart';
import 'package:http/http.dart' as http;
import 'package:file_picker/file_picker.dart';
// ignore: deprecated_member_use, avoid_web_libraries_in_flutter
import 'dart:html' as html;
// import 'package:web/web.dart' as html;

class FileManager implements FileManagerInterface {
  @override
  Future<dynamic> uploadFile(
    FilePickerResult? result,
    String uploadUrl,
  ) async {
    final bytes = result?.files.single.bytes;
    if (bytes == null) return;
    final uri = Uri.parse(uploadUrl);
    var resp = await http.post(uri, body: bytes);
    return resp;
  }

  @override
  Future<void> downloadFile(dynamic response, String path) async {
    final blob = html.Blob([response.bodyBytes]);
    final anchorElement = html.AnchorElement(
      href: html.Url.createObjectUrlFromBlob(blob).toString(),
    )..setAttribute('download', path);
    html.document.body!.children.add(anchorElement);
    anchorElement.click();
    html.document.body!.children.remove(anchorElement);
  }
}     // end of class FileManager


/* end of filemanagerweb.dart */
