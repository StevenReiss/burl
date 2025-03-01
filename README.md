BURL : Build UR Library

BURL is a library management system for maintaining a home library of up to several
thousand books.  (I can't guarantee it will scale beyond that in its current incarnation.)

It can be run either cloud-based, on a local network, or on a single machine.  It can
store the library information in a SQL database, as a CSV file, or as a JSON file.
(A mongodb back end is planned, but not yet available.)

It provides both a command line interface and a web/mobile interface (although the
later is still under development.)

Books can be entered by typing ISBNs (or eventually scanning -- but there are apps available
that will scan and record these that can be used as a reasonable front end).  The system
looks up information in either the Library of Congress, OpenLibrary, or GoogleBooks (whichever
comes first, currently in that order) in order to get more complete information about the
book without you having to enter it manually.

The actual information stored and presented is specified in an XML file, so the system
can easily be customized to you particular specifications.

To run the system you will need a directory $(BURL)/secret with your private information.
The files for this directory can be found in $(BURL)/nonsecret with the various private
fields needing to be replaced with your private information.  The config.* files in the
nonsecret directory can be edited and moved to ~/.config/burl (without the config. prefix)
to let users customize some parts of the system.

Documentation might be forthcoming.

To build BURL, you will need IVY (our utility library, available from the same GITHUB repo.)
