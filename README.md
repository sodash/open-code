# open-code
Open source Java code from Winterwell/SoDash

## How to Install

So you've git cloned this repo. Great!
There is a bit more work though to get it compiling.

We assume you're doing this in Eclipse.

You'll also want to get a couple more repos:

	git clone git@github.com:winterstein/flexi-gson.git
	git clone git@github.com:good-loop/elasticsearch-java-client.git

Import projects into Eclipse -- everything in open-code, plus elasticsearch-java-client and flexi-gson

### Install and use Bob the Builder

Download `bob-all.jar` from <https://www.winterwell.com/software/downloads/bob-all.jar>

Run it in the different projects (winterwell.utils, winterwell.web, etc) to get the depenencies (which are downloaded from Maven central). Bob is run via:

`java -jar (path to)bob-all.jar`

For example (if you downloaded bob-all.jar to Downloads):

```
cd open-code/winterwell.utils
java -jar ~/Downloads/bob-all.jar 
```

Which would build winterwell.utils.jar
