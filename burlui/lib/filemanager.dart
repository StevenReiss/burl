/********************************************************************************/
/*                                                                              */
/*              filemanagerinterface.dart                                       */
/*                                                                              */
/*      Abstract class to handle file upload and download                       */
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

import 'package:file_picker/file_picker.dart';

export 'filemanager_io.dart'
    if (dart.library.html) 'filemanager_web.dart';

abstract interface class FileManagerInterface {
  Future<dynamic> uploadFile(FilePickerResult? result, String url);
  Future<void> downloadFile(dynamic response, String path);
}

/* end of filemanagerinterface.dart */
