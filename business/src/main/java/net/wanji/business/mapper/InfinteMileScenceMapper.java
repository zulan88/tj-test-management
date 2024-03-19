package net.wanji.business.mapper;

import net.wanji.business.domain.InfinteMileScenceExo;
import net.wanji.business.entity.InfinteMileScence;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author wj
 * @since 2024-02-23
 */
public interface InfinteMileScenceMapper extends BaseMapper<InfinteMileScence> {

    @Select("SELECT im.* , mp.name as map_name FROM infinte_mile_scence im LEFT JOIN tj_atlas_venue mp on im.map_id=mp.id ORDER BY im.create_date DESC")
    List<InfinteMileScenceExo> selectInfinteMileScenceExo();

    @Select("SELECT slice_img FROM infinte_mile_scence WHERE id = #{id}")
    String getSliceImage(@Param("id") Integer id);

}
