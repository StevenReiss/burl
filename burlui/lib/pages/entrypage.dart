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
import 'dart:convert';

/********************************************************************************/
/*                                                                              */
/*      Top level widgets                                                       */
/*                                                                              */
/********************************************************************************/

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

/********************************************************************************/
/*                                                                              */
/*      Entry page state: the actual worker                                     */
/*                                                                              */
/********************************************************************************/

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
    _controllers.clear();
    _resetFields();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: widgets.largeBoldText(
          "View/Edit Library Entry (${_itemData.getId()})",
          scaler: 1.1,
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
    List<TableRow> rows = [];
    String acc = _libData.getUserAccess();
    for (String fld in globals.fieldData.getFieldNames()) {
      Widget lbl = Text(globals.fieldData.getLabel(fld));
      Widget fldw = _getFieldWidget(fld, acc);
      TableRow row = TableRow(children: <Widget>[lbl, fldw]);
      rows.add(row);
      TableRow spacer = TableRow(
        children: [SizedBox(height: 1), SizedBox(height: 1)],
      );
      rows.add(spacer);
    }
    Map<int, TableColumnWidth> widths = {
      0: IntrinsicColumnWidth(),
      1: FlexColumnWidth(),
    };
    Widget w1 = Table(
      children: rows,
      columnWidths: widths,
      defaultVerticalAlignment: TableCellVerticalAlignment.middle,
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
      _doRevert,
      enabled: _hasChanged,
      tooltip: "Save any changes",
    );
    Widget saveBtn = widgets.submitButton(
      "Save",
      _doSave,
      enabled: _hasChanged,
      tooltip: "Revert any changes",
    );
    Widget w2 = Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [revertBtn, saveBtn, cancelBtn, acceptBtn],
    );

    Widget w3 = Column(
      mainAxisAlignment: MainAxisAlignment.start,
      children: <Widget>[w1, widgets.fieldSeparator(), w2],
    );

    return w3;
  }

  Widget _getFieldWidget(String fld, String acc) {
    String? disp = globals.fieldData.getDisplay(fld);
    TextEditingController? ctrl = _controllers[fld];
    bool en = globals.fieldData.canEdit(acc, fld);
    if (disp == 'YES_NO' && en) {
      Widget fldw = widgets.textField(
        controller: ctrl,
        enabled: false,
        maxLines: 0,
        collapse: true,
      );
      bool value = (ctrl?.text == 'yes');
      Widget toggle = widgets.booleanField(
        value: value,
        onChanged: (bool? fg) {
          _updateYesNo(fg, ctrl);
        },
      );
      Widget w1 = Row(
        mainAxisAlignment: MainAxisAlignment.start,
        children: <Widget>[toggle, Expanded(child: fldw)],
      );
      return w1;
    } else {
      Widget fldw = widgets.textField(
        controller: ctrl,
        enabled: en,
        maxLines: 0,
        onChanged: (String s) {
          _fieldEdited(fld, s);
        },
        collapse: true,
      );
      return fldw;
    }
  }

  void _updateYesNo(bool? fg, TextEditingController? ctrl) {
    if (fg == null) return;
    String val = (fg ? "yes" : "no");
    ctrl?.text = val;
    setState(() {
      _hasChanged = true;
    });
  }

  bool _canRemove() {
    switch (_libData.getUserAccess()) {
      case "LIBRARIAN":
      case "OWNER":
        return true;
      default:
        return false;
    }
  }

  void _logout() async {
    BuildContext dcontext = context;
    await util.postJsonOnly("logout");
    globals.burlSession = null;
    if (dcontext.mounted) {
      widgets.gotoDirect(dcontext, BurlLogin());
    }
  }

  void _removeEntry() async {
    BuildContext dcontext = context;
    bool confirm = await widgets.getValidation(
      context,
      "Do You Really want to delete this item",
    );
    if (!confirm) return;
    Map<String, String?> data = {
      "library": _libData.getLibraryId().toString(),
      "entry": _itemData.getId().toString(),
    };
    Map<String, dynamic> rslt = await util.postJson(
      "removeentry",
      body: data,
    );
    if (rslt["status"] == "OK") {
      if (dcontext.mounted) {
        Navigator.pop(dcontext, "OK");
      }
    }
  }

  void _fieldEdited(String fld, String? text) {
    setState(() {
      _hasChanged = true;
    });
  }

  Future<void> _doCancel() async {
    BuildContext dcontext = context;
    await _revertEdits();
    if (dcontext.mounted) {
      Navigator.pop(dcontext);
    }
  }

  Future<void> _doRevert() async {
    await _revertEdits();
    setState(() {
      _hasChanged = false;
    });
  }

  Future<void> _revertEdits() async {
    for (String fld in globals.fieldData.getFieldNames()) {
      TextEditingController? ctrl = _controllers[fld];
      ctrl!.text = _itemData.getField(fld);
    }
  }

  void _resetFields() {
    for (String fld in globals.fieldData.getFieldNames()) {
      TextEditingController ctrl = TextEditingController();
      String t2 = _itemData.getMultiField(fld);
      // List<int> runes = t1.runes.toList();
      // String t2 = utf8.decode(runes);
      ctrl.text = t2;
      _controllers[fld] = ctrl;
    }
  }

  Future<void> _saveEdits() async {
    Map<String, dynamic> edits = {};
    for (String fld in globals.fieldData.getFieldNames()) {
      TextEditingController? ctrl = _controllers[fld];
      if (ctrl != null) {
        String oldv = _itemData.getField(fld);
        String newv = ctrl.text;
        if (globals.fieldData.isViewMultiple(fld)) {
          newv = newv.replaceAll("\n", " | ");
        }
        if (oldv != newv) {
          edits[fld] = newv;
        }
      }
    }
    if (edits.isNotEmpty) {
      Map<String, String?> data = {
        "library": _libData.getLibraryId().toString(),
        "entry": _itemData.getId().toString(),
        "edits": json.encode(edits),
      };
      Map<String, dynamic> rslt = await util.postJson(
        "editentry",
        body: data,
      );
      if (rslt["status"] == "OK") {
        Map<String, dynamic> data = rslt["entry"];
        _itemData.reload(data);
        setState(() {});
      }
    }
  }

  Future<void> _doSave() async {
    await _saveEdits();
    _resetFields();
    setState(() {
      _hasChanged = false;
    });
  }

  Future<void> _doAccept() async {
    BuildContext dcontext = context;
    if (_hasChanged) {
      await _saveEdits();
    }
    if (dcontext.mounted) {
      Navigator.pop(dcontext, "OK");
    }
  }
} // end of class _BurlEntryPageState
