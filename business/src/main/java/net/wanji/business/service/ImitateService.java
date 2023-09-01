package net.wanji.business.service;

import com.alibaba.fastjson.JSONObject;
import net.wanji.business.common.Constants.Extension;
import net.wanji.business.common.Constants.WebsocketKey;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjCasePartConfigMapper;
import net.wanji.common.common.SimulationTrajectoryDto;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.config.WanjiConfig;
import net.wanji.common.utils.GeoUtil;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.file.FileUploadUtils;
import net.wanji.common.utils.file.FileUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/7 13:05
 * @Descriptoin:
 */
@Service
public class ImitateService {

    private static final Logger log = LoggerFactory.getLogger("business");

    @Autowired
    private RestService restService;

    @Autowired
    private TjCasePartConfigMapper casePartConfigMapper;

    public void saveRouteFile(Integer caseId, List<SimulationTrajectoryDto> data) throws ExecutionException, InterruptedException {
        log.info(StringUtils.format("保存{}路径文件", caseId));
        TjCase tjCase = new TjCase();
        tjCase.setId(caseId);
        // 保存本地文件
        try {
            String path = FileUtils.writeE1List(data, WanjiConfig.getRoutePath(), Extension.TXT);
            log.info("saveRouteFile routePath:{}", path);
            tjCase.setRouteFile(path);
            tjCase.setUpdatedDate(LocalDateTime.now());
//            tjCaseMapper.updateById(tjCase);
            log.info("更新完成");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
