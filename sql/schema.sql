SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP DATABASE IF EXISTS edu_archive;
CREATE DATABASE edu_archive DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE edu_archive;

/*
  面向工程教育认证的教学档案管理系统
  设计目标：
  1) 字段原子化（1NF）
  2) 通过中间表拆解多对多关系（2NF）
  3) 非主键字段仅依赖主键，避免传递依赖（3NF）
*/

-- 1. 角色表
CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
  role_code VARCHAR(64) NOT NULL COMMENT '角色编码(唯一)',
  role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
  role_desc VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_sys_role_code (role_code),
  KEY idx_sys_role_status (status)
) ENGINE=InnoDB COMMENT='角色表';

-- 2. 用户表
CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(64) NOT NULL COMMENT '登录用户名(唯一)',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
  real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
  email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  gender TINYINT DEFAULT NULL COMMENT '性别:1男,2女,0未知',
  dept_name VARCHAR(128) DEFAULT NULL COMMENT '所属部门/教研室',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
  last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_sys_user_username (username),
  UNIQUE KEY uk_sys_user_email (email),
  UNIQUE KEY uk_sys_user_phone (phone),
  KEY idx_sys_user_status (status)
) ENGINE=InnoDB COMMENT='用户表';

-- 用户-角色关联表（多对多）
CREATE TABLE sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_sys_user_role_role_id (role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB COMMENT='用户角色关联表';

-- 3. 菜单权限表（菜单/按钮/数据权限统一管理）
CREATE TABLE sys_menu_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父节点ID，0为根',
  perm_code VARCHAR(128) NOT NULL COMMENT '权限编码(唯一)',
  perm_name VARCHAR(128) NOT NULL COMMENT '权限名称',
  perm_type VARCHAR(16) NOT NULL COMMENT '权限类型:MENU/BUTTON/DATA',
  path VARCHAR(255) DEFAULT NULL COMMENT '路由路径',
  component VARCHAR(255) DEFAULT NULL COMMENT '前端组件路径',
  icon VARCHAR(64) DEFAULT NULL COMMENT '图标',
  visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示:1是,0否',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_menu_perm_code (perm_code),
  KEY idx_menu_perm_parent (parent_id),
  KEY idx_menu_perm_type (perm_type),
  KEY idx_menu_perm_status (status)
) ENGINE=InnoDB COMMENT='菜单权限表';

-- 角色-权限关联表（多对多）
CREATE TABLE sys_role_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  permission_id BIGINT NOT NULL COMMENT '权限ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_role_permission (role_id, permission_id),
  KEY idx_role_permission_pid (permission_id),
  CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
  CONSTRAINT fk_role_perm_perm FOREIGN KEY (permission_id) REFERENCES sys_menu_permission(id)
) ENGINE=InnoDB COMMENT='角色权限关联表';

-- 4. 档案分类表
CREATE TABLE archive_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID，0为顶级',
  category_code VARCHAR(64) NOT NULL COMMENT '分类编码(唯一)',
  category_name VARCHAR(128) NOT NULL COMMENT '分类名称',
  category_desc VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_archive_category_code (category_code),
  KEY idx_archive_category_parent (parent_id),
  KEY idx_archive_category_status (status)
) ENGINE=InnoDB COMMENT='档案分类表';

-- 5. 档案信息表（档案主表）
CREATE TABLE archive_info (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '档案ID',
  archive_no VARCHAR(64) NOT NULL COMMENT '档案编号(唯一)',
  title VARCHAR(255) NOT NULL COMMENT '档案标题',
  category_id BIGINT NOT NULL COMMENT '分类ID',
  file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
  file_path VARCHAR(500) NOT NULL COMMENT '存储路径',
  file_ext VARCHAR(20) NOT NULL COMMENT '文件扩展名',
  file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
  version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
  lifecycle_status VARCHAR(32) NOT NULL COMMENT '生命周期状态:COLLECTED/SUBMITTED/APPROVED/REJECTED/DESTROY_PENDING/DESTROYED',
  archive_time DATETIME DEFAULT NULL COMMENT '归档时间',
  collected_by BIGINT NOT NULL COMMENT '采集人用户ID',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_archive_no (archive_no),
  KEY idx_archive_category_id (category_id),
  KEY idx_archive_status (lifecycle_status),
  KEY idx_archive_archive_time (archive_time),
  KEY idx_archive_collected_by (collected_by),
  FULLTEXT KEY ft_archive_title (title),
  CONSTRAINT fk_archive_category FOREIGN KEY (category_id) REFERENCES archive_category(id),
  CONSTRAINT fk_archive_user FOREIGN KEY (collected_by) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='档案信息表';

-- 6. 审核流程表（含普通审核与销毁审核记录）
CREATE TABLE archive_audit_flow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '审核记录ID',
  archive_id BIGINT NOT NULL COMMENT '档案ID',
  audit_type VARCHAR(32) NOT NULL COMMENT '审核类型:ARCHIVE_AUDIT/DESTROY_AUDIT',
  step_no INT NOT NULL DEFAULT 1 COMMENT '流程步骤号',
  submitter_id BIGINT NOT NULL COMMENT '提交人ID',
  reviewer_id BIGINT DEFAULT NULL COMMENT '审核人ID',
  audit_status VARCHAR(32) NOT NULL COMMENT '状态:PENDING/PASSED/REJECTED',
  audit_comment VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
  submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  review_time DATETIME DEFAULT NULL COMMENT '审核时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_audit_archive_id (archive_id),
  KEY idx_audit_type_status (audit_type, audit_status),
  KEY idx_audit_reviewer (reviewer_id),
  CONSTRAINT fk_audit_archive FOREIGN KEY (archive_id) REFERENCES archive_info(id),
  CONSTRAINT fk_audit_submitter FOREIGN KEY (submitter_id) REFERENCES sys_user(id),
  CONSTRAINT fk_audit_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='审核流程表';

-- 7. 工程教育认证指标表
CREATE TABLE edu_indicator (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '指标ID',
  indicator_code VARCHAR(64) NOT NULL COMMENT '指标编码(唯一)',
  indicator_name VARCHAR(255) NOT NULL COMMENT '指标名称',
  indicator_desc VARCHAR(1000) DEFAULT NULL COMMENT '指标描述',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_indicator_code (indicator_code),
  KEY idx_indicator_status (status)
) ENGINE=InnoDB COMMENT='工程教育认证指标表';

-- 8. 毕业要求表
CREATE TABLE graduation_requirement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '毕业要求ID',
  requirement_code VARCHAR(64) NOT NULL COMMENT '毕业要求编码(唯一)',
  requirement_name VARCHAR(255) NOT NULL COMMENT '毕业要求名称',
  requirement_desc VARCHAR(1000) DEFAULT NULL COMMENT '毕业要求描述',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_grad_req_code (requirement_code),
  KEY idx_grad_req_status (status)
) ENGINE=InnoDB COMMENT='毕业要求表';

-- 9. 课程目标表
CREATE TABLE course_objective (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课程目标ID',
  course_code VARCHAR(64) NOT NULL COMMENT '课程编码',
  course_name VARCHAR(128) NOT NULL COMMENT '课程名称',
  objective_code VARCHAR(64) NOT NULL COMMENT '课程目标编码(课程内唯一)',
  objective_name VARCHAR(255) NOT NULL COMMENT '课程目标名称',
  objective_desc VARCHAR(1000) DEFAULT NULL COMMENT '课程目标描述',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0禁用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_course_objective (course_code, objective_code),
  KEY idx_course_name (course_name),
  KEY idx_course_objective_status (status)
) ENGINE=InnoDB COMMENT='课程目标表';

-- 指标-毕业要求关系表（多对多）
CREATE TABLE indicator_requirement_rel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  indicator_id BIGINT NOT NULL COMMENT '指标ID',
  requirement_id BIGINT NOT NULL COMMENT '毕业要求ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_indicator_requirement (indicator_id, requirement_id),
  KEY idx_indicator_requirement_rid (requirement_id),
  CONSTRAINT fk_ir_indicator FOREIGN KEY (indicator_id) REFERENCES edu_indicator(id),
  CONSTRAINT fk_ir_requirement FOREIGN KEY (requirement_id) REFERENCES graduation_requirement(id)
) ENGINE=InnoDB COMMENT='指标与毕业要求关联表';

-- 课程目标-毕业要求关系表（多对多）
CREATE TABLE course_objective_requirement_rel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  objective_id BIGINT NOT NULL COMMENT '课程目标ID',
  requirement_id BIGINT NOT NULL COMMENT '毕业要求ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_objective_requirement (objective_id, requirement_id),
  KEY idx_objective_requirement_rid (requirement_id),
  CONSTRAINT fk_cor_objective FOREIGN KEY (objective_id) REFERENCES course_objective(id),
  CONSTRAINT fk_cor_requirement FOREIGN KEY (requirement_id) REFERENCES graduation_requirement(id)
) ENGINE=InnoDB COMMENT='课程目标与毕业要求关联表';

-- 10. 档案-指标关联表（含与毕业要求/课程目标自动关联落地）
CREATE TABLE archive_indicator_rel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  archive_id BIGINT NOT NULL COMMENT '档案ID',
  indicator_id BIGINT NOT NULL COMMENT '认证指标ID',
  requirement_id BIGINT DEFAULT NULL COMMENT '毕业要求ID(冗余关联便于查询)',
  objective_id BIGINT DEFAULT NULL COMMENT '课程目标ID(冗余关联便于查询)',
  bind_source VARCHAR(16) NOT NULL DEFAULT 'AUTO' COMMENT '绑定来源:AUTO/MANUAL',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_archive_indicator (archive_id, indicator_id, requirement_id, objective_id),
  KEY idx_air_indicator (indicator_id),
  KEY idx_air_requirement (requirement_id),
  KEY idx_air_objective (objective_id),
  CONSTRAINT fk_air_archive FOREIGN KEY (archive_id) REFERENCES archive_info(id),
  CONSTRAINT fk_air_indicator FOREIGN KEY (indicator_id) REFERENCES edu_indicator(id),
  CONSTRAINT fk_air_requirement FOREIGN KEY (requirement_id) REFERENCES graduation_requirement(id),
  CONSTRAINT fk_air_objective FOREIGN KEY (objective_id) REFERENCES course_objective(id)
) ENGINE=InnoDB COMMENT='档案-指标关联表';

-- 11. 日志表（统一操作/登录/异常日志）
CREATE TABLE sys_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  log_type VARCHAR(16) NOT NULL COMMENT '日志类型:LOGIN/OPERATION/ERROR',
  module_name VARCHAR(64) DEFAULT NULL COMMENT '模块名称',
  action_name VARCHAR(128) DEFAULT NULL COMMENT '动作名称',
  user_id BIGINT DEFAULT NULL COMMENT '操作用户ID',
  username VARCHAR(64) DEFAULT NULL COMMENT '用户名快照',
  request_uri VARCHAR(255) DEFAULT NULL COMMENT '请求URI',
  request_method VARCHAR(16) DEFAULT NULL COMMENT '请求方法',
  ip_addr VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  user_agent VARCHAR(255) DEFAULT NULL COMMENT '用户代理',
  result_status VARCHAR(16) DEFAULT NULL COMMENT '结果状态:SUCCESS/FAIL',
  error_stack TEXT DEFAULT NULL COMMENT '异常堆栈',
  detail_json JSON DEFAULT NULL COMMENT '详情JSON',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_sys_log_type (log_type),
  KEY idx_sys_log_user (user_id),
  KEY idx_sys_log_created_at (created_at),
  KEY idx_sys_log_module_action (module_name, action_name),
  CONSTRAINT fk_sys_log_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='日志表';

-- 12. 系统配置表
CREATE TABLE sys_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
  config_key VARCHAR(128) NOT NULL COMMENT '配置键(唯一)',
  config_value VARCHAR(2000) NOT NULL COMMENT '配置值',
  value_type VARCHAR(32) NOT NULL DEFAULT 'STRING' COMMENT '值类型:STRING/NUMBER/BOOLEAN/JSON',
  config_group VARCHAR(64) DEFAULT NULL COMMENT '配置分组',
  is_builtin TINYINT NOT NULL DEFAULT 0 COMMENT '是否内置:1是,0否',
  is_encrypted TINYINT NOT NULL DEFAULT 0 COMMENT '是否加密存储:1是,0否',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  updated_by BIGINT DEFAULT NULL COMMENT '最后更新人ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_sys_config_key (config_key),
  KEY idx_sys_config_group (config_group),
  CONSTRAINT fk_sys_config_user FOREIGN KEY (updated_by) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='系统配置表';

-- -----------------------------
-- 基础初始化数据
-- -----------------------------
INSERT INTO sys_role(role_code, role_name, role_desc, status, sort_no) VALUES
('ADMIN', '管理员', '系统超级管理员', 1, 1),
('TEACHER', '教师', '课程与档案维护教师', 1, 2),
('AUDITOR', '审核员', '档案审核与认证材料核验', 1, 3);

INSERT INTO sys_user(username, password_hash, real_name, email, phone, status) VALUES
('admin', '$2a$10$Ql4LxjYqAynJv8kGqvT7DO2lI6Q5lGvDJ8uXtzt5Rj0pVxYl8fXSe', '系统管理员', 'admin@example.com', '13800000001', 1),
('teacher', '$2a$10$Ql4LxjYqAynJv8kGqvT7DO2lI6Q5lGvDJ8uXtzt5Rj0pVxYl8fXSe', '示例教师', 'teacher@example.com', '13800000002', 1),
('auditor', '$2a$10$Ql4LxjYqAynJv8kGqvT7DO2lI6Q5lGvDJ8uXtzt5Rj0pVxYl8fXSe', '示例审核员', 'auditor@example.com', '13800000003', 1);

INSERT INTO sys_user_role(user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON
  (u.username = 'admin' AND r.role_code = 'ADMIN')
  OR (u.username = 'teacher' AND r.role_code = 'TEACHER')
  OR (u.username = 'auditor' AND r.role_code = 'AUDITOR');

INSERT INTO sys_menu_permission(parent_id, perm_code, perm_name, perm_type, path, component, sort_no) VALUES
(0, 'menu:user', '用户管理', 'MENU', '/users', 'views/UserView.vue', 10),
(0, 'menu:archive', '档案管理', 'MENU', '/archive', 'views/ArchiveView.vue', 20),
(0, 'menu:indicator', '指标映射', 'MENU', '/indicator', 'views/IndicatorView.vue', 30),
(0, 'menu:report', '统计报表', 'MENU', '/report', 'views/ReportView.vue', 40),
(0, 'menu:system', '系统管理', 'MENU', '/system', 'views/SystemView.vue', 50),
(0, 'button:archive:upload', '上传档案', 'BUTTON', NULL, NULL, 201),
(0, 'button:archive:audit', '审核档案', 'BUTTON', NULL, NULL, 202),
(0, 'button:indicator:bind', '绑定指标', 'BUTTON', NULL, NULL, 203),
(0, 'data:archive:all', '档案全量数据', 'DATA', NULL, NULL, 301),
(0, 'data:archive:self', '本人档案数据', 'DATA', NULL, NULL, 302);

INSERT INTO sys_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_menu_permission p
WHERE
  (r.role_code = 'ADMIN')
  OR (r.role_code = 'TEACHER' AND p.perm_code IN ('menu:archive', 'menu:indicator', 'menu:report', 'button:archive:upload', 'button:indicator:bind', 'data:archive:self'))
  OR (r.role_code = 'AUDITOR' AND p.perm_code IN ('menu:archive', 'menu:report', 'button:archive:audit', 'data:archive:all'));

INSERT INTO archive_category(parent_id, category_code, category_name, category_desc, status, sort_no) VALUES
(0, 'SYLLABUS', '教学大纲', '课程教学大纲类档案', 1, 1),
(0, 'EXAM_PAPER', '试卷资料', '命题与试卷归档材料', 1, 2),
(0, 'COURSE_REPORT', '课程达成报告', '课程目标达成分析材料', 1, 3),
(0, 'CERT_SUPPORT', '认证支撑材料', '工程教育认证支撑档案', 1, 4);

INSERT INTO edu_indicator(indicator_code, indicator_name, indicator_desc, status) VALUES
('IND-1-1', '工程知识', '能够将数学、自然科学和工程基础用于复杂工程问题', 1),
('IND-2-1', '问题分析', '能够识别、表达并分析复杂工程问题', 1);

INSERT INTO graduation_requirement(requirement_code, requirement_name, requirement_desc, status) VALUES
('GR-1', '工程知识要求', '具备数学与工程基础知识并用于问题求解', 1),
('GR-2', '问题分析要求', '能够进行问题识别、建模与综合分析', 1);

INSERT INTO course_objective(course_code, course_name, objective_code, objective_name, objective_desc, status) VALUES
('CS101', '程序设计基础', 'CO1', '掌握程序结构与算法设计', '能够独立完成中小规模程序开发', 1),
('CS101', '程序设计基础', 'CO2', '具备问题分析与调试能力', '能够对复杂逻辑问题进行分解与验证', 1);

INSERT INTO indicator_requirement_rel(indicator_id, requirement_id)
SELECT i.id, g.id
FROM edu_indicator i
JOIN graduation_requirement g
WHERE
  (i.indicator_code = 'IND-1-1' AND g.requirement_code = 'GR-1')
  OR (i.indicator_code = 'IND-2-1' AND g.requirement_code = 'GR-2');

INSERT INTO course_objective_requirement_rel(objective_id, requirement_id)
SELECT c.id, g.id
FROM course_objective c
JOIN graduation_requirement g
WHERE
  (c.objective_code = 'CO1' AND g.requirement_code = 'GR-1')
  OR (c.objective_code = 'CO2' AND g.requirement_code = 'GR-2');

INSERT INTO sys_config(config_key, config_value, value_type, config_group, is_builtin, is_encrypted, remark)
VALUES
('system.upload.maxSizeMB', '50', 'NUMBER', 'FILE', 1, 0, '上传大小限制(MB)'),
('system.audit.autoAssign', 'false', 'BOOLEAN', 'AUDIT', 1, 0, '审核是否自动分配'),
('security.jwt.expireHours', '12', 'NUMBER', 'SECURITY', 1, 0, 'JWT有效时长(小时)');

SET FOREIGN_KEY_CHECKS = 1;
