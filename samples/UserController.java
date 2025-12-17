package egovframework.example.sample.web;

import egovframework.example.sample.service.UserService;
import egovframework.example.sample.service.UserVO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.util.List;

/**
 * 사용자 관리 Controller
 *
 * 전자정부프레임워크 스타일의 예시 코드입니다.
 */
@Controller
@RequestMapping("/user")
public class UserController {

    @Resource(name = "userService")
    private UserService userService;

    /**
     * 사용자 목록 조회
     */
    @GetMapping("/list.do")
    public String selectUserList(ModelMap model) throws Exception {
        List<UserVO> userList = userService.selectUserList();
        model.addAttribute("userList", userList);
        return "user/userList";
    }

    /**
     * 사용자 상세 조회
     */
    @GetMapping("/detail.do")
    public String selectUser(@RequestParam("userId") String userId, ModelMap model) throws Exception {
        UserVO user = userService.selectUser(userId);

        if (user != null) {
            // 부서 정보 추가 조회
            String deptName = userService.selectDeptName(user.getDeptId());
            user.setDeptName(deptName);
        }

        model.addAttribute("user", user);
        return "user/userDetail";
    }

    /**
     * 사용자 등록
     */
    @PostMapping("/insert.do")
    public String insertUser(UserVO userVO) throws Exception {
        // 중복 체크
        boolean isDuplicate = userService.checkDuplicateUser(userVO.getUserId());

        if (isDuplicate) {
            return "redirect:/user/list.do?error=duplicate";
        }

        userService.insertUser(userVO);
        return "redirect:/user/list.do";
    }

    /**
     * 사용자 수정
     */
    @PostMapping("/update.do")
    public String updateUser(UserVO userVO) throws Exception {
        userService.updateUser(userVO);
        return "redirect:/user/detail.do?userId=" + userVO.getUserId();
    }

    /**
     * 사용자 삭제
     */
    @PostMapping("/delete.do")
    public String deleteUser(@RequestParam("userId") String userId) throws Exception {
        userService.deleteUser(userId);
        return "redirect:/user/list.do";
    }
}
