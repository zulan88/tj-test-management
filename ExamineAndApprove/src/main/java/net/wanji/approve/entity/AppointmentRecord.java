package net.wanji.approve.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 预约记录表
 * </p>
 *
 * @author wj
 * @since 2023-12-05
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("appointment_record")
public class AppointmentRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Integer id;

    /**
     * 流程单号
     */
    @TableField("record_id")
    private String recordId;

    /**
     * 单位名称
     */
    @TableField("unit_name")
    private String unitName;

    /**
     * 对接人
     */
    @TableField("contact_person")
    private String contactPerson;

    /**
     * 联系方式
     */
    @TableField("phone_number")
    private String phoneNumber;

    /**
     * 测试类型
     */
    @TableField("type")
    private String type;

    /**
     * 被测对象ID
     */
    @TableField("measurand_id")
    private Integer measurandId;

    /**
     * 被测对象类型
     */
    @TableField("measurand_type")
    private String measurandType;

    /**
     * 用例id
     */
    @TableField("case_ids")
    private String caseIds;

    /**
     * 提交时间
     */
    @TableField("commit_date")
    private LocalDateTime commitDate;

    /**
     * 期望的排期
     */
    @TableField("expected_date")
    private String expectedDate;

    /**
     * 测试排期
     */
    @TableField("test_schedule")
    private String testSchedule;

    /**
     * 状态  0-待审批 1-审批通过 2-驳回 3-已取消
     */
    @TableField("status")
    private Integer status;

    /**
     * 花费
     */
    @TableField("expense")
    private Integer expense;

    /**
     * 申请表
     */
    @TableField("application_from")
    private String applicationFrom;

    /**
     * 最后审批时间
     */
    @TableField("last_approval_time")
    private LocalDateTime lastApprovalTime;

    /**
     * 驳回理由
     */
    @TableField("rejection_mes")
    private String rejectionMes;

}
