package org.abx.repository.controller;

import jakarta.servlet.ServletException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/repository")
public class RepositoryController {


    @RequestMapping(value = "/test")
    public String test() throws ServletException, IOException {
        return "test";
    }

}
