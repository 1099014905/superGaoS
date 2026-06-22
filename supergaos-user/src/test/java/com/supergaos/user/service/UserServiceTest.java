package com.supergaos.user.service;

import com.supergaos.common.exception.BusinessException;
import com.supergaos.user.dto.LoginDTO;
import com.supergaos.user.dto.RegisterDTO;
import com.supergaos.user.entity.User;
import com.supergaos.user.mapper.UserMapper;
import com.supergaos.user.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private BCryptPasswordEncoder encoder;

    @InjectMocks
    private UserService userService;

    private LoginDTO loginDTO;
    private RegisterDTO registerDTO;
    private User existingUser;

    @BeforeEach
    void setUp() {
        loginDTO = new LoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password123");

        registerDTO = new RegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setPassword("pass456");
        registerDTO.setNickname("New User");

        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");
        existingUser.setPassword("$2a$10$encodedPasswordHash");
        existingUser.setNickname("Test User");
    }

    // --- login tests ---

    @Test
    void login_withValidCredentials_shouldReturnToken() {
        when(userMapper.findByUsername("testuser")).thenReturn(existingUser);
        when(encoder.matches("password123", "$2a$10$encodedPasswordHash")).thenReturn(true);
        when(jwtUtil.generateToken(1L)).thenReturn("jwt-token-123");

        String token = userService.login(loginDTO);

        assertEquals("jwt-token-123", token);
        verify(userMapper).findByUsername("testuser");
        verify(jwtUtil).generateToken(1L);
    }

    @Test
    void login_withWrongPassword_shouldThrowBusinessException() {
        when(userMapper.findByUsername("testuser")).thenReturn(existingUser);
        when(encoder.matches("password123", "$2a$10$encodedPasswordHash")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login(loginDTO));
        assertEquals(5001, ex.getErrorCode());
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    void login_withNonExistentUser_shouldThrowBusinessException() {
        when(userMapper.findByUsername("testuser")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login(loginDTO));
        assertEquals(5001, ex.getErrorCode());
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    void login_withEmptyUsername_shouldThrowBusinessException() {
        when(userMapper.findByUsername("")).thenReturn(null);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("");
        dto.setPassword("pass");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login(dto));
        assertEquals(5001, ex.getErrorCode());
    }

    // --- register tests ---

    @Test
    void register_withNewUsername_shouldInsertUser() {
        when(userMapper.findByUsername("newuser")).thenReturn(null);
        when(encoder.encode("pass456")).thenReturn("$2a$10$encodedHash");

        userService.register(registerDTO);

        verify(userMapper).findByUsername("newuser");
        verify(encoder).encode("pass456");
        verify(userMapper).insert(argThat(user -> {
            assertEquals("newuser", user.getUsername());
            assertEquals("$2a$10$encodedHash", user.getPassword());
            assertEquals("New User", user.getNickname());
            return true;
        }));
    }

    @Test
    void register_withExistingUsername_shouldThrowBusinessException() {
        when(userMapper.findByUsername("newuser")).thenReturn(existingUser);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.register(registerDTO));
        assertEquals(5002, ex.getErrorCode());
        assertEquals("用户名已存在", ex.getMessage());
        verify(userMapper, never()).insert(any());
    }

    @Test
    void register_withoutNickname_shouldUseNull() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("nonick");
        dto.setPassword("pass");
        // nickname not set — should be null

        when(userMapper.findByUsername("nonick")).thenReturn(null);
        when(encoder.encode("pass")).thenReturn("hash");

        userService.register(dto);

        verify(userMapper).insert(argThat(user -> {
            assertEquals("nonick", user.getUsername());
            assertNull(user.getNickname());
            return true;
        }));
    }
}
