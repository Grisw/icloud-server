package coo.lxt.island.server.icloud.jpa;

import coo.lxt.island.server.icloud.table.ICloudWebServiceDO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface IICloudWebServiceRepository {
    Optional<ICloudWebServiceDO> findByServiceName(String serviceName);

    List<ICloudWebServiceDO> findAll();

    @Modifying
    @Transactional(rollbackFor = Exception.class)
    @Query(value = "insert into icloud_web_services (service_name, url, status) values " +
            "(:serviceName, :url, :status) on duplicate key update " +
            "service_name=:serviceName, url=:url, status=:status", nativeQuery = true)
    int saveOrUpdate(String serviceName, String url, String status);
}