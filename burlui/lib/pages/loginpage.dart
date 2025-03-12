/********************************************************************************/
/*                                                                              */
/*              loginpage.dart                                                  */
/*                                                                              */
/*      Handle Login for BURL                                                   */
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
import '../globals.dart' as globals;
import '../util.dart' as util;
import '../widgets.dart' as widgets;
import 'registerpage.dart';
import 'homepage.dart' as home;
import 'forgotpasswordpage.dart';
import 'package:shared_preferences/shared_preferences.dart';

//
//    Private Variables
//
bool _loginValid = false;

//
//    Check login using preferences or prior login
//

Future<bool> testLogin() async {
  if (_loginValid) return true;
  SharedPreferences prefs = await SharedPreferences.getInstance();
  String? uid = prefs.getString('uid');
  String? pwd = prefs.getString('pwd');
  if (uid != null && pwd != null) {
    _HandleLogin login = _HandleLogin(uid, pwd);
    String? rslt = await login.authUser();
    if (rslt == null) {
      _loginValid = true;
      return true;
    }
  }
  return false;
}

//
//      Logiin Widget
//

class BurlLogin extends StatelessWidget {
  const BurlLogin({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Burl Login',
      theme: widgets.getTheme(),
      home: const BurlLoginWidget(),
    );
  }
}

class BurlLoginWidget extends StatefulWidget {
  const BurlLoginWidget({super.key});

  @override
  State<BurlLoginWidget> createState() => _BurlLoginWidgetState();
}

class _BurlLoginWidgetState extends State<BurlLoginWidget> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  String? _curUser;
  String? _curPassword;
  String _loginError = '';
  final TextEditingController _userController = TextEditingController();
  final TextEditingController _pwdController = TextEditingController();
  bool _rememberMe = false;

  _BurlLoginWidgetState() {
    _loginValid = false;
  }

  @override
  void initState() {
    _loadUserAndPassword();
    super.initState();
  }

  void _gotoFirstPage() {
    widgets.goto(context, const home.BurlHomePage(true));
  }

  void _gotoRegister() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const BurlRegister()),
    );
  }

  void _gotoForgotPassword() {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => const BurlForgotPasswordWidget(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: widgets.appBar("Login"),
      body: widgets.topLevelPage(
        context,
        Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            mainAxisSize: MainAxisSize.max,
            children: <Widget>[
              Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: <Widget>[
                    widgets.getTopLevelLogo(context),
                    widgets.getPadding(16),
                    widgets.loginTextField(
                      context,
                      hint: "Email",
                      label: "Email",
                      validator: _validateUserName,
                      controller: _userController,
                      fraction: 0.8,
                      textInputAction: TextInputAction.next,
                    ),
                    widgets.fieldSeparator(),
                    widgets.loginTextField(
                      context,
                      hint: "Password",
                      label: "Password",
                      validator: _validatePassword,
                      controller: _pwdController,
                      fraction: 0.8,
                      obscureText: true,
                      textInputAction: TextInputAction.go,
                    ),
                    widgets.errorField(_loginError),
                    Container(
                      constraints: const BoxConstraints(
                        minWidth: 150,
                        maxWidth: 350,
                      ),
                      width: MediaQuery.of(context).size.width * 0.4,
                      child: widgets.submitButton(
                        "LOGIN",
                        _handleLogin,
                      ),
                    ),
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      mainAxisAlignment: MainAxisAlignment.center,
                      crossAxisAlignment: CrossAxisAlignment.center,
                      children: <Widget>[
                        Checkbox(
                          value: _rememberMe,
                          onChanged: _handleRememberMe,
                        ),
                        const Text("Remember Me"),
                      ],
                    ),
                  ],
                ),
              ),
              widgets.getPadding(16),
              widgets.textButton(
                "Not a user? Register Here.",
                _gotoRegister,
              ),
              widgets.textButton(
                "Forgot Password?",
                _gotoForgotPassword,
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _handleLogin() async {
    _loginValid = false;
    setState(() {
      _loginError = '';
    });
    if (_formKey.currentState!.validate()) {
      _formKey.currentState!.save();
      _HandleLogin login = _HandleLogin(
        _curUser as String,
        _curPassword as String,
      );
      String? rslt = await login.authUser();
      if (rslt == 'TEMPORARY') {
        _loginValid = true;
        //   _gotoChangePassword();
      } else if (rslt != null) {
        setState(() {
          _loginError = rslt;
        });
      } else {
        _loginValid = true;
        _saveData();
        _gotoFirstPage();
      }
    }
  }

  String? _validatePassword(String? value) {
    _curPassword = value;
    if (value == null || value.isEmpty) {
      return "Password must not be null";
    }
    return null;
  }

  String? _validateUserName(String? value) {
    _curUser = value;
    if (value == null || value.isEmpty) {
      return "Username must not be null";
    }
    return null;
  }

  void _handleRememberMe(bool? fg) async {
    if (fg == null) return;
    _rememberMe = fg;
    _saveData();
    setState(() {
      _rememberMe = fg;
    });
  }

  void _saveData() {
    SharedPreferences.getInstance().then((prefs) {
      prefs.setBool('remember_me', _rememberMe);
      prefs.setString('uid', _rememberMe ? _userController.text : "");
      prefs.setString('pwd', _rememberMe ? _pwdController.text : "");
    });
  }

  void _loadUserAndPassword() async {
    try {
      SharedPreferences prefs = await SharedPreferences.getInstance();
      var uid = prefs.getString('uid');
      var pwd = prefs.getString('pwd');
      var rem = prefs.getBool('remember_me');
      if (rem != null && rem) {
        setState(() {
          _rememberMe = true;
        });
        _userController.text = uid ?? "";
        _pwdController.text = pwd ?? "";
      }
    } catch (e) {
      _rememberMe = false;
      _userController.text = "";
      _pwdController.text = "";
    }
  }
}

//
//    Class to actually handle loggin in
//

class _HandleLogin {
  String? _curPadding;
  String? _curSession;
  String? _curSalt;
  final String _curPassword;
  final String _curUser;

  _HandleLogin(this._curUser, this._curPassword);

  Future _prelogin() async {
    Map<String, String?> data = {"email": _curUser.toLowerCase()};
    Map<String, dynamic> js = await util.getJson("login", body: data);
    _curPadding = js['code'];
    _curSession = js['session'];
    _curSalt = js['salt'];
    globals.burlSession = _curSession;
  }

  Future<String?> authUser() async {
    if (_curPadding == null) {
      await _prelogin();
    }
    String pwd = _curPassword;
    String usr = _curUser.toLowerCase();
    String pad = _curPadding as String;
    String salt = _curSalt as String;
    String p1 = util.hasher(pwd);
    String p2 = util.hasher(p1 + salt);
    String p3 = util.hasher(p2 + pad);

    var body = {
      'session': _curSession,
      'email': usr,
      'padding': pad,
      'password': p3,
    };
    Map<String, dynamic> jresp = await util.postJson(
      "login",
      body: body,
    );
    if (jresp['status'] == "OK") {
      globals.burlSession = jresp['session'];
      _curSession = jresp['session'];
      var temp = jresp['TEMPORARY'];
      if (temp != null) return "TEMPORARY";
      return null;
    }
    return jresp['message'];
  }
}
