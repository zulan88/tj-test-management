package net.wanji.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用例仿真记录表
 * </p>
 *
 * @author wj
 * @since 2023-07-03
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tj_case_verification")
public class TjCaseVerification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 用例ID
     */
    @TableField("case_id")
    private Integer caseId;

    /**
     * 用例仿真信息
     */
    @TableField("detail_info")
    private String detailInfo;

    /**
     * 状态（待核验：NEW；系统核验中：SYS_VERIFYING；系统核验完成：SYS_VERIFY_FINISHED；人工验证完成：FINISHED）；
     */
    @TableField("status")
    private String status;

    /**
     * 全量轨迹topic
     */
    @TableField("topic")
    private String topic;

    /**
     * 轨迹json文件地址
     */
    @TableField("local_file")
    private String localFile;
}
