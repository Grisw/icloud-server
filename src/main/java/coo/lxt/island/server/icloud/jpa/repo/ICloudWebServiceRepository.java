package coo.lxt.island.server.icloud.jpa.repo;

import coo.lxt.island.server.icloud.jpa.IICloudWebServiceRepository;
import coo.lxt.island.server.icloud.table.ICloudWebServiceDO;
import org.springframework.data.repository.Repository;


public interface ICloudWebServiceRepository extends Repository<ICloudWebServiceDO, Long>, IICloudWebServiceRepository {
}
