/********************************************************************************/
/*                                                                              */
/*              entrypage.dart                                                  */
/*                                                                              */
/*      Main page for a library                                                 */
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

import "../librarydata.dart";
import "../itemdata.dart";
import '../widgets.dart' as widgets;
import '../util.dart' as util;
import '../globals.dart' as globals;
import 'loginpage.dart';

class BurlEntryWidget extends StatelessWidget {
  final LibraryData _libData;
  final ItemData _itemData;

  const BurlEntryWidget(this._libData, this._itemData, {super.key});

  @override
  Widget build(BuildContext context) {
    return BurlEntryPage(_libData, _itemData);
  }
} // end of class BurlLibraryWidget

class BurlEntryPage extends StatefulWidget {
  final LibraryData _libData;
  final ItemData _itemData;

  const BurlEntryPage(this._libData, this._itemData, {super.key});

  @override
  State<BurlEntryPage> createState() => _BurlEntryPageState();
} // end of class BurlLibraryPage

class _BurlEntryPageState extends State<BurlEntryPage> {
  LibraryData _libData = LibraryData.unknown();
  ItemData _itemData = ItemData.unknown();
  bool _hasChanged = false;
  final Map<String, TextEditingController> _controllers = {};

  _BurlEntryPageState();

  @override
  void initState() {
    _libData = widget._libData;
    _itemData = widget._itemData;
    for (String fld in globals.fieldData.getFieldNames()) {
      TextEditingController ctrl = TextEditingController();
      ctrl.text = _itemData.getField(fld);
    }
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          "View/Edit Library Entry",
          style: const TextStyle(
            fontWeight: FontWeight.bold,
            color: Colors.black,
          ),
        ),
        actions: [widgets.topMenuAction(_getMenuActions())],
      ),
      body: widgets.topLevelPage(context, _getEntryWidget()),
    );
  }

  List<widgets.MenuAction> _getMenuActions() {
    List<widgets.MenuAction> rslt = [];
    if (_canRemove()) {
      rslt.add(
        widgets.MenuAction(
          "Remove entry",
          _removeEntry,
          "Remove this entry from the library.  This can't be undone.",
        ),
      );
    }
    rslt.add(widgets.MenuAction("Log out", _logout, "Log out of BURL"));
    return rslt;
  }

  Widget _getEntryWidget() {
    List<Widget> childs = [];
    for (String fld in globals.fieldData.getFieldNames()) {
      TextEditingController? ctrl = _controllers[fld];
      Widget lbl = Text(globals.fieldData.getLabel(fld));
      childs.add(lbl);
      Widget fldw = widgets.textField(
        controller: ctrl,
        onChanged: (String s) {
          _fieldEdited(fld, s);
        },
      );
      childs.add(fldw);
    }
    Widget w1 = GridView.count(
      crossAxisCount: 2,
      crossAxisSpacing: 4,
      mainAxisSpacing: 2,
      children: childs,
    );

    Widget cancelBtn = widgets.submitButton(
      "Cancel",
      _doCancel,
      tooltip: "Revert changes and go back to libray",
    );
    Widget acceptBtn = widgets.submitButton(
      "Accept",
      _doAccept,
      tooltip: "Save changes (if any) and go back to library",
    );
    Widget revertBtn = widgets.submitButton(
      "Revert",
      _doSave,
      enabled: _hasChanged,
      tooltip: "Save any changes",
    );
    Widget saveBtn = widgets.submitButton(
      "Save",
      _doRevert,
      enabled: _hasChanged,
      tooltip: "Revert any changes",
    );
    Widget w2 = Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [revertBtn, saveBtn, cancelBtn, acceptBtn],
    );

    Widget w3 = Column(
      mainAxisAlignment: MainAxisAlignment.start,
      children: <Widget>[Expanded(child: w1), widgets.fieldSeparator(), w2],
    );

    return w3;
  }

  bool _canRemove() {
    switch (_libData.getUserAccess()) {
      case "ADMIN":
      case "LIBRARIAN":
      case "OWNER":
        return true;
      default:
        return false;
    }
  }

  void _logout() async {
    BuildContext dcontext = context;
    await util.postJsonOnly("/rest/logout");
    globals.burlSession = null;
    if (dcontext.mounted) {
      widgets.gotoDirect(dcontext, BurlLogin());
    }
  }

  void _removeEntry() async {
    BuildContext dcontext = context;
    Map<String, String?> data = {
      "library": _libData.getLibraryId().toString(),
      "entry": _itemData.getId().toString(),
    };
    Map<String, dynamic> rslt = await util.postJson("removeentry", body: data);
    if (rslt["status"] == "OK") {
      if (dcontext.mounted) {
        Navigator.pop(dcontext);
      }
    }
  }

  void _fieldEdited(String fld, String? text) {
    _hasChanged = true;
  }

  Future<void> _doCancel() async {
    BuildContext dcontext = context;
    await _doRevert();
    if (dcontext.mounted) {
      Navigator.pop(dcontext);
    }
  }

  Future<void> _doRevert() async {
    for (String fld in globals.fieldData.getFieldNames()) {
      TextEditingController? ctrl = _controllers[fld];
      ctrl!.text = _itemData.getField(fld);
    }
  }

  Future<void> _doSave() async {
    Map<String, dynamic> edits = {};
    for (String fld in globals.fieldData.getFieldNames()) {
      TextEditingController? ctrl = _controllers[fld];
      if (ctrl != null) {
        String oldv = _itemData.getField(fld);
        String newv = ctrl.text;
        if (oldv != newv) {
          edits[fld] = newv;
        }
      }
    }
    if (edits.isNotEmpty) {
      Map<String, String?> data = {
        "library": _libData.getLibraryId().toString(),
        "entry": _itemData.getId().toString(),
        "edits": edits.toString(),
      };
      Map<String, dynamic> rslt = await util.postJson("editentry", body: data);
      if (rslt["status"] == "OK") {
        Map<String, dynamic> data = rslt["entry"];
        _itemData.reload(data);
      }
    }
    _hasChanged = false;
  }

  Future<void> _doAccept() async {
    BuildContext dcontext = context;
    if (_hasChanged) {
      await _doSave();
    }
    if (dcontext.mounted) {
      Navigator.pop(dcontext);
    }
  }
} // end of class _BurlEntryPageState
