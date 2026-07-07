package org.example.Service;
import io.jsonwebtoken.Claims;
import org.example.Entity.ChangePasswordDTO;
import org.example.Entity.LoginDTO;
import org.example.Entity.RegisterDTO;
import org.example.Entity.User;
import org.example.Mapper.UserMapper;
import org.example.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {
    @Autowired
    private PasswordEncoder passwordEncoder;
       public String register(RegisterDTO dto){
           User user= new User();
           user.setUsername(dto.getUsername());
           user.setPassword(passwordEncoder.encode(dto.getPassword()));
           userMapper.insert(user);
           Map<String, Object> claims = new HashMap<>();
           claims.put("username", dto.getUsername());
           long userid = user.getId();
           return "Register success"+JwtUtil.generateToken(String.valueOf(userid), claims);
       }
       @Autowired
       private UserMapper userMapper;
       public String login(LoginDTO dto){
           User user = userMapper.selectByUsername(dto.getUsername());

           if (user == null){
               return "user doesn't exist";
           }
           if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
               return "password error";
           }
           Map<String, Object> claims = new HashMap<>();
           claims.put("username", dto.getUsername());
           long userid = user.getId();
           return "Login success"+JwtUtil.generateToken(String.valueOf(userid), claims);

       }
       public int verify(String authHeader){
           String token = authHeader.substring(7);
           try {
               Claims claims = JwtUtil.parseToken(token);

               // 3. 提取标准字段和自定义字段
               // 获取 subject (张三)
               return Integer.parseInt(claims.getSubject());
           } catch (RuntimeException e) {
               System.out.println("解析失败: " + e.getMessage());
            return -1;
           }
       }
       public String password(ChangePasswordDTO dto, String authHeader) {
           if (verify(authHeader)!=-1){
               User user = userMapper.selectById(verify(authHeader));
               if (passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
                   user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
                   userMapper.update(user);
                   return "password changed";
               }
               return "incorrect password";
           }
           return "authorization error";
       }
}
