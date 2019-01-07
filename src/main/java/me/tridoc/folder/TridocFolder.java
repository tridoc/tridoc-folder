package me.tridoc.folder;

import java.io.FileWriter;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.wymiwyg.commons.util.arguments.ArgumentHandler;

/**
 * Example to watch a directory (or tree) for changes to files.
 */
public class TridocFolder {

    private final Path dir;
    private final Path processedFilesDir;
    private final TridocFolderArgs arguments;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    private TridocFolder(TridocFolderArgs tridocFolderArgs) throws IOException {
        dir = Paths.get(tridocFolderArgs.getFolder());
        logFile = dir.resolve("tridoc-folder-log.txt");
        processedFilesDir = dir.resolve("uploaded");
        Files.createDirectories(processedFilesDir);
        arguments = tridocFolderArgs;
    }

    /**
     * Monitor the directory for new files
     */
    void monitor() throws IOException {
        log("Started monitoring directory", null);
        WatchService watcher = FileSystems.getDefault().newWatchService();
        WatchKey watchKey = dir.register(watcher, ENTRY_CREATE);
        for (;;) {

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                break;
            }

            for (WatchEvent event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();
                // TBD - provide example of how OVERFLOW event is handled
                if (!(kind == OVERFLOW)) {
                    // Context for directory entry event is the file name of entry
                    WatchEvent<Path> ev = cast(event);
                    Path name = ev.context();
                    Path child = dir.resolve(name);
                    processFile(child);
                    // print out event
                    //System.out.format("%s: %s\n", event.kind().name(), child);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
        watchKey.cancel();
    }

    public static void main(String[] args) throws IOException {
        TridocFolderArgs tridocFolderArgs = ArgumentHandler.readArguments(TridocFolderArgs.class, args);
        if (tridocFolderArgs == null) {
            System.err.println("Please provide correct arguments");
            return;
        }

        TridocFolder uploadDir = new TridocFolder(tridocFolderArgs);
        uploadDir.processFiles();
        uploadDir.monitor();
    }

    private void processFiles() throws IOException {
        log("Processing files currently in folder", null);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (final Iterator<Path> file = stream.iterator(); file.hasNext();) {
                Path child = dir.resolve(file.next());
                if (child.equals(processedFilesDir) || child.equals(logFile)) {
                    continue;
                }
                processFile(child);
            }
        }
    }

    private void processFile(Path file) throws IOException {
        if(!file.toString().endsWith(".pdf") && !file.toString().endsWith(".PDF")) {
            log(file+" doesn't end with \".pdf\", ignoring.", null);
            return;
        }
        System.out.format("Processing %s", file);
        System.out.println();
        if (uploadFile(file)) {
            Path destinationPath = processedFilesDir.resolve(file.getFileName());
            destinationPath = ensureUniqe(destinationPath);
            insistingMove(file, destinationPath, 10, 100);
        }
    }
    
    private void insistingMove(Path source, Path target, int retry, int wait) throws IOException {
        try {
            Files.move(source, target);
        } catch (FileSystemException e) {
            if (retry > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                insistingMove(source, target, retry - 1, wait * 2);
            } else {
                throw e;
            }
        }
    }

    private synchronized Path ensureUniqe(Path path) {
        if (Files.exists(path)) {
            for (int i = 0; i < 10000; i++) {
                Path alternativePath = Paths.get(path.toString() + "-" + i);
                if (!Files.exists(alternativePath)) {
                    return alternativePath;
                }
            }
        } else {
            return path;
        }
        throw new RuntimeException("Couldn't find a suitable path for " + path);
    }

    private boolean uploadFile(Path file) throws IOException {
        try (CloseableHttpClient httpClient = createHttpClient()) {
            URI titleLocation;
            {
                HttpPost httpPost = new HttpPost(new URI(arguments.getInstance()).resolve("/doc").toString());
                httpPost.setHeader("Content-Type", "application/pdf");
                Base64.Encoder encoder = Base64.getEncoder();
                String encoded = encoder.encodeToString((arguments.getUserName()+":"+arguments.getPassword()).getBytes());
                httpPost.setHeader("authorization", "Basic "+encoded);
                httpPost.setEntity(new FileEntity(file.toFile()));
                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    final StatusLine statusLine = response.getStatusLine();
                    if (statusLine.getStatusCode() >= 400) {
                        throw new IOException("HTTP " + statusLine.getStatusCode()
                                + " " + statusLine.getReasonPhrase());
                    }
                    titleLocation = new URI(arguments.getInstance()).resolve(response.getFirstHeader("Location").getValue()+"/title");

                }
            }
            {
                HttpPut httpPut = new HttpPut(titleLocation);
                httpPut.setHeader("Content-Type", "application/json");
                httpPut.setEntity(new StringEntity("{\"title\": \"" + removeExtension(file.getFileName().toString()) + "\"}"));
                try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
                    final StatusLine statusLine = response.getStatusLine();
                    if (statusLine.getStatusCode() >= 400) {
                        throw new IOException("HTTP " + statusLine.getStatusCode()
                                + " " + statusLine.getReasonPhrase());
                    }

                }
            }
            return true;
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Instance URI is invalid");
        } catch (Exception ex) {
            log("Failed to upload "+file, ex);
            return false;
        }
    }

    /**
     * Creates a CloseableHttpClient that authenticates using the credentials
     * supplied in the arguments.
     *
     * @return the HTTP Client
     */
    public CloseableHttpClient createHttpClient() {
        final HttpClientBuilder hcb = HttpClientBuilder.create();
        final CredentialsProvider credsProvider = new BasicCredentialsProvider();
        if (arguments.getUserName() != null) {
            Credentials credentials = new UsernamePasswordCredentials(arguments.getUserName(), arguments.getPassword());
            credsProvider.setCredentials(AuthScope.ANY, credentials);
        }
        return hcb.setDefaultCredentialsProvider(credsProvider).build();
    }

    private String removeExtension(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    private void log(String message, Exception ex) throws IOException {
        FileWriter writer = new FileWriter(logFile.toFile(), true);
        try (PrintWriter out = new PrintWriter(writer)) {
            out.println("-------------------");
            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            df.setTimeZone(tz);
            out.println(df.format(new Date())+" - "+message);
            if (ex != null) {
                ex.printStackTrace(out);
            }
        }
        
    }
    private Path logFile;
}
