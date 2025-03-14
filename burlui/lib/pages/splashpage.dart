/********************************************************************************/
/*                                                                              */
/*              splashpage.dart                                                 */
/*                                                                              */
/*      Initial splash page                                                     */
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

import 'package:burlui/widgets.dart' as widgets;
import 'package:flutter/material.dart';
import 'loginpage.dart' as login;
import 'homepage.dart' as home;
import '../globals.dart' as globals;

String _curStep = "";

class SplashPage extends StatelessWidget {
  const SplashPage({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'BURL Start...',
      theme: widgets.getTheme(),
      home: const SplashWidget(),
    );
  }
}

class SplashWidget extends StatefulWidget {
  const SplashWidget({super.key});

  @override
  State<SplashWidget> createState() => _SplashWidgetState();
}

class _SplashWidgetState extends State<SplashWidget> {
  _SplashWidgetState();

  @override
  void initState() {
    super.initState();
    splashTasks();
  }

  void setStep(String step) {
    setState(() {
      _curStep = step;
    });
  }

  void splashTasks() async {
    setStep("Loading field information...");
    await globals.fieldData.loadData();

    setStep("Checking for saved login...");
    bool value = await login.testLogin();
    if (!value) {
      if (mounted) {
        widgets.gotoDirect(context, const login.BurlLoginWidget());
      }
    } else {
      if (mounted) {
        widgets.gotoDirect(context, const home.BurlHomeWidget(true));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("")),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Image.asset(
              "assets/images/burllogo.png",
              width: MediaQuery.of(context).size.width * 0.6,
              height: MediaQuery.of(context).size.height * 0.6,
            ),
            widgets.fieldSeparator(),
            widgets.largeBoldText(
              _curStep,
              textAlign: TextAlign.center,
              scaler: 1.25,
            ),
          ],
        ),
      ),
    );
  }
}
