#! /bin/csh -fx

source setup.sql

echo WORKING ON DATABASE $db

if ("X$DROPCMD" != X) then
   $DROPCMD $host $db
endif

$run $host $DEFAULTDB << EOF
DROP DATABASE IF EXISTS $db $DROPOPT;
EOF

$run $host $DEFAULTDB << EOF
CREATE DATABASE $db $ENCODE;
EOF


$run $host $db <<EOF

$runcmd

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

DROP TABLE IF EXISTS BurlUsers CASCADE;
DROP TABLE IF EXISTS BurlValidator CASCADE;
DROP TABLE IF EXISTS BurlLibraries CASCADE;
DROP TABLE IF EXISTS BurlUserAccess CASCADE;
DROP TABLE IF EXISTS BurlSession CASCADE;
DROP TABLE IF EXISTS BurlRepoStores CASCADE;


CREATE TABLE BurlUsers (
   id $iddeftype  NOT NULL PRIMARY KEY,
   email text NOT NULL,
   password text NOT NULL,
   salt text NOT NULL,
   temp_password text DEFAULT NULL,
   valid bool NOT NULL DEFAULT false
$ENDTABLE;
CREATE INDEX UsersEmail ON BurlUsers ( email );


CREATE TABLE BurlValidator (
   id $iddeftype  NOT NULL PRIMARY KEY,
   userid $idtype NOT NULL,
   validator text NOT NULL,
   timeout $datetime NOT NULL,
   FOREIGN KEY (userid) REFERENCES BurlUsers(id) ON DELETE CASCADE
$ENDTABLE;
CREATE INDEX ValidUser ON BurlValidator (userid);


CREATE TABLE BurlLibraries (
   id $iddeftype NOT NULL PRIMARY KEY,
   name text NOT NULL,
   namekey text NOT NULL,
   repo_type int DEFAULT 0,		-- from BurlRepoType.ordinal()
   UNIQUE(namekey)
$ENDTABLE;
CREATE INDEX LibrariesNamekey ON BurlLibraries(namekey);


CREATE TABLE BurlUserAccess (
   email text NOT NULL, 		-- use email since user mignt not be registered
   libraryid $iddeftype NOT NULL,
   access_level int DEFAULT 0,		-- from BurlUserAccess.ordinal()
   UNIQUE(email,libraryid),
   FOREIGN KEY (libraryid) REFERENCES BurlLibraries(id) ON DELETE CASCADE
$ENDTABLE;
CREATE INDEX AccessUsers ON BurlUserAccess (email);
CREATE INDEX AccessLibraries ON BurlUserAccess (libraryid);
CREATE INDEX AccessPerms ON BurlUserAccess(email,libraryid);


CREATE TABLE BurlSession (
   session text NOT NULL PRIMARY KEY,
   userid $idtype DEFAULT NULL,
   libraryid $idtype DEFAULT NULL,
   code text,
   creation_time $datetime DEFAULT CURRENT_TIMESTAMP,
   last_used $datetime DEFAULT CURRENT_TIMESTAMP,
   FOREIGN KEY (userid) REFERENCES BurlUsers(id) ON DELETE CASCADE
$ENDTABLE;


CREATE TABLE BurlRepoStores (
   name text NOT NULL PRIMARY KEY,
   fields text
$ENDTABLE;



EOF

