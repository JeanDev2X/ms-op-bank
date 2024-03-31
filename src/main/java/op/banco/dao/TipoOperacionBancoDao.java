package op.banco.dao;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import op.banco.documents.TipoOperacionBanco;

public interface TipoOperacionBancoDao extends ReactiveMongoRepository<TipoOperacionBanco, String> {

}
