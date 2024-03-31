package op.banco.events;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OperacionCreatedEvent extends Event<Operacion> {

}
