/********************************************************************************/
/*                                                                              */
/*              changepassworddialog.dart                                                    */
/*                                                                              */
/*      Dialog to change password                                               */
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
import 'package:flutter/material.dart';

Future changePasswordDialog(BuildContext context) async {
  TextEditingController p1controller = TextEditingController();
  TextEditingController p2controller = TextEditingController();
  BuildContext dcontext = context;
  String? pwdError;

  void cancel() {
    Navigator.of(dcontext).pop("CANCEL");
  }

  String? validatePassword(String? value) {
    if (!util.validatePassword(value)) {
      return "Invalid password";
    }
    return null;
  }

  Future updatePassword() async {
    pwdError = null;
    String p1 = p1controller.text;
    String p2 = p2controller.text;
    String? err = validatePassword(p1);
    if (err == null && p1 != p2) err = "Passwords don't match";
    if (err != null) {
      pwdError = err;
      return;
    }
    var data = {'userpwd': util.hasher(p1)};

    await util.postJsonOnly("changepassword", body: data);
    if (dcontext.mounted) {
      Navigator.of(dcontext).pop("OK");
    }
  }

  Widget cancelBtn = widgets.submitButton("Cancel", cancel);
  Widget acceptBtn = widgets.submitButton("OK", updatePassword);

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
              "Change Password",
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 16,
              ),
            ),
            const SizedBox(height: 15),
            widgets.textFormField(
              label: "New Password",
              controller: p1controller,
              obscureText: true,
            ),
            const SizedBox(height: 15),
            widgets.textFormField(
              label: "Verify Password",
              controller: p2controller,
              obscureText: true,
            ),
            const SizedBox(height: 8),
            widgets.errorField(pwdError),
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
