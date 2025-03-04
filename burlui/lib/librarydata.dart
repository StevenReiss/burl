/********************************************************************************/
/*                                                                              */
/*              librarydata.dart                                                */
/*                                                                              */
/*      Data about a particular library                                         */
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

class LibraryData {
  late String _name;
  late int _libraryId;
  late String _nameKey;
  late String _repoType;
  late String _userAccess;
  late String _owner;

  LibraryData(d) {
    update(d);
  }

  LibraryData.unknown() {
    _name = "UNKNOWN";
    _nameKey = "UNKNOWN";
    _libraryId = 0;
    _repoType = "DATABASE";
    _userAccess = "NONE";
    _owner = "";
  }
  LibraryData.clone(LibraryData d) {
    _name = d._name;
    _nameKey = d._nameKey;
    _libraryId = d._libraryId;
    _repoType = d._repoType;
    _userAccess = d._userAccess;
    _owner = d._owner;
  }

  void update(d) {
    _libraryId = d['id'] as int;
    _name = d['name'] as String;
    _nameKey = d['namekey'] as String;
    _repoType = d['repo_type'] as String;
    _userAccess = d['access'] as String;
    _owner = d['owner'] as String;
  }

  String getName() {
    return _name;
  }

  String getNameKey() {
    return _nameKey;
  }

  int getLibraryId() {
    return _libraryId;
  }

  String getRepoType() {
    return _repoType;
  }

  String getUserAccess() {
    return _userAccess;
  }

  String getOwner() {
    return _owner;
  }
}     // end of signdata.dart

