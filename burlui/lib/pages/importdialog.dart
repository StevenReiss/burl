/********************************************************************************/
/*                                                                              */
/*              importdialog.dart                                               */
/*                                                                              */
/*      Dialog to import data from a CSV file                                   */
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
import 'dart:convert';

Future importDialog(BuildContext context, LibraryData lib) async {
  BuildContext dcontext = context;
  TextEditingController fileControl = TextEditingController();
  PlatformFile? resultFile;
  String mode = "NEW";
  List<String> modes = ["NEW", "SKIP", "AUGMENT", "REPLACE", "FORCE"];
  bool hadfocus = false;

  void cancel() {
    if (dcontext.mounted) {
      Navigator.of(dcontext).pop("CANCEL");
    }
  }

  void submit() async {
    PlatformFile? rf = resultFile;
    if (rf == null) return;
    Stream<List<int>>? fileReadStream = rf.readStream;
    if (fileReadStream == null) {
      return;
    }

    List<String> lines = [];
    await fileReadStream
        .transform(utf8.decoder)
        .transform(LineSplitter())
        .forEach((line) {
          if (line.isNotEmpty) lines.add(line);
        });
    Map<String, Object> filedata = {"rows": lines};
    String d = jsonEncode(filedata);

    Map<String, String?> data = {
      "library": lib.getLibraryId().toString(),
      "update": mode,
      "csvdata": d,
    };
    await util.postJson("import", body: data);
    if (dcontext.mounted) {
      Navigator.of(dcontext).pop("OK");
    }
  }

  void chooseFile() async {
    hadfocus = true;
    FilePickerResult? result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      withData: true,
      withReadStream: true,
      dialogTitle: "Select CSV file to Import",
      allowedExtensions: ["csv"],
    );
    if (result == null) return;
    PlatformFile file = result.files.first;
    String? p = file.path;
    p ??= file.name;
    fileControl.text = p;
    resultFile = file;
  }

  void setMode(String? md) {
    if (md != null) mode = md;
    hadfocus = false;
  }

  //   Widget fileBtn = widgets.submitButton("Choose File", chooseFile);
  Widget cancelBtn = widgets.submitButton("Cancel", cancel);
  Widget submitBtn = widgets.submitButton(
    "Do Import",
    submit,
    //  enabled: resultFile != null,
  );

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
              "Import Data into Library",
              scaler: 1.5,
            ),
            widgets.fieldSeparator(15),
            widgets.dropDownWidget<String>(
              modes,
              value: mode,
              onChanged: setMode,
              label: "Update Mode",
              tooltip:
                  "Select update mode when item already exists in library."
                  "Items will be matched on burl_id if that is in the import. "
                  "Ohterwise they will be matched on isbn or lccn.  Modes "
                  "include: NEW (add new item even it there is a match), "
                  "SKIP (ignore import if previous existed), "
                  "AUGMENT (add data from import if original was empty), "
                  "REPLACE (use data from import unless it is empty) "
                  "and FORCE (always use data from import) ",
            ),
            widgets.fieldSeparator(15),
            const Text(
              "BURL will import data in CSV format.  The "
              "file should be one that has been previously exported by BURL, "
              "but can be edited or otherwise modified by the user.  It can "
              "also be a CSV file you created that looks like a BURL export. "
              "There should be a header line with field names.",
            ),
            widgets.fieldSeparator(16),
            Focus(
              child: widgets.textField(
                label: "Import from File",
                controller: fileControl,
                //  enabled: false,
                readOnly: true,
              ),
              onFocusChange: (hasfocus) {
                if (hasfocus && !hadfocus) {
                  chooseFile();
                  hadfocus = true;
                }
              },
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: <Widget>[
                cancelBtn,
                const SizedBox(width: 15),
                //  fileBtn,
                //  const SizedBox(width: 15),
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
