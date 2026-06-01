-- 抽奖系统数据库表结构
-- 数据库: lottery_db

-- 奖品配置表
CREATE TABLE IF NOT EXISTS prize_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '奖品名称',
    icon VARCHAR(255) COMMENT '奖品图标',
    probability DOUBLE NOT NULL COMMENT '中奖概率',
    sort_order INT NOT NULL COMMENT '排序顺序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖品配置表';

-- 抽奖结果表
CREATE TABLE IF NOT EXISTS lottery_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    prize VARCHAR(100) NOT NULL COMMENT '奖品名称',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '抽奖时间',
    claimed_by VARCHAR(50) COMMENT '领奖人',
    claimed_at DATETIME COMMENT '领奖时间',
    INDEX idx_created_at (created_at),
    INDEX idx_claimed_by (claimed_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖结果表';

-- 初始化默认奖品配置
INSERT INTO prize_config (name, icon, probability, sort_order) VALUES
('一等奖', '🏆', 0.01, 1),
('二等奖', '🎁', 0.05, 2),
('三等奖', '🎈', 0.10, 3),
('参与奖', '🍀', 0.84, 4);