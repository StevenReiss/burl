/********************************************************************************/
/*                                                                              */
/*              printlabelsdialog.dart                                          */
/*                                                                              */
/*      Dialog to print a set of labels                                         */
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

Future printLabelsDialog(BuildContext context, LibraryData lib) async {
  BuildContext dcontext = context;
  TextEditingController fileControl = TextEditingController();
  bool reset = false;

  fileControl.text = "labels.rtf";

  void submit() async {
    Map<String, String?> data = {
      "library": lib.getLibraryId().toString(),
      "reset": reset.toString(),
    };
    await util.postJsonDownload("labels", fileControl.text, body: data);
    if (dcontext.mounted) {
      if (reset) {
        Navigator.of(dcontext).pop("RESET");
      } else {
        Navigator.of(dcontext).pop("OK");
      }
    }
  }

  void chooseFile() async {
    String? output = await FilePicker.platform.saveFile(
      dialogTitle: 'Please select output rtf file: ',
      fileName: fileControl.text,
    );
    if (output != null) {
      fileControl.text = output;
    }
  }

  Widget w = StatefulBuilder(
    builder: (context, setState) {
      dcontext = context;
      void handleReset(bool? v) {
        if (v != null) {
          setState(() {
            reset = v;
          });
        }
      }

      void cancel() {
        if (dcontext.mounted) {
          NavigatorState n = Navigator.of(dcontext);
          n.pop("CANCEL");
          // Navigator.of(dcontext).pop("CANCEL");
        }
      }

      Widget fileBtn = widgets.submitButton("Choose File", chooseFile);
      Widget cancelBtn = widgets.submitButton("Cancel", cancel);
      Widget submitBtn = widgets.submitButton("Get Labels", submit);

      return Dialog(
        child: Padding(
          padding: const EdgeInsets.all(20.0),
          child: SizedBox(
            width: MediaQuery.of(context).size.width * 0.8,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              mainAxisAlignment: MainAxisAlignment.center,
              children: <Widget>[
                widgets.largeBoldText(
                  "Print the Next Set of Labels",
                  scaler: 1.5,
                ),
                widgets.fieldSeparator(15),
                const Text(
                  "BURL will print labels for the next set of items "
                  "and save it to a rtf file that can be loaded into Word "
                  "or similar system and printed onto adhesive paper. ",
                ),
                widgets.fieldSeparator(16),
                widgets.textField(
                  label: "Save in File",
                  controller: fileControl,
                ),
                widgets.fieldSeparator(15),
                widgets.booleanField(
                  label: "Reset Print Labels",
                  value: reset,
                  onChanged: handleReset,
                  tooltip:
                      "Reset the Print Labels field to no for all items printed",
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
    },
  );

  return showDialog(
    context: context,
    builder: (context) {
      return w;
    },
  );
}
