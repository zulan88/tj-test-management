package net.wanji.business.schedule;

import net.wanji.business.entity.TjDeviceDetail;
import net.wanji.business.service.TjDeviceDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeviceStatusReflash {

    @Autowired
    TjDeviceDetailService tjDeviceDetailService;

    @Scheduled(cron = "0 */5 * * * *")
    public void reflash() {
        List<TjDeviceDetail> list = tjDeviceDetailService.list();
        for (TjDeviceDetail tjDeviceDetail : list) {
            Integer status = tjDeviceDetailService.selectDeviceState(tjDeviceDetail.getDeviceId(), tjDeviceDetail.getCommandChannel(), false);
            tjDeviceDetail.setStatus(status);
        }
        tjDeviceDetailService.updateBatchById(list);
    }

}
