package egovframework.example.sample.service.impl;

import egovframework.example.sample.service.UserService;
import egovframework.example.sample.service.UserVO;
import egovframework.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 사용자 관리 Service 구현체
 *
 * 전자정부프레임워크 스타일의 예시 코드입니다.
 */
@Service("userService")
public class UserServiceImpl extends EgovAbstractServiceImpl implements UserService {

    @Resource(name = "userDAO")
    private UserDAO userDAO;

    @Resource(name = "deptDAO")
    private DeptDAO deptDAO;

    @Override
    public List<UserVO> selectUserList() throws Exception {
        return userDAO.selectUserList();
    }

    @Override
    public UserVO selectUser(String userId) throws Exception {
        return userDAO.selectUser(userId);
    }

    @Override
    public String selectDeptName(String deptId) throws Exception {
        DeptVO dept = deptDAO.selectDept(deptId);
        return dept != null ? dept.getDeptName() : "";
    }

    @Override
    public boolean checkDuplicateUser(String userId) throws Exception {
        UserVO existingUser = userDAO.selectUser(userId);
        return existingUser != null;
    }

    @Override
    public void insertUser(UserVO userVO) throws Exception {
        // 비밀번호 암호화
        String encodedPassword = encodePassword(userVO.getPassword());
        userVO.setPassword(encodedPassword);

        userDAO.insertUser(userVO);

        // 사용자 권한 등록
        userDAO.insertUserRole(userVO.getUserId(), "ROLE_USER");
    }

    @Override
    public void updateUser(UserVO userVO) throws Exception {
        // 비밀번호가 변경된 경우에만 암호화
        if (userVO.getPassword() != null && !userVO.getPassword().isEmpty()) {
            String encodedPassword = encodePassword(userVO.getPassword());
            userVO.setPassword(encodedPassword);
        }

        userDAO.updateUser(userVO);
    }

    @Override
    public void deleteUser(String userId) throws Exception {
        // 사용자 권한 먼저 삭제
        userDAO.deleteUserRole(userId);
        // 사용자 삭제
        userDAO.deleteUser(userId);
    }

    private String encodePassword(String password) {
        // TODO: 실제 암호화 로직
        return password;
    }
}
