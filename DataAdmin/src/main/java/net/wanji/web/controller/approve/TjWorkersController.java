package net.wanji.web.controller.approve;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import net.wanji.approve.entity.TjWorkers;
import net.wanji.approve.service.TjWorkersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/workers")
@Api(tags = "工作人员管理")  // 添加Controller的Swagger标签
public class TjWorkersController {

    @Autowired
    private TjWorkersService workersService;

    // 分页查询，带条件
    @GetMapping("/list")
    @ApiOperation("分页查询工作人员列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNum", value = "页码", dataType = "int", paramType = "query", defaultValue = "1"),
            @ApiImplicitParam(name = "pageSize", value = "每页记录数", dataType = "int", paramType = "query", defaultValue = "10"),
            @ApiImplicitParam(name = "type", value = "人员类型", dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "name", value = "姓名", dataType = "String", paramType = "query")
    })
    public IPage<TjWorkers> listWorkers(@RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "10") int pageSize,
                                        @RequestParam(required = false) String type,
                                        @RequestParam(required = false) String name) {
        return workersService.listWorkers(pageNum, pageSize, type, name);
    }

    // 根据id查询
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询工作人员")
    @ApiImplicitParam(name = "id", value = "工作人员ID", dataType = "Long", paramType = "path")
    public TjWorkers getWorkerById(@PathVariable Long id) {
        return workersService.getWorkerById(id);
    }

    // 新增
    @PostMapping("/add")
    @ApiOperation("新增工作人员")
    @ApiImplicitParam(name = "worker", value = "工作人员信息", dataType = "TjWorkers", paramType = "body")
    public boolean addWorker(@RequestBody TjWorkers worker) {
        return workersService.addWorker(worker);
    }

    // 更新
    @PutMapping("/update")
    @ApiOperation("更新工作人员信息")
    @ApiImplicitParam(name = "worker", value = "工作人员信息", dataType = "TjWorkers", paramType = "body")
    public boolean updateWorker(@RequestBody TjWorkers worker) {
        return workersService.updateWorker(worker);
    }

    // 根据id删除
    @DeleteMapping("/delete/{id}")
    @ApiOperation("根据ID删除工作人员")
    @ApiImplicitParam(name = "id", value = "工作人员ID", dataType = "Long", paramType = "path")
    public boolean deleteWorkerById(@PathVariable Integer id) {
        return workersService.deleteWorkerById(id);
    }

    // 批量删除
    @DeleteMapping("/deleteBatch")
    @ApiOperation("批量删除工作人员")
    @ApiImplicitParam(name = "ids", value = "工作人员ID列表", dataType = "List<Long>", paramType = "body")
    public boolean deleteWorkersByIds(@RequestBody List<Integer> ids) {
        return workersService.deleteWorkersByIds(ids);
    }
}