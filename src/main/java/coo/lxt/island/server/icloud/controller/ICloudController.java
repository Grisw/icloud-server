package coo.lxt.island.server.icloud.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/icloud")
public class ICloudController {

    @GetMapping("/hello")
    public String hello(@RequestParam("name") String name) {
        return "Hello. " + name;
    }
}
