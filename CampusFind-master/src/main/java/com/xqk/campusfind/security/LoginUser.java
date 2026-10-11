package com.xqk.campusfind.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录用户主体：作为 JWT 解析后放入 SecurityContext 的 principal，
 * 携带后续鉴权所需的用户核心信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {
    private Long id;
    private String username;
    private Integer role; // 0-普通用户(地铁员工) 1-系统管理员
}
