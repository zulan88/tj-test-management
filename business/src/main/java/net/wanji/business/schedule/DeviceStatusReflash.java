package net.wanji.business.schedule;

import net.wanji.business.entity.TjDeviceDetail;
import net.wanji.business.entity.TjTask;
import net.wanji.business.service.TjDeviceDetailService;
import net.wanji.business.service.TjTaskService;
import net.wanji.common.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
public class DeviceStatusReflash {

    @Autowired
    TjDeviceDetailService tjDeviceDetailService;

    @Autowired
    private TjTaskService tjTaskService;

    @Autowired
    private RedisUtil redisUtil;

    @Scheduled(cron = "0 */5 * * * ?")
    public void reflash() {
        List<TjDeviceDetail> list = tjDeviceDetailService.list();
        for (TjDeviceDetail tjDeviceDetail : list) {
            Integer status = tjDeviceDetailService.selectDeviceState(tjDeviceDetail.getDeviceId(), tjDeviceDetail.getCommandChannel(), false);
            tjDeviceDetail.setStatus(status);
        }
        tjDeviceDetailService.updateBatchById(list);
    }

    /**
     * 判断任务是否逾期
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void editstatus(){
        List<TjTask> list = tjTaskService.list();
        for (TjTask tjTask : list) {
            Date date = new Date();
            if(date.compareTo(tjTask.getEndTime())>0){
                tjTask.setOpStatus(4);
                tjTaskService.updateById(tjTask);
            }
        }
    }

    @Scheduled(cron = "0 */15 * * * ?")
    public void editTWstatus(){
        List<TjTask> list = tjTaskService.list();
        for (TjTask tjTask : list) {
            if(tjTask.getStatus().equals("prepping")){
                if(!redisUtil.exists("tw_"+tjTask.getId())){
                    tjTask.setStatus(tjTask.getLastStatus());
                    tjTaskService.updateById(tjTask);
                }
            }
        }
    }

}
