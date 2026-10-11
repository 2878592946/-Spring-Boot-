DROP DATABASE IF EXISTS lostfound;
CREATE DATABASE IF NOT EXISTS lostfound DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lostfound;

CREATE TABLE user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    image VARCHAR(500) DEFAULT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) DEFAULT NULL,
    description TEXT DEFAULT NULL,
    location VARCHAR(200) DEFAULT NULL,
    time VARCHAR(50) DEFAULT NULL,
    status INT NOT NULL DEFAULT 0,
    publish_type INT NOT NULL DEFAULT 1,
    contact_info VARCHAR(200) DEFAULT NULL,
    user_id BIGINT NOT NULL,
    claimer_name VARCHAR(50) DEFAULT NULL,
    publisher_name VARCHAR(50) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE claim (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    description TEXT DEFAULT NULL,
    status INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_item_id (item_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO user (username, password, role) VALUES 
('root', '$2a$10$rVHMOJ.RrfnQCYF3i8uXj.VZlqsPJlPqN7LdGxad68LJZdL17lhWy', 1),
('station_master', '$2a$10$rVHMOJ.RrfnQCYF3i8uXj.VZlqsPJlPqN7LdGxad68LJZdL17lhWy', 1),
('passenger001', '$2a$10$rVHMOJ.RrfnQCYF3i8uXj.VZlqsPJlPqN7LdGxad68LJZdL17lhWy', 0),
('passenger002', '$2a$10$rVHMOJ.RrfnQCYF3i8uXj.VZlqsPJlPqN7LdGxad68LJZdL17lhWy', 0),
('staff_zhang', '$2a$10$rVHMOJ.RrfnQCYF3i8uXj.VZlqsPJlPqN7LdGxad68LJZdL17lhWy', 0);

INSERT INTO item (image, name, type, description, location, time, status, publish_type, contact_info, user_id) VALUES 
('/upload/bag001.jpg', 'Black Backpack', 'Backpack', 'Contains laptop and charger', 'Metro Line 1 People Square', '2026-09-17 08:30', 1, 1, '138****5678', 2),
('/upload/phone001.jpg', 'iPhone 14 Pro', 'Phone', 'Purple with case', 'Metro Line 2 Jingan Temple', '2026-09-17 09:15', 1, 1, '139****1234', 2),
('/upload/wallet001.jpg', 'Brown Wallet', 'Wallet', 'With ID card and bank cards', 'Metro Line 3 Zhongshan Park', '2026-09-16 18:20', 1, 1, '136****9876', 2),
('/upload/umbrella001.jpg', 'Blue Umbrella', 'Umbrella', 'Brand: Paradise', 'Metro Line 4 Century Avenue', '2026-09-16 14:00', 1, 1, '137****5432', 2),
('/upload/headphones001.jpg', 'AirPods Pro', 'Headphones', 'White with stickers', 'Metro Line 10 Hongqiao Station', '2026-09-15 20:30', 1, 1, '135****6789', 2),
('/upload/keys001.jpg', 'Set of Keys', 'Keys', '5 keys with red keychain', 'Metro Line 7 Jingan Temple', '2026-09-15 12:00', 1, 1, '138****2345', 2),
('/upload/glasses001.jpg', 'Reading Glasses', 'Glasses', 'Black frame, 500 degrees', 'Metro Line 9 Xujiahui', '2026-09-14 17:45', 1, 1, '136****7890', 2),
(NULL, 'Lost Black Wallet', 'Wallet', 'ID card inside, name Zhang San', 'Metro Line 1 Xinzhuang', '2026-09-17 07:30', 1, 2, '138****0001', 3),
(NULL, 'Lost Laptop Bag', 'Backpack', 'Dell brand with important files', 'Metro Line 2 Lujiazui', '2026-09-16 19:00', 1, 2, '139****0002', 4),
(NULL, 'Lost Children Schoolbag', 'Backpack', 'Red with cartoon pattern', 'Metro Line 3 Caoyang Road', '2026-09-16 16:30', 1, 2, '136****0003', 5);

INSERT INTO claim (item_id, user_id, description, status) VALUES 
(1, 3, 'This is my backpack, I can describe the contents', 1),
(2, 4, 'Lost my phone yesterday, color matches', 0),
(3, 5, 'This is my husband wallet with his ID', 1),
(8, 2, 'Found a wallet matching this description', 0),
(9, 2, 'Found laptop bag at Lujiazui station', 0);

SELECT 'Database initialized successfully!' AS message;
SELECT COUNT(*) AS user_count FROM user;
SELECT COUNT(*) AS item_count FROM item;
SELECT COUNT(*) AS claim_count FROM claim;
