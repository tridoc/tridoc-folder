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

## Running in background

If you're a unix user you'll probably now how to make sure he script starts at startup, as a windows user you might find the following helpfull:

### Create bat-script

Create a file `tridoc-folder.bat` where you start a tridoc-folder for every tridoc-backend you'd like to connect to.

```(bat)
START "tridoc work" /B java -jar C:\Path\to\tridoc-folder-1.0-SNAPSHOT.jar -U tridoc -P MyPwd -I https://docs.example.com/ -F C:\Users\me\Desktop\tridoc-work
START "tridoc personal" /B java -jar C:\Path\to\tridoc-folder-1.0-SNAPSHOT.jar -U tridoc -P MyPwd -I https://docs.example.org/ -F C:\Users\me\Desktop\tridoc-personal
```

The above script could directly be used as a startup script but this would cause a terminal window to be open at all time. So we recommend to additionaly create a `tridoc-folder.vbs` script. Assuming the above bat-script is located at `C:\Users\me\bin\tridoc-folder.bat` the new `tridoc-folder.vbs` would look as follows:

```
Dim WinScriptHost
Set WinScriptHost = CreateObject("WScript.Shell")
WinScriptHost.Run Chr(34) & "C:\Users\me\bin\tridoc-folder.bat" & Chr(34), 0
Set WinScriptHost = Nothing
```

Now open the stratup folder (press Windows+R, then type shell:startup) and create a link to `tridoc-folder.vbs` in the startup folder. Restart the computer, and starting dropping PDFs into the folder(s).
