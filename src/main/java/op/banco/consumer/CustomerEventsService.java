package op.banco.consumer;

import lombok.extern.slf4j.Slf4j;
import op.banco.documents.OperacionCuentaBanco;
import op.banco.documents.TipoOperacionBanco;
import op.banco.events.Event;
import op.banco.events.OperacionCreatedEvent;
import op.banco.events.TransaccionCreatedEvent;
import op.banco.service.OperacionBancoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomerEventsService {
	
	public static final String BANCO = "BCP";
	
	@Autowired
	private OperacionBancoService productoService;
	
	@KafkaListener(topics = "${topic.operacion.name:operacion}",containerFactory = "kafkaListenerContainerFactory",groupId = "grupo1")
	public void operacion(Event<?> event) {
		if (event.getClass().isAssignableFrom(OperacionCreatedEvent.class)) {
			OperacionCreatedEvent operacionCreatedEvent = (OperacionCreatedEvent) event;
			log.info("Received Customer created event .... with Id={}, data={}",
					operacionCreatedEvent.getId(),
					operacionCreatedEvent.getData().toString());
			System.out.println(operacionCreatedEvent.getData().getDni());
			System.out.println(operacionCreatedEvent.getData().getNumeroCelularOrigen());
			System.out.println(operacionCreatedEvent.getData().getNumeroCelularDestino());
			
			OperacionCuentaBanco op = new OperacionCuentaBanco();
			TipoOperacionBanco tipoOp = new TipoOperacionBanco("1",operacionCreatedEvent.getData().getTipoOperacion());
			
	        op.setDni(operacionCreatedEvent.getData().getDni());
	        op.setCuenta_origen(operacionCreatedEvent.getData().getNumeroCelularOrigen());
	        op.setCuenta_destino(operacionCreatedEvent.getData().getNumeroCelularDestino());
	        op.setMontoPago(operacionCreatedEvent.getData().getMontoPago());
	        op.setTipoOperacion(tipoOp);
	        op.setCodigo_bancario_origen(BANCO);
	        op.setCodigo_bancario_destino(BANCO);
	        
	        productoService.envioYanki(op);
	        
	        System.out.println("Desde yanki");
			
		}

	}
	
	@KafkaListener(topics = "${topic.transaccion.name:transaccions}",containerFactory = "kafkaListenerContainerFactory",groupId = "grupo1")
	public void transaccionBootcoin(Event<?> event) {
		System.out.println("hola");
		System.out.println("EVENTO["+event);
		
		if (event.getClass().isAssignableFrom(TransaccionCreatedEvent.class)) {
			TransaccionCreatedEvent transaccionCreatedEvent = (TransaccionCreatedEvent) event;
			log.info("TransaccionCreatedEvent",
					transaccionCreatedEvent.getId(),
					transaccionCreatedEvent.getData().toString());
			System.out.println(transaccionCreatedEvent.getData().getOrden().getUsuario().getDni());
			System.out.println(transaccionCreatedEvent.getData().getOrigen());
			System.out.println(transaccionCreatedEvent.getData().getDestino());
			
			OperacionCuentaBanco op = new OperacionCuentaBanco();
			TipoOperacionBanco tipoOp = new TipoOperacionBanco("4","CuentaCuenta");
			op.setDni(transaccionCreatedEvent.getData().getOrden().getUsuario().getDni());
			op.setMontoPago(transaccionCreatedEvent.getData().getMontoPago());
	        op.setTipoOperacion(tipoOp);
	        op.setCodigo_bancario_origen(BANCO);
	        op.setCodigo_bancario_destino(BANCO);
			if(transaccionCreatedEvent.getData().getOrden().getTipoOperacion().toUpperCase().equals("VENTA")) {				
		        op.setCuenta_origen(transaccionCreatedEvent.getData().getDestino());
		        op.setCuenta_destino(transaccionCreatedEvent.getData().getOrigen());		        
			}else if(transaccionCreatedEvent.getData().getOrden().getTipoOperacion().toUpperCase().equals("COMPRA")) {				
				op.setCuenta_origen(transaccionCreatedEvent.getData().getOrigen());
		        op.setCuenta_destino(transaccionCreatedEvent.getData().getDestino());
			}
			
	        productoService.envioBoitcoin(op);
	        
	        System.out.println("Desde yanki");
			
		}
		
	}

	

}
