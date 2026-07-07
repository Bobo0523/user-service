package org.example.Mapper;
import org.apache.ibatis.annotations.*;
import org.example.Entity.User;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM users WHERE id = #{id}")
    User selectById(int id);


    // 2. 根据用户名查询用户（已补全注解）
    @Select("SELECT * FROM users WHERE username = #{username}")
    User selectByUsername(String username);

    // 3. 插入新用户（已补全，并配置了自动回填自增 ID）
    @Insert("INSERT INTO users(username, password, email) " +
            "VALUES(#{username}, #{password}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id") // 让 MyBatis 自动把生成的主键 ID 赋值给 user 对象的 id 属性
    void insert(User user);

    // 4. 更新用户信息（已补全，通常只更新非空字段，注解方式适合全量更新）
    @Update("UPDATE users SET username = #{username}, password = #{password}, " +
            "email = #{email} WHERE id = #{id}")
    int update(User user);

    // 5. 根据 ID 删除用户
    @Delete("DELETE FROM users WHERE id = #{id}")
    int delete(Long id);
}
