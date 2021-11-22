package com.example.service.impl;

import com.example.dao.UserDao;
import com.example.entity.User;
import com.example.exception.CustomException;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author chenzufeng
 * @date 2021/11/17
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Override
    public User login(User user) {
        User userDb = userDao.login(user);
        // 如果使用 if(userDb.getId()) 会出现空指针
        if (userDb != null) {
            return userDb;
        }

        throw new CustomException("登录失败！");
    }
}
