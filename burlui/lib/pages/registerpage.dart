/********************************************************************************/
/*                                                                              */
/*              registerpage.dart                                                    */
/*                                                                              */
/*      Handle BURL registration                                                */
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
import '../globals.dart' as globals;
import '../widgets.dart' as widgets;
import 'loginpage.dart';

class BurlRegister extends StatelessWidget {
  const BurlRegister({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Burl Registration',
      theme: widgets.getTheme(),
      home: const BurlRegisterWidget(),
    );
  }
}

class BurlRegisterWidget extends StatefulWidget {
  const BurlRegisterWidget({super.key});

  @override
  State<BurlRegisterWidget> createState() => _BurlRegisterWidgetState();
}

class _BurlRegisterWidgetState extends State<BurlRegisterWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  String _curEmail = "";
  String _curPassword = "";
  String _confirmPassword = "";
  late String _registerError;

  _BurlRegisterWidgetState() {
    _curEmail = "";
    _curPassword = "";
    _confirmPassword = "";
    _registerError = '';
  }

  Future<String> _preRegister() async {
    Map<String, dynamic> js = await util.getJson("register");
    globals.burlSession = js['session'];
    return js['salt'];
  }

  Future<String?> _registerUser() async {
    String pwd = _curPassword;
    String em = _curEmail;
    String email = em.toLowerCase();
    String salt = await _preRegister();
    String p1 = util.hasher(pwd);
    String p2 = util.hasher(p1 + salt);

    var body = {'email': email, 'password': p2, 'salt': salt};
    Map<String, dynamic> jresp = await util.postJson(
      "register",
      body: body,
    );
    if (jresp['status'] == "OK") return null;
    return jresp['message'];
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("Sign Up")),
      body: widgets.topLevelPage(
        context,
        Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: <Widget>[
                    widgets.getTopLevelLogo(context),
                    const Padding(padding: EdgeInsets.all(8.0)),
                    widgets.loginTextField(
                      context,
                      hint: "Valid Email Address",
                      label: "Email",
                      keyboardType: TextInputType.emailAddress,
                      validator: _validateEmail,
                      onChanged: _updateEmail,
                    ),
                    widgets.fieldSeparator(),
                    widgets.loginTextField(
                      context,
                      hint: "Password",
                      label: "Password",
                      validator: _validatePassword,
                      obscureText: true,
                      onChanged: _updatePassword,
                    ),
                    widgets.fieldSeparator(),
                    widgets.loginTextField(
                      context,
                      hint: "Confirm Password",
                      label: "Confirm Password",
                      validator: _validateConfirmPassword,
                      obscureText: true,
                      onChanged: _updateConfirmPassword,
                      onSubmitted: _tryRegister,
                    ),
                    widgets.errorField(_registerError),
                    widgets.fieldSeparator(),
                    //   const Text("You will have to validate your email before logging in."),
                    //   widgets.fieldSeparator(),
                    widgets.submitButton("REGISTER", _handleRegister),
                  ],
                ),
              ),
              widgets.textButton("Already a user, login", _gotoLogin),
            ],
          ),
        ),
      ),
    );
  }

  void _tryRegister([String? val]) {
    if (_curEmail.isNotEmpty &&
        _curPassword.isNotEmpty &&
        _confirmPassword.isNotEmpty) {
      _handleRegister();
    }
  }

  void _handleRegister() async {
    _clearRegisterError();
    if (_formKey.currentState!.validate()) {
      String? rslt = await _registerUser();
      if (rslt != null) {
        setState(() {
          _registerError = rslt;
        });
        return;
      }
      _gotoLogin();
    }
  }

  void _gotoLogin() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const BurlLogin()),
    );
  }

  String? _validateConfirmPassword(String? value) {
    _clearRegisterError();
    if (value != _curPassword) {
      return "Passwords must match";
    }
    return null;
  }

  void _updateConfirmPassword(String? value) {
    _clearRegisterError();
    if (value != null) {
      _confirmPassword = value;
    }
  }

  String? _validatePassword(String? value) {
    _curPassword = value ?? "";
    _clearRegisterError();
    if (value == null || value.isEmpty) {
      return "Password must not be null";
    } else if (!util.validatePassword(value)) {
      return "Invalid password";
    }
    return null;
  }

  void _updatePassword(String? value) {
    _clearRegisterError();
    if (value != null) {
      _curPassword = value;
    }
  }

  String? _validateEmail(String? value) {
    _curEmail = value ?? "";
    _clearRegisterError();
    if (value == null || value.isEmpty) {
      return "Email must not be null";
    } else if (!util.validateEmail(value)) {
      return "Invalid email address";
    }
    return null;
  }

  void _updateEmail(String? value) {
    _clearRegisterError();
    if (value != null) {
      _curEmail = value;
    }
  }

  void _clearRegisterError() {
    if (_registerError.isNotEmpty) {
      setState(() {
        _registerError = "";
      });
    }
  }
}
