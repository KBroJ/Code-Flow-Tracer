package egovframework.example.sample.service.impl;

import egovframework.rte.psl.dataaccess.EgovAbstractDAO;
import org.springframework.stereotype.Repository;

/**
 * 부서 관리 DAO
 *
 * 전자정부프레임워크 스타일의 예시 코드입니다.
 * iBatis/MyBatis를 사용하여 SQL을 호출합니다.
 */
@Repository("deptDAO")
public class DeptDAO extends EgovAbstractDAO {

    /**
     * 부서 상세 조회
     */
    public DeptVO selectDept(String deptId) throws Exception {
        return (DeptVO) select("deptDAO.selectDept", deptId);
    }

    /**
     * 부서 목록 조회
     */
    @SuppressWarnings("unchecked")
    public java.util.List<DeptVO> selectDeptList() throws Exception {
        return (java.util.List<DeptVO>) list("deptDAO.selectDeptList", null);
    }
}
