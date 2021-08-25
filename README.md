# WALA Plugin

⚠ Archived repository: this repository is now archive. It has been superseded by [OPAL plugin located in fasten repository](https://github.com/fasten-project/fasten/tree/develop/analyzer/javacg-opal).

#### This tool generates call graphs in FASTEN format using [WALA](http://wala.sourceforge.net/wiki/index.php/Main_Page) call graph generator version '1.0.0'.

## Arguments
Generating a call graph from a Maven coordinate:
- `-c` `--coord` Maven coordinate for which a call graph will be generated in the following format: `groupID:artifactID:version`.
- `-t` `--timestamp` Specify a product timestamp for the generated call graph. If omitted a placeholder timestamp will be used.

Generating a call graph from a set of Maven coordinates:
- `-s` `--set` A path containing a list of Maven coordinates in JSON format.

Generating a call graph from a `.jar` file:
- `-f` `--file` A path tho the `.jar` file for which a call graph should be generated.
- `-p` `--product` Specify a product name for the generated call graph. If omitted a placeholder name will be used.
- `-v` `--version` Specify a product version for the generated call graph. If omitted a placeholder version will be used.
- `-d` `--dependencies` Specify dependencies for the generated call graph. If omitted no dependencies are added.

Writing the output:
- `-o` `--output` Specifies a directory into which a generated call graph will be written. Filename will be `<productName>-v<version>.json`
- `--stdout` If present a generated call graph will be written to standard output.

## Usage: 
```
java -jar javacg-wala-0.0.1-SNAPSHOT-with-dependencies.jar [-s=Set | [-c=COORD] | [-f=PATH [-p=PRODUCT] [-v=VERSION] [-d=DEPENDENCIES] [-d=DEPENDENCIES]...]] [--stdout] [-o=OUT] [-t=TS]
```

## Examples
#### From Maven Coordinate
Generating call graph for `org.slf4j:slf4j-api:1.7.29`:
```
-c org.slf4j:slf4j-api:1.7.29 --stdout
```

Result will be written in the given path:
```
-c org.slf4j:slf4j-api:1.7.29 -o <ResultPath>
```
#### From a set of Maven coordinates
Following is the format of the coordinates' list:
```
{"groupId":"groupId","artifactId":"artifactId","version":"1.0.0","date":123}
{"groupId":"otherGroupId","artifactId":"otherArtifactId","version":"2.0.0","date":123}
```
```
-s <InputPath> -o <ResultPath>
```

#### From `.jar` file
The input will be a path to a `.jar` file instead of maven coordinate:
```
-f <InputPath> -p product_name -v 1.0.0 -t 123 --stdout
```
