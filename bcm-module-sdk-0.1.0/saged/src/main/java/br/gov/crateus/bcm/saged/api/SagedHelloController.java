package br.gov.crateus.bcm.saged.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/saged")
@Tag(name = "saged")
public class SagedHelloController {

	@GetMapping("/hello")
	@Operation(summary = "Smoke test do módulo SAGED")
	public Map<String, String> hello() {
		return Map.of("module", "saged", "status", "up");
	}
}