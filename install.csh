#! /bin/csh -f

pushd /pro/ivy
# ant
make jar
popd

ant
if ($status > 0) exit;

git commit -a --dry-run >&! /dev/null
if ($status == 0) then
   git commit -a
   if ($status > 0) exit;
endif

git push

ssh sherpa.cs.brown.edu '(cd /vol/burl; git pull)'
if ($status > 0) exit;

# scp burlui/assets/*.html sherpa.cs.brown.edu:/vol/web/html/burl
scp burlui/assets/images/*.png sherpa.cs.brown.edu:/vol/web/html/burl/images

set ivylib = ( ivy.jar postgresql.jar mysql.jar json.jar jakarta.mail.jar jakarta.activation.jar \
      slf4j-api.jar junit.jar jsoup.jar mongojava.jar  )
foreach i ( $ivylib )
   scp /pro/ivy/lib/$i sherpa.cs.brown.edu:/vol/ivy/lib
end


ssh sherpa.cs.brown.edu mkdir -p /vol/burl/secret
pushd secret
update.csh
popd

ssh sherpa.cs.brown.edu '(cd /vol/burl; ant)'
if ($status > 0) exit;

ssh sherpa.cs.brown.edu '(cd /vol/burl; startserver.csh)'

pushd burlui
buildweb.csh >&! buildweb.out
popd
