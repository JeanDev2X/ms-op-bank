package spring.boot.webflu.ms.op.banco.app.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import spring.boot.webflu.ms.op.banco.app.client.ProductoBancoClient;
import spring.boot.webflu.ms.op.banco.app.client.ProductoBancoCreditoClient;
import spring.boot.webflu.ms.op.banco.app.dao.OperacionBancoDao;
import spring.boot.webflu.ms.op.banco.app.documents.OperacionCuentaBanco;
import spring.boot.webflu.ms.op.banco.app.documents.TipoOperacionBanco;
import spring.boot.webflu.ms.op.banco.app.dto.CuentaBanco;
import spring.boot.webflu.ms.op.banco.app.service.OperacionBancoService;
import spring.boot.webflu.ms.op.banco.app.exception.RequestException;

@Service
public class OperacionBancoServiceImpl implements OperacionBancoService {
	
	Double comision = 0.0;

	private static final Logger log = LoggerFactory.getLogger(OperacionBancoServiceImpl.class);
	
	@Autowired
	public OperacionBancoDao productoDao;

	@Autowired
	public OperacionBancoDao tipoProductoDao;
		
	@Autowired
	private ProductoBancoClient productoBancoClient;
	
	@Autowired
	private ProductoBancoCreditoClient productoBancoCreditoClient;

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

	@Override
	public Mono<OperacionCuentaBanco> saveOperacionRetiro(OperacionCuentaBanco operacion) {
		//OBTENIENDO EL NUMERO DE CUENTA + EL BANCO AL QUE PERTENECE
		Mono<CuentaBanco> oper1 = productoBancoClient.findByNumeroCuenta(operacion.getCuenta_origen(),operacion.getCodigo_bancario_origen());
		
		System.out.println("listo");
		
		return oper1.flatMap(c1 -> {
			if (c1.getTipoProducto().getId().equalsIgnoreCase("1")) { //ahorro
				
				comision = 5.0;

			} else if (c1.getTipoProducto().getId().equalsIgnoreCase("2")) {//corriente

				comision = 20.0;

			} else if (c1.getTipoProducto().getId().equalsIgnoreCase("3")) {//plazo fijo

				comision = 30.0;

				//4 cuenta ahorro personal VIP				
				//5 empresarial PYME 				
			} else if (c1.getTipoProducto().getId().equalsIgnoreCase("4")	
					|| c1.getTipoProducto().getId().equalsIgnoreCase("5")
					) {				
				comision = 40.0;
			}
			
			//CONTAR EL NUMERO DE MOVIMIENTOS
			Mono<Long> valor = productoDao.
					consultaMovimientos(operacion.getDni(), operacion.getCuenta_origen(),c1.getCodigoBanco())
					.count();
			
			return valor.flatMap(p -> {
				// NUMERO DE COMISIONES, SI LOS MOVIMIENTOS ES MAYOR A 20 - 2 para pruebas
				System.out.println("--->>>>"+p);
				if (p > 2) {
					System.out.println("Numero de transacciones >>>>>>" + p);
					operacion.setComision(comision);
				}
				
				//REALIZAR UN RETIRNO EN EL MS-PRODUCTO BANCARIO
				Mono<CuentaBanco> opRetiro = productoBancoClient
						.retiroBancario(operacion.getCuenta_origen(),operacion.getMontoPago(),operacion.getComision(),operacion.getCodigo_bancario_origen());
				
				return opRetiro.flatMap(c -> {

					if (c.getNumeroCuenta() == null) {
						return Mono.empty();
					}
					
					//PARA QUE REGISTRE UNA TRANSACCION
					TipoOperacionBanco tipo = new TipoOperacionBanco();					
					tipo.setIdTipo("2");
					tipo.setDescripcion("Retiro");
					operacion.setTipoOperacion(tipo);

					return productoDao.save(operacion);

				});
			});			
		});
		
//		OperacionCuentaBanco opera = new OperacionCuentaBanco();
//		opera.setDni("12346");
//		Mono<OperacionCuentaBanco> op = Mono.just(opera);
//		op.subscribe(p -> System.out.println("-->" + p));		
//		return op;
	}

	//----------------------------------------------------------------------------------------------
	
	

}
