#! /bin/csh -fvx


foreach i ( *.java )
   sed -e 's/Main/Control/g' $i \
   | sed -e 's/main/control/g' \
   | sed -e 's/ControlControl/ControlMain/g' >! $i.new
end






















														 &
