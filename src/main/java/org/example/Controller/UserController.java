package org.example.Controller;

import io.jsonwebtoken.Claims;
import org.example.Entity.ChangePasswordDTO;
import org.example.Entity.LoginDTO;
import org.example.Entity.RegisterDTO;
import org.example.Service.UserService;
import org.example.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import javax.xml.transform.Result;


@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public String register(@RequestBody RegisterDTO dto) {
       return userService.register(dto);
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginDTO dto) {
       return userService.login(dto);
    }


    @PutMapping("/password")
    public String changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ChangePasswordDTO dto) {
       return userService.password(dto, authHeader);
    }
}

//    @GetMapping
//    public Result listUser() {
//
//    }
//
//    @GetMapping("/search")
//    public Result searchUser(...) {}
//    }

