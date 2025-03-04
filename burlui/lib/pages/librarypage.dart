/********************************************************************************/
/*                                                                              */
/*              librarypage.dart                                                */
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

class BurlLibraryWidget extends StatelessWidget {
  final LibraryData _libData;

  const BurlLibraryWidget(this._libData, {super.key});

  @override
  Widget build(BuildContext context) {
    return BurlLibraryPage(_libData);
  }
} // end of class BurlLibraryWidget

class BurlLibraryPage extends StatefulWidget {
  final LibraryData _libData;

  const BurlLibraryPage(this._libData, {super.key});

  @override
  State<BurlLibraryPage> createState() => _BurlPageState();
} // end of class BurlLibraryPage

class _BurlPageState extends State<BurlLibraryPage> {
  LibraryData _libData = LibraryData.unknown();
  final TextEditingController _findControl = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final List<ItemData> _itemList = [];
  String? _iterId;
  bool _isDone = false;

  @override
  void initState() {
    _libData = widget._libData;
    _scrollController.addListener(_loadMore);
    super.initState();
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          _libData.getName(),
          style: const TextStyle(
            fontWeight: FontWeight.bold,
            color: Colors.black,
          ),
        ),
        actions: [widgets.topMenuAction(_getMenuActions())],
      ),
      body: widgets.topLevelNSPage(
        context,
        FutureBuilder<List<ItemData>>(
          future: _fetchInitialData(),
          builder: (BuildContext ctx, AsyncSnapshot<List<ItemData>> snapshot) {
            if (snapshot.hasError) {
              return Center(child: Text("Error: ${snapshot.error}"));
            } else if (!snapshot.hasData) {
              return Center(child: CircularProgressIndicator());
            } else {
              return _getPageWidget();
            }
          },
        ),
      ),
    );
  }

  Widget _getPageWidget() {
    Widget w = Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: <Widget>[
        widgets.fieldSeparator(),
        widgets.textFormField(
          hint: "Search Terms for items",
          label: "Search Terms",
          controller: _findControl,
          onEditingComplete: _handleSearch,
        ),
        widgets.fieldSeparator(),
        ListView.builder(
          controller: _scrollController,
          itemCount: _itemList.length,
          itemBuilder: _getItemTile,
        ),
      ],
    );
    return w;
  }

  List<widgets.MenuAction> _getMenuActions() {
    List<widgets.MenuAction> rslt = [];
    rslt.add(
      widgets.MenuAction(
        "Add Items to Library",
        _addBooks,
        "Add one or more books or items given ISBN/LCCN. "
            "Note this done in background and may take a while.",
      ),
    );
    rslt.add(
      widgets.MenuAction(
        "Add Item Manually",
        _addManualItem,
        "Add a new book or item manually",
      ),
    );
    rslt.add(
      widgets.MenuAction("Refresh", _refreshList, "Refresh this list of items"),
    );
    if (_libData.getUserAccess() == 'OWNER' ||
        _libData.getUserAccess() == "ADMIN") {
      rslt.add(
        widgets.MenuAction(
          "Add User",
          _addUser,
          "Add or remove a user by email for this library",
        ),
      );
    }
    if (_libData.getUserAccess() != 'OWNER') {
      rslt.add(
        widgets.MenuAction(
          "Detach Library",
          _detachLibrary,
          "Remove this library from your set of accessible libraries",
        ),
      );
    } else {
      rslt.add(
        widgets.MenuAction(
          "Delete Library",
          _deleteLibrary,
          "Remove this library completely",
        ),
      );
    }
    rslt.add(
      widgets.MenuAction(
        "Change Password",
        _changePassword,
        "Change your login password",
      ),
    );
    rslt.add(widgets.MenuAction("Log out", _logout, "Log out of BURL"));

    return rslt;
  }

  void _addBooks() async {
    // get file or list of items to add, call add
  }

  void _addManualItem() async {
    // Add empty entry, then go to the page for that entry
  }

  void _refreshList() async {
    // update the list of items being displayed
  }

  void _addUser() async {
    // provide a dialog to specify user email and the permissions
  }

  void _detachLibrary() async {
    // change permissions for yourself on this library to NONE
  }

  void _deleteLibrary() async {
    // remove the library
  }

  void _changePassword() async {
    // bring up change password dialog
  }

  void _logout() async {
    // handle logout, go to login page
  }

  void _handleSearch() async {
    await _fetchInitialData();
    setState(() {});
  }

  Future<List<ItemData>>? _fetchInitialData() async {
    // do a find on the library, save the iterator id, set the first elements
    // also set _isdone to true if returned iterator id is null
    // this should also be called by _handleSearch
    if (_iterId != null) {
      Map<String, dynamic> dd = {
        "library": _libData.getLibraryId(),
        "count": -1,
        "filterid": _iterId,
      };
      await util.postJson("entries", body: dd);
    }
    _isDone = false;
    _iterId = null;
    _itemList.clear();
    Map<String, dynamic> data = {
      "library": _libData.getLibraryId(),
      "count": 20,
    };
    if (_findControl.text.isNotEmpty) {
      data["filterstr"] = _findControl.text;
    }
    Map<String, dynamic> rslts = await util.postJson("entries", body: data);
    if (rslts["status"] == "OK") {
      List<Map<String, dynamic>> items = rslts["data"];
      for (Map<String, dynamic> item in items) {
        ItemData id = ItemData(item);
        _itemList.add(id);
      }
      _iterId = rslts["filterid"];
      if (_iterId != null) _isDone = false;
    }
    return _itemList;
  }

  Future<List<ItemData>>? _fetchMoreData() async {
    if (_iterId == null) return [];
    Map<String, dynamic> data = {
      "library": _libData.getLibraryId(),
      "filterid": _iterId,
      "count": 20,
    };
    Map<String, dynamic> rslts = await util.postJson("entries", body: data);
    if (rslts["status"] == "OK") {
      List<Map<String, dynamic>> items = rslts["data"];
      for (Map<String, dynamic> item in items) {
        ItemData id = ItemData(item);
        _itemList.add(id);
      }
      _iterId = rslts["filterid"];
      if (_iterId != null) _isDone = false;
    }

    return _itemList;
  }

  void _loadMore() {
    if (_scrollController.position.pixels ==
        _scrollController.position.maxScrollExtent) {
      _fetchMoreData();
    }
    // handel getting more entries and adding to _itemList
  }

  ListTile _getItemTile(BuildContext ctx, int index) {
    return ListTile();
  }
} // end of class _BurlPageState
