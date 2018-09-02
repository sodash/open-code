# open-code
Open source Java code from Winterwell/SoDash

## How to Install

So you've git cloned this repo. Great!
There is a bit more work though to get it compiling.

We assume you're doing this in Eclipse.

You'll also want to get a couple more repos:

	git clone git@github.com:winterstein/flexi-gson.git
	git clone git@github.com:winterstein/elasticsearch-java-client.git

Import projects into Eclipse -- everything in open-code, plus elasticsearch-java-client and flexi-gson

### Install and use Bob the Builder

Download `bob-all.jar` from <https://www.winterwell.com/software/downloads/bob-all.jar>

Run it in the different projects (winterwell.utils, winterwell.web, etc) to get the depenencies (which are downloaded from Maven central). Bob is run via:

`java -jar (path to)bob-all.jar`


### Older jars

In Eclipse's Java Build Path editor, import a new User Library
from the file `open-code/userlibraries.userlibraries`
This will give you the library `elasticsearch`

You will also need lots of jars in the `middleware` project
 -- get these by copy from Daniel, Miles or Carson.

This step is probably not needed (the code will usually find itself): Set the env variable WINTERWELL_HOME to point to the folder above open-code.
