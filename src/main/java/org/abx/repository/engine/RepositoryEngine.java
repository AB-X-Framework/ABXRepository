package org.abx.repository.engine;

import org.abx.repository.model.RepoConfig;

import java.io.File;
import java.util.List;

public interface RepositoryEngine {

    static boolean deleteFolder(File folder) {
        // Check if the folder exists
        if (!folder.exists()) {
            return true;
        }

        // Delete all files and subdirectories recursively
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) { // Check for null to avoid NullPointerException
                for (File file : files) {
                    deleteFolder(file); // Recursive call
                }
            }
        }
        // Delete the folder or file
        return folder.delete();
    }

    String WorkingSince = "Working. Last update at: ";

    void setDir(String dir);

    /**
     * Update clone, change branch whatever
     */
    public String update(RepoConfig config);

    public String push(RepoConfig config);

    public String rollbackFile(RepoConfig config, String path);

    public List<String> diff(RepoConfig config) throws Exception;

    public String reset(RepoConfig config);
}
