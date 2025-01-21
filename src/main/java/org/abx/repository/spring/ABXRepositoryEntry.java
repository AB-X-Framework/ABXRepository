package org.abx.repository.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "org.abx.services",
        "org.abx.repository.spring",
        "org.abx.jwt",
        "org.abx.heartbeat",
        "org.abx.repository.controller"})
public class ABXRepositoryEntry {

    public static void main(String[] args) {
        SpringApplication.run(ABXRepositoryEntry.class, args);
    }

}
