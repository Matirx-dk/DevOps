-- 修复 test 环境 IP 管理菜单缺失
-- 适用库：ry-cloud
-- 作用：
-- 1) 确保 IP 管理菜单 (menu_id=2001) 挂于运维中心 (parent_id=2000) 下
-- 2) 确保 role_id 1 和 2 都能访问该菜单

START TRANSACTION;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
) VALUES
(
  2001,
  'IP管理',
  2000, 1,
  'ip', 'ip/index', '', '',
  1, 0, 'C', '0', '0', 'ops:ip:list', 'server',
  'admin', NOW(), '', NULL,
  'IP 地址管理菜单'
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

-- ops:ip 权限按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
  (200101, 'IP查询',   2001, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'ops:ip:query',   '#', 'admin', NOW(), '', NULL, '')
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), perms=VALUES(perms);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
  (200102, 'IP新增',   2001, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'ops:ip:add',     '#', 'admin', NOW(), '', NULL, '')
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), perms=VALUES(perms);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
  (200103, 'IP修改',   2001, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'ops:ip:edit',    '#', 'admin', NOW(), '', NULL, '')
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), perms=VALUES(perms);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
  (200104, 'IP删除',   2001, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'ops:ip:remove',  '#', 'admin', NOW(), '', NULL, '')
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), perms=VALUES(perms);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
  (200105, 'IP导出',   2001, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'ops:ip:export',  '#', 'admin', NOW(), '', NULL, '')
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), perms=VALUES(perms);

-- 角色菜单绑定
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
  (1, 2000), (1, 2001), (1, 200101), (1, 200102), (1, 200103), (1, 200104), (1, 200105),
  (2, 2000), (2, 2001), (2, 200101), (2, 200102), (2, 200103), (2, 200104), (2, 200105)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);

COMMIT;

-- 校验：
-- SELECT menu_id, menu_name, parent_id, order_num, path, component FROM sys_menu WHERE menu_id LIKE '200%' ORDER BY menu_id;
-- SELECT role_id, menu_id FROM sys_role_menu WHERE role_id IN (1,2) AND menu_id LIKE '200%' ORDER BY role_id, menu_id;
