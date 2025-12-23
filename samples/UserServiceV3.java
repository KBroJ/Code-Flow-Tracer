package com.example.service;

import com.example.vo.UserVO;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * UserService 세 번째 구현체 (V3)
 * 다중 구현체 경고 테스트용
 */
@Service("userServiceV3")
public class UserServiceV3 implements UserService {

    @Resource(name = "userDAO")
    private UserDAO userDAO;

    @Override
    public List<UserVO> selectUserList(Map<String, Object> params) {
        return userDAO.selectUserList(params);
    }

    @Override
    public UserVO selectUser(Long userId) {
        return userDAO.selectUser(userId);
    }
}
