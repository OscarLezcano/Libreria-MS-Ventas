package com.bigobooks.services;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

/**
 * Registro y autenticacion de usuarios. Los usuarios nuevos se crean con el rol
 * por defecto configurable ({@code security.auth.default-role}); si el rol no
 * existe todavia se crea (la tabla de roles parte vacia).
 */
@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserAccountRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final UserAccountMapper userAccountMapper;
	private final String defaultRoleName;

	public AuthService(UserAccountRepository userRepository, RoleRepository roleRepository,
			PasswordEncoder passwordEncoder, JwtService jwtService, UserAccountMapper userAccountMapper,
			@Value("${security.auth.default-role:READER}") String defaultRoleName) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.userAccountMapper = userAccountMapper;
		this.defaultRoleName = defaultRoleName;
	}

	@Transactional
	public UserAccount register(RegisterDto request) {
		if (request == null) {
			throw new IllegalArgumentException("Debe indicar los datos del usuario");
		}
		String email = requireText(request.getEmail(), "email").toLowerCase();
		String password = requireText(request.getPassword(), "password");
		if (password.length() < 6) {
			throw new IllegalArgumentException("La contrasena debe tener al menos 6 caracteres");
		}
		if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
			throw new ConflictException("Ya existe un usuario con el email " + email);
		}

		UserAccount user = new UserAccount();
		user.setName(requireText(request.getName(), "name"));
		user.setLastName(requireText(request.getLastName(), "lastName"));
		user.setEmail(email);
		user.setCity(requireText(request.getCity(), "city"));
		user.setWarehouseId(request.getWarehouseId());
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setRole(resolveDefaultRole());

		UserAccount saved = userRepository.save(user);
		log.info("Usuario {} registrado con rol {}", saved.getEmail(),
				saved.getRole() != null ? saved.getRole().getName() : "-");
		return saved;
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginDto request) {
		if (request == null) {
			throw new IllegalArgumentException("Debe indicar email y password");
		}
		String email = requireText(request.getEmail(), "email").toLowerCase();
		String password = requireText(request.getPassword(), "password");

		UserAccount user = userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new UnauthorizedException("Credenciales invalidas"));
		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new UnauthorizedException("Credenciales invalidas");
		}

		AuthResponse response = new AuthResponse();
		response.setToken(jwtService.generateToken(user));
		response.setTokenType("Bearer");
		response.setExpiresIn(jwtService.getExpirationSeconds());
		response.setUser(userAccountMapper.toDto(user));
		log.info("Login correcto de {}", user.getEmail());
		return response;
	}

	private Role resolveDefaultRole() {
		return roleRepository.findByNameIgnoreCase(defaultRoleName).orElseGet(() -> {
			Role role = new Role();
			role.setName(defaultRoleName);
			role.setPermissions(new ArrayList<>());
			Role saved = roleRepository.save(role);
			log.info("Rol por defecto {} creado", saved.getName());
			return saved;
		});
	}

	private String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " es obligatorio");
		}
		return value.trim();
	}
}
