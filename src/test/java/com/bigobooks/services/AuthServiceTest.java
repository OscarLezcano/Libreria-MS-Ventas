package com.bigobooks.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bigobooks.dto.AuthResponse;
import com.bigobooks.dto.LoginDto;
import com.bigobooks.dto.RegisterDto;
import com.bigobooks.entities.auth.Role;
import com.bigobooks.entities.auth.UserAccount;
import com.bigobooks.exception.ConflictException;
import com.bigobooks.exception.UnauthorizedException;
import com.bigobooks.mappers.UserAccountMapper;
import com.bigobooks.repositories.RoleRepository;
import com.bigobooks.repositories.UserAccountRepository;
import com.bigobooks.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserAccountRepository userRepository;
	@Mock
	private RoleRepository roleRepository;
	@Mock
	private JwtService jwtService;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private final UserAccountMapper userAccountMapper = new UserAccountMapper();
	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService, userAccountMapper,
				"READER");
	}

	@Test
	void registerAsignaRolPorDefectoExistente() {
		Role reader = new Role();
		reader.setId(3L);
		reader.setName("READER");
		when(userRepository.findByEmailIgnoreCase("ana@test.com")).thenReturn(Optional.empty());
		when(roleRepository.findByNameIgnoreCase("READER")).thenReturn(Optional.of(reader));
		when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		UserAccount saved = authService.register(registerDto("Ana", "ana@test.com", "secreto"));

		assertEquals("ana@test.com", saved.getEmail());
		assertEquals(reader, saved.getRole());
		assertEquals("Ana", saved.getName());
	}

	@Test
	void registerCreaElRolPorDefectoSiNoExiste() {
		when(userRepository.findByEmailIgnoreCase("ana@test.com")).thenReturn(Optional.empty());
		when(roleRepository.findByNameIgnoreCase("READER")).thenReturn(Optional.empty());
		when(roleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		UserAccount saved = authService.register(registerDto("Ana", "ana@test.com", "secreto"));

		assertEquals("READER", saved.getRole().getName());
		verify(roleRepository).save(any());
	}

	@Test
	void registerRechazaEmailDuplicado() {
		when(userRepository.findByEmailIgnoreCase("ana@test.com")).thenReturn(Optional.of(new UserAccount()));

		assertThrows(ConflictException.class,
				() -> authService.register(registerDto("Ana", "ana@test.com", "secreto")));
	}

	@Test
	void registerValidaCamposObligatorios() {
		RegisterDto request = registerDto("Ana", "ana@test.com", "");

		assertThrows(IllegalArgumentException.class, () -> authService.register(request));
	}

	@Test
	void loginDevuelveToken() {
		UserAccount user = new UserAccount();
		user.setId(7L);
		user.setEmail("ana@test.com");
		user.setPasswordHash(passwordEncoder.encode("secreto"));
		Role reader = new Role();
		reader.setId(3L);
		reader.setName("READER");
		user.setRole(reader);
		when(userRepository.findByEmailIgnoreCase("ana@test.com")).thenReturn(Optional.of(user));
		when(jwtService.generateToken(user)).thenReturn("token.jwt");
		when(jwtService.getExpirationSeconds()).thenReturn(7200L);

		LoginDto request = new LoginDto();
		request.setEmail("ana@test.com");
		request.setPassword("secreto");

		AuthResponse response = authService.login(request);

		assertEquals("token.jwt", response.getToken());
		assertEquals("Bearer", response.getTokenType());
		assertEquals(7200L, response.getExpiresIn());
		assertEquals("ana@test.com", response.getUser().getEmail());
	}

	@Test
	void loginRechazaCredencialesInvalidas() {
		when(userRepository.findByEmailIgnoreCase("ana@test.com")).thenReturn(Optional.empty());

		LoginDto request = new LoginDto();
		request.setEmail("ana@test.com");
		request.setPassword("mala");

		assertThrows(UnauthorizedException.class, () -> authService.login(request));
	}

	private RegisterDto registerDto(String name, String email, String password) {
		RegisterDto request = new RegisterDto();
		request.setName(name);
		request.setLastName("Perez");
		request.setEmail(email);
		request.setPassword(password);
		request.setCity("Asuncion");
		return request;
	}
}
