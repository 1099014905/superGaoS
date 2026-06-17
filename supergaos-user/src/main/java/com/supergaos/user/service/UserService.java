package com.supergaos.user.service;

import com.supergaos.common.exception.BusinessException;
import com.supergaos.user.dto.LoginDTO;
import com.supergaos.user.dto.RegisterDTO;
import com.supergaos.user.entity.User;
import com.supergaos.user.mapper.UserMapper;
import com.supergaos.user.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder;

    public UserService(UserMapper userMapper, JwtUtil jwtUtil, BCryptPasswordEncoder encoder) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.encoder = encoder;
    }

    public String login(LoginDTO dto) {
        User user = userMapper.findByUsername(dto.getUsername());
        if (user == null || !encoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(5001, "用户名或密码错误");
        }
        return jwtUtil.generateToken(user.getId());
    }

    public void register(RegisterDTO dto) {
        User existing = userMapper.findByUsername(dto.getUsername());
        if (existing != null) {
            throw new BusinessException(5002, "用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        userMapper.insert(user);
    }
}
