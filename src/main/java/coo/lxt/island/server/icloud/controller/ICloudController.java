package coo.lxt.island.server.icloud.controller;

import coo.lxt.island.common.icloud.api.IICloudApi;
import coo.lxt.island.common.icloud.vo.ICloudAuthVO;
import coo.lxt.island.common.icloud.vo.ICloudSessionVO;
import coo.lxt.island.server.icloud.service.ICloudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/icloud")
public class ICloudController implements IICloudApi {

    @Autowired
    private ICloudService iCloudService;

    @Override
    public ICloudSessionVO login(ICloudAuthVO authVO) throws IOException {
        return iCloudService.login(authVO);
    }
}
