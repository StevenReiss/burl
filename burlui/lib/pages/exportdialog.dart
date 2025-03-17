/********************************************************************************/
/*                                                                              */
/*              exportdialog.dart                                               */
/*                                                                              */
/*      Dialog to export all or part of a library                               */
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

import '../widgets.dart' as widgets;
import '../util.dart' as util;
import '../librarydata.dart';
import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';

Future exportDialog(
  BuildContext context,
  LibraryData lib,
  String exporttype, {
  String? sortby,
  bool sortInvert = false,
  String? filter,
}) async {
  BuildContext dcontext = context;
  TextEditingController fileControl = TextEditingController();

  fileControl.text = "export.${exporttype.toLowerCase()}";
  String what =
      (sortby == null && filter == null)
          ? "All Entries"
          : "Selected Entries";

  void cancel() {
    if (dcontext.mounted) {
      Navigator.of(dcontext).pop("CANCEL");
    }
  }

  void submit() async {
    Map<String, String?> data = {
      "library": lib.getLibraryId().toString(),
      "invert": sortInvert.toString(),
    };
    if (sortby != null) data['orderby'] = sortby;
    if (filter != null) data['filter'] = filter;
    await util.postJsonDownload(
      "exportlibrary",
      fileControl.text,
      body: data,
    );
    if (dcontext.mounted) {
      Navigator.of(dcontext).pop("OK");
    }
  }

  void chooseFile() async {
    String? output = await FilePicker.platform.saveFile(
      dialogTitle: 'Please select output $exporttype file: ',
      fileName: fileControl.text,
    );
    if (output != null) {
      fileControl.text = output;
    }
  }

  Widget fileBtn = widgets.submitButton("Choose File", chooseFile);
  Widget cancelBtn = widgets.submitButton("Cancel", cancel);
  Widget submitBtn = widgets.submitButton("Do Export", submit);

  Dialog dlg = Dialog(
    child: Padding(
      padding: const EdgeInsets.all(20.0),
      child: SizedBox(
        width: MediaQuery.of(context).size.width * 0.8,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            widgets.largeBoldText(
              "Export $what as $exporttype",
              scaler: 1.5,
            ),
            widgets.fieldSeparator(15),
            Text(
              "BURL will export the data as a $exporttype file.  This file "
              "can be edited and then later imported into this or another "
              "library.",
            ),
            widgets.fieldSeparator(16),
            widgets.textField(
              label: "Save in File",
              controller: fileControl,
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: <Widget>[
                cancelBtn,
                const SizedBox(width: 15),
                fileBtn,
                const SizedBox(width: 15),
                submitBtn,
              ],
            ),
          ],
        ),
      ),
    ),
  );

  return showDialog(
    context: context,
    builder: (context) {
      dcontext = context;
      return dlg;
    },
  );
}
