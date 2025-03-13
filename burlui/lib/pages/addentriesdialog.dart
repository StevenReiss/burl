/********************************************************************************/
/*                                                                              */
/*              addentriesdialog.dart                                                    */
/*                                                                              */
/*      Dialog to add entries by isbn or lccn                                   */
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
import 'dart:io';

Future addEntriesDialog(BuildContext context, LibraryData lib) async {
  TextEditingController isbncontroller = TextEditingController();
  BuildContext dcontext = context;
  String mode = "NEW";

  void cancel() {
    if (dcontext.mounted) {
      Navigator.of(dcontext).pop("CANCEL");
    }
  }

  void submit() async {
    Map<String, String?> data = {
      "library": lib.getLibraryId().toString(),
      "mode": mode,
      "count": "TRUE",
      "isbnstr": isbncontroller.text,
    };
    Map<String, dynamic> rslt = await util.postJson(
      "addisbns",
      body: data,
    );
    if (rslt["status"] == "OK") {
      if (dcontext.mounted) {
        Navigator.of(dcontext).pop("OK");
      }
    }
  }

  void addFile() async {
    FilePickerResult? rslt = await FilePicker.platform.pickFiles(
      type: FileType.any,
      allowCompression: false,
      dialogTitle: "Select text file containing ISBNs or LCCNs",
      lockParentWindow: true,
      withData: true,
    );
    if (rslt == null) return;
    File file = File(rslt.files.single.path!);
    String cnts = await file.readAsString();
    isbncontroller.text += "\n$cnts";
  }

  void setMode(String? md) {
    if (md != null) mode = md;
  }

  Widget cancelBtn = widgets.submitButton("Cancel", cancel);
  Widget submitBtn = widgets.submitButton("Submit", submit);
  Widget fileBtn = widgets.submitButton("Add From File", addFile);
  List<String> modes = ["NEW", "SKIP", "AUGMENT", "REPLACE", "FORCE"];

  Dialog dlg = Dialog(
    child: Padding(
      padding: const EdgeInsets.all(20.0),
      child: SizedBox(
        width: MediaQuery.of(context).size.width * 0.8,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            const Text(
              "Add Items to Library by ISBN/LCCN",
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 16,
              ),
            ),
            widgets.fieldSeparator(15),
            widgets.dropDownWidget<String>(
              modes,
              value: mode,
              onChanged: setMode,
              label: "Update Mode",
              tooltip:
                  "Select update mode when item already exists in library",
            ),
            widgets.fieldSeparator(),
            Expanded(
              child: widgets.textField(
                label: "Entry IDs",
                hint: "Enter ISBNs or LCCNs",
                controller: isbncontroller,
                maxLines: 0,
              ),
            ),
            widgets.fieldSeparator(),
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
