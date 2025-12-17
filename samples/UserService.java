package egovframework.example.sample.service;

import java.util.List;

/**
 * 사용자 관리 Service 인터페이스
 */
public interface UserService {

    /**
     * 사용자 목록 조회
     */
    List<UserVO> selectUserList() throws Exception;

    /**
     * 사용자 상세 조회
     */
    UserVO selectUser(String userId) throws Exception;

    /**
     * 부서명 조회
     */
    String selectDeptName(String deptId) throws Exception;

    /**
     * 사용자 중복 체크
     */
    boolean checkDuplicateUser(String userId) throws Exception;

    /**
     * 사용자 등록
     */
    void insertUser(UserVO userVO) throws Exception;

    /**
     * 사용자 수정
     */
    void updateUser(UserVO userVO) throws Exception;

    /**
     * 사용자 삭제
     */
    void deleteUser(String userId) throws Exception;
}
