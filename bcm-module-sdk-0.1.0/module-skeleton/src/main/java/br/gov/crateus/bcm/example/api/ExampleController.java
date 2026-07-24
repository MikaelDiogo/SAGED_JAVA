package br.gov.crateus.bcm.example.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Replace this package with your domain module (rename {@code example} → your English module id).
 */
@RestController
@RequestMapping("/api/v1/example")
@Tag(name = "example")
public class ExampleController {

	@GetMapping("/hello")
	@Operation(summary = "Public hello for Dev Host smoke test")
	public Map<String, String> hello() {
		return Map.of("module", "example", "message", "Replace this controller with your domain API");
	}

	@GetMapping("/admin-only")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Requires realm role ADMIN — use to verify JWT wiring")
	public Map<String, String> adminOnly() {
		return Map.of("ok", "true");
	}
}
