package spring.boot.webflu.ms.op.banco.app.service.impl;

import java.util.Date;

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
import spring.boot.webflu.ms.op.banco.app.dto.TipoProducto;
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
	
	//----------------------------------------------------------------------------------------------

	//RETIROS - TRANSACCION : UPDATE-CUENTAS-SALDO - 2 TRACCIONES COBRA COMISION(RETIRO O DEPOSITO) - TIPO TARGETA
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
				if (c1.getSaldo() == 0) {

					throw new RequestException(
							"NO PUEDE REALIZAR RETIROS, MONTO MINIMO EN LA CUENTA S/.0");
				}
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
//						return Mono.empty();
						return Mono.error(new InterruptedException("TARGETA INVALIDA"));
					}
					
					//PARA QUE REGISTRE UNA TRANSACCION
					TipoOperacionBanco tipo = new TipoOperacionBanco();					
					tipo.setId("2");
					tipo.setDescripcion("retiro");
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
	
	//DEPOSITO - 2 TRACCIONES COBRA COMISION(RETIRO O DEPOSITO)
	@Override
	public Mono<OperacionCuentaBanco> saveOperacionDeposito(OperacionCuentaBanco operacion) {
		//PARA OBTENER LA CUENTA DE BANCO - CUENTA BANCARIA
		Mono<CuentaBanco> oper1 = productoBancoClient.findByNumeroCuenta(operacion.getCuenta_origen(),operacion.getCodigo_bancario_origen());
		
		return oper1.flatMap(c1 -> {
			
			System.out.println("Datos cuenta : " + c1.toString());			
			System.out.println("Datos tipo Cuenta : " + c1.getTipoProducto().toString());
			
			if (c1.getTipoProducto().getId().equalsIgnoreCase("1")) { //ahorro
				comision = 2.0;
				operacion.setProductoComision(c1.getTipoProducto().getDescripcion());
			} else if (c1.getTipoProducto().getId().equalsIgnoreCase("2")) {//corriente
				comision = 3.0;
				operacion.setProductoComision(c1.getTipoProducto().getDescripcion());
			} else if (c1.getTipoProducto().getId().equalsIgnoreCase("3")) {//plazo fijo
				comision = 4.0;
				operacion.setProductoComision(c1.getTipoProducto().getDescripcion());				
				//4 cuenta ahorro personal VIP				
				//5 empresarial PYME 				
			}else if (c1.getTipoProducto().getId().equalsIgnoreCase("4")
					|| c1.getTipoProducto().getId().equalsIgnoreCase("5")
					) {
				
				comision = 5.0;
				operacion.setProductoComision(c1.getTipoProducto().getDescripcion());
				if (c1.getSaldo() == 0) {
					throw new RequestException(
							"NO PUEDE REALIZAR RETIROS, MONTO MINIMO EN LA CUENTA S/.0");
				}
			}
			
			//CONSULTAR EL NUMERO DE OPERACIONES
			Mono<Long> valor = productoDao.
					consultaMovimientos(operacion.getDni(), operacion.getCuenta_origen(),c1.getCodigoBanco())
					.count();
			
			return valor.flatMap(p -> {
				if (p > 2) {
					//ASIGNA LA COMISION
					System.out.println("Numero de transacciones >>>>>>" + p);
					operacion.setComision(comision);					
				}
				//REALIZA EL DEPOSITO EN LA CUENTA DE BANCO
				Mono<CuentaBanco> oper = productoBancoClient
						.despositoBancario(operacion.getMontoPago(),operacion.getCuenta_origen(),operacion.getComision(),operacion.getCodigo_bancario_origen());
				
				System.out.println("paso el metodo");
				System.out.println("operacion -->" + operacion);
				
				return oper.flatMap(c -> {
					if (c.getNumeroCuenta() == null) {
						return Mono.error(new InterruptedException("TARGETA INVALIDA"));
					}

					//PARA QUE REGISTRE UNA TRANSACCION
					TipoOperacionBanco tipo = new TipoOperacionBanco();
					tipo.setId("1");
					tipo.setDescripcion("deposito");
					operacion.setTipoOperacion(tipo);
					
					
					return productoDao.save(operacion);

				});

			});
			
		});
		
		
	}

	//DEPOSITO y RETIROS - OPERACION TRANSFERENCIA DE CUENTA A CUENTA
	@Override
	public Mono<OperacionCuentaBanco> operacionCuentaCuenta(OperacionCuentaBanco operacion) {
		Mono<CuentaBanco> oper1 = productoBancoClient.findByNumeroCuenta(operacion.getCuenta_origen(),operacion.getCodigo_bancario_origen());
		
		oper1.subscribe(o -> System.out.println("Cliente" + o.toString()));
		
		return oper1.flatMap(c1 -> {
			
			if (c1.getTipoProducto().getId().equalsIgnoreCase("1")) {

				comision = 2.5;

			} else if (c1.getTipoProducto().getId().equalsIgnoreCase("2")) {

				comision = 3.5;

			} else if (c1.getTipoProducto().getId().equalsIgnoreCase("3")) {

				comision = 4.5;

			} else if (c1.getTipoProducto().getId().equalsIgnoreCase("4")
					|| c1.getTipoProducto().getId().equalsIgnoreCase("5")
					) {

				if (c1.getSaldo() == 0) {

					throw new RequestException(
							"Ya no puede realizar retiros, debe tener un monton minimo" + " de S/.0 en su cuenta.");
				}
			}
			
			//movimiento de operaciones
			Mono<Long> valor = productoDao
					.consultaMovimientos(operacion.getDni(), operacion.getCuenta_origen(),c1.getCodigoBanco())
					.count();
			
			return valor.flatMap(p -> {
				//EL número máximo de transacciones libres de comisiones 20. * Para probar se coloca *2
				if (p > 2) {
					operacion.setComision(comision);
				}
				
				//como va ha ser un deposito de una cuenta a otra, se debe de retirar de una cuenta para ser
				//depositada en otra
				
				//REALIZAR UN RETIRNO EN EL MS-PRODUCTO BANCARIO
				Mono<CuentaBanco> oper2 = productoBancoClient
						.retiroBancario(operacion.getCuenta_origen(),operacion.getMontoPago(),operacion.getComision(),operacion.getCodigo_bancario_origen());
				
				return oper2.flatMap(c->{
					
					if (c.getNumeroCuenta() == null) {
						return Mono.empty();
					}
					
					//REALIZAR UN PAGO/DEPOSITO DESTINO
					Mono<CuentaBanco> oper3 = productoBancoClient
							.despositoBancario(operacion.getMontoPago(),operacion.getCuenta_destino(),operacion.getComision(),operacion.getCodigo_bancario_destino());
					
					return oper3.flatMap(o -> {
						
						if (c.getNumeroCuenta() == null) {
							return Mono.empty();
						}
						
						TipoOperacionBanco tipo = new TipoOperacionBanco();
						tipo.setId("4");
						tipo.setDescripcion("cuentaCuenta");
						operacion.setTipoOperacion(tipo);

						return productoDao.save(operacion);
						
					});					
				});				
			});			
		});
		
		
	}

	//PAGO DE CUENTA CREDITO CON UNA CUENTA DE BANCO
	@Override
	public Mono<OperacionCuentaBanco> saveOperacionCuentaCuentaCredito(OperacionCuentaBanco operacion) {
		
		//OBTENER LA CUENTA DE BANCO
		Mono<CuentaBanco> oper1 = productoBancoClient.findByNumeroCuenta(operacion.getCuenta_origen(),operacion.getCodigo_bancario_origen());
		oper1.subscribe(o -> System.out.println("Cliente" + o.toString()));
		
		//OBTENER LA CUENTA DE CREDITO
		System.out.println("DATOS OPERACION : " + operacion.toString());			
		Mono<CuentaBanco> opCredito  = productoBancoCreditoClient.findByNumeroCuentaCredito(operacion.getCuenta_destino(),operacion.getCodigo_bancario_destino());
		
		return opCredito.defaultIfEmpty(new CuentaBanco()).flatMap(p->{
			
			if(p.getCodigoBanco() == null) {
				throw new RequestException("LA CUENTA DE CREDITO - NO EXISTE NO PUEDE PAGAR");
			}else {
				return oper1.flatMap(c1 -> {
					
					if (c1.getTipoProducto().getId().equalsIgnoreCase("1")) {  // si es producto = ahorro

						comision = 2.5;

					} else if (c1.getTipoProducto().getId().equalsIgnoreCase("2")) { //si es cuenta corrientes

						comision = 3.5;

					} else if (c1.getTipoProducto().getId().equalsIgnoreCase("3")) { //si es cuenta plazo fijo

						comision = 4.5;

						//las demas cuentas deben de tener un monto minimo
					} else if (c1.getTipoProducto().getId().equalsIgnoreCase("4")|| c1.getTipoProducto().getId().equalsIgnoreCase("5")) {

						comision = 5.0;
						
						if (c1.getSaldo() == 0) {

							throw new RequestException(
									"NO PUEDE REALIZAR RETIROS, MONTO MINIMO EN LA CUENTA S/.0");
						}
						
					}
					
					//consultar todso los moviemientos realizados
					Mono<Long> valor = productoDao.
							consultaMovimientos(operacion.getDni(), operacion.getCuenta_origen(),c1.getCodigoBanco())
							.count();
					
					return valor.flatMap(x -> {
					
						System.out.println("Numero de transacciones >>>>>>" + p);
						// NUMERO DE OPERACIONES -> MOVIMIENTOS
						if (x > 2) {
							System.out.println("Numero de transacciones >>>>>>" + p);
							operacion.setComision(comision);
						}
						
						//REALIZAR UN RETIRNO EN EL MS-PRODUCTO BANCARIO
						Mono<CuentaBanco> oper2 = productoBancoClient
								.retiroBancario(operacion.getCuenta_origen(),operacion.getMontoPago(),operacion.getComision(),operacion.getCodigo_bancario_origen());
						
						System.out.println("paso el metodo");
						
						return oper2.flatMap(c -> {
							if (c.getNumeroCuenta() == null) {
								return Mono.empty();
							}
							
							System.out.println("PARAMETROS : " + "MONTO : " + operacion.getMontoPago() + " BANCO : "  + operacion.getCodigo_bancario_origen());
							
							//REALIZAR UN PAGO DE UNA CUENTA DE CREDITO					
							Mono<CuentaBanco> oper3 = productoBancoCreditoClient.despositoBancario(operacion.getMontoPago(),operacion.getCuenta_destino(),operacion.getCodigo_bancario_destino());	
							
							System.out.println("paso un pago");
							
							return oper3.flatMap(d -> {
								
								if (c.getNumeroCuenta() == null) {
									return Mono.empty();
								}
								// PARA QUE REGISTRE UNA TRANSACCION
								TipoOperacionBanco tipo = new TipoOperacionBanco();
								tipo.setId("3");
								tipo.setDescripcion("cuentaCredito");
								operacion.setTipoOperacion(tipo);

								return productoDao.save(operacion);
								
							});
							
						});						
					});					
				});				
			}			
		});
		
	}

	@Override
	public Flux<OperacionCuentaBanco> findAllOperacionByDniCliente(String dni) {
		return productoDao.viewDniCliente(dni);
	}

	@Override
	public Flux<OperacionCuentaBanco> findComision(String dni,Date fecha) {
		return productoDao.findByDniAndComisionGreaterThanZero(dni,fecha);
	}

}
