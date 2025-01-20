package org.abx.repository.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/repository")
public class RepositoryController {


    @RequestMapping(value = "/test")
    @PreAuthorize("permitAll()")
    public String test() throws ServletException, IOException {
        return "test";
    }

}
