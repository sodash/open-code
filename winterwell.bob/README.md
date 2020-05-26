
# Bob the Builder

See also: https://www.winterwell.com/software/bob/

<img style='float:right; max-height:200px;' src='https://www.winterwell.com/res/images/bob-the-builder-edited-scaled.png' alt='Bob the Builder'>

## Can we build it?

"Bob" or "winterwell.bob" is a java project which leverages maven's abilities, in order to ultimately build other winterwell projects.

## Install / Getting Started with Bob

Winterwell runs a flavour of Debian Linux. These instructions assume that you have a similar setup.

### Pre-requisites

 - Java
 - maven (apt install mvn)

### Install Bob

Recommended: Use npm:   

	`npm i -g java-bob`

Then run `bob`   
The first run will download the jar file to ~/bin/bob-all.jar.

Alternative: Download bob-all.jar from <https://www.winterwell.com/software/downloads/bob-all.jar>

### Updating Bob

You can run `bob --help` to see what version you have installed.

Run `bob -update` to download the latest jar.

Note: this does not update the `java-bob` npm wrapper -- but you rarely if ever will need to update that.
If you do have to, then `npm update -g java-bob` should do the trick.

## Run Bob

Suppose your project is in the folder `myproject`, with a Bob build script in `myproject/builder/BuildMyProject.java` 

1. `cd myproject`

2. Run Bob:
	- (a) If you installed via npm:
	   
		bob

	- (b) If you installed by downloading the bob-all.jar, invoke it:
	
		java -ea -jar bob-all.jar

Bob should find that build script, compile, and execute it.

You can also pass in the script explicitly:

	`bob builder/BuildMyProject.java`
	
Or 	`bob BuildMyProject` should find it.


## bobwarehouse (or: Where does Bob keep its stuff?)

This folder is auto-generated and managed by Bob. It contains

 - `bobhistory.csv` The run history. Delete this file to make Bob do everything from scratch.
 Note: this is roughly equivalent to running `bob -clean`.
 
 - `calls.dot` Call graph info in graphviz dot format.

 - git clones of dependency projects.
 
The bobwarehouse folder can safely be deleted, although that will make your next bob run rather slow as it will get rebuilt.

Bob will also make local bob.log files and boblog folders. These are useful for debugging.
Bob writes to them -- it does not read any info from them. They can safely be deleted without
side-effects.  

## Issues

Something broke? 

Maybe run `bob -upgrade` to make sure you have the latest Bob?

Maybe do a clean build `bob -clean`

Still broken? Email Good-Loop support or Daniel Winterstein
