package coo.lxt.island.server.icloud.jpa.repo;

import coo.lxt.island.server.icloud.jpa.IICloudSessionRepository;
import coo.lxt.island.server.icloud.table.ICloudSessionDO;
import org.springframework.data.repository.Repository;


public interface ICloudSessionRepository extends Repository<ICloudSessionDO, Long>, IICloudSessionRepository {
}
