#!/bin/bash

source="parser.cup" # nazov zdrojoveho suboru
target="BDParser"   # nazov triedy parsera
symbols="SymBD"     # nazov triedy symbolov
package="brainc_brainduck.impl"      # aky balik ma mat parser aj symboly

echo "Removing old files..."
rm -f $target.java $symbols.java *.states cup.error
echo "Generating JAVA files..."
java -jar d:/bin/java-cup/java-cup-11a.jar \
    -expect 0 -progress -package $package -parser $target -symbols $symbols -interface \
    -dump $source &> cup.error


