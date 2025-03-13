/********************************************************************************/
/*                                                                              */
/*              forgotpasswordpage.dart                                         */
/*                                                                              */
/*      Global definitions and constants for BURL user interface                */
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
import '../util.dart' as util;
import '../widgets.dart' as widgets;
import 'loginpage.dart';

class BurlForgotPasswordWidget extends StatefulWidget {
  const BurlForgotPasswordWidget({super.key});

  @override
  State<BurlForgotPasswordWidget> createState() =>
      _BurlForgotPasswordWidgetState();
}

class _BurlForgotPasswordWidgetState extends State<BurlForgotPasswordWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  String? _emailGiven;

  _BurlForgotPasswordWidgetState();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: widgets.appBar("Forgot Password"),
      body: Center(
        child: widgets.topLevelPage(
          context,
          Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: <Widget>[
                    SizedBox(
                      width: MediaQuery.of(context).size.width * 0.4,
                      child: Center(
                        child: Image.asset(
                          "assets/images/iqsignstlogo.png",
                          fit: BoxFit.contain,
                        ),
                      ),
                    ),
                    const Padding(padding: EdgeInsets.all(16.0)),
                    Container(
                      constraints: const BoxConstraints(
                        minWidth: 100,
                        maxWidth: 600,
                      ),
                      width: MediaQuery.of(context).size.width * 0.8,
                      child: widgets.textFormField(
                        hint: "Email",
                        label: "Email",
                        validator: _validateEmail,
                      ),
                    ),
                    const Padding(padding: EdgeInsets.all(16.0)),
                    Container(
                      constraints: const BoxConstraints(
                        minWidth: 200,
                        maxWidth: 350,
                      ),
                      width: MediaQuery.of(context).size.width * 0.4,
                      child: widgets.submitButton(
                        "Request Password Email",
                        _handleForgotPassword,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _handleForgotPassword() async {
    final form = _formKey.currentState;
    if (form!.validate()) {
      form.save();
      await _forgotPassword();
      _gotoLogin();
    }
  }

  String? _validateEmail(String? value) {
    _emailGiven = value;
    if (value == null || value.isEmpty) {
      return "Email must not be null";
    } else if (!util.validateEmail(value)) {
      return "Invalid email address";
    }
    return null;
  }

  void _gotoLogin() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const BurlLogin()),
    );
  }

  Future _forgotPassword() async {
    String em = (_emailGiven as String).toLowerCase();
    var body = {'email': em};
    await util.postJsonOnly("forgotpassword", body: body);
  }
}

