#! /bin/csh -fx

source setup.sql

echo WORKING ON DATABASE $db



$run $host $db <<EOF

$runcmd

pslect
ALTER TABLE burlrepo_aitywpomcdqj RENAME COLUMN labeled TO print_labels;
ALTER TABLE burlrepo_aitywpomcdqj ADD COLUMN verified text;
ALTER TABLE burlrepo_aitywpomcdqj ALTER COLUMN verified SET DEFAULT 'no';
UPDATE burlrepo_aitywpomcdqj SET print_labels = 'no';
UPDATE burlrepo_aitywpomcdqj SET verified = 'no';



EOF



















































