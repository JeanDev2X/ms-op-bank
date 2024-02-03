package spring.boot.webflu.ms.op.banco.app.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import spring.boot.webflu.ms.op.banco.app.dao.OperacionBancoDao;
import spring.boot.webflu.ms.op.banco.app.documents.OperacionCuentaBanco;
import spring.boot.webflu.ms.op.banco.app.service.OperacionBancoService;

@Service
public class OperacionBancoServiceImpl implements OperacionBancoService {
	
	Double comision = 0.0;

	private static final Logger log = LoggerFactory.getLogger(OperacionBancoServiceImpl.class);
	
	@Autowired
	public OperacionBancoDao productoDao;

	@Autowired
	public OperacionBancoDao tipoProductoDao;

	@Override
	public Flux<OperacionCuentaBanco> findAllOperacion() {
		return productoDao.findAll();

	}

	@Override
	public Mono<OperacionCuentaBanco> findByIdOperacion(String id) {
		return productoDao.findById(id);

	}
	
	@Override
	public Mono<OperacionCuentaBanco> saveOperacion(OperacionCuentaBanco producto) {
		return productoDao.save(producto);
	}

}
