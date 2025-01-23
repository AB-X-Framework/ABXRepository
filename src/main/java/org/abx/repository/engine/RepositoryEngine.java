package org.abx.repository.engine;

import org.abx.repository.model.RepoConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public interface RepositoryEngine {

    static void deleteFolder(Path folderPath) throws IOException {
        try (Stream<Path> paths = Files.walk(folderPath)) {
            paths.sorted(Comparator.reverseOrder()) // Delete files first, then directories
                    .forEach(path -> {
                        try {
                            Files.delete(path); // Delete each file and directory
                        } catch (IOException e) {
                            System.err.println("Error deleting " + path + ": " + e.getMessage());
                        }
                    });
        }
    }

    public static final String WorkingSince = "Working. Last update at: ";

    public void setDir(String dir);

    /**
     * Update clone, change branch whatever
     */
    public String update(RepoConfig config);

    public String push(RepoConfig config);

    public String rollbackFile(RepoConfig config, String path);

    public String rollback(RepoConfig config);

    public String commit(RepoConfig config);

    public List<String> diff(RepoConfig config);
}
