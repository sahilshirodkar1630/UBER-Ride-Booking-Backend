INSERT INTO app_user (name, email, password) VALUES
('Aarav Sharma', 'aarav@gmail.com', 'Password@123'),
('Sanya Kapoor', 'sanya.kapoor@email.com', 'SecurePass!456'),
('Vikram Mehta', 'vikram.mehta@email.com', 'Vikram@789'),
('Riya Verma', 'riya.verma@email.com', 'RiyaPass#321'),
('Aditya Malhotra', 'aditya.malhotra@email.com', 'Aditya$456'),
('Neha Bansal', 'neha.bansal@email.com', 'Neha@987'),
('Rohan Khanna', 'rohan.khanna@email.com', 'RohanPass!123'),
('Pooja Iyer', 'pooja.iyer@email.com', 'Pooja#456'),
('Kabir Nair', 'kabir.nair@email.com', 'Kabir!789'),
('Ananya Joshi', 'ananya.joshi@email.com', 'Ananya$234');


INSERT INTO user_roles (user_id, roles) VALUES
(1, 'RIDER'),
(2, 'DRIVER'),
(2, 'RIDER'),
(3, 'RIDER'),
(4, 'DRIVER'),
(5, 'RIDER'),
(6, 'RIDER'),
(7, 'DRIVER'),
(7, 'RIDER'),
(8, 'RIDER'),
(9, 'DRIVER'),
(10, 'RIDER');

insert into rider (id,user_id,rating) values
(1,1,4.9);

INSERT INTO driver (id, user_id, rating,
available, current_location) VALUES
(1, 2, 4.7, true, ST_GeomFromText('POINT(77.1100 28.7000)', 4326)),
(2, 4, 4.5, true, ST_GeomFromText('POINT(77.1200 28.7100)', 4326)),
(3, 7, 4.8, false, ST_GeomFromText('POINT(77.1050 28.6950)', 4326)),
(4, 9, 4.6, true, ST_GeomFromText('POINT(77.1150 28.7050)', 4326));

INSERT INTO wallet(id,user_id,balance) VALUES
(1,1,100),
(2,4,500);
