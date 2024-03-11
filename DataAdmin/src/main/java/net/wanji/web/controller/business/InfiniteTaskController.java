package net.wanji.web.controller.business;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.wanji.business.service.TjInfinityTaskService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author hcy
 * @version 1.0
 * @className InfiniteTaskController
 * @description TODO
 * @date 2024/3/11 10:54
 **/
@Api(tags = "特色测试服务-测试任务-无限里程")
@Slf4j
@RestController
@RequestMapping("/taskInfinite")
public class InfiniteTaskController {
  private final TjInfinityTaskService tjInfinityTaskService;

  public InfiniteTaskController(TjInfinityTaskService tjInfinityTaskService) {
    this.tjInfinityTaskService = tjInfinityTaskService;
  }

  // 任务新增

  // 任务删除
  // 任务更新
  // 任务查询
}
