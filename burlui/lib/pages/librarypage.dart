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
import '../globals.dart' as globals;
import 'loginpage.dart';
import 'entrypage.dart';

const String defaultSort = "<DEFAULT>";

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
  State<BurlLibraryPage> createState() => _BurlLibraryPageState();
} // end of class BurlLibraryPage

class _BurlLibraryPageState extends State<BurlLibraryPage> {
  LibraryData _libData = LibraryData.unknown();
  final TextEditingController _findControl = TextEditingController();
  final List<ItemData> _itemList = [];
  int _numItems = -1;
  String? _iterId;
  bool _isDone = false;
  int _maxRead = 0;
  double _scrollext = 0;
  final List<String> _sortFields = [];
  final ScrollController _scrollController = ScrollController();
  String? _sortOn;
  bool _sortInvert = false;

  _BurlLibraryPageState();

  @override
  void initState() {
    _libData = widget._libData;
    _scrollController.addListener(_loadMore);
    _getSortFields();
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
          future:
              (_numItems < 0 ? _fetchInitialData() : _fetchMoreData()),
          builder: (
            BuildContext ctx,
            AsyncSnapshot<List<ItemData>> snapshot,
          ) {
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
          textInputAction: TextInputAction.done,
          onEditingComplete: _handleSearch,
        ),
        widgets.fieldSeparator(),
        Row(
          mainAxisAlignment: MainAxisAlignment.start,
          children: <Widget>[
            const Text("Sort by: "),
            widgets.dropDown(
              _sortFields,
              value: defaultSort,
              onChanged: _changeSort,
              tooltip: "Select the field to sort the results on",
            ),
            widgets.textButton(
              (_sortInvert ? "Inverse Order" : "Normal Order"),
              _changeSortOrder,
              tooltip: "Push to invert the sort order",
            ),
          ],
        ),
        widgets.fieldSeparator(),
        Expanded(
          child: ListView.separated(
            itemCount: _numItems,
            itemBuilder: _getItemTile,
            separatorBuilder: _getItemSeparator,
            controller: _scrollController,
          ),
        ),
      ],
    );
    return w;
  }

  List<widgets.MenuAction> _getMenuActions() {
    List<widgets.MenuAction> rslt = [];
    if (_canAddToLibrary()) {
      rslt.add(
        widgets.MenuAction(
          "Add Items by ISBN/LCCN",
          _addBooks,
          "Add one or more books or items given a file or ISBN/LCCN numbers. "
              "Note this done in background and may take a while.",
        ),
      );
      rslt.add(
        widgets.MenuAction(
          "Add Single Item Manually (Initially Empty)",
          _addManualItem,
          "Add a new book or item manually",
        ),
      );
    }
    rslt.add(
      widgets.MenuAction(
        "Refresh",
        _refreshList,
        "Refresh this list of items",
      ),
    );
    if (_canPrintLabels()) {
      rslt.add(
        widgets.MenuAction(
          "Print Labels",
          _printLabels,
          "Print labels for those items that need them",
        ),
      );
    }
    if (_canAddUsers()) {
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

  bool _canAddToLibrary() {
    switch (_libData.getUserAccess()) {
      case "ADMIN":
      case "OWNER":
      case "LIBRARIAN":
        return true;
      default:
        return false;
    }
  }

  bool _canAddUsers() {
    switch (_libData.getUserAccess()) {
      case "ADMIN":
      case "OWNER":
        return true;
      default:
        return false;
    }
  }

  bool _canPrintLabels() {
    switch (_libData.getUserAccess()) {
      case "NONE":
      case "VIEWER":
      case "EDITOR":
        return false;
      default:
        return true;
    }
  }

  void _addBooks() async {
    // show add dialog
    // get file or list of items to add, call add
  }

  void _addManualItem() async {
    Map<String, String?> data = {
      "library": _libData.getLibraryId().toString(),
    };
    Map<String, dynamic> rslt = await util.postJson(
      "addentry",
      body: data,
    );
    if (rslt["status"] == "OK") {
      // take entityid from the result and go to entry page for it
    }
  }

  void _refreshList() async {
    await _fetchInitialData();
    setState(() {});
  }

  void _addUser() async {
    // provide a dialog to specify user email and the permissions
  }

  void _detachLibrary() async {
    // ask user if they are sure
    BuildContext dcontext = context;
    Map<String, String?> data = {
      "library": _libData.getLibraryId().toString(),
    };
    data["email"] = "*";
    data["access"] = "NONE";
    Map<String, dynamic> rslt = await util.postJson(
      "addentry",
      body: data,
    );
    if (rslt["status"] == "OK") {
      if (dcontext.mounted) {
        Navigator.pop(dcontext);
      }
    }
  }

  void _deleteLibrary() async {
    // ask user if they are sure
    BuildContext dcontext = context;
    Map<String, String?> data = {};
    data["library"] = _libData.getLibraryId().toString();
    Map<String, dynamic> rslt = await util.postJson(
      "removelibrary",
      body: data,
    );
    if (rslt["status"] == "OK") {
      if (dcontext.mounted) {
        Navigator.pop(dcontext);
      }
    }
  }

  void _changePassword() async {
    // bring up change password dialog
  }

  void _printLabels() async {
    // dialog to get file name to store result
    // call print labels to get result file
    // save result file
  }

  void _logout() async {
    BuildContext dcontext = context;
    await util.postJsonOnly("/rest/logout");
    globals.burlSession = null;
    if (dcontext.mounted) {
      widgets.gotoDirect(dcontext, BurlLogin());
    }
  }

  void _handleSearch() async {
    await _fetchInitialData();
    setState(() {});
  }

  void _changeSort(String? on) async {
    if (on == "<Default>") {
      _sortOn = null;
    } else {
      _sortOn = on;
    }
    await _fetchInitialData();
    setState(() {});
  }

  void _changeSortOrder() async {
    _sortInvert = !_sortInvert;
    await _fetchInitialData();
    setState(() {});
  }

  void _loadMore() {
    if (_scrollController.position.pixels > _scrollext) {
      _scrollext = _scrollController.position.pixels;
      setState(() {});
    }
  }

  void _getSortFields() {
    _sortFields.add(defaultSort);
    for (String fn in globals.fieldData.getFieldNames()) {
      if (globals.fieldData.isSortable(fn)) {
        _sortFields.add(fn);
      }
    }
  }

  Future<List<ItemData>>? _fetchInitialData() async {
    // do a find on the library, save the iterator id, set the first elements
    // also set _isdone to true if returned iterator id is null
    // this should also be called by _handleSearch
    if (_iterId != null) {
      Map<String, String?> dd = {
        "library": _libData.getLibraryId().toString(),
        "count": "-1",
        "filterid": _iterId,
      };
      await util.postJson("entries", body: dd);
    }
    _isDone = false;
    _iterId = null;
    _itemList.clear();
    _numItems = 0;
    _maxRead = 0;
    _scrollController.jumpTo(_scrollController.initialScrollOffset);
    Map<String, String?> data = {
      "library": _libData.getLibraryId().toString(),
      "count": "20",
    };
    if (_findControl.text.isNotEmpty) {
      data["filter"] = _findControl.text;
    }
    if (_sortOn != null) {
      data["orderby"] = _sortOn;
    }
    if (_sortInvert) {
      data["invert"] = "true";
    }

    Map<String, dynamic> rslts = await util.postJson(
      "entries",
      body: data,
    );
    if (rslts["status"] == "OK") {
      List<dynamic> items = rslts["data"];
      for (Map<String, dynamic> item in items) {
        ItemData id = ItemData(item);
        _itemList.add(id);
      }
      _iterId = rslts["filterid"];
      _numItems = rslts["count"];
      if (_iterId != null) _isDone = false;
    }

    return _itemList;
  }

  Future<List<ItemData>> _fetchMoreData() async {
    if (_maxRead < _itemList.length) return _itemList;

    while (_maxRead >= _itemList.length &&
        _maxRead < _numItems &&
        !_isDone) {
      if (_iterId == null) break;
      Map<String, String?> data = {
        "library": _libData.getLibraryId().toString(),
        "filterid": _iterId,
        "count": "20",
      };

      Map<String, dynamic> rslts = await util.postJson(
        "entries",
        body: data,
      );
      if (rslts["status"] == "OK") {
        List<dynamic> items = rslts["data"];
        _numItems = rslts["count"];
        for (Map<String, dynamic> item in items) {
          ItemData id = ItemData(item);
          _itemList.add(id);
        }
        _iterId = rslts["filterid"];
        if (_iterId != null) _isDone = false;
        continue;
      } else {
        break;
      }
    }

    return _itemList;
  }

  Widget? _getItemTile(BuildContext ctx, int index) {
    if (index < 0 || index >= _numItems) return null;

    if (index >= _itemList.length && !_isDone) {
      if (index > _maxRead) {
        _maxRead = index;
      }
      return const Text("Loading...");
    }

    if (index >= _itemList.length) {
      return null;
    }

    ItemData id = _itemList[index];
    String lcc = id.getField("LCC");
    String ttl = id.getField("Title");
    String aut = id.getField("Primary");
    String isbn = id.getField("ISBN");
    if (aut.isEmpty) {
      aut = id.getField("Authors");
    }
    int idx = id.getId();

    // if lcc doesn't end with date, then add last part of imprint to lcc

    String txt = "";
    if (ttl.contains("/")) {
      txt = ttl.replaceAll("/", "\n");
    } else {
      txt = "$ttl\n$aut";
    }
    txt = "$txt\n$isbn";
    Widget w = Row(
      mainAxisAlignment: MainAxisAlignment.start,
      children: <Widget>[
        SizedBox(
          width: 100.0,
          child: Text(lcc, overflow: TextOverflow.visible),
        ),
        const SizedBox(width: 3),
        _coverWidget(id),
        const SizedBox(width: 3),
        Expanded(child: Text(txt, maxLines: 4)),
      ],
    );
    Widget w1 = GestureDetector(
      key: Key("Item $idx"),
      onTap: () {
        _handleSelect(index);
      },
      child: w,
    );

    return w1;
  }

  Widget _getItemSeparator(BuildContext ctx, int index) {
    return Divider(height: 1, thickness: 1, color: Colors.black);
  }

  Widget _coverWidget(ItemData id) {
    String isbn = id.getField("ISBN");
    String lccn = id.getField("LCCN");
    String oclc = id.getField("OCLC");
    String sfx = "";
    if (isbn.isNotEmpty) {
      sfx = "/isbn/$isbn";
    } else if (lccn.isNotEmpty) {
      sfx = "/lccn/$lccn";
    } else if (oclc.isNotEmpty) {
      sfx = "/oclc/$oclc";
    }

    if (sfx.isNotEmpty) {
      String url = "https://covers.openlibrary.org/b$sfx-S.jpg";
      Widget child = Image.network(
        url,
        errorBuilder: _badImage,
        loadingBuilder: _loadingImage,
        height: 60,
      );
      return child;
    } else {
      Widget child = SizedBox(width: 32);
      return child;
    }
  }

  Widget _badImage(
    BuildContext context,
    Object exception,
    StackTrace? st,
  ) {
    return const Text('ð¢');
  }

  Widget _loadingImage(
    BuildContext context,
    Widget child,
    ImageChunkEvent? progress,
  ) {
    if (progress == null) {
      // if size is 0, then return null image
      return child;
    }
    return const Text('ð¢');
  }

  void _handleSelect(int index) async {
    await widgets.gotoThen(
      context,
      BurlEntryWidget(_libData, _itemList[index]),
    );
    setState(() {});
  }
} // end of class _BurlPageState
