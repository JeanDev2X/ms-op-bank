package spring.boot.webflu.ms.op.banco.app.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import spring.boot.webflu.ms.op.banco.app.documents.OperacionCuentaBanco;
import spring.boot.webflu.ms.op.banco.app.service.OperacionBancoService;

@RequestMapping("/api/operacionBancaria") //  OperCuentasCorrientes
@RestController
public class OperacionBancoControllers {

	@Autowired
	private OperacionBancoService productoService;

	@GetMapping
	public Mono<ResponseEntity<Flux<OperacionCuentaBanco>>> findAll() {
		return Mono.just(
				ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(productoService.findAllOperacion())

		);
	}

	@GetMapping("/{id}")
	public Mono<ResponseEntity<OperacionCuentaBanco>> viewId(@PathVariable String id) {
		return productoService.findByIdOperacion(id)
				.map(p -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(p))
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}
	
			
}



