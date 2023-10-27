package net.wanji.framework.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.Charset;

/**
 * Redis使用FastJson序列化
 * 
 * @author ruoyi
 */
public class FastJsonNoClassRedisSerializer<T> implements RedisSerializer<T>
{
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private Class<T> clazz;

    public FastJsonNoClassRedisSerializer(Class<T> clazz)
    {
        super();
        this.clazz = clazz;
    }

    @Override
    public byte[] serialize(T t) throws SerializationException
    {
        if (t == null)
        {
            return new byte[0];
        }
        return JSON.toJSONString(t).getBytes(DEFAULT_CHARSET);
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException
    {
        if (bytes == null || bytes.length <= 0)
        {
            return null;
        }
        String str = new String(bytes, DEFAULT_CHARSET);

        str = str.replace("cn.net.wanji.system.api","net.wanji.common.core.domain");
        str = str.replace("Set[\"*:*:*\"]","[\"*:*:*\"]");
        str = str.replace("\"roles\":Set[\"admin\"],","");
        str = str.replace("sysUser","user");
        str = str.replace("\"admin\":true,", "\"@type\":\"net.wanji.common.core.domain.entity.SysUser\",\"admin\":true,");
//        clazz.isLocalClass();
        return JSON.parseObject(str, clazz, JSONReader.Feature.SupportAutoType);
    }
}