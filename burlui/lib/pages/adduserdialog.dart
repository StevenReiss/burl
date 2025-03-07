/********************************************************************************/
/*                                                                              */
/*              adduserdialog.dart                                              */
/*                                                                              */
/*      Dialog to add a user to a library                                       */
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

Future addUserDialog(BuildContext context, LibraryData libdata) async {
  TextEditingController emailcontroller = TextEditingController();
  BuildContext dcontext = context;
  String? emailError;
  String acclevel = "VIEWER";
  List<String> acclevels = [
    "NONE",
    "VIEWER",
    "EDITOR",
    "LIBRARIAN",
    "OWNER",
  ];

  void cancel() {
    Navigator.of(dcontext).pop("CANCEL");
  }

  String? validateEmail(String? value) {
    if (value == null || !util.validateEmail(value)) {
      return "Invalid email address";
    }
    return null;
  }

  void setLevel(String? lvl) {
    if (lvl != null) acclevel = lvl;
  }

  Future addUser() async {
    emailError = null;
    String p1 = emailcontroller.text;
    String? err = validateEmail(p1);
    if (err != null) {
      emailError = err;
      return;
    }
    Map<String, String?> data = {
      'email': emailcontroller.text,
      'access': acclevel,
      'library': libdata.getLibraryId().toString(),
    };

    Map<String, dynamic> rslt = await util.postJson(
      "addlibraryuser",
      body: data,
    );
    if (rslt["status"] == "OK") {
      if (dcontext.mounted) {
        Navigator.of(dcontext).pop("OK");
      }
    } else {
      emailError = rslt["message"];
    }
  }

  Widget cancelBtn = widgets.submitButton("Cancel", cancel);
  Widget acceptBtn = widgets.submitButton("OK", addUser);

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
              "Add/Remove User from Library",
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 16,
              ),
            ),
            const SizedBox(height: 15),
            widgets.textFormField(
              label: "User's Email",
              controller: emailcontroller,
            ),
            const SizedBox(height: 15),
            widgets.dropDownWidget(
              acclevels,
              value: acclevel,
              onChanged: setLevel,
              label: "Access Level",
              tooltip:
                  "Set User's access level.  Use NONE to remove the user",
            ),
            const SizedBox(height: 8),
            widgets.errorField(emailError),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                cancelBtn,
                const SizedBox(width: 15),
                acceptBtn,
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
