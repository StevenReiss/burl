/********************************************************************************/
/*                                                                              */
/*              createlibrarydialog.dart                                        */
/*                                                                              */
/*      Dialog to create a new library                                          */
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

import 'package:flutter/material.dart';
import '../widgets.dart' as widgets;
import '../util.dart' as util;
import '../librarydata.dart';

Future createLibraryDialog(BuildContext context, List<LibraryData> libdata) async {
  TextEditingController namecontroller = TextEditingController();
  String repotype = "DATABASE";
  BuildContext dcontext = context;
  String? libError;
  StateSetter? statefct;

  List<String> repoTypes = ["DATABASE", "CSV", "JSON"];

  void cancel() {
    Navigator.of(dcontext).pop("CANCEL");
  }

  String? validateName(String? value) {
    if (value == null || value.isEmpty) return "Empty name";
    RegExp pat = RegExp(r'^[a-zA-z][-A-Za-z0-9_ ]*$');
    if (pat.hasMatch(value)) return null;
    return "Invalid name";
  }

  Future createLibrary() async {
    String libname = namecontroller.text;
    String? err = validateName(libname);
    if (err != null) {
      statefct!(() {
        libError = err;
      });
      return;
    }
    var data = {"name": libname, "repotype": repotype};
    Map<String, dynamic> rslt = await util.postJson("createlibrary", body: data);
    if (rslt['status'] == "OK") {
      LibraryData ld = LibraryData(rslt);
      libdata.add(ld);
      if (dcontext.mounted) {
        Navigator.of(dcontext).pop("OK");
      }
    } else {
      statefct!(() {
        libError = rslt["message"];
      });
    }
  }

  void setRepoType(String? type) {
    if (type != null) repotype = type;
  }

  Widget cancelBtn = widgets.submitButton("Cancel", cancel);
  Widget acceptBtn = widgets.submitButton("OK", createLibrary);

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
              "Create new Library",
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            const SizedBox(height: 15),
            widgets.textFormField(
              label: "Library Name",
              controller: namecontroller,
              hint: "Name for your new library",
              tooltip:
                  "Provide a name for your library that all potential users "
                  "will recognize as yours.",
            ),
            const SizedBox(height: 15),
            widgets.dropDownWidget(
              repoTypes,
              value: repotype,
              onChanged: setRepoType,
              label: "Repository Type: ",
              hint: "Choose the type of repository for this library",
              tooltip: "Choose how the repository for this library will be stored",
            ),
            const SizedBox(height: 8),
            widgets.errorField(libError),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [cancelBtn, const SizedBox(width: 15), acceptBtn],
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
      return StatefulBuilder(
        builder: (context, setState) {
          statefct = setState;
          return dlg;
        },
      );
    },
  );
}
