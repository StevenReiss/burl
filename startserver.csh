#! /bin/csh -f

pm2 stop burl

cat < /dev/null > burl.log

pm2 start --log burl.log --name burl bin/burlserver.sh

pm2 save


