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
import 'changepassworddialog.dart';
import 'addentriesdialog.dart';
import 'adduserdialog.dart';
import 'printlabelsdialog.dart';
import 'exportdialog.dart';
import 'importdialog.dart';
import 'dart:async';

const String defaultSort = "Order Added";
const bool selectClickAnywhere = false;

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
  final ScrollController _scrollController = ScrollController(
    keepScrollOffset: false,
  );
  String? _sortOn;
  bool _sortInvert = false;
  bool _doingFetch = false;
  String? _selectModeField;
  Set<int> _selectedItems = {};
  int _lastSelect = -1;
  bool _lastSelectState = false;
  bool _haveChanges = false;
  final TextEditingController _selectValueControl =
      TextEditingController();

  _BurlLibraryPageState();

  @override
  void initState() {
    _libData = widget._libData;
    _scrollController.addListener(_loadMore);
    _getSortFields();
    _initializeSelection();
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
        title: widgets.largeBoldText(_libData.getName(), scaler: 1.25),
        leading:
            _selectModeField != null
                ? widgets.tooltipWidget(
                  "Press to go back to library page.  Long press to "
                  "clear selection and go back.",
                  IconButton(
                    icon: const Icon(Icons.close),
                    onPressed: _checkEndSelectionMode,
                    onLongPress: _endClearSelectionMode,
                  ),
                )
                : const SizedBox(),
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
    String sorton = defaultSort;
    String? son = _sortOn;
    if (son != null) sorton = son;
    Widget list = ListView.separated(
      itemCount: _numItems,
      itemBuilder: _getItemTile,
      separatorBuilder: _getItemSeparator,
      controller: _scrollController,
    );
    Widget l1 = Scrollbar(
      trackVisibility: true,
      thumbVisibility: true,
      controller: _scrollController,
      child: list,
    );
    list = l1;

    if (_numItems == 0) {
      list = const Text("No Results Found");
    }
    String count = "$_numItems Entries";

    Widget toprow = Row(
      mainAxisAlignment: MainAxisAlignment.start,
      children: <Widget>[
        const Text("Sort by: "),
        widgets.dropDown(
          _sortFields,
          value: sorton,
          onChanged: _changeSort,
          tooltip: "Select the field to sort the results on",
        ),
        widgets.textButton(
          (_sortInvert ? "Inverse Order" : "Normal Order"),
          _changeSortOrder,
          tooltip: "Push to invert the sort order",
        ),
        Expanded(
          child: Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: <Widget>[Text(count)],
          ),
        ),
      ],
    );
    Widget? editrow;
    if (_selectModeField != null) {
      editrow = Row(
        mainAxisAlignment: MainAxisAlignment.start,
        children: <Widget>[
          Text("New $_selectModeField Value:  "),
          Expanded(
            child: widgets.textField(
              controller: _selectValueControl,
              enabled: true,
              maxLines: 0,
              textInputAction: TextInputAction.next,
              collapse: true,
              onChanged: _selectValueChanged,
            ),
          ),
          widgets.submitButton(
            "Update",
            () {
              _selectEdit(_selectModeField!);
            },
            enabled: _canSelectEdit(),
            tooltip: "Change the value of select items",
          ),
          SizedBox(),
          widgets.submitButton(
            "Clear Selections",
            _selectNone,
            enabled: _selectedItems.isNotEmpty,
            tooltip: "Remove all selections",
          ),
          Expanded(child: SizedBox()),
        ],
      );
      // add space for new value and set button
    }
    Widget w = Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: <Widget>[
        widgets.fieldSeparator(),
        _getSearchBox(),
        widgets.fieldSeparator(),
        toprow,
        if (editrow != null) widgets.fieldSeparator(),
        if (editrow != null) editrow,
        widgets.fieldDivider(),
        widgets.fieldSeparator(),
        Expanded(child: list),
      ],
    );
    return w;
  }

  Widget _getSearchBox() {
    return widgets.textFormField(
      hint: "Search Terms for items",
      label: "Search Terms",
      controller: _findControl,
      textInputAction: TextInputAction.done,
      onEditingComplete: _handleSearch,
      suffixIcon: IconButton(
        icon: Icon(Icons.clear),
        onPressed: _clearSearch,
      ),
    );
  }

  List<widgets.MenuAction> _getMenuActions() {
    List<widgets.MenuAction> rslt = [];
    if (_selectModeField != null) {
      rslt.add(
        widgets.MenuAction(
          "Select All",
          _selectAll,
          "Select all items",
        ),
      );
      rslt.add(
        widgets.MenuAction(
          "Clear Selection",
          _selectNone,
          "Clear all selections",
        ),
      );
      rslt.add(
        widgets.MenuAction(
          "Exit '$_selectModeField' group edit",
          _endSelectionMode,
          "Exit Selection mode for $_selectModeField",
        ),
      );
      return rslt;
    }
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
          "Add Single Item Manually",
          _addManualItem,
          "Add a new book or item manually.  This starts out as an empty item.",
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
    if (_canExport()) {
      rslt.add(
        widgets.MenuAction(
          "Export Library as CSV",
          _exportAll,
          "Export the whole library as a CSV file",
        ),
      );
      rslt.add(
        widgets.MenuAction(
          "Export Current Display as CSV",
          _exportDisplay,
          "Export the currently selected and sorted items as CSV",
        ),
      );
    }
    if (_canAddToLibrary()) {
      rslt.add(
        widgets.MenuAction(
          "Import Entries from CSV File",
          _importEntries,
          "Import new or changed values from a CSV file",
        ),
      );
    }

    String acc = _libData.getUserAccess();
    for (String fld in globals.fieldData.getFieldNames()) {
      if (globals.fieldData.isGroupEdit(fld) &&
          globals.fieldData.canEdit(acc, fld)) {
        rslt.add(
          widgets.MenuAction(
            "Group Edit '$fld' field",
            () => _startSelectionMode(fld),
            "Select a set of items and group change the value of $fld",
          ),
        );
      }
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
      case "OWNER":
      case "LIBRARIAN":
        return true;
      default:
        return false;
    }
  }

  bool _canAddUsers() {
    switch (_libData.getUserAccess()) {
      case "OWNER":
      case "LIBRARIAN":
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

  bool _canExport() {
    switch (_libData.getUserAccess()) {
      case "NONE":
        return false;
      default:
        return true;
    }
  }

  void _addBooks() async {
    await addEntriesDialog(context, _libData);
  }

  void _addManualItem() async {
    BuildContext dcontext = context;
    Map<String, String?> data = {
      "library": _libData.getLibraryId().toString(),
    };
    Map<String, dynamic> rslt = await util.postJson(
      "addentry",
      body: data,
    );
    if (rslt["status"] == "OK") {
      ItemData item = ItemData(rslt["entry"]);
      if (dcontext.mounted) {
        await widgets.gotoThen(
          dcontext,
          BurlEntryWidget(_libData, item),
        );
      }
      setState(() {});

      // take entityid from the result and go to entry page for it
    }
  }

  Future<void> _refreshList() async {
    await _fetchInitialData(true);
    setState(() {});
  }

  void _addUser() async {
    await addUserDialog(context, _libData);
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
    BuildContext dcontext = context;
    bool confirm = await widgets.getValidation(
      context,
      "Do You Really want to delete the library",
    );
    if (!confirm) return;
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
    await changePasswordDialog(context);
  }

  void _printLabels() async {
    dynamic val = await printLabelsDialog(context, _libData);
    if (val != "OK" && val != "CANCEL") {
      await _fetchInitialData(false);
      setState(() {});
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

  void _clearSearch() async {
    _findControl.clear();
    await _handleSearch();
  }

  Future<void> _handleSearch() async {
    await _fetchInitialData(true);
    setState(() {});
  }

  void _changeSort(String? on) async {
    if (on == defaultSort) {
      _sortOn = null;
    } else {
      _sortOn = on;
    }
    await _fetchInitialData(true);
    setState(() {});
  }

  void _changeSortOrder() async {
    _sortInvert = !_sortInvert;
    await _fetchInitialData(true);
    setState(() {});
  }

  void _exportAll() async {
    await exportDialog(context, _libData, "CSV");
  }

  void _exportDisplay() async {
    await exportDialog(
      context,
      _libData,
      "CSV",
      sortby: _sortOn,
      sortInvert: _sortInvert,
      filter: _findControl.text,
    );
  }

  void _importEntries() async {
    await importDialog(context, _libData);
    await _fetchInitialData(true);
    setState(() {});
  }

  void _loadMore() {
    if (_scrollController.position.pixels >= _scrollext) {
      _scrollext = _scrollController.position.pixels;
      setState(() {});
    } else if (_isDone) {
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

  Future<List<ItemData>>? _fetchInitialData([
    bool resetscroll = false,
  ]) async {
    // do a find on the library, save the iterator id, set the first elements
    // also set _isdone to true if returned iterator id is null
    // this should also be called by _handleSearch
    if (_iterId != null) {
      // remove old iterator
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
    _scrollext = 0;

    if (resetscroll && _scrollController.hasClients) {
      _scrollController.jumpTo(0);
    }

    Map<String, String?> data = {
      "library": _libData.getLibraryId().toString(),
      "count": globals.itemCount.toString(),
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
      _isDone = _iterId == null;
    }

    return _itemList;
  }

  Future<List<ItemData>> _fetchMoreData() async {
    if (_maxRead < _itemList.length || _isDone) {
      if (_isDone && _itemList.length > _numItems) {
        _itemList.removeRange(_numItems, _itemList.length);
      }
      if (_iterId == null) {
        _isDone = true;
      }
      return _itemList;
    }
    if (_doingFetch) return _itemList;
    _doingFetch = true;
    try {
      while (_maxRead >= _itemList.length &&
          _maxRead < _numItems &&
          !_isDone) {
        if (_iterId == null) break;
        Map<String, String?> data = {
          "library": _libData.getLibraryId().toString(),
          "filterid": _iterId,
          "count": globals.itemCount.toString(),
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
          _isDone = _iterId == null;
          continue;
        } else {
          _iterId = null;
          _isDone = true;
          break;
        }
      }
    } finally {
      _doingFetch = false;
    }

    return _itemList;
  }

  Widget? _getItemTile(BuildContext ctx, int index) {
    if (index < 0 || index >= _numItems) return null;
    if (index > _maxRead) {
      _maxRead = index;
    }
    if (index >= _itemList.length && !_isDone) {
      return const Text("Loading...");
    }

    if (index >= _itemList.length) {
      return null;
    }

    ItemData id = _itemList[index];
    int idx = id.getId();
    String lcc = id.getField("LCC");
    String ttl = id.getField("Title");
    String aut = id.getField("Primary");
    String isbn = id.getField("ISBN");
    String date = id.getField("Date");
    String imprint = id.getField("Imprint");
    String ddn = id.getField("Dewey");
    String shelf = id.getField("Shelf");
    String bid = id.getId().toString();

    String? other = imprint;
    String? son = _sortOn;
    if (son != null) {
      switch (son) {
        case "Imprint":
        case "Related Names":
        case "Subjects":
          other = id.getField(son);
          break;
      }
    }
    if (date.isNotEmpty && !lcc.contains(date)) {
      lcc = "$lcc $date";
    }
    if (aut.isEmpty) {
      aut = id.getField("Authors");
    }
    if (ddn.isNotEmpty) {
      isbn = "$isbn      $ddn";
    }
    if (shelf.isNotEmpty) {
      isbn = "$isbn      SHELF: $shelf";
    }

    String txt = "";
    if (ttl.contains(" /")) {
      txt = ttl.replaceAll(" /", "\n");
    } else {
      txt = "$ttl\n$aut";
    }
    txt = "$txt\n$isbn\n$other [$bid]";
    Widget w = Row(
      mainAxisAlignment: MainAxisAlignment.start,
      children: <Widget>[
        if (_selectModeField != null)
          Checkbox(
            value: _isSelected(index),
            onChanged: (bool? x) => _toggleSelection(index),
          ),
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
    if (_selectModeField != null) {
      Widget w2 = GestureDetector(
        key: Key("Item $idx"),
        onTap: () {
          _tapSelection(index, 0);
        },
        onSecondaryTap: () {
          _tapSelection(index, 1);
        },
        onTertiaryTapDown: (dynamic) {
          _tapSelection(index, 2);
        },
        child: w,
      );
      return w2;
    }
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

  Future<dynamic> _handleSelect(int index) async {
    ItemData item = _itemList[index];
    int id = item.getId();
    String? v = await widgets.gotoThen(
      context,
      BurlEntryWidget(_libData, item),
    );
    bool havenew = false;
    if (v != null && v != "OK") {
      id = int.parse(v);
      index = _numItems;
      _numItems += 1;
      _isDone = false;
      havenew = true;
      _maxRead = index;
      await _fetchMoreData();
    }
    Map<String, String?> data = {
      "library": _libData.getLibraryId().toString(),
      "entry": id.toString(),
    };
    Map<String, dynamic> itemrslt = await util.postJson(
      "getentry",
      body: data,
    );
    ItemData? newitem;
    if (itemrslt["status"] == "OK") {
      newitem = ItemData(itemrslt["entry"]);
      if (index < _itemList.length) {
        _itemList[index] = newitem;
      } else if (index == _itemList.length) {
        _itemList.add(newitem);
      } else {}
    } else {
      _itemList.removeAt(index);
    }
    setState(() {});
    if (havenew) _handleSelect(index);
  }

  void _initializeSelection() {
    _selectedItems = {};
  }

  void _startSelectionMode(String fld) {
    setState(() {
      _selectModeField = fld;
      _selectValueControl.text = '';
    });
  }

  void _endSelectionMode() {
    setState(() {
      _selectModeField = null;
    });
  }

  void _checkEndSelectionMode() async {
    if (_haveChanges) {
      bool fg = await widgets.getValidation(
        context,
        "Exit Group Edit",
        "Are you sure you want to exit from group edit without updating?",
      );
      if (!fg) return;
    }
    _endSelectionMode();
  }

  void _endClearSelectionMode() {
    _selectNone();
    _endSelectionMode();
  }

  bool _isSelected(int index) {
    return _selectedItems.contains(_itemList[index].getId());
  }

  void _toggleSelection(int index) {
    _haveChanges = true;
    int id = _itemList[index].getId();
    bool fg = false;
    if (!_selectedItems.remove(id)) {
      _selectedItems.add(id);
      _lastSelect = index;
      fg = true;
    }
    _lastSelectState = fg;
    setState(() {});
  }

  void _setSelection(int index, bool fg) {
    _haveChanges = true;
    int id = _itemList[index].getId();
    if (fg) {
      _selectedItems.add(id);
    } else {
      _selectedItems.remove(id);
    }
    _lastSelectState = fg;
    _lastSelect = index;
    setState(() {});
  }

  void _tapSelection(int index, int btn) {
    if (btn == 0 || _lastSelect < 0) {
      _toggleSelection(index);
    } else if (_lastSelect < index) {
      for (int i = _lastSelect + 1; i <= index; ++i) {
        _setSelection(i, _lastSelectState);
      }
    } else {
      int to = _lastSelect;
      for (int i = index; i < to; ++i) {
        _setSelection(i, _lastSelectState);
      }
    }
  }

  void _selectAll() async {
    for (int i = 0; i < _numItems; ++i) {
      _setSelection(i, true);
    }
    setState(() {});
  }

  void _selectNone() async {
    for (int i = 0; i < _numItems; ++i) {
      _setSelection(i, false);
    }
    setState(() {});
  }

  bool _canSelectEdit() {
    if (_selectModeField == null) return false;
    if (_selectedItems.isEmpty) return false;
    return true;
  }

  void _selectValueChanged(String? v) {
    _haveChanges = true;
  }

  void _selectEdit(String fld) async {
    Set<int> shownitms = {};
    for (ItemData id in _itemList) {
      shownitms.add(id.getId());
    }
    shownitms = shownitms.intersection(_selectedItems);
    List<int> itms = shownitms.toList();
    // should ensure that all itms in the list actually correspond to items
    // on display
    Map<String, String?> data = {
      "library": _libData.getLibraryId().toString(),
      "field": _selectModeField ?? "",
      "items": itms.toString(),
      "value": _selectValueControl.text,
    };
    Map<String, dynamic> rslt = await util.postJson(
      "groupedit",
      body: data,
    );
    if (rslt["status"] == "OK") {
      _haveChanges = false;
      await _fetchInitialData(false);
      setState(() {});
    }
  }
} // end of class _BurlPageState
