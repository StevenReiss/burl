#! /bin/csh -fx

source setup.sql

echo WORKING ON DATABASE $db



$run $host $db <<EOF

$runcmd

ALTER TABLE BurlWorkQueue ADD COLUMN userid $idtype;



EOF



















































