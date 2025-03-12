/********************************************************************************/
/*                                                                              */
/*              homepage.dart                                                   */
/*                                                                              */
/*      Home page for BURL: show user available libraries                       */
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

import '../util.dart' as util;
import '../widgets.dart' as widgets;
import '../globals.dart' as globals;
import '../librarydata.dart';
import 'package:flutter/material.dart';
import 'loginpage.dart';
import 'librarypage.dart';
import 'createlibrarydialog.dart' as createlibrary;
import '../lookandfeel.dart' as laf;

class BurlHomeWidget extends StatelessWidget {
  final bool _initial;

  const BurlHomeWidget(this._initial, {super.key});

  @override
  Widget build(BuildContext context) {
    return BurlHomePage(_initial);
  }
} // end of class BurlHomeWidget

class BurlHomePage extends StatefulWidget {
  final bool _initial;

  const BurlHomePage(this._initial, {super.key});

  @override
  State<BurlHomePage> createState() => _BurlHomePageState();
}

class _BurlHomePageState extends State<BurlHomePage> {
  List<LibraryData> _libraryData = [];
  bool _haveData = false;
  bool _initial = false;

  @override
  void initState() {
    _initial = widget._initial;
    _getLibraries();
    super.initState();
  }

  Future _getLibraries() async {
    _libraryData = await getLibraries();
    _haveData = true;
    LibraryData? sd0 = _libraryData.singleOrNull;
    if (_initial && sd0 != null) {
      _initial = false;
      await _gotoLibraryPage(sd0);
    } else {
      setState(() {});
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        flexibleSpace: const Image(
          image: AssetImage('assets/images/burllogo.png'),
          fit: BoxFit.contain,
        ),
        actions: [
          widgets.topMenuAction(<widgets.MenuAction>[
            widgets.MenuAction(
              "Create New Library",
              _gotoCreateLibrary,
              "Create a new library",
            ),
            widgets.MenuAction(
              "Unenroll from Burl",
              _removeUserAction,
              "Delete you Burl account and all associated information.  "
                  "This cannot be undone",
            ),
            widgets.MenuAction(
              "Log Out",
              _logoutAction,
              "Log out from Burl",
            ),
          ]),
        ],
      ),
      body: widgets.topLevelPage(context, _libraryListWidget(), true),
    );
  }

  Widget _libraryListWidget() {
    if (_haveData) {
      Widget w1 = ListView.builder(
        padding: const EdgeInsets.all(10.0),
        itemCount: _libraryData.length,
        itemBuilder: _getTile,
      );
      Widget w2 = Column(
        mainAxisAlignment: MainAxisAlignment.start,
        children: <Widget>[
          Text("YOUR LIBRARIES:", textScaler: TextScaler.linear(2.0)),
          Divider(
            height: 8,
            thickness: 8,
            color: laf.topLevelBackground,
          ),
          Expanded(child: w1),
        ],
      );
      return w2;
    } else {
      return widgets.circularProgressIndicator();
    }
  }

  ListTile _getTile(context, int i) {
    LibraryData ld = _libraryData[i];
    return ListTile(
      title: Text(
        ld.getName(),
        style: const TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.bold,
        ),
      ),
      subtitle: Text(
        ld.getOwner(),
        style: const TextStyle(fontSize: 14),
      ),
      trailing: Text("Access: ${ld.getUserAccess()}"),
      onTap: () => {_gotoLibraryPage(ld)},
    );
  }

  Future<void> _removeUserAction() async {
    await _handleRemoveUser().then(_gotoLogin);
  }

  Future<void> _logoutAction() async {
    await _handleLogout().then(_gotoLogin);
  }

  Future _gotoLibraryPage(LibraryData sd) async {
    await widgets.gotoThen(context, BurlLibraryWidget(sd));
    _libraryData = await getLibraries();
    setState(() {});
  }

  dynamic _gotoLogin(bool fg) {
    if (!fg) return;
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const BurlLogin()),
    );
  }

  Future<bool> _handleLogout() async {
    BuildContext dcontext = context;
    await util.postJsonOnly("logout");
    globals.burlSession = null;
    if (dcontext.mounted) {
      widgets.gotoDirect(dcontext, BurlLogin());
    }
    return true;
  }

  dynamic _gotoCreateLibrary() async {
    await createlibrary.createLibraryDialog(context, _libraryData);
    setState(() {});
  }

  Future<bool> _handleRemoveUser() async {
    String msg =
        "Thank you for trying Burl. We are sorry to see you go.\n";
    msg +=
        "If you really meant to leave, then click YES.  If this was a ";
    msg += "mistake then click NO";

    bool fg = await widgets.getValidation(context, msg);
    if (!fg) return false;

    Map<String, dynamic> js = await util.postJson("removeuser");
    if (js['status'] == 'OK') {
      globals.burlSession = null;
      fg = true;
    } else {
      fg = false;
    }
    return fg;
  }
}

Future<List<LibraryData>> getLibraries() async {
  Map<String, dynamic> js = await util.postJson("findlibraries");
  var rslt = <LibraryData>[];
  if (js['status'] == 'OK') {
    var jsd = js['libs'];
    for (final sd1 in jsd) {
      LibraryData sd = LibraryData(sd1);
      rslt.add(sd);
    }
  }
  return rslt;
}
