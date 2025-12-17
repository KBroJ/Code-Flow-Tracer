package egovframework.example.sample.service.impl;

import egovframework.example.sample.service.UserVO;
import egovframework.rte.psl.dataaccess.EgovAbstractDAO;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 사용자 관리 DAO
 *
 * 전자정부프레임워크 스타일의 예시 코드입니다.
 * iBatis/MyBatis를 사용하여 SQL을 호출합니다.
 */
@Repository("userDAO")
public class UserDAO extends EgovAbstractDAO {

    /**
     * 사용자 목록 조회
     */
    @SuppressWarnings("unchecked")
    public List<UserVO> selectUserList() throws Exception {
        return (List<UserVO>) list("userDAO.selectUserList", null);
    }

    /**
     * 사용자 상세 조회
     */
    public UserVO selectUser(String userId) throws Exception {
        return (UserVO) select("userDAO.selectUser", userId);
    }

    /**
     * 사용자 등록
     */
    public void insertUser(UserVO userVO) throws Exception {
        insert("userDAO.insertUser", userVO);
    }

    /**
     * 사용자 수정
     */
    public void updateUser(UserVO userVO) throws Exception {
        update("userDAO.updateUser", userVO);
    }

    /**
     * 사용자 삭제
     */
    public void deleteUser(String userId) throws Exception {
        delete("userDAO.deleteUser", userId);
    }

    /**
     * 사용자 권한 등록
     */
    public void insertUserRole(String userId, String roleId) throws Exception {
        UserRoleVO userRole = new UserRoleVO();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        insert("userDAO.insertUserRole", userRole);
    }

    /**
     * 사용자 권한 삭제
     */
    public void deleteUserRole(String userId) throws Exception {
        delete("userDAO.deleteUserRole", userId);
    }
}
