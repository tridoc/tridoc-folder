package me.tridoc.folder;

import org.wymiwyg.commons.util.arguments.CommandLine;

/**
 *
 * @author user
 */
public interface TridocFolderArgs {
    
    @CommandLine (
        longName ="instance",
        shortName = "I", 
        required = true,
        description = "URI to the Tridoc instance"
    )
    public String getInstance();
    
    @CommandLine (
        longName ="userName",
        shortName = "U", 
        required = false,
        description = "The user-name used to access the Tridoc instance"
    )
    public String getUserName();
    
    @CommandLine (
        longName ="password",
        shortName = "P", 
        required = false,
        description = "The password used to access the Tridoc instance"
    )
    public String getPassword();
    
    @CommandLine (
        longName ="folder",
        shortName = "F",
        required = false,
        description = "The path to the Tridoc Folder"
    )
    public String getFolder();
    
}
