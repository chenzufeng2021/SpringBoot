package com.example.dao;

import com.example.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
@Mapper
public interface UserDao {
    User login(User user);
}
