#! /bin/csh -fx

source setup.sql

echo WORKING ON DATABASE $db



$run $host $db <<EOF

$runcmd

-- ALTER TABLE burlrepo_aitywpomcdqj;


-- ALTER TABLE burlrepo_hbhulrcyfrmw RENAME COLUMN labeled TO print_labels;
-- ALTER TABLE burlrepo_hbhulrcyfrmw ADD COLUMN verified text;
-- ALTER TABLE burlrepo_hbhulrcyfrmw ALTER COLUMN verified SET DEFAULT 'no';
UPDATE burlrepo_hbhulrcyfrmw SET print_labels = "no";



EOF



















































































