package br.gov.crateus.bcm.devhost.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Maps Keycloak realm_access.roles to ROLE_* authorities (same shape as BCM production).
 */
public class RealmRoleAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	@Override
	@SuppressWarnings("unchecked")
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		Map<String, Object> realmAccess = jwt.getClaim("realm_access");
		if (realmAccess == null) {
			return List.of();
		}
		Object rolesObj = realmAccess.get("roles");
		if (!(rolesObj instanceof Collection<?> roles)) {
			return List.of();
		}
		List<GrantedAuthority> authorities = new ArrayList<>();
		for (Object role : roles) {
			if (role != null) {
				authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
			}
		}
		return authorities;
	}
}
