package com.bigobooks.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.bigobooks.util.exception.UnauthorizedException;

/**
 * Acceso al usuario autenticado (JWT) desde los metodos generados por OpenAPI.
 * El servicio de Auth emite el claim "uid" con el id del usuario.
 */
@Component
public class CurrentUser {

	/** Id del usuario autenticado (claim uid). */
	public Long id() {
		Jwt jwt = jwt();
		if (jwt == null) {
			throw new UnauthorizedException("No hay un token valido en la peticion");
		}
		Object uid = jwt.getClaim("uid");
		if (uid instanceof Number number) {
			return number.longValue();
		}
		if (uid instanceof String text && !text.isBlank()) {
			try {
				return Long.valueOf(text.trim());
			} catch (NumberFormatException ignored) {
				// Se informa abajo con un mensaje claro.
			}
		}
		throw new UnauthorizedException("El token no contiene el id del usuario (uid)");
	}

	/** true si el usuario autenticado tiene alguno de los roles indicados. */
	public boolean hasAnyRole(String... roles) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return false;
		}
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			String value = authority.getAuthority();
			for (String role : roles) {
				if (value.equals(role) || value.equals("ROLE_" + role)) {
					return true;
				}
			}
		}
		return false;
	}

	private Jwt jwt() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
			return jwt;
		}
		return null;
	}
}
