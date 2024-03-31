package op.banco.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import op.banco.dao.TipoOperacionBancoDao;
import op.banco.documents.TipoOperacionBanco;
import op.banco.service.TipoOperacionBancoService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TipoOperacionBancoServiceImpl implements TipoOperacionBancoService{
	
	@Autowired
	public TipoOperacionBancoDao  tipoProductoDao;
	
	@Override
	public Flux<TipoOperacionBanco> findAllTipoproducto()
	{
	return tipoProductoDao.findAll();
	
	}
	@Override
	public Mono<TipoOperacionBanco> findByIdTipoProducto(String id)
	{
	return tipoProductoDao.findById(id);
	
	}
	
	@Override
	public Mono<TipoOperacionBanco> saveTipoProducto(TipoOperacionBanco tipoCliente)
	{
	return tipoProductoDao.save(tipoCliente);
	}
	
	@Override
	public Mono<Void> deleteTipo(TipoOperacionBanco tipoProducto) {
		return tipoProductoDao.delete(tipoProducto);
	}
	
}
