#! /bin/csh

# for brown cs user /people/spr/iqsign/
flutter build web --base-href /burl/ --source-maps

pushd build/web
tar cf - . | (cd /Library/WebServer/Documents/iqsign; tar xf -)
popd

pushd build/web
scp -r * sherpa.cs.brown.edu:/vol/web/html/burl
popd












































