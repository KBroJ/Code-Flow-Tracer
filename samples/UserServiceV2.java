package com.example.service;

import com.example.vo.UserVO;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * UserService 대체 구현체 (V2)
 *
 * 다중 구현체 경고 테스트용:
 * - UserServiceImpl과 동일한 UserService 인터페이스를 구현
 * - 정적 분석 시 어떤 구현체가 실제로 사용되는지 알 수 없음
 * - 분석 도구는 첫 번째 발견된 구현체(UserServiceImpl)를 사용
 */
@Service("userServiceV2")
public class UserServiceV2 implements UserService {

    @Resource(name = "userDAO")
    private UserDAO userDAO;

    @Override
    public List<UserVO> selectUserList(Map<String, Object> params) {
        // V2 구현: 캐싱 로직 추가 등 다른 구현 가능
        return userDAO.selectUserList(params);
    }

    @Override
    public UserVO selectUser(Long userId) {
        return userDAO.selectUser(userId);
    }
}
