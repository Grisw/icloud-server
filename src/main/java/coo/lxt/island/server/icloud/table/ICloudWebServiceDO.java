package coo.lxt.island.server.icloud.table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Getter
@Setter
@ToString
@Table(name = "icloud_web_services")
public class ICloudWebServiceDO implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false, unique = true, length = 255)
    private String serviceName;

    @Column(name = "url", length = 255)
    private String url;

    @Column(name = "status", length = 255)
    private String status;

}
