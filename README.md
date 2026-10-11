# CampusFind 地铁失物招领系统 — 项目学习文档

> 本文档旨在帮助你**快速看懂、学会**这个项目的整体架构、核心代码和开发思路。  
> 适合 Spring Boot + Vue 3 初学者、毕设答辩准备、二次开发入门。

---

## 一、项目概览

### 1.1 项目是什么？

这是一个 **地铁失物招领管理平台**，分两类用户：

| 角色 | 能做什么 |
|------|---------|
| **普通员工**（role=0） | 发布寻物/招领启事、浏览大厅、申请认领、确认归还 |
| **管理员**（role=1） | 审核发布申请、查看统计数据 |

### 1.2 核心业务流程

```
用户发布物品 → 管理员审核通过 → 物品出现在大厅
     ↓                                    ↓
另一方用户看到 ← ← ← ← ← ← ← ← ← ← 失主/拾获者申请认领
     ↓
发布者同意 → 双方交换联系方式 → 确认归还 → 自动归档
```

### 1.3 技术栈一览

| 层级 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot | 3.2.5 |
| **ORM 框架** | MyBatis-Plus | 3.5.10.1 |
| **数据库** | MySQL | 8.0 |
| **认证鉴权** | Spring Security + JWT（jjwt） | 6.x / 0.11.5 |
| **密码加密** | BCrypt（Spring Security Crypto） | — |
| **对象存储** | MinIO（S3 兼容） | SDK 8.5.12 |
| **前端框架** | Vue 3（Composition API） | 3.4+ |
| **UI 组件库** | Element Plus | 2.7+ |
| **HTTP 客户端** | Axios | 1.7+ |
| **前端构建** | Vite | 5.4+ |            
| **JDK** | Oracle OpenJDK | 17 |

---

## 二、项目目录结构详解

```
CampusFind-master/
├── database/
│   └── init_database.sql          ← 数据库初始化脚本（建库建表+测试数据）
├── src/main/
│   ├── java/com/xqk/campusfind/
│   │   ├── CampusFindApplication.java   ← Spring Boot 启动类（也定义了 BCrypt Bean）
│   │   ├── common/                      ← 公共模块
│   │   │   ├── Result.java              ← 统一响应封装（code + message + data）
│   │   │   ├── ResultCode.java          ← 状态码枚举（200/400/401/500）
│   │   │   └── GlobalExceptionHandler.java ← 全局异常拦截器
│   │   ├── config/                      ← 配置类
│   │   │   ├── SecurityConfig.java      ← Spring Security + JWT + 统一跨域
│   │   │   └── MinioConfig.java         ← MinIO 客户端 Bean（对象存储）
│   │   ├── security/                    ← 认证鉴权模块 ⭐
│   │   │   ├── JwtUtil.java             ← JWT 签发 / 解析
│   │   │   ├── JwtAuthenticationFilter.java ← 令牌过滤器
│   │   │   ├── LoginUser.java           ← 登录主体（id/username/role）
│   │   │   └── SecurityUtils.java       ← 从令牌读取当前用户
│   │   ├── entity/                      ← 实体类（对应数据库表）
│   │   │   ├── User.java                ← 用户表
│   │   │   ├── Item.java                ← 物品表
│   │   │   └── Claim.java               ← 认领记录表
│   │   ├── dto/
│   │   │   └── MatchResult.java         ← 智能匹配结果（非数据库表）
│   │   ├── mapper/                      ← 数据访问层（MyBatis-Plus BaseMapper）
│   │   │   ├── UserMapper.java
│   │   │   ├── ItemMapper.java          ← 含自定义多表联查方法
│   │   │   └── ClaimMapper.java         ← 含自定义多表联查方法
│   │   ├── service/                     ← 业务逻辑层
│   │   │   ├── UserService.java / impl/
│   │   │   ├── ItemService.java / impl/ ← 核心：智能匹配算法在这里
│   │   │   └── ClaimService.java / impl/
│   │   └── controller/                  ← 控制层（对外暴露 REST API）
│   │       ├── UserController.java      ← 登录 + 注册
│   │       ├── ItemController.java      ← 物品增删查 + 审核 + 上传 + 匹配
│   │       └── ClaimController.java     ← 认领全流程
│   └── resources/
│       ├── application.yml              ← 核心配置文件
│       ├── mapper/                      ← MyBatis XML（复杂 SQL 写在这里）
│       │   ├── UserMapper.xml
│       │   ├── ItemMapper.xml
│       │   └── ClaimMapper.xml
│       └── static/src/                  ← 前端 Vue 3 源码
│           ├── main.js                  ← Vue 入口
│           ├── App.vue                  ← 根组件
│           ├── router/index.js          ← 路由配置
│           ├── api/
│           │   ├── request.js           ← Axios 封装（自动携带 JWT + 401/403 处理）
│           │   └── index.js             ← 所有 API 接口定义
│           └── views/                   ← 页面组件
│               ├── Login.vue            ← 登录页
│               ├── Register.vue         ← 注册页
│               ├── Layout.vue           ← 主框架（侧边栏+顶栏）
│               ├── Home.vue             ← 数据大厅（首页表格）
│               ├── Add.vue              ← 发布物品（含智能匹配）
│               ├── Admin.vue            ← 管理员审核页
│               ├── My.vue               ← 我的认领记录
│               ├── MyPublish.vue        ← 我的发布
│               └── MyPendingClaims.vue  ← 待确认认领
├── minio/                               ← 对象存储（项目自包含，不依赖外部盘）
│   ├── minio.exe                        ← MinIO 可执行文件
│   └── data/                            ← 数据目录（含 lostfound 桶）
├── start-minio.ps1                      ← 一键启动 MinIO（无需 Docker）
├── docker-compose.yml                   ← 可选：用 Docker 启动 MinIO
└── pom.xml                              ← Maven 依赖管理
```

---

## 三、数据库设计（3 张表）

### 3.1 user 表 — 用户

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT（自增主键） | 用户唯一标识 |
| username | VARCHAR(50) | 用户名（唯一） |
| password | VARCHAR(255) | BCrypt 加密后的密码密文 |
| role | INT | 0=普通员工，1=管理员 |

### 3.2 item 表 — 物品

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT（自增主键） | 物品唯一标识 |
| image | VARCHAR(500) | 图片 URL |
| name | VARCHAR(100) | 物品名称 |
| type | VARCHAR(50) | 分类（数码/证件/图书等） |
| description | TEXT | 特征描述 |
| location | VARCHAR(200) | 地点 |
| time | VARCHAR(50) | 时间 |
| **status** | INT | **核心状态字段**：0=待审核, 1=已发布, 2=认领中, 3=已归还 |
| **publish_type** | INT | 1=招领启事(捡到), 2=寻物启事(丢失) |
| contact_info | VARCHAR(200) | 联系方式（确认后展示） |
| user_id | BIGINT | 发布者 ID（外键关联 user） |

### 3.3 claim 表 — 认领记录

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT（自增主键） | 认领记录唯一标识 |
| item_id | BIGINT | 关联的物品 ID |
| user_id | BIGINT | 申请者 ID |
| description | TEXT | 认领说明/凭证 |
| **status** | INT | **核心状态字段**：0=审核中, 1=已通过, 2=已驳回, 5=已归档 |

### 3.4 表关系图

```
user (1) ──────< (N) item       一个用户可以发布多个物品
user (1) ──────< (N) claim      一个用户可以发起多次认领
item (1) ──────< (N) claim      一个物品可以被多人申请认领
```

---

## 四、后端核心代码逐层讲解

### 4.1 启动类 — CampusFindApplication.java

```java
@SpringBootApplication  // 标记为 Spring Boot 应用
public class CampusFindApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusFindApplication.class, args);  // 启动
    }

    @Bean  // 把 BCrypt 加密器注册到 Spring 容器
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**学习要点**：`@Bean` 注解让 `PasswordEncoder` 成为 Spring 管理的单例对象，任何地方都可以通过 `@Autowired` 注入使用。

### 4.2 统一响应封装 — Result.java

```java
@Data
public class Result<T> {
    private Integer code;     // 状态码
    private String message;   // 提示信息
    private T data;           // 返回数据

    public static <T> Result<T> success(T data) { ... }  // 成功
    public static <T> Result<T> error(String message) { ... }  // 失败
}
```

**学习要点**：所有接口都返回统一格式 `{ code: 200, message: "操作成功", data: {...} }`，前端只需判断 `code == 200` 即可。

### 4.3 实体类 — 以 Item.java 为例

```java
@Data                    // Lombok：自动生成 getter/setter/toString
@TableName("item")       // MyBatis-Plus：映射到 item 表
public class Item {
    @TableId(type = IdType.AUTO)  // 主键自增
    private Long id;
    private String name;
    private Integer status;       // 0=待审核, 1=已发布, 2=认领中, 3=已归还
    private Integer publishType;  // 1=招领, 2=寻物

    @TableField(exist = false)    // ⭐ 关键字段：不映射数据库，仅用于展示
    private String claimerName;   // 认领人名称（联查时填充）
    @TableField(exist = false)
    private String publisherName; // 发布者名称（联查时填充）
}
```

**学习要点**：`@TableField(exist = false)` 是 MyBatis-Plus 的精髓——数据库没有这个字段，但联表查询时需要展示。

### 4.4 Mapper 层 — 数据访问

**简单查询**（MyBatis-Plus 内置，不用写 SQL）：

```java
// UserMapper 继承 BaseMapper<User>，自动拥有：
// selectById(), selectList(), insert(), updateById(), deleteById() 等
```

**复杂联查**（需要写 XML）：

```xml
<!-- ItemMapper.xml：查询公开物品列表，同时查出发布者和认领人名字 -->
<select id="selectPublicItemList" resultType="com.xqk.campusfind.entity.Item">
    SELECT i.*, u1.username AS publisherName, u2.username AS claimerName
    FROM item i
        LEFT JOIN user u1 ON i.user_id = u1.id
        LEFT JOIN claim c ON i.id = c.item_id AND c.status = 0
        LEFT JOIN user u2 ON c.user_id = u2.id
    WHERE i.status IN (1, 2)
    ORDER BY i.id DESC
</select>
```

**学习要点**：
- `BaseMapper<T>` 提供了单表 CRUD 的零代码实现
- 多表联查仍然需要写 XML，用 `<select>` 标签 + `resultType` 映射回实体类

### 4.5 Service 层 — 核心业务逻辑

#### 4.5.1 登录逻辑（UserServiceImpl）

```java
public User login(String username, String password) {
    // 1. 按用户名查询
    User user = userMapper.selectOne(
        new LambdaQueryWrapper<User>().eq(User::getUsername, username)
    );
    // 2. BCrypt 密文比对（前端明文 vs 数据库密文）
    if (user != null && passwordEncoder.matches(password, user.getPassword())) {
        return user;
    }
    return null;
}
```

**学习要点**：`passwordEncoder.matches(明文, 密文)` 是 BCrypt 的标准用法，永远不要把密码反解。

#### 4.5.2 智能匹配算法（ItemServiceImpl.smartMatch）

这是本项目的**亮点功能**，逻辑如下：

```
1. 确定匹配方向：招领启事(1) 匹配 寻物启事(2)，反之亦然
2. 获取所有候选物品（状态为可认领、类型相反）
3. 对每个候选物品计算相似度（满分 100）：
   ├─ 类型相同 → +30 分
   ├─ 名称相似 → +40 分（按 Jaccard 相似度计算）
   ├─ 地点相近 → +15 分
   └─ 时间接近 → +15 分
4. 过滤掉相似度 < 30% 的结果
5. 按相似度降序排列，返回前 10 个
```

**学习要点**：这是一个基于加权评分的简单匹配算法，面试/答辩时可以重点讲解。

### 4.6 Controller 层 — REST API 接口汇总

#### 用户模块（/user）

| 方法 | 路径 | 功能 | 要点 |
|------|------|------|------|
| POST | `/user/login` | 登录 | BCrypt 校验成功后返回 `{ token, user }`，密码脱敏 |
| POST | `/user/register` | 注册 | 管理员需密钥 `inputKey`，密码 BCrypt 加密存储 |
| GET | `/user/me` | 当前用户 | 从 JWT 解析出的登录身份（需登录） |

#### 物品模块（/item）

| 方法 | 路径 | 功能 | 权限 | 要点 |
|------|------|------|------|------|
| POST | `/item/add` | 发布物品 | 登录 | 发布者 userId 取自令牌，初始 status=0 |
| GET | `/item/list` | 获取大厅列表 | 登录 | 只返回 status=1,2 的 |
| GET | `/item/pending` | 获取待审核列表 | **管理员** | 仅 ROLE_ADMIN |
| POST | `/item/review` | 审核物品 | **管理员** | 仅 ROLE_ADMIN，传 id + status |
| GET | `/item/stats` | 仪表盘统计 | 登录 | 总数/认领中/已归还/结案率 |
| POST | `/item/upload` | 图片上传 | 登录 | 上传到 MinIO，UUID 重命名防覆盖 |
| POST | `/item/match` | 智能匹配 | 登录 | 返回相似度排序结果 |
| GET | `/item/my` | 我的发布 | 登录 | userId 取自令牌（不再传参） |
| DELETE | `/item/delete/{id}` | 删除物品 | 登录 | 校验发布者（userId 取自令牌） |

#### 认领模块（/claim）

| 方法 | 路径 | 功能 | 要点 |
|------|------|------|------|
| POST | `/claim/add` | 发起认领 | 认领人 userId 取自令牌；物品变 status=2 |
| GET | `/claim/my` | 我的认领记录 | userId 取自令牌，联查物品名称 |
| POST | `/claim/confirm` | 确认/拒绝认领 | action=1同意, =2拒绝；发布者取自令牌 |
| GET | `/claim/pending-confirm` | 待确认列表 | 发布者视角（userId 取自令牌） |
| POST | `/claim/complete` | 确认归还 | 双方都可操作，物品归档（令牌校验身份） |
| GET | `/claim/contact/{claimId}` | 查看联系方式 | 必须已同意且本人查看（令牌校验） |

### 4.7 认证与鉴权框架（Spring Security + JWT）⭐

项目已从“仅前端显隐控制”升级为**后端强制鉴权**的无状态方案：

```
登录流程：
前端 POST /user/login (username, password)
   → UserService 用 BCrypt 校验
   → JwtUtil 生成 token（claim: userId / username / role）
   → 返回 { token, user }，前端存 localStorage

请求流程（之后每次调用）：
Axios 请求拦截器自动加 header: Authorization: Bearer <token>
   → JwtAuthenticationFilter 解析并校验 token
   → 通过则构造 LoginUser + 权限(ROLE_ADMIN/ROLE_USER) 写入 SecurityContext
   → SecurityConfig 依据 URL 规则放行 / 拦截
   → 失败：401(未登录) / 403(越权) 统一返回 JSON
```

**关键安全设计**：所有涉及“身份归属”的接口（发布、认领、我的数据、确认、删除、联系方式）一律从令牌读取当前 userId（`SecurityUtils.getCurrentUserId()`），忽略前端传入的 `userId`，从根本上防止越权篡改。

| 组件 | 职责 |
|------|------|
| `JwtUtil` | 令牌签发 / 解析（HS256） |
| `LoginUser` | 登录主体（id / username / role） |
| `JwtAuthenticationFilter` | 从请求头解析令牌并写入 SecurityContext |
| `SecurityUtils` | 业务代码读取当前登录用户 / userId |
| `SecurityConfig` | 过滤器链 + URL 鉴权规则 + CORS + 401/403 处理 |

**角色映射**：`role=1 → ROLE_ADMIN`，`role=0 → ROLE_USER`；管理员专属接口用 `hasRole("ADMIN")` 保护。

---

## 五、前端核心代码逐模块讲解

### 5.1 请求封装 — request.js

```javascript
const request = axios.create({
    baseURL: 'http://localhost:8080',  // 后端地址
    timeout: 5000
})

// 请求拦截器：自动携带 JWT 令牌
request.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
})

// 响应拦截器：自动解包 + 统一处理 401/403
request.interceptors.response.use(
    response => {
        const res = response.data;
        if (res.code != 200) {
            ElMessage.error(res.message);
            return Promise.reject(new Error(res.message));
        }
        return res.data;  // ⭐ 直接返回 data 部分，省去每处解包
    },
    error => {
        const status = error.response?.status;
        if (status === 401) {            // 未登录/令牌失效
            ElMessage.error('登录已失效，请重新登录');
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            window.location.href = '/';
        } else if (status === 403) {     // 权限不足
            ElMessage.error('权限不足，禁止访问');
        } else {
            ElMessage.error('网络连接错误');
        }
        return Promise.reject(error);
    }
)
```

**学习要点**：请求拦截器自动附加令牌，响应拦截器既解包 `data`、又集中处理登录失效(401)与越权(403)，组件里无需重复判断。

### 5.2 API 接口定义 — api/index.js

```javascript
// 每个后端接口对应一个函数
export const login = (data) => {
  return request.post('/user/login', null, { params: data })
}
export const getList = () => {
  return request.get('/item/list')
}
```

**学习要点**：把所有 API 集中在一个文件，组件里 `import { getList } from '../api'` 即可调用。

### 5.3 路由配置 — router/index.js

```javascript
const routes = [
  { path: '/', component: Login },           // 根路径 → 登录页
  { path: '/register', component: Register }, // 注册页（独立，无侧边栏）
  {
    path: '/main',
    component: Layout,    // 主框架（含侧边栏）
    redirect: '/home',
    children: [           // ⭐ 嵌套路由：子页面渲染在 Layout 的 <router-view> 中
      { path: '/home', component: Home },
      { path: '/add', component: Add },
      { path: '/pending', component: Admin },
      { path: '/my-claims', component: My },
      { path: '/my-publish', component: MyPublish },
      { path: '/my-pending-claims', component: MyPendingClaims }
    ]
  }
]
```

**学习要点**：`Layout.vue` 是外壳，子页面通过 `<router-view />` 动态渲染到内容区。

### 5.4 用户状态管理

项目**没有用 Vuex/Pinia**，而是用 `localStorage` 存储令牌与用户信息：

```javascript
// 登录成功后：分别存 JWT 令牌和用户信息
localStorage.setItem('token', res.token)
localStorage.setItem('user', JSON.stringify(res.user))

// 任何地方取（用于菜单显隐等展示）
const user = computed(() => {
  const cached = localStorage.getItem('user')
  return cached ? JSON.parse(cached) : { username: 'GUEST', role: 0 }
})
```

**学习要点**：简单项目用 `localStorage` 就够了；复杂项目建议上 Pinia。

### 5.5 权限控制（Layout.vue）

```html
<!-- 普通员工看到的菜单 -->
<template v-if="user.role === 0">
  <el-menu-item index="/add">发布寻物/招领</el-menu-item>
  <el-menu-item index="/my-claims">我的认领记录</el-menu-item>
  ...
</template>

<!-- 管理员看到的菜单 -->
<template v-if="user.role === 1">
  <el-menu-item index="/pending">审核发布申请</el-menu-item>
</template>
```

**学习要点**：前端通过 `v-if` 根据角色动态控制侧边栏菜单——但这**只是展示层**；真正的安全拦截在后端 Spring Security 完成（见 4.7），即使篡改前端 `role` 也无法访问无权限的接口。

---

## 六、关键配置文件解读

### 6.1 application.yml

```yaml
server:
  port: 8080                    # 后端端口

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lostfound   # 数据库名：lostfound
    username: root
    password: abc0314az

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml       # XML 文件位置
  configuration:
    map-underline-to-camel-case: true            # 自动驼峰映射（user_id → userId）
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 控制台打印 SQL

minio:
  endpoint: http://localhost:9000                # MinIO 服务地址
  access-key: minioadmin
  secret-key: minioadmin
  bucket: lostfound                              # 存储桶（首次自动创建并公开读）
  public-url: http://localhost:9000/lostfound    # 图片对外访问前缀

secure:
  admin-key: gly                                 # 管理员注册密钥

jwt:
  secret: campusfind-lostfound-jwt-secret-key-...  # HS256 签名密钥（≥ 256 位）
  expiration: 86400000                           # 令牌有效期（毫秒，默认 24h）
```

### 6.2 安全与跨域配置（SecurityConfig.java）

Spring Security 过滤器链 + 统一跨域（原先独立的 CorsConfig 已合并至此）：

```java
http.csrf(AbstractHttpConfigurer::disable)                          // 关闭 CSRF（无状态）
    .cors(cors -> cors.configurationSource(corsConfigurationSource())) // 允许 5173
    .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))    // 不用 Session
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/user/login", "/user/register").permitAll()      // 放行登录/注册
        .requestMatchers("/item/pending", "/item/review").hasRole("ADMIN") // 管理员专属
        .requestMatchers("/item/**", "/claim/**", "/user/**").authenticated() // 需登录
        .anyRequest().permitAll())
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

---

## 七、状态流转全景图

### 7.1 物品状态（item.status）

```
        发布
         ↓
    ┌─────────┐   审核通过    ┌─────────┐   有人认领    ┌─────────┐   确认归还    ┌─────────┐
    │ 0 待审核 │ ──────────→ │ 1 已发布 │ ──────────→ │ 2 认领中 │ ──────────→ │ 3 已归还 │
    └─────────┘              └─────────┘              └─────────┘              └─────────┘
         ↓                        ↓
      审核驳回                   被拒绝后回到 status=1
      （删除）
```

### 7.2 认领状态（claim.status）

```
    ┌─────────┐   同意    ┌─────────┐   确认归还    ┌─────────┐
    │ 0 审核中 │ ───────→ │ 1 已通过 │ ──────────→ │ 5 已归档 │
    └─────────┘           └─────────┘              └─────────┘
         ↓
      拒绝 → status=2（物品回到已发布）
```

---

## 八、项目启动步骤

### 8.0 启动 MinIO（对象存储，图片上传依赖）

项目已自带 `minio/minio.exe`，数据也存于项目内 `minio/data`，一键启动（无需 Docker）：

```powershell
cd CampusFind-master
powershell -ExecutionPolicy Bypass -File .\start-minio.ps1
# API: http://localhost:9000  控制台: http://localhost:9010 (minioadmin/minioadmin)
```

> 重复运行会因 9000 端口已占用而自动跳过；关闭启动窗口即停止服务。

### 8.1 后端启动

```bash
# 1. 确保 MySQL 已启动，执行初始化脚本
mysql -u root -p < database/init_database.sql

# 2. 修改 application.yml 中的数据库密码为你自己的

# 3. 用 IDEA 打开项目，运行 CampusFindApplication.java
#    或使用 Maven 命令：
cd CampusFind-master
mvnw spring-boot:run
```

后端启动后访问 `http://localhost:8080`

### 8.2 前端启动

```bash
cd CampusFind-master/src/main/resources/static
npm install
npm run dev
```

前端启动后访问 `http://localhost:5173`

### 8.3 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| root | 12345 | 管理员 |
| station_master | 12345 | 管理员 |
| passenger001 | 12345 | 普通用户 |
| passenger002 | 12345 | 普通用户 |
| staff_zhang | 12345 | 普通用户 |

> 注册管理员需要在注册页填写"管理员授权密钥"：`gly`

---

## 九、答辩/面试高频问题 & 回答思路

### Q1: 密码是怎么存储的？
> 使用 BCrypt 算法加密后存储。注册时 `passwordEncoder.encode(明文)` 生成密文存入数据库；登录时 `passwordEncoder.matches(明文, 密文)` 比对，数据库中不存储任何明文密码。

### Q2: 智能匹配是怎么实现的？
> 基于加权评分算法。从 4 个维度打分：物品类型（30分）、名称相似度（40分，用 Jaccard 算法）、地点相关性（15分）、时间接近度（15分）。总分 ≥ 30 才展示，按分数降序返回前 10 条。

### Q3: 前后端怎么通信的？
> 前端用 Axios 发送 HTTP 请求到后端 REST API，后端返回统一的 `{ code, message, data }` JSON 格式。前端通过 Axios 拦截器自动解包和错误处理。

### Q4: 权限控制/登录鉴权怎么做的？
> 采用 **Spring Security + JWT** 无状态方案。登录成功后签发 JWT（含 userId/username/role），前端存 localStorage 并在请求头带 `Authorization: Bearer`；后端 `JwtAuthenticationFilter` 解析令牌写入 SecurityContext，`SecurityConfig` 做接口级鉴权（`/item/pending`、`/item/review` 仅管理员，其余业务接口需登录）。关键安全点：**写/查涉及身份的接口一律从令牌取 userId，忽略前端传参**，杜绝越权篡改；前端 `role` 仅用于菜单显隐。401（未登录）/403（越权）统一返回 JSON。

### Q5: MyBatis-Plus 相比原生 MyBatis 有什么优势？
> 单表 CRUD 零 SQL 代码（BaseMapper 内置方法）；LambdaQueryWrapper 提供类型安全的条件构造；自动驼峰映射；分页插件等。复杂联查仍然用 XML。

### Q6: `@TableField(exist = false)` 是什么作用？
> 告诉 MyBatis-Plus 这个字段不映射数据库表的任何列，仅在联表查询时用于承载额外数据（如发布者名称），避免插入/更新时报错。

---

## 十、二次开发指南

### 10.1 新增一个接口的步骤

1. **Entity**：如果需要新表，创建实体类 + `@TableName`
2. **Mapper**：创建 Mapper 接口继承 `BaseMapper<T>`，复杂 SQL 写 XML
3. **Service**：创建 Service 接口 + Impl 实现类，写业务逻辑
4. **Controller**：创建 Controller，用 `@GetMapping` / `@PostMapping` 暴露接口
5. **前端 api/index.js**：添加对应的 API 函数
6. **前端 views/**：创建或修改页面组件调用 API

### 10.2 常见修改场景

| 需求 | 改哪里 |
|------|--------|
| 修改数据库密码 | `application.yml` → `spring.datasource.password` |
| 修改管理员密钥 | `application.yml` → `secure.admin-key` |
| 修改前端请求地址 | `api/request.js` → `baseURL` |
| 新增物品分类 | `Add.vue` 和 `Home.vue` 中的 `<el-option>` 列表 |
| 修改匹配权重 | `ItemServiceImpl.java` → `calculateSimilarity()` 方法 |
| 修改对象存储 | `application.yml` → `minio.*`（endpoint/bucket/密钥） |
| 修改 JWT 密钥/有效期 | `application.yml` → `jwt.secret` / `jwt.expiration` |

---

## 十一、核心设计模式与亮点总结

| 设计点 | 说明 |
|--------|------|
| **统一响应封装** | `Result<T>` 让所有接口返回格式一致 |
| **全局异常处理** | `@RestControllerAdvice` 拦截参数校验异常，返回友好提示 |
| **BCrypt 加密** | 密码永不明文存储，安全性高 |
| **Spring Security + JWT** | 无状态认证 + 接口级鉴权，身份以令牌为准防越权 |
| **MinIO 对象存储** | 图片存对象存储桶，项目自包含（exe+data 随项目走） |
| **MyBatis-Plus 零 SQL** | 单表操作无需写 SQL，开发效率极高 |
| **XML 联表查询** | 复杂查询仍用 XML，兼顾灵活性和可维护性 |
| **智能匹配算法** | 多维度加权评分，是项目的技术亮点 |
| **前端 Axios 拦截器** | 自动解包 + 统一错误处理，减少重复代码 |
| **Vue Router 嵌套路由** | Layout 外壳 + 子页面动态渲染，结构清晰 |
| **localStorage 状态管理** | 轻量方案，适合小型项目 |
| **动态图片上传** | UUID 重命名 + 配置化路径，不硬编码 |

---

> 📌 **学习建议**：先跑通项目 → 再读 Controller 理解接口 → 然后看 Service 理解业务 → 最后研究 XML 和匹配算法。
