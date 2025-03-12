#! /bin/csh -fx

source setup.sql

echo WORKING ON DATABASE $db



$run $host $db <<EOF

$runcmd

DROP TABLE IF EXISTS BurlWorkQueue;

CREATE TABLE BurlWorkQueue (
   id $iddeftype NOT NULL PRIMARY KEY,
   libraryid $idtype NOT NULL,
   item text NOT NULL,
   count bool NOT NULL DEFAULT true,
   mode int DEFAULT 0
$ENDTABLE;

EOF



















































































































