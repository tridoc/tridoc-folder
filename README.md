# tridoc-folder

Add files to tridoc by simply dropping them to a folder

## Building

To build you need [Apache Maven](https://maven.apache.org).

    mvn package

This will create an executable jar named like `tridoc-folder-1.0-SNAPSHOT.jar` in the `target` directory.

## Running

Running the program without arguments will show the available options:

```
$java -jar target\tridoc-folder-1.0-SNAPSHOT.jar
Missing required argument: -I|--instance
This command has the following arguments: -I|--instance string [-P|--password string] [-U|--userName string] [-F|--folder string]
  -I|--instance string   URI to the Tridoc instance
  -P|--password string   The password used to access the Tridoc instance
  -U|--userName string   The user-name used to access the Tridoc instance
  -F|--folder string     The path to the Tridoc Folder
```

The program uploads all files in he specified folder and any file that is added while the program is running.