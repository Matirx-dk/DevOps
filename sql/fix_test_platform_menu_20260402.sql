-- 修复 test 环境“平台管理”二级菜单缺失/文案不一致问题
-- 适用库：ry-cloud
-- 作用：
-- 1) 确保 menu_id 100/101/102/106 存在于 parent_id=1 下
-- 2) 统一菜单文案为：用户与权限 / 角色与授权 / 菜单与导航 / 系统配置
-- 3) 确保 role_id 1 和 2 都拥有这些菜单映射
-- 4) 使用 UTF-8 HEX 写法，避免终端/客户端字符集导致的中文乱码

START TRANSACTION;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
) VALUES
(
  100,
  CONVERT(0xE794A8E688B7E4B88EE69D83E99990 USING utf8mb4),
  1, 1,
  'user', 'system/user/index', '', '',
  1, 0, 'C', '0', '0', 'system:user:list', 'user',
  'admin', NOW(), '', NULL,
  CONVERT(0xE794A8E688B7E7AEA1E79086E88F9CE58D95 USING utf8mb4)
),
(
  101,
  CONVERT(0xE8A792E889B2E4B88EE68E88E69D83 USING utf8mb4),
  1, 2,
  'role', 'system/role/index', '', '',
  1, 0, 'C', '0', '0', 'system:role:list', 'peoples',
  'admin', NOW(), '', NULL,
  CONVERT(0xE8A792E889B2E7AEA1E79086E88F9CE58D95 USING utf8mb4)
),
(
  102,
  CONVERT(0xE88F9CE58D95E4B88EE5AFBCE888AA USING utf8mb4),
  1, 3,
  'menu', 'system/menu/index', '', '',
  1, 0, 'C', '0', '0', 'system:menu:list', 'tree-table',
  'admin', NOW(), '', NULL,
  CONVERT(0xE88F9CE58D95E7AEA1E79086E88F9CE58D95 USING utf8mb4)
),
(
  106,
  CONVERT(0xE7B3BBE7BB9FE9858DE7BDAE USING utf8mb4),
  1, 4,
  'config', 'system/config/index', '', '',
  1, 0, 'C', '0', '0', 'system:config:list', 'edit',
  'admin', NOW(), '', NULL,
  CONVERT(0xE58F82E695B0E8AEBEE7BDAEE88F9CE58D95 USING utf8mb4)
)
ON DUPLICATE KEY UPDATE
  menu_name   = VALUES(menu_name),
  parent_id   = VALUES(parent_id),
  order_num   = VALUES(order_num),
  path        = VALUES(path),
  component   = VALUES(component),
  query       = VALUES(query),
  route_name  = VALUES(route_name),
  is_frame    = VALUES(is_frame),
  is_cache    = VALUES(is_cache),
  menu_type   = VALUES(menu_type),
  visible     = VALUES(visible),
  status      = VALUES(status),
  perms       = VALUES(perms),
  icon        = VALUES(icon),
  remark      = VALUES(remark);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
  (1, 1), (1, 100), (1, 101), (1, 102), (1, 106),
  (2, 1), (2, 100), (2, 101), (2, 102), (2, 106)
ON DUPLICATE KEY UPDATE
  role_id = VALUES(role_id),
  menu_id = VALUES(menu_id);

COMMIT;

-- 校验：
-- SELECT menu_id, menu_name, parent_id, order_num, path, component FROM sys_menu WHERE menu_id IN (100,101,102,106) ORDER BY menu_id;
-- SELECT role_id, menu_id FROM sys_role_menu WHERE role_id IN (1,2) AND menu_id IN (1,100,101,102,106) ORDER BY role_id, menu_id;
