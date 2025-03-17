/********************************************************************************/
/*                                                                              */
/*              filemanager_io.dart                                             */
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

import 'dart:io';
import 'filemanager.dart';
import 'package:http/http.dart' as http;
import 'package:file_picker/file_picker.dart';

class FileManager implements FileManagerInterface {
  @override
  Future<dynamic> uploadFile(
    FilePickerResult? result,
    String uploadUrl,
  ) async {
    final path = result?.files.single.path;
    if (path == null) return;
    final bytes = await File(path).readAsBytes();
    final uri = Uri.parse(uploadUrl);
    var resp = await http.post(uri, body: bytes);
    return resp;
  }

  @override
  Future<void> downloadFile(dynamic response, String path) async {
    final saveFile = File(path);
    await saveFile.writeAsBytes(response.bodyBytes);
  }
}

/* end of filemanager.dart */
