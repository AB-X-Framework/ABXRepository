package org.abx.repository.controller;

import jakarta.servlet.ServletException;
import org.abx.repository.jwt.JWTUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/repository")
public class RepositoryController {


    @Autowired
    private JWTUtils jwtUtils;

    @RequestMapping(value = "/test")
    public String test() throws ServletException, IOException {
        return "test"+jwtUtils.getPublicKey();
    }

}
