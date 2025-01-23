package org.abx.repository.controller;

import org.abx.util.Pair;

public class RepositoryFile  {

    String repositoryName;
    String path;

    public RepositoryFile(String repositoryName, String path) {
        this.repositoryName = repositoryName;
        this.path = path;
    }
}
