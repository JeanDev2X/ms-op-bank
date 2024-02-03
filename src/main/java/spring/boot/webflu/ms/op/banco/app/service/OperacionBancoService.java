package spring.boot.webflu.ms.op.banco.app.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import spring.boot.webflu.ms.op.banco.app.documents.OperacionCuentaBanco;

public interface OperacionBancoService {

	Flux<OperacionCuentaBanco> findAllOperacion();
	Mono<OperacionCuentaBanco> findByIdOperacion(String id);
	Mono<OperacionCuentaBanco> saveOperacion(OperacionCuentaBanco operacion);
	//------------------------------------------------------------------------
	Mono<OperacionCuentaBanco> saveOperacionRetiro(OperacionCuentaBanco operacion);
}
