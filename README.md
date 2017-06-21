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

In Eclipse's Java Build Path editor, import a new User Library
from the file `open-code/userlibraries.userlibraries`
This will give you the library elasticsearch_5

You will also need lots of jars in the `middleware` project
 -- get these by copy from Daniel, Miles or Carson.

Step probably not needed (the code should find itself): 
Set the env variable WINTERWELL_HOME to point to the folder above open-code.